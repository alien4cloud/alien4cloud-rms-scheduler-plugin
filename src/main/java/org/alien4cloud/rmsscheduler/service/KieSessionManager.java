package org.alien4cloud.rmsscheduler.service;

import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.events.AlienEvent;
import alien4cloud.events.BeforeDeploymentUndeployedEvent;
import alien4cloud.events.DeploymentRecoveredEvent;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.tosca.context.ToscaContext;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.RMSPluginConfiguration;
import org.alien4cloud.rmsscheduler.dao.RMSDao;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.*;
import org.alien4cloud.rmsscheduler.model.timeline.*;
import org.alien4cloud.rmsscheduler.service.actions.CancelWorkflowAction;
import org.alien4cloud.rmsscheduler.service.actions.LaunchWorkflowAction;
import org.alien4cloud.rmsscheduler.utils.Const;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;
import org.apache.lucene.util.NamedThreadFactory;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.event.rule.*;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.utils.KieHelper;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In charge of :
 * <ul>
 *     <li>Creating and managing KIE sessions (1 per deployment).</li>
 *     <li>React to fact changes, and eventually do something (launch wf, cancel execution ...).</li>
 *     <li>Regularly fire rules onto KIE sessions (heartbeat).</li>
 * </ul>
 */
@Service
@Slf4j
public class KieSessionManager extends DefaultRuleRuntimeEventListener implements ApplicationListener<AlienEvent> {

    /**
     * Executor service in charge of regularly fire rules for existing sessions.
     */
    private ScheduledExecutorService schedulerService;

    @Resource
    private RuleGenerator ruleGenerator;

    @Resource
    private SessionDao sessionDao;

    @Resource
    private RMSDao rmsDao;

    @Inject
    private DeploymentService deploymentService;

    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;

    @Resource
    private RMSPluginConfiguration pluginConfiguration;

    @Inject
    private CancelWorkflowAction cancelWorkflowAction;
    @Inject
    private LaunchWorkflowAction launchWorkflowAction;

    @PostConstruct
    public void init() {
        // If this plugin is started after orchestrator plugin, we must init session with active deployments
        this.recoverKieSessions();
        this.schedulerService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("rms-rules-scheduler"));

