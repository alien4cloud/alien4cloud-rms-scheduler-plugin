package org.alien4cloud.rmsscheduler.service;

import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;

public interface RuleAction {

    void execute(RuleTrigger ruleTrigger, SessionHandler sessionHandler, FactHandle factHandle);

}
