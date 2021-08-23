package org.alien4cloud.rmsscheduler.model.timeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.DateField;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.TimeStamp;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import java.util.Date;

/**
 * Base class of the hierarchy, generic timestamped event, with just a startTime (should be a point in UI).
 */
@Getter
@Setter
@JsonIgnoreProperties
public abstract class AbstractTimelineEvent {

    @Id
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    public String id;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String deploymentId;

    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String ruleId;

    @TermFilter
    @DateField
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    private Date startTime;

    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String type;

}
