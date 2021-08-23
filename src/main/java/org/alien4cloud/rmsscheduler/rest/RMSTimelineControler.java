package org.alien4cloud.rmsscheduler.rest;

import alien4cloud.dao.IESSearchQueryBuilderHelper;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.RMSDao;
import org.alien4cloud.rmsscheduler.dao.SearchTimeLineEventRequest;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.alien4cloud.rmsscheduler.model.timeline.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

/**
 * A REST endpoint to publish events that can trigger rules.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/rmsscheduler/timeline" })
public class RMSTimelineControler {

    @Autowired
    private RMSDao RMSDao;

    @Resource
    private SessionDao sessionDao;

    @RequestMapping(value = "/sessions/{deploymentId}", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Boolean> hasSession(@PathVariable String deploymentId) {
        SessionHandler sessionHandler = sessionDao.get(deploymentId);
        return RestResponseBuilder.<Boolean> builder().data((sessionHandler != null)).build();
    }

    /**
     *
     */
    @RequestMapping(value = "/rules/{deploymentId}", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Rule[]> getRules(@PathVariable String deploymentId) {

        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("deploymentId", new String[] { deploymentId });
        Rule[] data = RMSDao.find(Rule.class, filters, Integer.MAX_VALUE).getData();

        return RestResponseBuilder.<Rule[]> builder().data(data).build();
    }

    @RequestMapping(value = "/events/{deploymentId}", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<AbstractTimelineEvent[]> getRuleEvents(@PathVariable String deploymentId) {
        Map<String, String[]> filters = Maps.newHashMap();
        filters.put("deploymentId", new String[] { deploymentId });
        AbstractTimelineEvent[] timelineRuleConditionEvents = RMSDao.find(TimelineRuleConditionEvent.class, filters, Integer.MAX_VALUE).getData();
        AbstractTimelineEvent[] timelineWindows = RMSDao.find(TimelineWindow.class, filters, Integer.MAX_VALUE).getData();
        AbstractTimelineEvent[] timelineActions = RMSDao.find(TimelineAction.class, filters, Integer.MAX_VALUE).getData();
        AbstractTimelineEvent[] events = new AbstractTimelineEvent[timelineRuleConditionEvents.length + timelineWindows.length +  timelineActions.length];
        System.arraycopy(timelineRuleConditionEvents, 0, events, 0, timelineRuleConditionEvents.length);
        System.arraycopy(timelineWindows, 0, events, 0 + timelineRuleConditionEvents.length, timelineWindows.length);
        System.arraycopy(timelineActions, 0, events, 0 + timelineRuleConditionEvents.length + timelineWindows.length, timelineActions.length);
        return RestResponseBuilder.<AbstractTimelineEvent[]> builder().data(events).build();
    }

    @RequestMapping(value = "/events/{deploymentId}", method = RequestMethod.POST, produces = "application/json")
    public RestResponse<AbstractTimelineEvent[]> getRuleEvents(@PathVariable String deploymentId, @RequestBody SearchTimeLineEventRequest searchRequest) {

        if (log.isTraceEnabled()) {
            log.trace("From: {} ({}), To: {} ({})", searchRequest.getFromDate(), new Date(searchRequest.getFromDate()), searchRequest.getToDate(), new Date(searchRequest.getToDate()));
        }

        /**
         * For this deploymentId, all windows that:
         * - start or end in the range
         * - do not end
         * - or start and end outside of the range
         */
        BoolQueryBuilder windowQueryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("deploymentId", deploymentId))
                .must(QueryBuilders.boolQuery()
                        .should(QueryBuilders.rangeQuery("startTime").gte(searchRequest.getFromDate()).lte(searchRequest.getToDate()))
                        .should(QueryBuilders.rangeQuery("endTime").gte(searchRequest.getFromDate()).lte(searchRequest.getToDate()))
                        .should(QueryBuilders.boolQuery()
                                .must(QueryBuilders.rangeQuery("startTime").lt(searchRequest.getFromDate()))
                                .must(QueryBuilders.rangeQuery("endTime").gt(searchRequest.getToDate())))
                        .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("endTime")))
                );
        //log.info(windowQueryBuilder.toString(true));

        // For this deploymentId, all events that start in the range
        BoolQueryBuilder eventQueryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("deploymentId", deploymentId))
                .must(QueryBuilders.rangeQuery("startTime").gte(searchRequest.getFromDate()).lte(searchRequest.getToDate()));
        //log.info(eventQueryBuilder.toString(true));

        IESSearchQueryBuilderHelper<TimelineRuleConditionEvent> ruleConditionEventQueryBuilder = RMSDao.buildSearchQuery(TimelineRuleConditionEvent.class, searchRequest.getQuery()).setFilters(eventQueryBuilder).prepareSearch();
        GetMultipleDataResult<TimelineRuleConditionEvent> timelineRuleConditionEventGetMultipleDataResult = ruleConditionEventQueryBuilder.search(0, 10000);
        AbstractTimelineEvent[] timelineRuleConditionEvents = timelineRuleConditionEventGetMultipleDataResult.getData();
        log.trace("{} timelineRuleConditionEvents identified", timelineRuleConditionEvents.length);
        int eventCount = timelineRuleConditionEvents.length;

        IESSearchQueryBuilderHelper<TimelineWindow> timelineWindowQueryBuilder = RMSDao.buildSearchQuery(TimelineWindow.class, searchRequest.getQuery()).setFilters(windowQueryBuilder).prepareSearch();
        GetMultipleDataResult<TimelineWindow> timelineWindowGetMultipleDataResult = timelineWindowQueryBuilder.search(0, 10000);
        AbstractTimelineEvent[] timelineWindows = timelineWindowGetMultipleDataResult.getData();
        log.trace("{} timelineWindows identified", timelineWindows.length);
        eventCount += timelineWindows.length;

        IESSearchQueryBuilderHelper<TimelineAction> timelineActionQueryBuilder = RMSDao.buildSearchQuery(TimelineAction.class, searchRequest.getQuery()).setFilters(windowQueryBuilder).prepareSearch();
        GetMultipleDataResult<TimelineAction> timelineActionGetMultipleDataResult = timelineActionQueryBuilder.search(0, 10000);
        AbstractTimelineEvent[] timelineActions = timelineActionGetMultipleDataResult.getData();
        log.trace("{} timelineActions identified", timelineActions.length);
        eventCount += timelineActions.length;

        AbstractTimelineEvent[] triggerEvents = null;
        if (searchRequest.isIncludeTriggerEvents()) {
            IESSearchQueryBuilderHelper<TriggerEvent> triggerEventQueryBuilder = RMSDao.buildSearchQuery(TriggerEvent.class, searchRequest.getQuery()).setFilters(eventQueryBuilder).prepareSearch();
            GetMultipleDataResult<TriggerEvent> triggerEventGetMultipleDataResult = triggerEventQueryBuilder.search(0, 10000);
            triggerEvents = triggerEventGetMultipleDataResult.getData();
            log.trace("{} triggerEvents identified", triggerEvents.length);
            eventCount += triggerEvents.length;
        }

        AbstractTimelineEvent[] events = new AbstractTimelineEvent[eventCount];
        System.arraycopy(timelineRuleConditionEvents, 0, events, 0, timelineRuleConditionEvents.length);
        System.arraycopy(timelineWindows, 0, events, 0 + timelineRuleConditionEvents.length, timelineWindows.length);
        System.arraycopy(timelineActions, 0, events, 0 + timelineRuleConditionEvents.length + timelineWindows.length, timelineActions.length);
        if (searchRequest.isIncludeTriggerEvents()) {
            System.arraycopy(triggerEvents, 0, events, 0 + timelineRuleConditionEvents.length + timelineWindows.length + timelineActions.length, triggerEvents.length);
        }
        return RestResponseBuilder.<AbstractTimelineEvent[]> builder().data(events).build();

    }


}
