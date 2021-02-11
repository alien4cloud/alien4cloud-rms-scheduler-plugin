package org.alien4cloud.rmsscheduler.modifier;

import alien4cloud.paas.wf.validation.WorkflowValidator;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.utils.PropertyUtil;
import com.google.common.collect.Sets;
import lombok.extern.java.Log;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.TopologyModifierSupport;
import org.alien4cloud.rmsscheduler.utils.Const;
import org.alien4cloud.rmsscheduler.dao.RuleDao;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.alien4cloud.rmsscheduler.service.RuleValidator;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.logging.Level;

/**
 * This modifier is in charge of detecting policies of type RMS, validate them and prepare rules for executions.
 */
@Log
@Component(Const.RMS_SCHEDULER_MODIFIER)
public class RMSSchedulerModifier extends TopologyModifierSupport {

    @Resource
    private RuleValidator ruleValidator;

    @Resource
    private RuleDao ruleDao;

    @Override
    @ToscaContextual
    public void process(Topology topology, FlowExecutionContext context) {
        log.info("Processing topology " + topology.getId());

        try {
            WorkflowValidator.disableValidationThreadLocal.set(true);
            doProcess(topology, context);
        } catch (Exception e) {
            context.getLog().error("Couldn't process " + Const.RMS_SCHEDULER_MODIFIER);
            log.log(Level.WARNING, "Couldn't process " + Const.RMS_SCHEDULER_MODIFIER, e);
        } finally {
            WorkflowValidator.disableValidationThreadLocal.remove();
        }
    }

    private void doProcess(Topology topology, FlowExecutionContext context) {
        Set<PolicyTemplate> policies = TopologyNavigationUtil.getPoliciesOfType(topology, Const.POLICY_TYPE, true);

        Set<Rule> ruleSet = Sets.newHashSet();
        String environmentId = context.getEnvironmentContext().get().getEnvironment().getId();

        for (PolicyTemplate policy : policies) {
            Rule rule = new Rule();
            rule.setId(environmentId + "_" + policy.getName());
            rule.setEnvironmentId(environmentId);
            rule.setTimerType(PropertyUtil.getScalarValue(policy.getProperties().get("timer_type")));
            rule.setTimerExpression(PropertyUtil.getScalarValue(policy.getProperties().get("cron_expression")));
            rule.setRetryOnError(Boolean.valueOf(PropertyUtil.getScalarValue(policy.getProperties().get("retry_on_error"))));
            rule.setOnlyOneRunning(Boolean.valueOf(PropertyUtil.getScalarValue(policy.getProperties().get("only_one_running"))));
            rule.setLoop(Boolean.valueOf(PropertyUtil.getScalarValue(policy.getProperties().get("loop"))));
            rule.setDuration(PropertyUtil.getScalarValue(policy.getProperties().get("duration")));
            rule.setRescheduleDelay(PropertyUtil.getScalarValue(policy.getProperties().get("reschedule_delay")));
            rule.setMaxRun(Integer.parseInt(PropertyUtil.getScalarValue(policy.getProperties().get("max_run"))));
            rule.setAction(PropertyUtil.getScalarValue(policy.getProperties().get("workflow_name")));
            ListPropertyValue conditions = (ListPropertyValue)policy.getProperties().get("conditions");
            final StringBuilder conditionsBuilder = new StringBuilder();
            if (conditions != null && conditions.getValue() != null && !conditions.getValue().isEmpty()) {
                conditions.getValue().forEach(o -> {
                    String r = o.toString();
                    if (ruleValidator.verify(r)) {
                        context.getLog().info("Accepted rule condition: " + r);
                    } else {
                        // TODO: better context log when errors
                        context.getLog().error("Unparsable rule condition statement : " + r);
                    }
                    conditionsBuilder.append("\r\n").append(r);
                });
            }
            rule.setConditions(conditionsBuilder.toString());
            context.getLog().info("Rule prepared for policy " + policy.getName());

            ruleSet.add(rule);
            ruleDao.create(environmentId, ruleSet);
        }
    }
}
