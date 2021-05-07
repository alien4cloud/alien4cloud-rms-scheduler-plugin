package org.alien4cloud.rmsscheduler.sensor.prometeus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.sensor.HttpPoller;
import org.alien4cloud.rmsscheduler.sensor.PollerResponseHandler;
import org.alien4cloud.rmsscheduler.sensor.config.PollerItemConfiguration;
import org.alien4cloud.rmsscheduler.sensor.prometeus.model.PrometeusResponse;
import org.alien4cloud.rmsscheduler.sensor.prometeus.model.PrometeusResponseData;
import org.alien4cloud.rmsscheduler.sensor.prometeus.model.PrometeusResponseDataResultItem;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Calendar;
import java.util.List;

@Slf4j
public class PrometeusPoller extends HttpPoller {

    @Override
    @PostConstruct
    public void init() {
        super.init();
    }

    @Override
    @PreDestroy
    public void shutdown() {
        super.shutdown();
    }

    public void pollItem(String itemName, PollerItemConfiguration itemConfig) {
        log.trace("Polling item named {}: {}", itemName, itemConfig);
        try {
            log.trace("Querying Prometeus using : {}", itemConfig.getRequest());
            HttpEntity queryEntity = buildHttpEntityWithDefaultHeader(itemConfig.getRequest());
            ListenableFuture<ResponseEntity<PrometeusResponse>> exchange = template.exchange(getConfiguration().getUrl(), HttpMethod.POST, queryEntity, PrometeusResponse.class);
            exchange.addCallback(new ListenableFutureCallback<ResponseEntity<PrometeusResponse>>() {
                @Override
                public void onFailure(Throwable throwable) {
                    log.warn("Exception while polling Prometeus item", throwable);
                }

                @Override
                public void onSuccess(ResponseEntity<PrometeusResponse> zabbixItemResponseResponseEntity) {
                    handleResponse(itemName, itemConfig, zabbixItemResponseResponseEntity);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error occurred while building Prometeus REST request", e);
        }

    }

    private void handleResponse(String itemName, PollerItemConfiguration itemConfig, ResponseEntity<PrometeusResponse> prometeusResponseEntity) {
        log.trace("Prometeus response entity: {}", prometeusResponseEntity);
        if (!prometeusResponseEntity.hasBody()) {
            log.trace("Zabbix response has no body, ignoring");
            return;
        }
        log.trace("Prometeus response entity body: {}", prometeusResponseEntity.getBody());
        PrometeusResponse response = prometeusResponseEntity.getBody();
        if (!response.getStatus().equals(PrometeusResponse.STATUS_SUCCESS)) {
            log.warn("Prometeus API return error type {} : {}", response.getErrorType(), response.getError());
            return;
        }
        if (response.getData() == null) {
            log.warn("Prometeus API return no data, ignoring");
            return;
        }
        if (!response.getData().getResultType().equals(PrometeusResponseData.RESULT_TYPE_VECTOR)) {
            log.warn("Prometeus response result type is {}, only 'vector' is managed, ignoring", response.getData().getResultType());
            return;
        }
        if (response.getData().getResult() == null) {
            log.warn("Prometeus response data result is null, ignoring");
            return;
        }

        List<PrometeusResponseDataResultItem> resultList = Lists.newArrayList(response.getData().getResult());
        PollerResponseHandler.handleResponse(resultList, item -> parseResult(itemName, item, itemConfig), itemConfig, this);
    }

    private MetricEvent parseResult(String itemName, PrometeusResponseDataResultItem result, PollerItemConfiguration itemConfig) {
        if (result == null) {
            log.debug("result content is empty, ignoring");
            return null;
        }
        if (result.getValue() == null) {
            log.warn("No value found for prometeus result {}", result);
            return null;
        }
        if (result.getValue().length != 2) {
            log.warn("Prometeus result value length expected 2 but found {} : {}", result.getValue().length, result);
            return null;
        }
        Calendar lastClockCalendar = Calendar.getInstance();
        // the timestamp is the first item of the value array
        try {
            double unixTimestamp = (double)(result.getValue()[0]);
            // lastclock is unix timestamp so seconds
            lastClockCalendar.setTimeInMillis((long)(unixTimestamp * 1000));
        } catch (NumberFormatException e) {
            log.warn("Not able to parse Prometeus result date : {}", result.getValue()[0]);
            return null;
        }
        // the value is the second item of the value array
        String value = result.getValue()[1].toString();
        return buildMetricEvent(itemName, lastClockCalendar, value, itemConfig, result.getMetric());
    }

    protected final HttpEntity<String> buildHttpEntityWithDefaultHeader(String request) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("query", request);
        return new HttpEntity(map, headers);
    }
}
