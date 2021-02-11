package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.drools.core.time.TimeUtils;

import java.util.Calendar;
import java.util.Date;

/**
 * A RuleTrigger represents an instance of a scheduled rule.
 * It's created by Drools rules themself and their changes are listened in order to execute action (ie. launch workflow).
 *
 * A RuleTrigger has a scheduleTime (begin of the temporal window inside it can trigger) and a expirationTime (end of this same temporal window).
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

    /**
     * The maximum the action will be repeated (in case of ERROR and retry_on_error or loop mode).
     */
    private int maxRun = -1;

    private int runCount = 0;

    public RuleTrigger(String ruleId, String environmentId, String deploymentId, String action, String expirationDelai, int maxRun) {
        this.ruleId = ruleId;
        this.environmentId = environmentId;
        this.deploymentId = deploymentId;
        this.action = action;
        this.maxRun = maxRun;
        long expirationDelay = TimeUtils.parseTimeString(expirationDelai);;
        // FIXME: quelque chose me fait penser que c'est pas à lui de faire ça
        // Utiliser l'expiration des events dans drools ?
        Calendar cal = Calendar.getInstance();
        this.scheduleTime = new Date(cal.getTimeInMillis());
        this.expirationTime = new Date(cal.getTimeInMillis() + expirationDelay);
        this.status = RuleTriggerStatus.SCHEDULED;
    }

    public void activate() {
        this.runCount++;
        this.status = RuleTriggerStatus.TRIGGERED;
    }

    /**
     * FIXME : not a good idea to have logic here, logic must be in rules !
     * @param delay
     */
    public void reschedule(String delay) {
        if (this.maxRun < 0 || ++this.runCount < this.maxRun) {
            // wa can schedule again
            this.status = RuleTriggerStatus.SCHEDULED;
            long scheduleDelay = TimeUtils.parseTimeString(delay);
            Calendar cal = Calendar.getInstance();
            this.scheduleTime = new Date(cal.getTimeInMillis() + scheduleDelay);
        } else {
            this.status = RuleTriggerStatus.DROPPED;
        }
    }

}
