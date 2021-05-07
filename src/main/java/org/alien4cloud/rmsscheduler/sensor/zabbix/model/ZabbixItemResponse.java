package org.alien4cloud.rmsscheduler.sensor.zabbix.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@ToString
@Getter
@Setter
public class ZabbixItemResponse extends ZabbixResponse {

    Map<String, Object>[] result;

}
