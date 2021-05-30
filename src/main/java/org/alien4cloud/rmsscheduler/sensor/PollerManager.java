package org.alien4cloud.rmsscheduler.sensor;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.sensor.config.PollerConfiguration;
import org.alien4cloud.rmsscheduler.sensor.config.PollersManagerConfiguration;
import org.alien4cloud.rmsscheduler.sensor.prometeus.PrometeusPoller;
import org.alien4cloud.rmsscheduler.sensor.zabbix.ZabbixPoller;
import org.alien4cloud.rmsscheduler.service.RMSEventPublisher;
import org.apache.lucene.util.NamedThreadFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Slf4j
public class PollerManager {

    @Inject
    private PollersManagerConfiguration configuration;

    private Map<String, Poller> pollers = Maps.newHashMap();

    @Inject
    protected RMSEventPublisher rmsEventPublisher;

    private ScheduledExecutorService schedulerService;

    @PostConstruct
    public void init() {
        if (configuration.getPollers() != null) {
            this.schedulerService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("rms-poller-scheduler"));
            log.info("In the plugin configurator with config: {}", configuration);
            configuration.getPollers().forEach((pollerName, pollerConfiguration) -> {
                // instanciate poller
                Poller poller = initPoller(pollerName, pollerConfiguration);
                if (poller != null) {
                    pollers.put(pollerName, poller);
                }
            });
        }
    }

    @PreDestroy
    public void shutdown() {
        pollers.forEach((s, poller) -> {
            poller.shutdown();
        });
    }

    private Poller initPoller(String pollerName, PollerConfiguration pollerConfiguration) {
        Poller poller = instanciatePoller(pollerConfiguration);
        if (poller == null) {
            log.warn("Not able to instanciate poller with config {}", pollerConfiguration);
        } else {
            poller.setName(pollerName);
            poller.setSchedulerService(this.schedulerService);
            poller.setConfiguration(pollerConfiguration);
            poller.setRmsEventPublisher(this.rmsEventPublisher);
            poller.init();
        }
        return poller;
    }

    private Poller instanciatePoller(PollerConfiguration pollerConfiguration) {
        switch(pollerConfiguration.getType()) {
            case Zabbix:
                return new ZabbixPoller();
            case Prometeus:
                return new PrometeusPoller();
            default:
                return null;
        }

    }

}