        log.info("Fire rule heartbeat period : {}ms", pluginConfiguration.getHeartbeatPeriod());
        // TODO: use quartz scheduler
        this.schedulerService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    fireAllSessionsRules();
                } catch(Exception e) {
                    log.error("Something wrong occured while heartbeating sessions", e);
                }
            }
        },0, pluginConfiguration.getHeartbeatPeriod(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        log.info("Context is shutting down, stop KIE sessions heartbeat");
        this.schedulerService.shutdown();
    }

    //@Scheduled(cron = "${" + RMSPluginConfiguration.CONFIG_PREFIX + ".heartbeatCron:" + RMSPluginConfiguration.DEFAULT_HEARTBEAT_CRON + "}")
    public void fireAllSessionsRules() {
        log.trace("Iterating over sessions in order to fire all rules");
        Calendar now = Calendar.getInstance();
        // our precision is seconds (cron), so let's set ms to 0
        //now.set(Calendar.MILLISECOND, 0);

        sessionDao.list().forEach(sessionHandler -> {
            KieSession kieSession = sessionHandler.getSession();
            // Refresh the ticktocker
            TickTocker tickTocker = (TickTocker)kieSession.getObject(sessionHandler.getTicktockerHandler());
            tickTocker.setNow(now.getTime());
            kieSession.update(sessionHandler.getTicktockerHandler(), tickTocker);
            // Fire rules
            try {
                long startTime = System.currentTimeMillis();
                kieSession.fireAllRules();
                log.trace("Took {}ms to fire all rules for session {}", System.currentTimeMillis() - startTime, sessionHandler.getId());
            } catch(Exception e) {
                log.error("Error while firing rules for session " + sessionHandler.getId(), e);
            }
        });
    }

    /**
     * This will be called at init. If the orchestrator plugin starts first, we will init sessions. Else, the sessions will
     * be initialized by {@link #onDeploymentRecoveredEvent(DeploymentRecoveredEvent)}.
     */
    private void recoverKieSessions() {
        Deployment[] activeDeployments = this.deploymentService.getActiveDeployments();
        log.info("Recovering {} deployments", activeDeployments.length);
        for (Deployment deployment : activeDeployments) {
            try {
                deploymentRuntimeStateService.getDeploymentStatus(deployment, new IPaaSCallback<DeploymentStatus>() {
                    @Override
                    public void onSuccess(DeploymentStatus data) {
                        log.info("Deployment {} status is {}", deployment.getId(), data);
                        if (data == DeploymentStatus.DEPLOYED) {
                            prepareKieSession(deployment.getId());
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {

                    }
                });
            } catch(OrchestratorDisabledException e) {
                log.warn("Plugin is not yet enabled, let's init sessions by listening for DeploymentRecoveredEvent");
            }
        }
    }

    // should manage n rule per session (1 session = 1 deployment, n policies)
    private synchronized void initKieSession(String deploymentId, Collection<Rule> rules) {

        if (sessionDao.get(deploymentId) != null) {
            log.warn("Session already exist for deployment {}", deploymentId);
            return;
        }
        KieHelper kieHelper = ruleGenerator.buildKieHelper(rules);

        Results results = kieHelper.verify();
        log.debug("Rule generation result" + results);
        if (results.hasMessages(Message.Level.ERROR)) {
            log.warn("Rule generation error !");
        }
        Map<String, Rule> rulesMap = Maps.newHashMap();
        rules.forEach(rule -> rulesMap.put(rule.getId(), rule));

        try {
            KieBase kieBase = kieHelper.build(EventProcessingOption.STREAM);
            KieSession kieSession = kieBase.newKieSession();
            kieSession.addEventListener(this);

            SessionHandler sessionHandler = new SessionHandler();
            sessionHandler.setId(deploymentId);
            sessionHandler.setSession(kieSession);
            sessionHandler.setRules(rulesMap);
            sessionHandler.setTicktockerHandler(kieSession.insert(new TickTocker()));
            sessionDao.create(sessionHandler);
        } catch(Exception e) {
            log.error("Something went wrong when preparing KIE session for deployment " + deploymentId, e);
        }
    }

    @Override
    public void objectInserted(ObjectInsertedEvent event) {
        super.objectInserted(event);
        Object o = event.getObject();

        if (log.isTraceEnabled()) {
            log.trace("Object inserted in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
        }
        if (o instanceof RuleTrigger) {
            if (log.isDebugEnabled()) {
                log.debug("RuleTrigger inserted in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
            }
            RuleTrigger rt = (RuleTrigger)o;

            TimelineWindow timelineEvent = new TimelineWindow();
            timelineEvent.setId(rt.getId());
            timelineEvent.setDeploymentId(rt.getDeploymentId());
            timelineEvent.setRuleId(rt.getRuleId());
            timelineEvent.setStartTime(rt.getScheduleTime());
            timelineEvent.setEndTime(rt.getExpirationTime());
            this.rmsDao.save(timelineEvent);

            Date now = getDateFromSession(event);

            TriggerEvent triggerEvent = new TriggerEvent();
            triggerEvent.setStatus(rt.getStatus());
            triggerEvent.setDeploymentId(rt.getDeploymentId());
            triggerEvent.setRuleId(rt.getRuleId());
            triggerEvent.setStartTime(now);
            triggerEvent.setTriggerId(rt.getId());
            this.rmsDao.save(triggerEvent);
        }
        if (o instanceof TimelineRuleConditionEvent) {
            if (log.isDebugEnabled()) {
                log.debug("TimelineRuleConditionEvent fired in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
            }
            TimelineRuleConditionEvent rte = (TimelineRuleConditionEvent)o;
            this.rmsDao.save(rte);
        }
    }

    private Date getDateFromSession(RuleRuntimeEvent event) {
        Date now = null;
        Optional<?> tickTocker = event.getKieRuntime().getObjects(h -> h instanceof TickTocker).stream().findFirst();
        if (tickTocker.isPresent()) {
            TickTocker tocker = (TickTocker)tickTocker.get();
            now = tocker.getNow();
        } else {
            // Fail over using current date (should never occur)
            log.debug("Not able to retreive date from event session");
            now = new Date();
        }
        return now;
    }

    @Override
    public void objectUpdated(ObjectUpdatedEvent event) {
        super.objectUpdated(event);
        Object o = event.getObject();
        if (log.isTraceEnabled()) {
            log.trace("Object updated in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
        }
        if (o instanceof RuleTrigger) {
            log.debug("RuleTrigger updated in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
            final RuleTrigger r = (RuleTrigger)o;
            final String executionId = r.getExecutionId();
            SessionHandler sessionHandler = sessionDao.get(r.getDeploymentId());
            if (sessionHandler == null) {
                log.warn("No session found for {}", r);
                return;
            }

            Date now = getDateFromSession(event);


            TriggerEvent triggerEvent = new TriggerEvent();
            triggerEvent.setStatus(r.getStatus());
            triggerEvent.setDeploymentId(r.getDeploymentId());
            triggerEvent.setRuleId(r.getRuleId());
            // how to take time from session ?
            triggerEvent.setStartTime(now);
            triggerEvent.setTriggerId(r.getId());
            this.rmsDao.save(triggerEvent);

            switch(r.getStatus()) {
                case SCHEDULED:
                    //rte.setState(RuleTriggerEventState.SCHEDULED);
                    break;
                case TRIGGERED:
                    log.debug("Launching worflow for {}", r);
                    launchWorkflowAction.execute(r, sessionHandler, event.getFactHandle());
                    break;
                case RUNNING:
                    // Start of the TimelineAction
                    TimelineAction timelineAction = new TimelineAction();
                    timelineAction.setId(executionId);
                    timelineAction.setDeploymentId(r.getDeploymentId());
                    timelineAction.setRuleId(r.getRuleId());
                    timelineAction.setExecutionId(executionId);
                    timelineAction.setStartTime(now);
                    timelineAction.setState(TimelineActionState.RUNNING);
                    timelineAction.setName(r.getAction());
                    this.rmsDao.save(timelineAction);
                    break;
                case DONE:
                    // End of the TimelineAction
                    timelineAction = this.rmsDao.findById(TimelineAction.class, executionId);
                    if (timelineAction != null) {
                        timelineAction.setEndTime(now);
                        timelineAction.setState(TimelineActionState.DONE);
                        this.rmsDao.save(timelineAction);
                    } else {
                        // TODO Warn
                        log.debug("Not able to find TimelineAction for execution {} to set it to {}", executionId, TimelineActionState.DONE);
                    }
                    break;
                case ERROR:
                    //rte.setState(RuleTriggerEventState.ERROR);
                    // End of the TimelineAction
                    if (executionId != null) {
                        // error can occr
                        timelineAction = this.rmsDao.findById(TimelineAction.class, executionId);
                        if (timelineAction != null) {
                            timelineAction.setEndTime(now);
                            timelineAction.setState(TimelineActionState.ERROR);
                            this.rmsDao.save(timelineAction);
                        } else {
                            // TODO Warn
                            log.debug("Not able to find TimelineAction for execution {} to set it to {}", executionId, TimelineActionState.ERROR);
                        }
                    }
                    break;
                case TIMEOUT:
                    // Cancel running execution only if option cancel_on_timeout is set
                    Rule rule = sessionHandler.getRules().get(r.getRuleId());
                    log.debug("Rule found: {}", rule);
                    if (rule.isCancelOnTimeout()) {
                        log.debug("Cancel execution {}", executionId);
                        cancelWorkflowAction.execute(r, sessionHandler, event.getFactHandle());
                    }
                    break;
                case CANCELLED:
                    // Cancel running execution only if option cancel_on_timeout is set
                    timelineAction = this.rmsDao.findById(TimelineAction.class, executionId);
                    // End of the TimelineAction
                    if (timelineAction != null) {
                        timelineAction.setEndTime(now);
                        timelineAction.setState(TimelineActionState.CANCELLED);
                        this.rmsDao.save(timelineAction);
                    } else {
                        // TODO Warn
                        log.debug("Not able to find TimelineAction for execution {} to set it to {}", executionId, TimelineActionState.CANCELLED);
                    }
                    break;
                case DROPPED:
                    break;
            }
        }
    }

    @Override
    public void objectDeleted(ObjectDeletedEvent event) {
        super.objectDeleted(event);
        Object o = event.getOldObject();
        if (log.isDebugEnabled()) {
            log.debug("Object deleted in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
        }
        if (o instanceof RuleTrigger) {

            Date now = getDateFromSession(event);

            RuleTrigger r = (RuleTrigger)o;
            TriggerEvent triggerEvent = new TriggerEvent();
            triggerEvent.setStatus(RuleTriggerStatus.DELETED);
            triggerEvent.setDeploymentId(r.getDeploymentId());
            triggerEvent.setRuleId(r.getRuleId());
            // how to take time from session ?
            triggerEvent.setStartTime(now);
            triggerEvent.setTriggerId(r.getId());
            this.rmsDao.save(triggerEvent);

            TimelineWindow timelineWindow = this.rmsDao.findById(TimelineWindow.class, r.getId());
            // End of the TimelineAction
            if (timelineWindow != null) {
                timelineWindow.setEndTime(now);
                // Distinguish deleted from others
                timelineWindow.setObsolete(true);
                this.rmsDao.save(timelineWindow);
            } else {
                // TODO Warn
            }

        }
    }

    public synchronized void onApplicationEvent(AlienEvent alienEvent) {
        log.debug("AlienEvent of type {} occured : {}", alienEvent.getClass(), alienEvent);
        if (alienEvent instanceof BeforeDeploymentUndeployedEvent) {
            onDeploymentUndeployedEvent((BeforeDeploymentUndeployedEvent)alienEvent);
        } else if (alienEvent instanceof DeploymentRecoveredEvent) {
            onDeploymentRecoveredEvent((DeploymentRecoveredEvent)alienEvent);
        }
    }

    public synchronized void prepareKieSession(String deploymentId) {
        Deployment deployment = deploymentService.get(deploymentId);
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        ToscaContext.init(deploymentTopology.getDependencies());
        try {
            Set<PolicyTemplate> policies = TopologyNavigationUtil.getPoliciesOfType(deploymentTopology, Const.POLICY_TYPE, true);
            if (!policies.isEmpty()) {
                Set<Rule> ruleSet = Sets.newHashSet();
                for (PolicyTemplate policy : policies) {
                    Rule rule = KieUtils.buildRuleFromPolicy(deployment.getEnvironmentId(), deploymentId, policy);
                    ruleSet.add(rule);
                    this.rmsDao.save(rule);
                }
                initKieSession(deploymentId, ruleSet);
            }
        } finally {
            ToscaContext.destroy();
        }
    }

    /**
     * The deployment has been recovered after system startup, init a KIE session.
     */
    private void onDeploymentRecoveredEvent(DeploymentRecoveredEvent deploymentRecoveredEvent) {
        prepareKieSession(deploymentRecoveredEvent.getDeploymentId());
    }

    // TODO: session should only be paused, then delete after undeployed has been successful
    private void onDeploymentUndeployedEvent(BeforeDeploymentUndeployedEvent deploymentUndeployedEvent) {
        log.debug("Deployment ends {}", deploymentUndeployedEvent.getDeploymentId());
        SessionHandler sessionHandler = sessionDao.get(deploymentUndeployedEvent.getDeploymentId());
        if (sessionHandler != null) {
            sessionHandler.getSession().halt();
            sessionHandler.getSession().dispose();
            sessionDao.delete(sessionHandler);
            //ruleDao.deleteHandledRules(deploymentUndeployedEvent.getDeploymentId());
        }
    }

}
