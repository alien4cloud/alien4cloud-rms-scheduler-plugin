package org.alien4cloud.rmsscheduler.sensor.zabbix;

import alien4cloud.utils.SpringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.sensor.HttpPoller;
import org.alien4cloud.rmsscheduler.sensor.PollerResponseHandler;
import org.alien4cloud.rmsscheduler.sensor.config.PollerItemConfiguration;
import org.alien4cloud.rmsscheduler.sensor.zabbix.model.*;
import org.alien4cloud.tosca.variable.SpelExpressionProcessor;
import org.springframework.http.*;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Slf4j
public class ZabbixPoller extends HttpPoller {

    private String auth;

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private Map<String, ZabbixItemQuery> queryItemCache = Maps.newHashMap();

    private void authenticate() {
        ZabbixAuthQuery zabbixAuthQuery = new ZabbixAuthQuery(getConfiguration().getUser(), getConfiguration().getPassword());
        try {
            HttpEntity entity = buildHttpEntityWithDefaultHeader(zabbixAuthQuery);
            log.debug("No authentication token available, need to authenticate");
            ListenableFuture<ResponseEntity<ZabbixAuthResponse>> exchange = template.exchange(getConfiguration().getUrl(), HttpMethod.POST, entity, ZabbixAuthResponse.class);
            exchange.addCallback(new ListenableFutureCallback<ResponseEntity<ZabbixAuthResponse>>() {
                @Override
                public void onFailure(Throwable throwable) {
                    log.warn("Failure while authenticating for Zabbix", throwable);
                }

                @Override
                public void onSuccess(ResponseEntity<ZabbixAuthResponse> zabbixAuthResponseResponseEntity) {
                    if (zabbixAuthResponseResponseEntity.hasBody()) {
                        auth = zabbixAuthResponseResponseEntity.getBody().getResult();
                        log.debug("Zabbix auth result : {}", auth);
                        poll();
                    }
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error occurred while building Zabbix REST request", e);
        }
    }

    @Override
    public void poll() {
        if (auth == null) {
            authenticate();
        } else {
            super.poll();
        }
    }

    private ZabbixItemQuery getQueryItem(String itemName, PollerItemConfiguration itemConfig) {
        ZabbixItemQuery queryItem = queryItemCache.get(itemName);
        if (queryItem == null) {
            queryItem = buildQueryItem(itemConfig);
            if (queryItem != null) {
                queryItemCache.put(itemName, queryItem);
            }
        }
        return queryItem;
    }

    private ZabbixItemQuery buildQueryItem(PollerItemConfiguration itemConfig) {
        Map<String, Object> map = null;
        try {
            map = mapper.readValue(itemConfig.getRequest(), Map.class);
        } catch (IOException e) {
            log.warn("Not able to parse item request : {}", itemConfig.getRequest());
            return null;
        }
        List<String> outputList = Lists.newArrayList();
        outputList.add("lastclock");
        outputList.add("lastvalue");
        if (itemConfig.getTags() != null) {
            itemConfig.getTags().keySet().forEach(k -> outputList.add(k));
        }
        map.put("output", outputList);
        ZabbixItemQuery zabbixItemQuery = new ZabbixItemQuery(auth, map);
        return zabbixItemQuery;
    }

    public void pollItem(String itemName, PollerItemConfiguration itemConfig) {
        log.trace("Polling item named {}", itemName);
        ZabbixItemQuery zabbixItemQuery = getQueryItem(itemName, itemConfig);
        if (zabbixItemQuery == null) {
            return;
        }
        zabbixItemQuery.setAuth(auth);
        log.trace("Polling item named <{}> using: {}", itemName, zabbixItemQuery);
        try {
            log.trace("Querying Zabbix using : {}", zabbixItemQuery);
            HttpEntity zabbixItemQueryEntity = buildHttpEntityWithDefaultHeader(zabbixItemQuery);
            ListenableFuture<ResponseEntity<ZabbixItemResponse>> exchange = template.exchange(getConfiguration().getUrl(), HttpMethod.POST, zabbixItemQueryEntity, ZabbixItemResponse.class);
            exchange.addCallback(new ListenableFutureCallback<ResponseEntity<ZabbixItemResponse>>() {
                @Override
                public void onFailure(Throwable throwable) {
                    log.warn("Exception while polling Zabbix item", throwable);
                }

                @Override
                public void onSuccess(ResponseEntity<ZabbixItemResponse> zabbixItemResponseResponseEntity) {
                    handleResponse(itemName, itemConfig, zabbixItemResponseResponseEntity);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error occurred while building Zabbix REST request", e);
        }

    }

    private void handleResponse(String itemName, PollerItemConfiguration itemConfig, ResponseEntity<ZabbixItemResponse> zabbixItemResponseResponseEntity) {
        log.trace("Zabbix response entity: {}", zabbixItemResponseResponseEntity);
        if (zabbixItemResponseResponseEntity.hasBody()) {
            log.trace("Zabbix response entity body: {}", zabbixItemResponseResponseEntity.getBody());
            ZabbixError error = zabbixItemResponseResponseEntity.getBody().getError();
            if (error != null) {
                log.warn("Zabbix API return error code {} ({} : {})", error.getCode(), error.getMessage(), error.getData());
                if (error.getCode() == -32602) {
                    log.warn("Need to re-authenticate to Zabbix API");
                    // re-login error
                    auth = null;
                }
            } else {
                // no error
                Map<String, Object>[] results = zabbixItemResponseResponseEntity.getBody().getResult();
                List<Map<String, Object>> resultsAsList = Lists.newArrayList(results);
                PollerResponseHandler.handleResponse(resultsAsList, item -> parseResult(itemName, item, itemConfig), itemConfig, this);
            }
        } else {
            log.trace("Zabbix response has no body");
        }
    }

    private MetricEvent parseResult(String itemName, Map<String, Object> result, PollerItemConfiguration itemConfig) {
        if (result == null) {
            log.debug("result content is empty, ignoring");
            return null;
        }
        if (!result.containsKey("lastclock")) {
            log.debug("result content has no 'lastclock' entry, ignoring");
            return null;
        }
        if (!result.containsKey("lastvalue")) {
            log.debug("result content has no 'lastvalue' entry, ignoring");
            return null;
        }
        long lastClock = Long.parseLong(result.get("lastclock").toString());
        Calendar lastClockCalendar = Calendar.getInstance();
        // lastclock is unix timestamp so seconds
        lastClockCalendar.setTimeInMillis(lastClock * 1000);

        String valueAsString = result.get("lastvalue").toString();

        return buildMetricEvent(itemName, lastClockCalendar, valueAsString, itemConfig, result);
    }

    protected final HttpEntity<String> buildHttpEntityWithDefaultHeader(Object body) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity(mapper.writeValueAsString(body),headers);
    }
}
