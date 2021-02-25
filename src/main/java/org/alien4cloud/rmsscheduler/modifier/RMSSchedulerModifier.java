package org.alien4cloud.rmsscheduler.modifier;

import alien4cloud.paas.wf.validation.WorkflowValidator;
import alien4cloud.tosca.context.ToscaContextual;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.TopologyModifierSupport;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.alien4cloud.rmsscheduler.service.RuleGenerator;
import org.alien4cloud.rmsscheduler.utils.Const;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * This modifier is in charge of detecting policies of type RMS, validate them and prepare rules for executions.
 */
@Slf4j
@Component(Const.RMS_SCHEDULER_MODIFIER)
public class RMSSchedulerModifier extends TopologyModifierSupport {

    @Resource
    private RuleGenerator ruleGenerator;

    @Override
    @ToscaContextual
    public void process(Topology topology, FlowExecutionContext context) {
        log.info("Processing topology " + topology.getId());

        try {
            WorkflowValidator.disableValidationThreadLocal.set(true);
            doProcess(topology, context);
        } catch (Exception e) {
            context.getLog().error("Couldn't process " + Const.RMS_SCHEDULER_MODIFIER);
            log.warn("Couldn't process " + Const.RMS_SCHEDULER_MODIFIER, e);
        } finally {
            WorkflowValidator.disableValidationThreadLocal.remove();
        }
    }

    private void doProcess(Topology topology, FlowExecutionContext context) {
        Set<PolicyTemplate> policies = TopologyNavigationUtil.getPoliciesOfType(topology, Const.POLICY_TYPE, true);

        Set<Rule> ruleSet = Sets.newHashSet();
        String environmentId = context.getEnvironmentContext().get().getEnvironment().getId();

        for (PolicyTemplate policy : policies) {
            Rule rule = KieUtils.buildRuleFromPölicy(environmentId, null, policy);
            log.debug("Rule created: {}", rule);
            if (ruleGenerator.verify(policy.getName(), rule, context.getLog())) {
                context.getLog().info("Rule prepared for policy " + policy.getName());
            } else {
                context.getLog().error("Invalid rule for policy " + policy.getName());
                return;
            }
            ruleSet.add(rule);
        }
        //ruleDao.create(environmentId, ruleSet);

    }
}
