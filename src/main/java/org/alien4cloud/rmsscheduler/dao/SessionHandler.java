package org.alien4cloud.rmsscheduler.dao;

import lombok.Getter;
import lombok.Setter;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

/**
 * Handle session info (can't be serialized).
 */
@Getter
@Setter
public class SessionHandler {
    String id;
    KieSession session;
    FactHandle ticktockerHandler;
}
