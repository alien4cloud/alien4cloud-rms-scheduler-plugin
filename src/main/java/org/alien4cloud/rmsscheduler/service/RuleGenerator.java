package org.alien4cloud.rmsscheduler.service;

import alien4cloud.tosca.serializer.VelocityUtil;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Rules are templated and generated using velocity.
 */
@Service
@Slf4j
public class RuleGenerator {

    public String generateRule(Rule rule) {
        Map<String, Object> velocityCtx = new HashMap<>();
        velocityCtx.put("rule", rule);

        ClassLoader oldctccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            StringWriter writer = new StringWriter();
            VelocityUtil.generate("rules/schedule-workflow-policy.dslr.vm", writer, velocityCtx);
            return writer.toString();
        } catch (Exception e) {
            log.error("Exception while templating rule " + rule.getId(), e);
            return ExceptionUtils.getFullStackTrace(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldctccl);
        }
    }
}
