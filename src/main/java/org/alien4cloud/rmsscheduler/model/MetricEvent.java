package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MetricEvent {
    private String label;
    private String value;
    private Double doubleValue;
    private Date timestamp;
    private Map<String, String> tags;

    public MetricEvent(String label, String value) {
        this.label = label;
        this.value = value;
        try {
            this.doubleValue = Double.parseDouble(value);
        } catch(NumberFormatException nfe) {
            // Nothing to do here
        }
        this.timestamp = new Date();
    }

}
