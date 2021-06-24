package org.alien4cloud.rmsscheduler.sensor;

import org.alien4cloud.rmsscheduler.sensor.config.PollerItemConfiguration;

public class VoidHttpPoller extends HttpPoller {

    @Override
    public void pollItem(String itemName, PollerItemConfiguration itemConfig) {

    }
}
