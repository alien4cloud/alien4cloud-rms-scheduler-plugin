package org.alien4cloud.rmsscheduler.dao;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import java.util.Map;

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
}
