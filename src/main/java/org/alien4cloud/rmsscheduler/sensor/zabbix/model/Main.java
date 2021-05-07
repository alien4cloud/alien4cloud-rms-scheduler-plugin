package org.alien4cloud.rmsscheduler.sensor.zabbix.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;

@Slf4j
public class Main {

    ObjectMapper mapper = new ObjectMapper();
    CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();

    public static void main(String... args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"name\":\"mkyong\", \"age\":{\"name\":\"mkyong\", \"age\":\"37\"}}";
        try {
            // convert JSON string to Map
            Map<String, Object> map = mapper.readValue(json, Map.class);
            System.out.println(map);
        } catch (IOException e) {
            e.printStackTrace();
        }

        double d = 100.25;
        log.info("d=" + d/2);

        Date theDate = new Date(1618933455000l);
        log.info("theDate" + theDate);

        Calendar lastClockCalendar = Calendar.getInstance();
        lastClockCalendar.setTimeInMillis(1618933455);

        log.info("Date" + lastClockCalendar.getTime());
        //Main main = new Main();
        //main.doSomething();

    }

    public void doSomething() throws Exception {
        httpclient.start();

        HttpComponentsAsyncClientHttpRequestFactory factory = new HttpComponentsAsyncClientHttpRequestFactory(httpclient);
        AsyncRestTemplate template = new AsyncRestTemplate(factory);

        ZabbixAuthQuery zabbixAuthQuery = new ZabbixAuthQuery("Admin", "zabbix");
        HttpEntity entity = buildHttpEntityWithDefaultHeader(zabbixAuthQuery);
        ListenableFuture<ResponseEntity<ZabbixAuthResponse>> exchange = template.exchange("http://54.216.28.49//zabbix/api_jsonrpc.php", HttpMethod.POST, entity, ZabbixAuthResponse.class);
        ResponseEntity<ZabbixAuthResponse> zabbixAuthResponseResponseEntity = exchange.get();
        String auth = zabbixAuthResponseResponseEntity.getBody().getResult();
        log.info("Auth: " + auth);

/*        Map<String, Object> params = Maps.newHashMap();
        params.put("host", "ip-172-31-5-216.eu-west-1.compute.internal");
        Map<String, Object> search = Maps.newHashMap();
        search.put("key_", "system.cpu.load[percpu,avg15]");
        params.put("search", search);
        ZabbixItemQuery zabbixItemQuery = new ZabbixItemQuery(auth, params);
        HttpEntity zabbixItemQueryEntity = buildHttpEntityWithDefaultHeader(zabbixItemQuery);
        ListenableFuture<ResponseEntity<ZabbixItemResponse>> exchangeItem = template.exchange("http://54.216.28.49//zabbix/api_jsonrpc.php", HttpMethod.POST, zabbixItemQueryEntity, ZabbixItemResponse.class);
        ResponseEntity<ZabbixItemResponse> zabbixItemResponseResponseEntity = exchangeItem.get();
        log.info(zabbixItemResponseResponseEntity.getBody().toString());*/

    }

    private final HttpEntity<String> buildHttpEntityWithDefaultHeader(Object body) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return new org.springframework.http.HttpEntity(mapper.writeValueAsString(body),headers);
    }

}
