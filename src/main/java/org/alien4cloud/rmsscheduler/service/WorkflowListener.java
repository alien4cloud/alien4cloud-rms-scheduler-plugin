package org.alien4cloud.rmsscheduler.service;

import alien4cloud.paas.IPaasEventListener;
import alien4cloud.paas.IPaasEventService;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.AbstractPaaSWorkflowMonitorEvent;
import alien4cloud.paas.model.PaaSWorkflowFailedEvent;
import alien4cloud.paas.model.PaaSWorkflowSucceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Collection;

/**
 * Listen to workflow change events and fire changes in KIE session if needed.
 */
@Component
@Slf4j
public class WorkflowListener {

    @Inject
    private IPaasEventService eventService;

    @Resource
    private SessionDao sessionDao;

    @PostConstruct
    public void init() {
        eventService.addListener(listener);
    }

    @PreDestroy
    public void term() {
        eventService.removeListener(listener);
    }

    IPaasEventListener listener = new IPaasEventListener() {
        @Override
        public void eventHappened(AbstractMonitorEvent event) {
            handleEvent((AbstractPaaSWorkflowMonitorEvent) event);
        }

        @Override
        public boolean canHandle(AbstractMonitorEvent event) {
            return (event instanceof AbstractPaaSWorkflowMonitorEvent);
        }
    };

    // TODO: Should we manage CANCELLED workflows ?
    private void handleEvent(AbstractPaaSWorkflowMonitorEvent event) {
        if (event instanceof PaaSWorkflowSucceededEvent) {
            updateFact(event, RuleTriggerStatus.SUCCESS);
        } else if (event instanceof PaaSWorkflowFailedEvent) {
            updateFact(event, RuleTriggerStatus.ERROR);
        }
    }

    /**
     * If a KIE session is related to this deployment, find the trigger related to the execution and update its status.
     */
    private void updateFact(AbstractPaaSWorkflowMonitorEvent event, RuleTriggerStatus ruleTriggerStatus) {
        SessionHandler sessionHandler = sessionDao.get(event.getDeploymentId());
        if (sessionHandler == null) {
            // if there is no RMS rules in this deployment, there is no KIE session
            log.debug("No session found for deploymentId {}", event.getDeploymentId());
            return;
        }
        // Find the trigger related to this execution
        Collection<FactHandle> factHandles = sessionHandler.getSession().getFactHandles(o ->
                o instanceof RuleTrigger && event.getExecutionId().equals(((RuleTrigger) o).getExecutionId())
        );
        if (factHandles.isEmpty()) {
            log.debug("No handler found for executionId {}", event.getExecutionId());
            return;
        }
        factHandles.forEach(factHandle -> {
            RuleTrigger ruleTrigger = (RuleTrigger)sessionHandler.getSession().getObject(factHandle);
            KieUtils.updateRuleTrigger(sessionHandler.getSession(), ruleTrigger, factHandle, ruleTriggerStatus);
        });
    }

}
