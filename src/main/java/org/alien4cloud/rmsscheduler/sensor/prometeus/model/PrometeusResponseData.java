package org.alien4cloud.rmsscheduler.sensor.prometeus.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class PrometeusResponseData {

    public final static String RESULT_TYPE_VECTOR = "vector";

    private String resultType;
    private PrometeusResponseDataResultItem[] result;
}
