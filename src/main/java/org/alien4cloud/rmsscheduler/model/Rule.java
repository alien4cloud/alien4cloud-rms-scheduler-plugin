package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.Setter;

/**
 * A rule is the Java representation of a Drools rule. It's related to an environment.
 * At deployment preparation stage, a rule is created for each RMSSchedulePolicy instance.
 * At deployment stage a KIE session is created.
 */
@Getter
@Setter
public class Rule {
    private String environmentId;

    /**
     * A rule has a deploymentId at the moment it is handled.
     */
    private String deploymentId;

    /**
     * The name of the policy in the topology.
     * environmentId + policyId is a good canditate for a ruleID ?
     */
    private String policyName;

    /**
     * Timer type according to Drools documentation (cron, int ...).
     * See https://docs.jboss.org/drools/release/7.48.0.Final/drools-docs/html_single/index.html#drl-timers-calendars-con_drl-rules
     */
    private String timerType;

    /**
     * The expression used for timer (must be a cron expression for a 'cron' timer).
     * See https://docs.jboss.org/drools/release/7.48.0.Final/drools-docs/html_single/index.html#drl-timers-calendars-con_drl-rules
     */
    private String timerExpression;

    private String ttlUnit;

    /**
     * The TTL is used to define the 'temporal window' of the rule.
     */
    private Integer ttl;

    /**
     * A string representation of the schedule conditions of the rule.
     */
    private String scheduleConditions;

    /**
     * The execution contitions of the rule.
     */
    private String conditions;

    /**
     * The final action this rule was written for. For the moment only the workflow name.
     *
     * TODO: Represent the action : launchWorkflow(run)
     */
    private String action;

    /**
     * Defines if the rule should be rescheduled when the action terminates with error.
     */
    private boolean retryOnError;

    /**
     * Defines if only one action should be executed at a given time (no overlap when true).
     */
    private boolean onlyOneRunning;

    /*
     * A rule is handled when the corresponding session has been created (ie. the deployment has been DEPLOYED)
     */
    private boolean handled;

    public String getId() {
        return this.environmentId + "_" + this.policyName;
    }

    @Override
    public String toString() {
        return "Rule{" +
                "environmentId='" + environmentId + '\'' +
                ", cronExpression='" + timerExpression + '\'' +
                ", ttl='" + ttl + ' ' + ttlUnit + '\'' +
                ", scheduleConditions=" + scheduleConditions +
                ", conditions=" + conditions +
                ", action='" + action + '\'' +
                ", retryOnError=" + retryOnError +
                ", onlyOneRunning=" + onlyOneRunning +
                '}';
    }
}
