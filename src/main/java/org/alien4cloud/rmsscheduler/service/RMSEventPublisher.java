package org.alien4cloud.rmsscheduler.service;

import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RMSEventPublisher {

    @Autowired
    private SessionDao sessionDao;

    public void publishEvent(MetricEvent event) {
        sessionDao.list().forEach(sessionHandler -> {
            sessionHandler.getSession().insert(event);
        });
    }
}
