package org.alien4cloud.rmsscheduler.utils;

import alien4cloud.utils.PropertyUtil;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.alien4cloud.rmsscheduler.model.timeline.TimelineAction;
import org.alien4cloud.rmsscheduler.model.timeline.TimelineActionState;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.drools.core.time.TimeUtils;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.io.ResourceFactory;

import java.io.IOException;
import java.util.Scanner;

public class KieUtils {

    public static String loadResource(String path) throws IOException {
        Resource resource = ResourceFactory.newClassPathResource(path);
        Scanner sc = new Scanner(resource.getInputStream());
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) {
            sb.append(sc.nextLine());
            sb.append("\r\n");
        }
        return sb.toString();
    }

    public static void updateTimelineAction(SessionHandler session, TimelineAction timelineAction, FactHandle factHandle, TimelineActionState state) {
        session.lock();
        try {
            timelineAction.setState(state);
            session.getSession().update(factHandle, timelineAction);
        } finally {
            session.unlock();
        }
    }

    public static Rule buildRuleFromPolicy(String environmentId, String deploymentId, PolicyTemplate policy) {
        Rule rule = new Rule();
        rule.setId(environmentId + "_" + policy.getName());
        rule.setName(policy.getName());
        rule.setEnvironmentId(environmentId);
        rule.setDeploymentId(deploymentId);
        rule.setTimerType(PropertyUtil.getScalarValue(policy.getProperties().get("timer_type")));
        rule.setTimerExpression(PropertyUtil.getScalarValue(policy.getProperties().get("cron_expression")));
        rule.setRetryOnError(Boolean.valueOf(PropertyUtil.getScalarValue(policy.getProperties().get("retry_on_error"))));
        rule.setOnlyOneRunning(Boolean.valueOf(PropertyUtil.getScalarValue(policy.getProperties().get("only_one_running"))));
        rule.setLoop(Boolean.valueOf(PropertyUtil.getScalarValue(policy.getProperties().get("loop"))));
        String duration = PropertyUtil.getScalarValue(policy.getProperties().get("duration"));
        if (duration != null) {
            rule.setDuration(TimeUtils.parseTimeString(duration));
        }
        String delay = PropertyUtil.getScalarValue(policy.getProperties().get("delay"));
        if (delay != null) {
            rule.setDelay(TimeUtils.parseTimeString(delay));
        }
        String max_run = PropertyUtil.getScalarValue(policy.getProperties().get("max_run"));
        if (max_run != null) {
            rule.setMaxRun(Integer.parseInt(max_run));
        }
        rule.setCancelOnTimeout(Boolean.valueOf(PropertyUtil.getScalarValue(policy.getProperties().get("cancel_on_timeout"))));
        rule.setAction(PropertyUtil.getScalarValue(policy.getProperties().get("workflow_name")));
        ListPropertyValue conditions = (ListPropertyValue)policy.getProperties().get("conditions");
        String[] conditionsArr = new String[0];
        if (conditions != null && conditions.getValue() != null && !conditions.getValue().isEmpty()) {
            conditionsArr = new String[conditions.getValue().size()];
            int i = 0;
            for (Object o : conditions.getValue()) {
                String r = o.toString();
                conditionsArr[i++] = r;
            }
        }
        rule.setConditions(conditionsArr);
        return rule;
    }

}
