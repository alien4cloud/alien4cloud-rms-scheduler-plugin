package org.alien4cloud.rmsscheduler.service;

import alien4cloud.deployment.DeploymentService;
import alien4cloud.deployment.WorkflowExecutionService;
import alien4cloud.events.AlienEvent;
import alien4cloud.events.DeploymentCreatedEvent;
import alien4cloud.events.DeploymentUndeployedEvent;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.rest.application.model.LaunchWorkflowRequest;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.RuleDao;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.alien4cloud.rmsscheduler.model.TickTocker;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.apache.lucene.util.NamedThreadFactory;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.utils.KieHelper;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * In charge of :
 * <ul>
 *     <li>Creating and managing KIE sessions (1 per deployment).</li>
 *     <li>React to fact changes.</li>
 *     <li>Regularly fire rules onto KIE sessions.</li>
 * </ul>
 */
@Service
@Slf4j
public class KieSessionManager extends DefaultRuleRuntimeEventListener implements ApplicationListener<AlienEvent> {

    private String ruleCompileDrl;
    private String ruleCompileDsl;

    @Resource
    private RuleGenerator ruleGenerator;

    @Resource
    private SessionDao sessionDao;

    @Inject
    private DeploymentService deploymentService;

    @Resource
    private RuleDao ruleDao;

    @Inject
    private WorkflowExecutionService workflowExecutionService;

    /**
     * Executor service in charge of regularly fire rules for existing sessions.
     */
    private ScheduledExecutorService schedulerService;

    @PostConstruct
    public void init() throws IOException {
        this.ruleCompileDrl = KieUtils.loadResource("rules/schedule-workflow-main.drl");
        this.ruleCompileDsl = KieUtils.loadResource("rules/drools-poc.dsl");
        this.recoverKieSessions();

        this.schedulerService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("rms-rules-scheduler"));

        this.schedulerService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                log.trace("Iterating over sessions in order to fire all rules");
                sessionDao.list().forEach(sessionHandler -> {
                    KieSession kieSession = sessionHandler.getSession();
                    // Refresh the ticktocker
                    TickTocker tickTocker = (TickTocker)kieSession.getObject(sessionHandler.getTicktockerHandler());
                    tickTocker.setNow(new Date());
                    kieSession.update(sessionHandler.getTicktockerHandler(), tickTocker);
                    // Fire rules
                    kieSession.fireAllRules();
                });
            }
        },0, 5, TimeUnit.SECONDS);
        // TODO: this period should be configurable
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
        });
    }

    // should manage n rule per session (1 session = 1 deployment, n policies)
    private void initKieSession(String deploymentId, Collection<Rule> rules) {

        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(ruleCompileDrl, ResourceType.DRL);
        // We need to name the DSL since it's referenced in DSLR
        kieHelper.addContent(ruleCompileDsl, "drools-poc.dsl");

        for (Rule rule : rules) {
            String ruleText = ruleGenerator.generateRule(rule);
            log.info(ruleText);
            kieHelper.addContent(ruleText, ResourceType.DSLR);
        }

        Results results = kieHelper.verify();
        log.info("Rule generation result" + results);
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
            if (r.getStatus() == RuleTriggerStatus.TRIGGERED) {
                onRuleTriggered(r, event.getFactHandle());
            }
        }
    }

    private void onRuleTriggered(RuleTrigger ruleTrigger, FactHandle factHandle) {
        SessionHandler sessionHandler = sessionDao.get(ruleTrigger.getDeploymentId());
        if (sessionHandler == null) {
            log.warn("No session found for {}", ruleTrigger);
            return;
        }

        LaunchWorkflowRequest request = new LaunchWorkflowRequest();
        Map<String, Object> params = Maps.newHashMap();

        log.info("Launching worflow for {}", ruleTrigger);
        try {
            workflowExecutionService.launchWorkflow(request, ruleTrigger.getEnvironmentId(), ruleTrigger.getAction(), params,
                    new IPaaSCallback<String>() {
                        @Override
                        public void onSuccess(String executionId) {
                            ruleTrigger.setExecutionId(executionId);
                            KieUtils.updateRuleTrigger(sessionHandler.getSession(), ruleTrigger, factHandle, RuleTriggerStatus.RUNNING);
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            KieUtils.updateRuleTrigger(sessionHandler.getSession(), ruleTrigger, factHandle, RuleTriggerStatus.ERROR);
                        }
                    });
        } catch (Exception e) {
            KieUtils.updateRuleTrigger(sessionHandler.getSession(), ruleTrigger, factHandle, RuleTriggerStatus.ERROR);
        }
    }

    @Override
    public void objectDeleted(ObjectDeletedEvent event) {
        super.objectDeleted(event);
        Object o = event.getOldObject();
        log.info("Object deleted in rule {}: {}", (event.getRule() == null) ? "Unknown" : event.getRule().getName(), o.toString());
        if (o instanceof RuleTrigger) {
            // TODO: delete ruleTrigger in DAO ?
        }
    }

    public synchronized void onApplicationEvent(AlienEvent alienEvent) {
        if (alienEvent instanceof DeploymentCreatedEvent) {
            onDeploymentCreatedEvent((DeploymentCreatedEvent)alienEvent);
        } else if (alienEvent instanceof DeploymentUndeployedEvent) {
            onDeploymentUndeployedEvent((DeploymentUndeployedEvent)alienEvent);
        }
    }

    private void onDeploymentCreatedEvent(DeploymentCreatedEvent deploymentCreatedEvent) {
        log.info("Deployment created {}", deploymentCreatedEvent.getDeploymentId());
        Deployment deployment = deploymentService.get(deploymentCreatedEvent.getDeploymentId());
        Collection<Rule> rules = this.ruleDao.handleRules(deployment.getEnvironmentId(), deployment.getId());
        if (!rules.isEmpty()) {
            initKieSession(deploymentCreatedEvent.getDeploymentId(), rules);
        }
    }

    private void onDeploymentUndeployedEvent(DeploymentUndeployedEvent deploymentUndeployedEvent) {
        log.info("Deployment ends {}", deploymentUndeployedEvent.getDeploymentId());
        SessionHandler sessionHandler = sessionDao.get(deploymentUndeployedEvent.getDeploymentId());
        if (sessionHandler != null) {
            sessionHandler.getSession().halt();
            sessionHandler.getSession().dispose();
            sessionDao.delete(sessionHandler);
            Deployment deployment = deploymentService.get(deploymentUndeployedEvent.getDeploymentId());
            ruleDao.deleteHandledRules(deployment.getEnvironmentId());
        }
    }

}
