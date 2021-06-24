package org.alien4cloud.rmsscheduler.sensor;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.sensor.config.AggregationFunction;
import org.alien4cloud.rmsscheduler.sensor.config.PollerItemConfiguration;

import java.util.Iterator;
import java.util.List;

/**
 * Responsible of managing aggregations when multiple results found in an sensor response.
 */
@Slf4j
public class PollerResponseHandler {

    public static <I> void handleResponse(List<I> resultList, PollerResponseHandler.ResponseParser<I> resultParser, PollerItemConfiguration itemConfig, MetricCallback metricCallback) {
        int itemCount = 0;
        MetricEvent aggregatedEvent = null;
        Iterator<I> it = resultList.iterator();
        while(it.hasNext()) {
            I item = it.next();
            MetricEvent metricEvent = resultParser.parseResponse(item);
            if (metricEvent != null) {
                if (resultList.size() == 1) {
                    metricCallback.handleMetricEvent(metricEvent);
                } else if (resultList.size() > 1) {
                    if (itemConfig.getAggregationFunction() != null) {
                        itemCount++;
                        if (itemCount == 1) {
                            // this is the first result just store it in aggregatedEvent
                            aggregatedEvent = metricEvent;
                        } else {
                            // for others, call aggregate
                            aggregatedEvent = aggregate(aggregatedEvent, metricEvent, itemConfig.getAggregationFunction());
                        }
                    } else {
                        // several results but no aggregation function, each result is published
                        // in this case, the config may define tags to segregate events
                        metricCallback.handleMetricEvent(metricEvent);
                    }
                }
            } else {
                log.trace("MetricEvent is null, ignoring");
            }
        }

        if (resultList.size() > 1 && itemCount > 0) {
            if (itemConfig.getAggregationFunction() != null) {
                if (itemConfig.getAggregationFunction() == AggregationFunction.avg && aggregatedEvent.getDoubleValue() != null) {
                    double avg = aggregatedEvent.getDoubleValue() / itemCount;
                    aggregatedEvent.setDoubleValue(avg);
                }
                aggregatedEvent.setTags(null);
                metricCallback.handleMetricEvent(aggregatedEvent);
            }
        }

    }

    private static MetricEvent aggregate(MetricEvent agregatedEvent, MetricEvent metricEvent, AggregationFunction aggregationFunction) {
        if (metricEvent.getDoubleValue() == null) {
            return agregatedEvent;
        }
        if (agregatedEvent.getDoubleValue() == null) {
            agregatedEvent.setDoubleValue(metricEvent.getDoubleValue());
        } else {
            switch(aggregationFunction) {
                case min:
                    agregatedEvent.setDoubleValue(Double.min(agregatedEvent.getDoubleValue(), metricEvent.getDoubleValue()));
                    break;
                case max:
                    agregatedEvent.setDoubleValue(Double.max(agregatedEvent.getDoubleValue(), metricEvent.getDoubleValue()));
                    break;
                case sum:
                case avg:
                    agregatedEvent.setDoubleValue(Double.sum(agregatedEvent.getDoubleValue(), metricEvent.getDoubleValue()));
                    break;
            }
        }
        if (metricEvent.getTimestamp().after(agregatedEvent.getTimestamp())) {
            // use the most recent timestamp for event timestamp
            agregatedEvent.setTimestamp(metricEvent.getTimestamp());
        }
        return agregatedEvent;
    }

    public interface ResponseParser<I> {
        MetricEvent parseResponse(I item);
    }

    public interface MetricCallback {
        void handleMetricEvent(MetricEvent event);
    }

}
