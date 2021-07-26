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
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.alien4cloud.rmsscheduler.model.TickTocker;
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
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
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
                fireAllSessionsRules();
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
            kieSession.fireAllRules();
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


        KieBase kieBase = kieHelper.build(EventProcessingOption.STREAM);
        KieSession kieSession = kieBase.newKieSession();
        kieSession.addEventListener(this);

        SessionHandler sessionHandler = new SessionHandler();
        sessionHandler.setId(deploymentId);
        sessionHandler.setSession(kieSession);
        sessionHandler.setRules(rulesMap);
        sessionHandler.setTicktockerHandler(kieSession.insert(new TickTocker()));
        sessionDao.create(sessionHandler);
    }

    @Override
    public void objectInserted(ObjectInsertedEvent event) {
        super.objectInserted(event);
        Object o = event.getObject();
        if (log.isTraceEnabled()) {
            log.trace("Object inserted in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
        }
        if (o instanceof RuleTrigger && log.isDebugEnabled()) {
            log.debug("RuleTrigger inserted in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
        }
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
            SessionHandler sessionHandler = sessionDao.get(r.getDeploymentId());
            if (sessionHandler == null) {
                log.debug("No session found for {}", r);
                return;
            }

            if (r.getStatus() == RuleTriggerStatus.TRIGGERED) {
                log.info("Launching worflow for {}", r);
                launchWorkflowAction.execute(r, sessionHandler, event.getFactHandle());
            } else if (r.getStatus() == RuleTriggerStatus.TIMEOUT) {
                // Cancel running execution only if option cancel_on_timeout is set
                Rule rule = sessionHandler.getRules().get(r.getRuleId());
                log.debug("Rule found: {}", rule);
                if (rule.isCancelOnTimeout()) {
                    log.info("Cancel execution {}", r.getExecutionId());
                    cancelWorkflowAction.execute(r, sessionHandler, event.getFactHandle());
                }
            }
        }
    }

    @Override
    public void objectDeleted(ObjectDeletedEvent event) {
        super.objectDeleted(event);
        Object o = event.getOldObject();
        log.debug("Object deleted in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
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
                    ruleSet.add(KieUtils.buildRuleFromPolicy(deployment.getEnvironmentId(), deploymentId, policy));
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
