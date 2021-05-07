package org.alien4cloud.rmsscheduler.sensor.zabbix.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ZabbixAuthResponse extends ZabbixResponse {

    private String result;
}
