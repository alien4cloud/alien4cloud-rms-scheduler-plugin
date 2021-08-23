package org.alien4cloud.rmsscheduler.dao;

import alien4cloud.rest.model.BasicSearchRequest;
import alien4cloud.rest.model.SortConfiguration;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class SearchTimeLineEventRequest extends BasicSearchRequest {

    private long fromDate;

    private long toDate;

    private boolean includeTriggerEvents;

    public SearchTimeLineEventRequest() {
        super(null, 0, 10000);
    }

}
