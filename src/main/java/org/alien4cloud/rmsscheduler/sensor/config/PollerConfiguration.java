package org.alien4cloud.rmsscheduler.sensor.config;

import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyConstraint;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.normative.types.ToscaTypes;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class PollerConfiguration {

    private PollerType type;

    private String url;

    private String user;
    private String password;

    // in seconds, delay between each poll
    private long period;

    private Map<String, PollerItemConfiguration> items;

}
