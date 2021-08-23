package org.alien4cloud.rmsscheduler.model.timeline;

import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.ESObject;

/**
 * Represents a time window into which a workflow can be triggered, if conditions are met.
 */
@ESObject
@Getter
@Setter
public class TimelineWindow extends AbstractTimelinePeriod {

    @BooleanField
    private boolean obsolete;

    public TimelineWindow() {
        this.setType(TimelineWindow.class.getSimpleName());
    }
}
