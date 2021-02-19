package org.alien4cloud.rmsscheduler.dao;

import com.google.common.collect.Maps;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
public class SessionDao {

    private Map<String, SessionHandler> store = Maps.newConcurrentMap();

    public void create(SessionHandler session) {
        store.put(session.getId(), session);
    }

    public SessionHandler get(String id) {
        return store.get(id);
    }

    public SessionHandler delete(SessionHandler session) {
        return store.remove(session.getId());
    }

    public Collection<SessionHandler> list() {
        return store.values();
    }

}
