package org.alien4cloud.rmsscheduler.service.actions;

import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;

public interface RuleAction<T extends Object> {
    void execute(T ruleTrigger, SessionHandler sessionHandler, FactHandle factHandle);
}
