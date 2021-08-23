package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.*;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

/**
 * A rule is the Java representation of a Drools rule. It's related to an environment.
 * At deployment preparation stage, a rule is created for each RMSSchedulePolicy instance.
 * At deployment stage a KIE session is created.
 */
@ESObject
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Rule {

    @Id
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String id;

    /**
     * The name of the rule is the name of the policy node in the topology.
     */
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String name;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String environmentId;

    /**
     * A rule has a deploymentId at the moment it is handled.
     */
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String deploymentId;

    /**
     * Timer type according to Drools documentation (cron, int ...).
     * See https://docs.jboss.org/drools/release/7.48.0.Final/drools-docs/html_single/index.html#drl-timers-calendars-con_drl-rules
     */
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String timerType;

    /**
     * The expression used for timer (must be a cron expression for a 'cron' timer).
     * See https://docs.jboss.org/drools/release/7.48.0.Final/drools-docs/html_single/index.html#drl-timers-calendars-con_drl-rules
     */
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String timerExpression;

    /**
     * Duration of the time window in ms.
     */
    @NumberField(index = IndexType.not_analyzed)
    private long duration;

    /**
     * A string representation of the schedule conditions of the rule.
     */
    private String scheduleConditions;

    /**
     * The execution conditions of the rule.
     */
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String[] conditions;

    /**
     * The final action this rule was written for. For the moment only the workflow name.
     *
     * TODO: Represent the action : launchWorkflow(run), executeOperation(node, operation)
     */
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String action;

    /**
     * Defines if the rule should be rescheduled when the action terminates with error.
     */
    @BooleanField
    private boolean retryOnError;

    /**
     * Reschedule even if success but only during time window.
     */
    @BooleanField
    private boolean loop;

    /**
     * The delay beetween reschedules (after error or if looping) in ms.
     */
    @NumberField(index = IndexType.not_analyzed)
    private long delay;

    /**
     * The maximum the action will be repeated (in case of ERROR and retry_on_error or loop mode).
     */
    @NumberField(index = IndexType.not_analyzed)
    private int maxRun = -1;

    /**
     * Defines if only one action should be executed at a given time (no overlap when true).
     */
    @BooleanField
    private boolean onlyOneRunning;

    /**
     * Defines if we must cancel a running workflow if time window elapsed.
     */
    @BooleanField
    private boolean cancelOnTimeout;

    /*
     * A rule is handled when the corresponding session has been created (ie. the deployment has been DEPLOYED)
     */
    //@BooleanField
    //private boolean handled;

}
