package org.alien4cloud.rmsscheduler.rest;

import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.DSLDao;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.service.RuleGenerator;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A REST endpoint to publish events that can trigger rules.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/rmsscheduler/dsl" })
public class RMSDslControler {

    @Autowired
    private DSLDao dslDao;

    @Autowired
    private RuleGenerator ruleGenerator;

    /**
     *
     */
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<String> getDsl() {
        return RestResponseBuilder.<String> builder().data(dslDao.getAdminDsl().getContent()).build();
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<Message>> update(@RequestBody String dslContent) {
        Results results = ruleGenerator.verifyDSL(dslContent);
        List<Message> messages = results.getMessages(Message.Level.ERROR);
        RestResponseBuilder builder = RestResponseBuilder.<List<Message>> builder().data(messages);
        if (messages.isEmpty()) {
            // NO error messages we can save the DSL
            dslDao.saveAdminDsl(dslContent);
        } else {
            builder.error(new RestError(0, "Not able to parse DSL"));
        }
        return builder.build();
    }

}
