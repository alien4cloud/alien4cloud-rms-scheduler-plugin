package org.alien4cloud.rmsscheduler.model.timeline;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

import java.util.UUID;

/**
 * For rule debugging purpose, an event that represents a RuleTrigger status change.
 */
@ESObject
@Getter
@Setter
public class TriggerEvent extends AbstractTimelineEvent {

    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String status;

    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String triggerId;

    public TriggerEvent() {
        this.setType(TriggerEvent.class.getSimpleName());
        this.setId(UUID.randomUUID().toString());
    }

}
