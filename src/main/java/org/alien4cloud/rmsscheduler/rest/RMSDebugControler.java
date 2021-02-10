package org.alien4cloud.rmsscheduler.rest;

import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.RuleDao;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.model.Rule;
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

    @Autowired
    private RuleDao ruleDao;

    @RequestMapping(value = "/rules", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Collection<Rule>> exploreRules() {
        RestResponseBuilder<Collection<Rule>> builder = RestResponseBuilder.<Collection<Rule>>builder();
        builder.data(ruleDao.list());
        return builder.build();
    }

}
