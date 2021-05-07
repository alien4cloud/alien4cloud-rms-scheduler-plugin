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

    //private String name;

    private AggregationFunction aggregationFunction;

    private String request;

    /**
     * In seconds, the time window tolerance : if the lastclock of an item is older than now - ttl, it will just be ignored.
     */
    private Integer ttl;

    /**
     * A map of tags to get from the zabbix item result. Key is the result entry to consider, value is the name of teh tag.
     */
    private Map<String, String> tags;

}
