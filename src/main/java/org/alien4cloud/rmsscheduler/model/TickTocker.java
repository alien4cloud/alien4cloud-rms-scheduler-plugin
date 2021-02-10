package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Used by rules to compare expirationDate of triggers.
 */
@Getter
@Setter
public class TickTocker {
    private Date now;

    public TickTocker() {
        this.now = new Date();
    }
}
