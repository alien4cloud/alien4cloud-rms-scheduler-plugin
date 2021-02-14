package org.alien4cloud.rmsscheduler.service.actions;

import alien4cloud.deployment.WorkflowExecutionService;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.rest.application.model.LaunchWorkflowRequest;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Service
public class LaunchWorkflowAction implements RuleAction {

    @Resource
    private WorkflowExecutionService workflowExecutionService;

    @Override
    public void execute(RuleTrigger ruleTrigger, SessionHandler sessionHandler, FactHandle factHandle) {
        LaunchWorkflowRequest request = new LaunchWorkflowRequest();
        Map<String, Object> params = Maps.newHashMap();

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
}
