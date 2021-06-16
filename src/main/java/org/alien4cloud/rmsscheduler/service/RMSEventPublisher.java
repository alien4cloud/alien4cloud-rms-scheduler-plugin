package org.alien4cloud.rmsscheduler.service;

import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.suggestions.ConditionsSuggestionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RMSEventPublisher {

    @Autowired
    private SessionDao sessionDao;

    @Resource
    private ConditionsSuggestionProvider conditionsSuggestionProvider;

    public void publishEvent(MetricEvent event) {
        sessionDao.list().forEach(sessionHandler -> {
            sessionHandler.getSession().insert(event);
        });
        conditionsSuggestionProvider.addMetricEvent(event);
    }
}
