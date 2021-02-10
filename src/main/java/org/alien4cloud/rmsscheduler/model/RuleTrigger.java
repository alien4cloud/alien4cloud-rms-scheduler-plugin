package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Calendar;
import java.util.Date;

/**
 * A RuleTrigger represents an instance of a scheduled rule.
 * It's created by Drools rules themself and their changes are listened in order to execute action (ie. launch workflow).
 */
@Getter
@Setter
public class RuleTrigger {

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

    private String ruleId;
    private String environmentId;
    private String deploymentId;
    private String executionId;
    private String action;
    private RuleTriggerStatus status;
    private Date scheduleTime;
    private Date expirationTime;

    @Override
    public String toString() {
        return "RuleTrigger{" +
                "ruleId='" + ruleId + '\'' +
                ", environmentId='" + environmentId + '\'' +
                ", deploymentId='" + deploymentId + '\'' +
                ", action='" + action + '\'' +
                ", status=" + status +
                ", scheduleTime=" + scheduleTime +
                ", expirationTime=" + expirationTime +
                '}';
    }
}
