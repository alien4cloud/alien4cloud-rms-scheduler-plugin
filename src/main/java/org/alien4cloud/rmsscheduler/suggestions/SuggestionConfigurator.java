package org.alien4cloud.rmsscheduler.suggestions;

import alien4cloud.model.suggestion.SuggestionEntry;
import alien4cloud.suggestions.services.SuggestionService;
import alien4cloud.utils.YamlParserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class SuggestionConfigurator {

    @Resource
    private SuggestionService suggestionService;

    @PostConstruct
    public void init() throws IOException {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("suggestions/rms-suggestion-configuration.yml")) {
            SuggestionEntry[] suggestions = YamlParserUtil.parse(input, SuggestionEntry[].class);
            for (SuggestionEntry suggestionEntry : suggestions) {
                log.info("Adding suggestion entry {}", suggestionEntry.getId());
                suggestionService.createSuggestionEntry(suggestionEntry);
            }
        }
    }

}
