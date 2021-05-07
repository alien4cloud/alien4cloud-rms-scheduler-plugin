package org.alien4cloud.rmsscheduler.sensor;

import org.alien4cloud.rmsscheduler.sensor.config.PollerConfiguration;
import org.alien4cloud.rmsscheduler.sensor.config.PollerItemConfiguration;
import org.alien4cloud.rmsscheduler.service.RMSEventPublisher;

import java.util.concurrent.ScheduledExecutorService;

public interface Poller {
    void init();
    String getName();
    void setName(String name);
    void poll();
    void shutdown();
    void setConfiguration(PollerConfiguration configuration);
    void pollItem(String itemName, PollerItemConfiguration itemConfig);
    void setRmsEventPublisher(RMSEventPublisher rmsEventPublisher);
    void setSchedulerService(ScheduledExecutorService schedulerService);
}
