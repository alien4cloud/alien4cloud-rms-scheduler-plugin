package org.alien4cloud.rmsscheduler.sensor.config;

import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyConstraint;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.alien4cloud.tosca.normative.types.ToscaTypes;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class PollerItemConfiguration {

    /**
     * When several results are returned for an item, you can use an aggregation function : avg, sum, min, or max.
     */
    private AggregationFunction aggregationFunction;

    private String request;

    /**
     * In seconds, the time window tolerance : if the lastclock of an item is older than now - ttl, it will just be ignored.
     */
    private Integer ttl;

    /**
     * A map of tags to get from the zabbix item result. Key is the result entry to consider, value is the name of the tag.
     */
    private Map<String, String> tags;

    /**
     * A Spring expression language than can transform the string value to something else.
     * The text value is available in the the context as 'value', so an example could be : "T(Double).parseDouble(value) / 1000" to parse and divide per 1000
     * If the result is a Double, it is used to set MetricEvent.doubleValue
     */
    private String transform;

}
