package org.alien4cloud.rmsscheduler.sensor.prometeus.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@ToString
@Getter
@Setter
public class PrometeusResponseDataResultItem {

    private Map<String, String> metric;
    private Object[] value;

}
