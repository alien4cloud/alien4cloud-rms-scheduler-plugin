package org.alien4cloud.rmsscheduler.model.timeline;

import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

/**
 * Represents an execution of a workflow.
 */
@ESObject
@Getter
@Setter
public class TimelineAction extends AbstractTimelinePeriod {

    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String name;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String executionId;

    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private TimelineActionState state;

    public TimelineAction() {
        this.setType(TimelineAction.class.getSimpleName());
    }
}
