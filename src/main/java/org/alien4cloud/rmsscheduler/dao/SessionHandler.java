package org.alien4cloud.rmsscheduler.dao;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handle session info (can't be serialized).
 */
@Getter
@Setter
public class SessionHandler {
    private String id;
    private KieSession session;
    private Map<String, Rule> rules;
    private FactHandle ticktockerHandler;

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private Lock lock;

    public SessionHandler() {
        this.lock = new ReentrantLock();
    }

    public void lock() {
        this.getLock().lock();
    }

    public void unlock() {
        this.getLock().unlock();
    }

}
