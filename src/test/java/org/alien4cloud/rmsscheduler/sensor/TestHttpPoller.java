package org.alien4cloud.rmsscheduler.sensor;

import com.google.common.collect.Maps;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.sensor.config.PollerItemConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

public class TestHttpPoller {

    private VoidHttpPoller voidHttpPoller = new VoidHttpPoller();

    @Test
    public void testBuildMetricEvent_simple() {
        PollerItemConfiguration config = getPollerItemConfiguration();
        MetricEvent metricEvent = voidHttpPoller.buildMetricEvent("test", Calendar.getInstance(), "123", config, Maps.newHashMap());
        Assert.assertNotNull(metricEvent);
        Assert.assertEquals("123", metricEvent.getValue());
        Assert.assertEquals(Double.valueOf(123), metricEvent.getDoubleValue());
    }

    @Test
    public void testBuildMetricEvent_ttl_expired() {
        PollerItemConfiguration config = getPollerItemConfiguration();
        Calendar timestamp = Calendar.getInstance();
        timestamp.add(Calendar.MINUTE, -1);
        MetricEvent metricEvent = voidHttpPoller.buildMetricEvent("test", timestamp, "123", config, Maps.newHashMap());
        Assert.assertNull(metricEvent);
    }

    @Test
    public void testBuildMetricEvent_transform_simple() {
        PollerItemConfiguration config = getPollerItemConfiguration();
        config.setTransform("T(Double).parseDouble(value)");
        MetricEvent metricEvent = voidHttpPoller.buildMetricEvent("test", Calendar.getInstance(), "1024", config, Maps.newHashMap());
        Assert.assertNotNull(metricEvent);
        Assert.assertEquals(Double.valueOf(1024), metricEvent.getDoubleValue());
    }

    @Test
    public void testBuildMetricEvent_transform_division() {
        PollerItemConfiguration config = getPollerItemConfiguration();
        config.setTransform("T(Double).parseDouble(value) / 1000 / 1000");
        MetricEvent metricEvent = voidHttpPoller.buildMetricEvent("test", Calendar.getInstance(), "56124767", config, Maps.newHashMap());
        Assert.assertNotNull(metricEvent);
        Assert.assertEquals(Double.valueOf(56.124767), metricEvent.getDoubleValue());
    }

    private PollerItemConfiguration getPollerItemConfiguration() {
        PollerItemConfiguration config = new PollerItemConfiguration();
        config.setTtl(30);
        return config;
    }

}
