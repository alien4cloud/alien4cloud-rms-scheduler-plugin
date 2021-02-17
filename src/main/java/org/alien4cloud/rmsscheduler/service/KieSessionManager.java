package org.alien4cloud.rmsscheduler.service;

import alien4cloud.deployment.DeploymentService;
import alien4cloud.events.AlienEvent;
import alien4cloud.events.BeforeDeploymentUndeployedEvent;
import alien4cloud.events.DeploymentCreatedEvent;
import alien4cloud.model.deployment.Deployment;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.RMSPluginConfiguration;
import org.alien4cloud.rmsscheduler.dao.RuleDao;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.alien4cloud.rmsscheduler.model.TickTocker;
import org.alien4cloud.rmsscheduler.service.actions.CancelWorkflowAction;
import org.alien4cloud.rmsscheduler.service.actions.LaunchWorkflowAction;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Resource
    private RuleDao ruleDao;

    @Resource
    private RMSPluginConfiguration pluginConfiguration;

    @Inject
    private CancelWorkflowAction cancelWorkflowAction;
    @Inject
    private LaunchWorkflowAction launchWorkflowAction;

    @PostConstruct
    public void init() {
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
     * Recovery : get rules from persistence and init sessions.
     */
    private void recoverKieSessions() {
        // TODO: should be done before YorcProvider : changes in Yorc recovery could impact rules
        Collection<Rule> initRules = this.ruleDao.listHandledRules();
        Map<String, List<Rule>> initRulesPerDeployment = initRules.stream()
                .collect(Collectors.groupingBy(Rule::getDeploymentId));
        initRulesPerDeployment.forEach((deploymentId, rules) -> {
            log.info("Init KIE session for deployment {} using {} rules", deploymentId, rules.size());
            initKieSession(deploymentId, rules);
            ruleDao.create(deploymentId, rules);
        });
    }

    // should manage n rule per session (1 session = 1 deployment, n policies)
    private void initKieSession(String deploymentId, Collection<Rule> rules) {

        KieHelper kieHelper = ruleGenerator.buildKieHelper(rules);

        Results results = kieHelper.verify();
        log.debug("Rule generation result" + results);
        if (results.hasMessages(Message.Level.ERROR)) {
            log.warn("Rule generation error !");
        }

        KieBase kieBase = kieHelper.build(EventProcessingOption.STREAM);
        KieSession kieSession = kieBase.newKieSession();
        kieSession.addEventListener(this);

        SessionHandler sessionHandler = new SessionHandler();
        sessionHandler.setId(deploymentId);
        sessionHandler.setSession(kieSession);
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
                Optional<Rule> rule = ruleDao.getHandledRule(r.getRuleId());
                log.debug("Rule found: {}", rule);
                if (rule.isPresent() && rule.get().isCancelOnTimeout()) {
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
        if (alienEvent instanceof DeploymentCreatedEvent) {
            onDeploymentCreatedEvent((DeploymentCreatedEvent)alienEvent);
        } else if (alienEvent instanceof BeforeDeploymentUndeployedEvent) {
            onDeploymentUndeployedEvent((BeforeDeploymentUndeployedEvent)alienEvent);
        }
    }

    /**
     * The deployment has been created, just handle rules prepared by modifier init a KIE session.
     */
    private void onDeploymentCreatedEvent(DeploymentCreatedEvent deploymentCreatedEvent) {
        log.debug("Deployment created {}", deploymentCreatedEvent.getDeploymentId());
        Deployment deployment = deploymentService.get(deploymentCreatedEvent.getDeploymentId());
        Collection<Rule> rules = this.ruleDao.handleRules(deployment.getEnvironmentId(), deployment.getId());
    }

    public void initKieSession(String deploymentId) {
        log.debug("Init KIE session for deployment {}", deploymentId);
        Collection<Rule> rules = this.ruleDao.listHandledRules(deploymentId);
        if (!rules.isEmpty()) {
            initKieSession(deploymentId, rules);
        }
    }

    // TODO: session should only be paused, then delete after undeployed has been successful
    private void onDeploymentUndeployedEvent(BeforeDeploymentUndeployedEvent deploymentUndeployedEvent) {
        log.debug("Deployment ends {}", deploymentUndeployedEvent.getDeploymentId());
        SessionHandler sessionHandler = sessionDao.get(deploymentUndeployedEvent.getDeploymentId());
        if (sessionHandler != null) {
            sessionHandler.getSession().halt();
            sessionHandler.getSession().dispose();
            sessionDao.delete(sessionHandler);
            ruleDao.deleteHandledRules(deploymentUndeployedEvent.getDeploymentId());
        }
    }

}
