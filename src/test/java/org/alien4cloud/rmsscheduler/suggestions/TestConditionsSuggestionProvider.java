package org.alien4cloud.rmsscheduler.suggestions;

import alien4cloud.model.suggestion.SuggestionRequestContext;
import org.alien4cloud.rmsscheduler.model.MetricEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

public class TestConditionsSuggestionProvider {

    @Test
    public void test_simple() {
        ConditionsSuggestionProvider conditionsSuggestionProvider = new ConditionsSuggestionProvider();

        conditionsSuggestionProvider.addDSL("Le dernier \"{metric_label}\" connu est {operator} {metric_value}", "");

        Collection<String> suggestions = conditionsSuggestionProvider.getSuggestions("", new SuggestionRequestContext());
        // 1 phrase, 6 operators -> 7 suggestions
        Assert.assertEquals(7, suggestions.size());
    }

    @Test
    public void test_2_phrases() {
        ConditionsSuggestionProvider conditionsSuggestionProvider = new ConditionsSuggestionProvider();

        conditionsSuggestionProvider.addDSL("Le dernier \"{metric_label}\" connu est {operator} {metric_value}", "");
        conditionsSuggestionProvider.addDSL("Le dernier \"{metric_label}\" connu est compris entre {metric_value} et {metric_value}", "");
//        MetricEvent metricEvent = new MetricEvent();
//        metricEvent.setLabel("toto");
//        metricEvent.setValue("0.010");
//        conditionsSuggestionProvider.addMetricEvent(metricEvent);
        Collection<String> suggestions = conditionsSuggestionProvider.getSuggestions("", new SuggestionRequestContext());
        // 6 operators, 2 phrase (1 without operator)  -> 7 suggestions
        Assert.assertEquals(8, suggestions.size());
    }

    @Test
    public void test_1_phrases_1_metric() {
        ConditionsSuggestionProvider conditionsSuggestionProvider = new ConditionsSuggestionProvider();

        conditionsSuggestionProvider.addDSL("Le dernier {metric_label} connu est {operator} {metric_value}", "");
        MetricEvent metricEvent = new MetricEvent();
        metricEvent.setLabel("toto");
        metricEvent.setValue("0.010");
        conditionsSuggestionProvider.addMetricEvent(metricEvent);
        Collection<String> suggestions = conditionsSuggestionProvider.getSuggestions("", new SuggestionRequestContext());
        // 6 operators, 1 phrase, 1 metric  -> 7+6 suggestions
        Assert.assertEquals(13, suggestions.size());
        Assert.assertTrue(suggestions.contains("Le dernier {metric_label} connu est {operator} {metric_value}"));
        Assert.assertTrue(suggestions.contains("Le dernier {metric_label} connu est == {metric_value}"));
        Assert.assertTrue(suggestions.contains("Le dernier {metric_label} connu est != {metric_value}"));
        Assert.assertTrue(suggestions.contains("Le dernier {metric_label} connu est < {metric_value}"));
        Assert.assertTrue(suggestions.contains("Le dernier {metric_label} connu est > {metric_value}"));
        Assert.assertTrue(suggestions.contains("Le dernier {metric_label} connu est <= {metric_value}"));
        Assert.assertTrue(suggestions.contains("Le dernier {metric_label} connu est >= {metric_value}"));
        Assert.assertTrue(suggestions.contains("Le dernier toto connu est == 0.010"));
        Assert.assertTrue(suggestions.contains("Le dernier toto connu est != 0.010"));
        Assert.assertTrue(suggestions.contains("Le dernier toto connu est < 0.010"));
        Assert.assertTrue(suggestions.contains("Le dernier toto connu est > 0.010"));
        Assert.assertTrue(suggestions.contains("Le dernier toto connu est <= 0.010"));
        Assert.assertTrue(suggestions.contains("Le dernier toto connu est >= 0.010"));
    }
}
