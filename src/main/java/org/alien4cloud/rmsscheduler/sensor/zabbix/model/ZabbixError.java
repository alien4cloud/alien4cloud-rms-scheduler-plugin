package org.alien4cloud.rmsscheduler.sensor.zabbix.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ZabbixError {

    private int code;
    private String message;
    private String data;

}
