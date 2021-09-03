package org.alien4cloud.rmsscheduler.model.timeline;

// TODO: need for a HANDLED status ? (when the trigger has been handled by 'A4C', ie. workflow has been launched but not necessary running).
public enum TimelineActionState {
    /** The action is executing. */
    RUNNING,
    /** The action has be executed with success. */
    DONE,
    /** The action has finished with error. */
    ERROR,
    /** The action was running, but the end of time window is reached. */
    TIMEOUT,
    /** The action has been cancelled. */
    CANCELLED
}
