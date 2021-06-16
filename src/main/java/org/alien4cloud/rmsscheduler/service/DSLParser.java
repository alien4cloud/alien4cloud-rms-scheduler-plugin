package org.alien4cloud.rmsscheduler.service;

import org.alien4cloud.rmsscheduler.suggestions.ConditionsSuggestionProvider;
import org.drools.compiler.lang.dsl.DSLMapping;
import org.drools.compiler.lang.dsl.DSLMappingEntry;
import org.drools.compiler.lang.dsl.DSLTokenizedMappingFile;
import org.kie.internal.builder.ResultSeverity;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringReader;

/**
 * Responsible of parsing DSLs and send conditions to suggestion provider.
 */
@Component
public class DSLParser {

    @Resource
    private ConditionsSuggestionProvider conditionsSuggestionProvider;

    public void parseDsl(String dslContent) throws DSLParserException {
        DSLTokenizedMappingFile f = new DSLTokenizedMappingFile();
        StringReader reader = new StringReader(dslContent);
        try {
            f.parseAndLoad(reader);
        } catch (IOException e) {
            throw new DSLParserException("Not able to parse DSL", e);
        }
        if (f.getErrors() != null && !f.getErrors().isEmpty()) {
            boolean hasError = f.getErrors().stream().anyMatch(r -> r.getSeverity() == ResultSeverity.ERROR);
            if (hasError) {
                throw new DSLParserException("The DSL has errors");
            }
        }
        DSLMapping mapping = f.getMapping();
        for (DSLMappingEntry e : mapping.getEntries()) {
            if (e.getSection() == DSLMappingEntry.Section.CONDITION) {
                conditionsSuggestionProvider.addDSL(e.getMappingKey(), e.getMappingValue());
            }
        }
    }

    public static class DSLParserException extends Exception {
        public DSLParserException(String m) {
            super(m);
        }
        public DSLParserException(String m, Exception e) {
            super(m, e);
        }
    }

}
