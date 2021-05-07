package org.alien4cloud.rmsscheduler.sensor;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.sensor.config.PollerConfiguration;
import org.alien4cloud.rmsscheduler.service.RMSEventPublisher;
import org.apache.lucene.util.NamedThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class ScheduledPoller implements Poller, PollerResponseHandler.MetricCallback {

    private PollerConfiguration configuration;

    @Setter
    private ScheduledExecutorService schedulerService;

    @Setter
    protected RMSEventPublisher rmsEventPublisher;

    public void init() {
        if (getConfiguration().getPeriod() > 0) {
            log.info("Poller period : {}s", getConfiguration().getPeriod());

            this.schedulerService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    poll();
                }
            }, getConfiguration().getPeriod(), getConfiguration().getPeriod(), TimeUnit.SECONDS);

        } else {
            log.warn("Poller period seems invalid ({}s), nothing will be scheduled", getConfiguration().getPeriod());
        }
    }

    @Override
    public void handleMetricEvent(MetricEvent event) {
        log.trace("Publishing event for poller <{}>: {}", getName(), event.toString());
        this.rmsEventPublisher.publishEvent(event);
    }

    @Override
    public void shutdown() {
        if (schedulerService != null) {
            schedulerService.shutdown();
        }
    }
    
    public PollerConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(PollerConfiguration configuration) {
        this.configuration = configuration;
    }

}
