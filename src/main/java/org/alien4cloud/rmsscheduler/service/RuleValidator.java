package org.alien4cloud.rmsscheduler.service;

import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.internal.utils.KieHelper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * In charge of validating rule statements.
 * Rule statement should be validated when modifier process RMS rule and at rule edition stage (TODO).
 *
 * TODO: should also verify timer config (not only condition statements) ...
 * TODO: merge with RuleGenerator then remove
 */
@Service
@Slf4j
public class RuleValidator {

    private String ruleCompileTemplate;
    private String ruleCompileDsl;

    @PostConstruct
    public void init() throws IOException {
        this.ruleCompileTemplate = KieUtils.loadResource("rules/drools-poc.dsrlt");
        this.ruleCompileDsl = KieUtils.loadResource("rules/schedule-workflow.dsl");
        log.debug("DSL Loaded : {}", this.ruleCompileDsl);
    }

    /**
     * TODO: better return object to embed errors.
     */
    public boolean verify(String statement) {
        log.debug("About to verify rule statement: " + statement);
        KieHelper kieHelper = new KieHelper();
        String rule = String.format(this.ruleCompileTemplate, statement);
        kieHelper.addContent(this.ruleCompileDsl, ResourceType.DSL);
        kieHelper.addContent(rule, ResourceType.DSLR);
        Results results = kieHelper.verify();
        log.warn("Rule compilation result" + results);
        return !results.hasMessages(Message.Level.ERROR);
    }

}
