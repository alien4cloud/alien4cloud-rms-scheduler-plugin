package org.alien4cloud.rmsscheduler.suggestions;

import alien4cloud.model.suggestion.Suggestion;
import alien4cloud.model.suggestion.SuggestionRequestContext;
import alien4cloud.suggestions.ISimpleSuggestionPluginProvider;
import alien4cloud.suggestions.ISuggestionPluginProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In charge of providing DSL conditions as suggestions for the end-user
 * when s.he fill a <code>org.alien4cloud.rmsscheduling.policies.RMSScheduleWorkflowPolicy</code>.
 */
@Component("rule-condition-suggestion-provider")
@Slf4j
public class ConditionsSuggestionProvider implements ISimpleSuggestionPluginProvider {

    private enum DslToken {
        METRIC_LABEL("{metric_label}", "\\{metric_label\\}"),
        METRIC_VALUE("{metric_value}", "\\{metric_value\\}"),
        OPERATOR("{operator}", "\\{operator\\}");

        public final String label;

        public final String pattern;

        DslToken(String label, String pattern) {
            this.label = label;
            this.pattern = pattern;
        }
    }

    private static final Pattern DSL_PATTERN = Pattern.compile("(\\{metric_value\\}|\\{metric_label\\}|\\{operator\\})");

    private static final Pattern DSL_CODE_PATTERN = Pattern.compile("MetricEvent\\(\\s*label\\s*==\\s*\"(.*)\"");

    private final Map<String, String> conditionDsls = Maps.newConcurrentMap();

    private final Map<String, MetricEvent> metricLabels = Maps.newConcurrentMap();

    private Collection<String> suggestions = Lists.newArrayList();

    private static final String[] OPERATORS = new String[]{"<", ">", "<=", ">=", "!=", "=="};

    public void addDSL(String left, String right) {
        conditionDsls.put(left, right);
        compileSuggestions();
    }

    public void addMetricEvent(MetricEvent metricEvent) {
        if (!metricLabels.containsKey(metricEvent.getLabel())) {
            log.debug("Adding MetricEvent {} to knowledge base", metricEvent);
            metricLabels.put(metricEvent.getLabel(), metricEvent);
            compileSuggestions();
        }
    }

    private void compileSuggestions() {
        Set<String> compiledSuggestions = Sets.newHashSet();
        conditionDsls.forEach((l, r) -> {
            // add DSLs to suggestions
            compiledSuggestions.add(l);
            compiledSuggestions.addAll(generateSentences(l, r));
        });
        log.debug("suggestions now contain {} entries : {}", compiledSuggestions.size(), compiledSuggestions);
        this.suggestions = compiledSuggestions;
    }

    @Override
    public Collection<String> getSuggestions(String input, SuggestionRequestContext context) {
        return suggestions;
    }

    private Collection<String> generateSentences(String left, String right) {
        List<String> result = Lists.newArrayList();
        Matcher matcher = DSL_PATTERN.matcher(left);
        Set<String> matches = Sets.newHashSet();
        while (matcher.find()) {
            String g = matcher.group(1);
            matches.add(g);
        }
        if (matches.contains(DslToken.OPERATOR.label)) {
            generateOperatorSentences(left, result);
        }
        if (!metricLabels.isEmpty()) {
            metricLabels.forEach((l, metricEvent) -> {
                if (matches.contains(DslToken.METRIC_LABEL.label)) {
                    String sentence = left.replaceAll(DslToken.METRIC_LABEL.pattern, l).replaceAll(DslToken.METRIC_VALUE.pattern, metricEvent.getValue());
                    generateOperatorSentences(sentence, result);
                } else if (matches.contains(DslToken.METRIC_VALUE.label)) {
                    // we have a value but no label, let's parse the code to identify a MetricEvent label
                    Matcher rightMatcher = DSL_CODE_PATTERN.matcher(right);
                    while (rightMatcher.find()) {
                        // replace the value using the one found in MetricEvent
                        String metricLabel = rightMatcher.group(1);
                        if (metricLabel.equals(l)) {
                            String sentence = left.replaceAll(DslToken.METRIC_VALUE.pattern, metricEvent.getValue());
                            generateOperatorSentences(sentence, result);
                        }
                    }
                }
            });
        }
        return result;
    }

    private static void generateOperatorSentences(String dsl, Collection<String> sentences) {
        for (String operator : OPERATORS) {
            sentences.add(dsl.replaceAll(DslToken.OPERATOR.pattern, operator));
        }
    }

    // TODO: write unit test
    public static void main(String... args) {

        ConditionsSuggestionProvider conditionsSuggestionProvider = new ConditionsSuggestionProvider();

        String dsl = "Le dernier \"{metric_label}\" connu est {operator} {metric_value}";
        conditionsSuggestionProvider.addDSL(dsl, "");
        conditionsSuggestionProvider.addDSL("Le dernier \"{metric_label}\" connu est compris entre {metric_value} et {metric_value}", "");
        MetricEvent metricEvent = new MetricEvent();
        metricEvent.setLabel("toto");
        metricEvent.setValue("2130");
        conditionsSuggestionProvider.addMetricEvent(metricEvent);
        conditionsSuggestionProvider.compileSuggestions();
        //Collection<String> suggestions = conditionsSuggestionProvider.getSuggestions();
        //suggestions.forEach(s -> System.out.println(s));

/*        Matcher matcher = DSL_PATTERN.matcher(dsl);
        System.out.println("" + matcher.matches());
        Set<String> matches = Sets.newHashSet();
        while (matcher.find()) {
            String g = matcher.group(1);
            System.out.printf("Adding group %s", g).println("");
            matches.add(g);
        }
        if (matches.contains(DslToken.OPERATOR.label)) {

        }

        if (matcher.matches()) {
            System.out.printf("%d", matcher.groupCount()).println("");
            System.out.println(matcher.group(1));
        }*/
    }
}
