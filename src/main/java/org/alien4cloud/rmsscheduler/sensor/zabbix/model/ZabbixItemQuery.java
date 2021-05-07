package org.alien4cloud.rmsscheduler.sensor.zabbix.model;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
public class ZabbixItemQuery extends ZabbixQuery {

    public ZabbixItemQuery(String auth, Map<String, Object> params) {
        super("item.get");
        this.setAuth(auth);
        this.setParams(params);
        this.setId(1);
    }

}
