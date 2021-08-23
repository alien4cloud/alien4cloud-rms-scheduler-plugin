package org.alien4cloud.rmsscheduler.model.timeline;

import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.NumberField;
import org.elasticsearch.mapping.IndexType;

import java.util.Date;

/**
 * Represents a change in a given condition state : when the condition is no more verified, or newly verified,
 * such kind of event is inserted in Drools session and then persisted into ES for UI usage.
 */
@ESObject
@Getter
@Setter
public class TimelineRuleConditionEvent extends AbstractTimelineEvent {

    @NumberField(index = IndexType.not_analyzed)
    private int conditionIdx;

    @BooleanField
    private boolean passed;

    public TimelineRuleConditionEvent() {
        this.setType(TimelineRuleConditionEvent.class.getSimpleName());
    }

    public TimelineRuleConditionEvent(String deploymentId, String ruleId, boolean passed, int conditionIdx) {
        super();
        this.setType(TimelineRuleConditionEvent.class.getSimpleName());
        setDeploymentId(deploymentId);
        setRuleId(ruleId);
        setPassed(passed);
        setConditionIdx(conditionIdx);
        setStartTime(new Date());
        // FIXME: Is it really a good idea to use timestamp as id here ?
        setId(Long.toString(getStartTime().getTime()));
    }


}
