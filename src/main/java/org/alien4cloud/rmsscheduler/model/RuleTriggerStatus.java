package org.alien4cloud.rmsscheduler.model;

// TODO: need for a HANDLED status ? (when the trigger has been handled by 'A4C', ie. workflow has been launched but not necessary running).
public enum RuleTriggerStatus {
    /** The cron expression has scheduled the rule */
    SCHEDULED,
    /** All conditions are met, the rule is triggered, the action should be executed. */
    TRIGGERED,
    /** The action is executing. */
    RUNNING,
    /** The action has be executed with success. */
    DONE,
    /** The action has finished with error. */
    ERROR,
    /** The rule was running, but the end of time window is reached. */
    TIMEOUT,
    /** The rule was running, but the end of time window is reached. */
    CANCELLED,
    /** The rule has not been triggered before it's expirationDate, it's cancelled. */
    DROPPED,
    /** Nothing more to do with this trigger, it has been deleted. */
    DELETED
}
