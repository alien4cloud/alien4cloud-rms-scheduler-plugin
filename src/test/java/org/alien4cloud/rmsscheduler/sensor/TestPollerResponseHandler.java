package org.alien4cloud.rmsscheduler.sensor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.sensor.config.AggregationFunction;
import org.alien4cloud.rmsscheduler.sensor.config.PollerItemConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

public class TestPollerResponseHandler {

    @Test
    public void test_simple() {
        List<TestData> data = Lists.newArrayList();
        data.add(new TestData("128", Calendar.getInstance(), Maps.newHashMap()));
        PollerItemConfiguration config = new PollerItemConfiguration();
        config.setTtl(10);

        TestMetricCallBack metricCallBack = new TestMetricCallBack();

        PollerResponseHandler.handleResponse(data, new DataParser(), config, metricCallBack);
        Assert.assertEquals(1, metricCallBack.events.size());
        Assert.assertEquals("128", metricCallBack.events.get(0).getValue());
        Assert.assertEquals(Double.valueOf(128), metricCallBack.events.get(0).getDoubleValue());
    }

    @Test
    public void test_multiple_result_without_aggregation() {
        List<TestData> data = Lists.newArrayList();
        data.add(new TestData("128", Calendar.getInstance(), Maps.newHashMap()));
        data.add(new TestData("129", Calendar.getInstance(), Maps.newHashMap()));
        PollerItemConfiguration config = new PollerItemConfiguration();
        config.setTtl(10);

        TestMetricCallBack metricCallBack = new TestMetricCallBack();

        PollerResponseHandler.handleResponse(data, new DataParser(), config, metricCallBack);
        Assert.assertEquals(2, metricCallBack.events.size());
        Assert.assertEquals("128", metricCallBack.events.get(0).getValue());
        Assert.assertEquals("129", metricCallBack.events.get(1).getValue());
    }

    @Test
    public void test_multiple_result_with_min() {
        List<TestData> data = Lists.newArrayList();
        data.add(new TestData("1", Calendar.getInstance(), Maps.newHashMap()));
        data.add(new TestData("128", Calendar.getInstance(), Maps.newHashMap()));
        data.add(new TestData("129", Calendar.getInstance(), Maps.newHashMap()));
        PollerItemConfiguration config = new PollerItemConfiguration();
        config.setTtl(10);
        config.setAggregationFunction(AggregationFunction.min);

        TestMetricCallBack metricCallBack = new TestMetricCallBack();

        PollerResponseHandler.handleResponse(data, new DataParser(), config, metricCallBack);
        Assert.assertEquals(1, metricCallBack.events.size());
        Assert.assertEquals(Double.valueOf(1), metricCallBack.events.get(0).getDoubleValue());
    }

    @Test
    public void test_multiple_result_with_max() {
        List<TestData> data = Lists.newArrayList();
        data.add(new TestData("1", Calendar.getInstance(), Maps.newHashMap()));
        data.add(new TestData("128", Calendar.getInstance(), Maps.newHashMap()));
        data.add(new TestData("129", Calendar.getInstance(), Maps.newHashMap()));
        PollerItemConfiguration config = new PollerItemConfiguration();
        config.setTtl(10);
        config.setAggregationFunction(AggregationFunction.max);

        TestMetricCallBack metricCallBack = new TestMetricCallBack();

        PollerResponseHandler.handleResponse(data, new DataParser(), config, metricCallBack);
        Assert.assertEquals(1, metricCallBack.events.size());
        Assert.assertEquals(Double.valueOf(129), metricCallBack.events.get(0).getDoubleValue());
    }

    @Test
    public void test_multiple_result_with_sum() {
        List<TestData> data = Lists.newArrayList();
        data.add(new TestData("10", Calendar.getInstance(), Maps.newHashMap()));
        data.add(new TestData("2", Calendar.getInstance(), Maps.newHashMap()));
        data.add(new TestData("7.87", Calendar.getInstance(), Maps.newHashMap()));
        PollerItemConfiguration config = new PollerItemConfiguration();
        config.setTtl(10);
        config.setAggregationFunction(AggregationFunction.sum);

        TestMetricCallBack metricCallBack = new TestMetricCallBack();

        PollerResponseHandler.handleResponse(data, new DataParser(), config, metricCallBack);
        Assert.assertEquals(1, metricCallBack.events.size());
        Assert.assertEquals(Double.valueOf(19.87), metricCallBack.events.get(0).getDoubleValue());
    }


    @Test
    public void test_multiple_result_with_avg() {
        List<TestData> data = Lists.newArrayList();
        data.add(new TestData("10", Calendar.getInstance(), Maps.newHashMap()));
        data.add(new TestData("20", Calendar.getInstance(), Maps.newHashMap()));
        data.add(new TestData("3", Calendar.getInstance(), Maps.newHashMap()));
        PollerItemConfiguration config = new PollerItemConfiguration();
        config.setTtl(10);
        config.setAggregationFunction(AggregationFunction.avg);

        TestMetricCallBack metricCallBack = new TestMetricCallBack();

        PollerResponseHandler.handleResponse(data, new DataParser(), config, metricCallBack);
        Assert.assertEquals(1, metricCallBack.events.size());
        Assert.assertEquals(Double.valueOf(11), metricCallBack.events.get(0).getDoubleValue());
    }

    private class DataParser implements PollerResponseHandler.ResponseParser<TestData> {
        @Override
        public MetricEvent parseResponse(TestData item) {
            MetricEvent event = new MetricEvent();
            event.setLabel("test");
            event.setValue(item.getValue());
            event.setDoubleValue(Double.parseDouble(item.getValue()));
            event.setTimestamp(item.getTimestamp().getTime());
            return event;
        }
    }

    private class TestMetricCallBack implements PollerResponseHandler.MetricCallback {
        List<MetricEvent> events = Lists.newArrayList();
        @Override
        public void handleMetricEvent(MetricEvent event) {
            events.add(event);
        }
    }

}
