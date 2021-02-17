package org.alien4cloud.rmsscheduler;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = RMSPluginConfiguration.CONFIG_PREFIX)
public class RMSPluginConfiguration {

    public static final String CONFIG_PREFIX = "alien4cloud-rms-scheduler-plugin";

    /**
     * The TTL for events : you should set this config regarding to the frequency with witch you expect to receive events in your loopback.
     * Must be expressed as a delay : 10m (minutes) 30s (seconds) 2h (hours) ...
     */
    private String metricEventTtl = "10m";

    /**
     * In ms, the period between 2 rule fires. A heartbeat will fire all rules on each session (1 session per active deployments).
     * <br/>
     * Choosing the right configuration depends on your needs. Unless you launch people to the moon, if you don't need a
     * very high precision to launch jobs that are conditioned, so each 30 secondes or each minutes is sufficient.
     * <br/>
     * If you want to test the engine, you should set to 1 to have a more reactive system.
     */
    private long heartbeatPeriod = 60 * 1000;

}
