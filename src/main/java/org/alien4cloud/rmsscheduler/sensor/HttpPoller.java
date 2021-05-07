package org.alien4cloud.rmsscheduler.sensor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.alien4cloud.rmsscheduler.sensor.config.AggregationFunction;
import org.alien4cloud.rmsscheduler.sensor.config.PollerItemConfiguration;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.web.client.AsyncRestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Map;

@Slf4j
public abstract class HttpPoller extends ScheduledPoller {

    protected final ObjectMapper mapper = new ObjectMapper();

    protected AsyncRestTemplate template;

    private CloseableHttpAsyncClient httpclient;

    @Getter
    @Setter
    private String name;

    public void init() {
        super.init();
        if (this.getConfiguration().getUrl() != null) {
            try {
                httpclient = getHttpClient();
                HttpComponentsAsyncClientHttpRequestFactory factory = new HttpComponentsAsyncClientHttpRequestFactory(httpclient);
                template = new AsyncRestTemplate(factory);
            } catch (Exception e) {
                log.error("Exception while creating http client", e);
            }
        } else {
            log.warn("Url is null, don't start http client.");
        }
    }

    @Override
    public void poll() {
        log.trace("{}: {} items to poll", getName(), getConfiguration().getItems().size());
        getConfiguration().getItems().forEach((itemName, itemConfiguration) -> {
            log.trace("Polling {}", itemConfiguration);
            pollItem(itemName, itemConfiguration);
        });
    }

    private CloseableHttpAsyncClient getHttpClient() throws Exception {
        SSLContextBuilder builder = SSLContexts.custom();
        builder.loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                return true;
            }
        });
        SSLContext sslContext = builder.build();
        SchemeIOSessionStrategy sslioSessionStrategy = new SSLIOSessionStrategy(sslContext,
                new HostnameVerifier(){
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
        Registry<SchemeIOSessionStrategy> sslioSessionRegistry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                .register("https", sslioSessionStrategy)
                .register("http", NoopIOSessionStrategy.INSTANCE)
                .build();
        PoolingNHttpClientConnectionManager ncm  = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor(),sslioSessionRegistry);
        CloseableHttpAsyncClient asyncHttpClient = HttpAsyncClients.custom().setConnectionManager(ncm).build();
        asyncHttpClient.start();
        return asyncHttpClient;
    }

    protected MetricEvent aggregate(MetricEvent agregatedEvent, MetricEvent metricEvent, AggregationFunction aggregationFunction) {
        if (metricEvent.getDoubleValue() == null) {
            return agregatedEvent;
        }
        if (agregatedEvent.getDoubleValue() == null) {
            agregatedEvent.setDoubleValue(metricEvent.getDoubleValue());
        } else {
            switch(aggregationFunction) {
                case min:
                    agregatedEvent.setDoubleValue(Double.min(agregatedEvent.getDoubleValue(), metricEvent.getDoubleValue()));
                    break;
                case max:
                    agregatedEvent.setDoubleValue(Double.max(agregatedEvent.getDoubleValue(), metricEvent.getDoubleValue()));
                    break;
                case sum:
                case avg:
                    agregatedEvent.setDoubleValue(Double.sum(agregatedEvent.getDoubleValue(), metricEvent.getDoubleValue()));
                    break;
            }
        }
        if (metricEvent.getTimestamp().after(agregatedEvent.getTimestamp())) {
            // use the most recent timestamp for event timestamp
            agregatedEvent.setTimestamp(metricEvent.getTimestamp());
        }
        return agregatedEvent;
    }

    protected MetricEvent buildMetricEvent(String itemName, Calendar lastClockCalendar, String valueAsString, PollerItemConfiguration itemConfig, Map<String, ?> mappedItem) {

        if (itemConfig.getTtl() != null) {
            Calendar expirationTime = Calendar.getInstance();
            expirationTime.add(Calendar.SECOND, -1 * itemConfig.getTtl());
            log.trace("Lastclock for item is {}, regarding ttl of {} seconds, expiration time is {}, ", lastClockCalendar.getTime(), itemConfig.getTtl(), expirationTime.getTime());
            if (lastClockCalendar.before(expirationTime)) {
                log.debug("Item is ignored regarding TTL configuration : {} < {} (TTL {} seconds)", lastClockCalendar.getTime(), expirationTime.getTime(), itemConfig.getTtl());
                return null;
            }
        }

        MetricEvent metricEvent = new MetricEvent();
        metricEvent.setLabel(itemName);
        metricEvent.setTimestamp(lastClockCalendar.getTime());
        metricEvent.setValue(valueAsString);
        try {
            metricEvent.setDoubleValue(Double.parseDouble(valueAsString));
        } catch(NumberFormatException nfe) {
            // Not an error
        }
        if (itemConfig.getTags() != null) {
            itemConfig.getTags().forEach((resultEntryName, tagName) -> {
                if (mappedItem.containsKey(resultEntryName)) {
                    if (metricEvent.getTags() == null) {
                        metricEvent.setTags(Maps.newHashMap());
                    }
                    metricEvent.getTags().put(tagName, mappedItem.get(resultEntryName).toString());
                }
            });
        }

        return metricEvent;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (httpclient != null) {
            try {
                httpclient.close();
            } catch (IOException e) {
                log.error("Exception while closing httpclient");
            }
        }
    }



}
