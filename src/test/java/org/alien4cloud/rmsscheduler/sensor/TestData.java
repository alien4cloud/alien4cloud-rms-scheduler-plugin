package org.alien4cloud.rmsscheduler.sensor;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kie.api.definition.rule.All;

import java.util.Calendar;
import java.util.Map;

@Getter
@AllArgsConstructor
public class TestData {

    private String value;
    private Calendar timestamp;

    private Map<String, String> data = Maps.newHashMap();

}
