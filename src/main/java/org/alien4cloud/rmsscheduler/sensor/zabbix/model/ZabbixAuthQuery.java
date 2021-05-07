package org.alien4cloud.rmsscheduler.sensor.zabbix.model;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ZabbixAuthQuery extends ZabbixQuery {

    public ZabbixAuthQuery(String user, String password) {
        super("user.login");
        Map<String, Object> params = Maps.newHashMap();
        params.put("user", user);
        params.put("password", password);
        this.setParams(params);
        this.setId(0);
    }

}
