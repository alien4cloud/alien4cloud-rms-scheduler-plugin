package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

/**
 * A rule is the Java representation of a Drools rule. It's related to an environment.
 * At deployment preparation stage, a rule is created for each RMSSchedulePolicy instance.
 * At deployment stage a KIE session is created.
 */
@Getter
@Setter
@ToString
@ESObject
@NoArgsConstructor
public class Rule {

    @Id
    private String id;
    
    private String environmentId;

    /**
     * A rule has a deploymentId at the moment it is handled.
     */
    private String deploymentId;

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

    /**
     * Duration of the time window ex 5m or 4h or 1d.
     */
    private String duration;

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
     * TODO: Represent the action : launchWorkflow(run), executeOperation(node, operation)
     */
    private String action;

    /**
     * Defines if the rule should be rescheduled when the action terminates with error.
     */
    private boolean retryOnError;

    /**
     * Reschedule even if success but only during time window.
     */
    private boolean loop;

    /**
     * The delay beetween reschedules (after error or if looping), ex 30s or 10m or 2h.
     */
    private String delay;

    /**
     * The maximum the action will be repeated (in case of ERROR and retry_on_error or loop mode).
     */
    private int maxRun = -1;

    /**
     * Defines if only one action should be executed at a given time (no overlap when true).
     */
    private boolean onlyOneRunning;

    /**
     * Defines if we must cancel a running workflow if time window elapsed.
     */
    private boolean cancelOnTimeout;

    /*
     * A rule is handled when the corresponding session has been created (ie. the deployment has been DEPLOYED)
     */
    private boolean handled;

}
