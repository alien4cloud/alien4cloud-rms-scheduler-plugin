package org.alien4cloud.rmsscheduler.sensor.zabbix.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ZabbixQuery {

    private String jsonrpc = "2.0";
    private final String method;
    private Map<String, Object> params;
    private int id;
    private String auth;

    public ZabbixQuery(String method) {
        this.method = method;
    }
}
