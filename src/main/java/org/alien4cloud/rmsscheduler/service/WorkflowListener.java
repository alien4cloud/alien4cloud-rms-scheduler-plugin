package org.alien4cloud.rmsscheduler.service;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.paas.IPaasEventListener;
import alien4cloud.paas.IPaasEventService;
import alien4cloud.paas.model.*;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.RMSDao;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.alien4cloud.rmsscheduler.model.timeline.TimelineAction;
import org.alien4cloud.rmsscheduler.model.timeline.TimelineActionState;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

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

    @Resource
    private KieSessionManager kieSessionManager;

    @Resource
    private RMSDao rmsDao;

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

    private void handleEvent(AbstractPaaSWorkflowMonitorEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("AbstractPaaSWorkflowMonitorEvent received for executionId {}: {}", event.getExecutionId(), event.getClass().getSimpleName());
        }
        if (event instanceof PaaSWorkflowStartedEvent) {
            if (event.getWorkflowId().equals(NormativeWorkflowNameConstants.UNINSTALL)) {
                // TODO: stop the KIE session
            } else {
                // TODO: make rule trigger has HANDLED ?
            }
        } else if (event instanceof PaaSWorkflowSucceededEvent) {
            if (event.getWorkflowId().equals(NormativeWorkflowNameConstants.INSTALL)) {
                kieSessionManager.prepareKieSession(event.getDeploymentId());
            } else {
                updateFact(event, TimelineActionState.DONE);
            }
        } else if (event instanceof PaaSWorkflowFailedEvent) {
            if (event.getWorkflowId().equals(NormativeWorkflowNameConstants.INSTALL)) {
                //ruleDao.deleteHandledRules(event.getDeploymentId());
            } else {
                updateFact(event, TimelineActionState.ERROR);
            }
        } else if (event instanceof PaaSWorkflowCancelledEvent) {
            if (event.getWorkflowId().equals(NormativeWorkflowNameConstants.INSTALL)) {
                //ruleDao.deleteHandledRules(event.getDeploymentId());
            } else {
                // we drop the rule (no retry, no loop)
                updateFact(event, TimelineActionState.CANCELLED);
            }
        }
    }

    /**
     * If a KIE session is related to this deployment, find the trigger related to the execution and update its status.
     */
    private void updateFact(AbstractPaaSWorkflowMonitorEvent event, TimelineActionState state) {
        SessionHandler sessionHandler = sessionDao.get(event.getDeploymentId());
        if (sessionHandler == null) {
            // if there is no RMS rules in this deployment, there is no KIE session
            log.debug("No session found for deploymentId {}", event.getDeploymentId());
            return;
        }
        sessionHandler.lock();
        try {
            // Find the trigger related to this execution
            Collection<FactHandle> factHandles = sessionHandler.getSession().getFactHandles(o ->
                    o instanceof TimelineAction && event.getExecutionId().equals(((TimelineAction) o).getExecutionId())
            );
            if (factHandles.isEmpty()) {
                log.debug("No handler found for executionId {}", event.getExecutionId());
                // in some circumstances, we need to update TimelineAction directly
                Map<String, String[]> filter = Maps.newHashMap();
                filter.put("executionId", new String[]{event.getExecutionId()});
                GetMultipleDataResult<TimelineAction> timelineActionResult = rmsDao.find(TimelineAction.class, filter, 10000);
                for (TimelineAction timelineAction : timelineActionResult.getData()) {
                    timelineAction.setEndTime(new Date(event.getDate()));
                    timelineAction.setState(state);
                    rmsDao.save(timelineAction);
                }
                return;
            }
            factHandles.forEach(factHandle -> {
                TimelineAction ruleTrigger = (TimelineAction) sessionHandler.getSession().getObject(factHandle);
                KieUtils.updateTimelineAction(sessionHandler, ruleTrigger, factHandle, state);
            });
        } finally {
            sessionHandler.unlock();
        }
    }

}
