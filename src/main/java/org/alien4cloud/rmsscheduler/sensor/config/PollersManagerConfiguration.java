package org.alien4cloud.rmsscheduler.sensor.config;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.rmsscheduler.RMSPluginConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = RMSPluginConfiguration.CONFIG_PREFIX + ".metrics")
public class PollersManagerConfiguration extends PollerConfiguration {

    Map<String, PollerConfiguration> pollers;

}
