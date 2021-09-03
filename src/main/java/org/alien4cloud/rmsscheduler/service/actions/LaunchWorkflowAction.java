package org.alien4cloud.rmsscheduler.service.actions;

import alien4cloud.deployment.WorkflowExecutionService;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.rest.application.model.LaunchWorkflowRequest;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.alien4cloud.rmsscheduler.model.timeline.TimelineAction;
import org.alien4cloud.rmsscheduler.model.timeline.TimelineActionState;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class LaunchWorkflowAction implements RuleAction<RuleTrigger> {

    @Resource
    private WorkflowExecutionService workflowExecutionService;

    @Override
    public void execute(RuleTrigger ruleTrigger, SessionHandler sessionHandler, FactHandle factHandle) {
        LaunchWorkflowRequest request = new LaunchWorkflowRequest();
        Map<String, Object> params = Maps.newHashMap();

        sessionHandler.lock();
        try {
            workflowExecutionService.launchWorkflow(request, ruleTrigger.getEnvironmentId(), ruleTrigger.getAction(), params,
                    new IPaaSCallback<String>() {
                        @Override
                        public void onSuccess(String executionId) {
                            log.debug("Launched workflow for {} executionId: {}", ruleTrigger.getRuleId(), executionId);
                            //ruleTrigger.setExecutionId(executionId);
                            //KieUtils.updateRuleTrigger(sessionHandler, ruleTrigger, factHandle, RuleTriggerStatus.RUNNING);

                            TimelineAction timeLineAction = new TimelineAction();
                            timeLineAction.setId(executionId);
                            timeLineAction.setRuleId(ruleTrigger.getRuleId());
                            timeLineAction.setTriggerId(ruleTrigger.getId());
                            timeLineAction.setState(TimelineActionState.RUNNING);
                            timeLineAction.setStartTime(new Date());
                            timeLineAction.setDeploymentId(ruleTrigger.getDeploymentId());
                            timeLineAction.setExecutionId(executionId);
                            timeLineAction.setName(ruleTrigger.getAction());
                            sessionHandler.lock();
                            try {
                                sessionHandler.getSession().insert(timeLineAction);
                            } finally {
                                sessionHandler.unlock();
                            }

                        }

                        @Override
                        public void onFailure(Throwable e) {
                            //KieUtils.updateRuleTrigger(sessionHandler, ruleTrigger, factHandle, RuleTriggerStatus.ERROR);
                        }
                    });
        } catch (Exception e) {
            //KieUtils.updateRuleTrigger(sessionHandler, ruleTrigger, factHandle, RuleTriggerStatus.ERROR);
        } finally {
            sessionHandler.unlock();
        }
    }
}
