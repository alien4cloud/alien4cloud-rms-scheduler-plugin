package org.alien4cloud.rmsscheduler.rest;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * A REST endpoint to publish events that can trigger rules.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/rmsscheduler/events" })
public class RMSEventControler {

    @Autowired
    private SessionDao sessionDao;

    /**
     * When an event is published, it is broadcast to every KIE sessions.
     */
    @RequestMapping(value = "/publish/{eventLabel}/{eventValue}", method = RequestMethod.PUT, produces = "application/json")
    public void publishEvent(@PathVariable String eventLabel, @PathVariable Long eventValue) {
        MetricEvent m = new MetricEvent(eventLabel, eventValue);
        sessionDao.list().forEach(sessionHandler -> {
            sessionHandler.getSession().insert(m);
        });
    }

}
