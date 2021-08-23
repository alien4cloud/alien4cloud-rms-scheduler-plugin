package org.alien4cloud.rmsscheduler.model.timeline;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.DateField;
import org.elasticsearch.annotation.TimeStamp;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import java.util.Date;

/**
 * An event that has a startTime and a endTime, so a duration (should be a range box in UI).
 */
@Getter
@Setter
public abstract class AbstractTimelinePeriod extends AbstractTimelineEvent {

    @TermFilter
    @DateField
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Date endTime;

}
