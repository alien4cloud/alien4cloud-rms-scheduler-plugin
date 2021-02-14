package org.alien4cloud.rmsscheduler.service.actions;

import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.model.PaaSDeploymentContext;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class CancelWorkflowAction implements RuleAction {

    @Resource
    private DeploymentService deploymentService;

    @Resource
    private DeploymentRuntimeStateService deploymentRuntimeStateService;

    @Resource
    private OrchestratorPluginService orchestratorPluginService;

    public void execute(RuleTrigger ruleTrigger, SessionHandler sessionHandler, FactHandle factHandle) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(ruleTrigger.getEnvironmentId());
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        PaaSDeploymentContext context = new PaaSDeploymentContext(deployment,deploymentTopology,null);

        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());

        orchestratorPlugin.cancelTask(context, ruleTrigger.getExecutionId(), new IPaaSCallback<String>() {
            @Override
            public void onSuccess(String data) {
                log.warn("Execution {} cancelled with success", ruleTrigger.getExecutionId());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.warn("Fail to cancel execution", throwable);
            }
        });

    }
}
