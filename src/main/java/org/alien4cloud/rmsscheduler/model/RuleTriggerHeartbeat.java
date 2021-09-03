package org.alien4cloud.rmsscheduler.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * A trigger heartbeat is inserted each time a Drools session is fired if a time window is active (a trigger is SCHEDULED).
 * It means : we need to evaluate conditions (if any). When remainingConditionsCount == 0, all conditions are passed, the trigger can be TRIGGERED.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RuleTriggerHeartbeat {

    /** Fed with Ticktocker timestamp (unique in a session). */
    @EqualsAndHashCode.Include
    private long id;

    private String triggerId;
    private String ruleId;
    private String deploymentId;

    /**
     * This is the number of remaining conditions that should be verified before really launching the trigger.
     */
    private int remainingConditionsCount;

    public RuleTriggerHeartbeat(long id, String triggerId, String ruleId, String deploymentId, int conditionsCount) {
        this.id = id;
        this.triggerId = triggerId;
        this.ruleId = ruleId;
        this.deploymentId = deploymentId;
        this.remainingConditionsCount = conditionsCount;
    }

    public void decrementRemainingConditions() {
        this.remainingConditionsCount--;
    }
}
