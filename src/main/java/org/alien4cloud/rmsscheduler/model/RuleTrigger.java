package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Calendar;
import java.util.Date;

/**
 * A RuleTrigger represents an instance of a scheduled rule.
 * It's created by Drools rules themself and their changes are listened in order to execute action (ie. launch workflow).
 */
@Getter
@Setter
@ToString
public class RuleTrigger {

    /**
     * Should be unique for a given deployment. environmentId_policyName is a good candidate.
     */
    private String ruleId;
    private String environmentId;
    private String deploymentId;

    /**
     * When triggered, the execution is set (see @{@link org.alien4cloud.rmsscheduler.service.WorkflowListener})
     */
    private String executionId;

    /**
     * The action to execute, typically the name of the workflow to launch.
     */
    private String action;
    private RuleTriggerStatus status;

    /**
     * Start of the time window the workflow can be launched.
     */
    private Date scheduleTime;

    /**
     * End of the time window the workflow can be launched.
     */
    private Date expirationTime;

    public RuleTrigger(String ruleId, String environmentId, String deploymentId, String action, int expirationField, int expirationAmount) {
        this.ruleId = ruleId;
        this.environmentId = environmentId;
        this.deploymentId = deploymentId;
        this.action = action;
        Calendar cal = Calendar.getInstance();
        this.scheduleTime = new Date(cal.getTimeInMillis());
        cal.add(expirationField, expirationAmount);
        this.expirationTime = new Date(cal.getTimeInMillis());
        this.status = RuleTriggerStatus.SCHEDULED;
    }

}
