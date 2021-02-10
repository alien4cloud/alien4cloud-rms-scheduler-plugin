package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class MetricEvent {
    private String label;
    private long value;
    private Date timestamp;

    public MetricEvent(String label, long value) {
        this.label = label;
        this.value = value;
        this.timestamp = new Date();
    }

    @Override
    public String toString() {
        return "MetricEvent{" +
                "label='" + label + '\'' +
                ", value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}
