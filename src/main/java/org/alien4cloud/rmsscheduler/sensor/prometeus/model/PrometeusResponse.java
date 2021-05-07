package org.alien4cloud.rmsscheduler.sensor.prometeus.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.alien4cloud.rmsscheduler.sensor.zabbix.model.ZabbixError;

@ToString
@Getter
@Setter
public class PrometeusResponse {

    public final static String STATUS_SUCCESS = "success";

    private String status;
    private String errorType;
    private String error;
    private PrometeusResponseData data;

}
