package org.alien4cloud.rmsscheduler.rest;

import alien4cloud.exception.NotFoundException;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * A REST endpoint to publish events that can trigger rules.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/rmsscheduler/debug" })
public class RMSDebugControler {

/*    @Autowired
    private RuleDao ruleDao;*/

    @Autowired
    private SessionDao sessionDao;

/*    @RequestMapping(value = "/rules", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Collection<Rule>> exploreRules() {
        RestResponseBuilder<Collection<Rule>> builder = RestResponseBuilder.<Collection<Rule>>builder();
        builder.data(ruleDao.list());
        return builder.build();
    }*/

    @RequestMapping(value = "/sessions/{sessionId}", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Collection<? extends Object>> exploreSession(@PathVariable String sessionId) {
        SessionHandler sessionHandler = sessionDao.get(sessionId);
        if (sessionHandler == null) {
            throw new NotFoundException("Session not found : " + sessionId);
        }
        RestResponseBuilder<Collection<? extends Object>> builder = RestResponseBuilder.<Collection<? extends Object>>builder();
        builder.data(sessionHandler.getSession().getObjects());
        return builder.build();
    }

    @RequestMapping(value = "/sessions", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Collection<String>> exploreSessions() {
        RestResponseBuilder<Collection<String>> builder = RestResponseBuilder.<Collection<String>>builder();
        Collection<String> result = Lists.newArrayList();
        sessionDao.list().forEach(sessionHandler -> {
                result.add(sessionHandler.getId());
            }
        );
        builder.data(result);
        return builder.build();
    }

}
