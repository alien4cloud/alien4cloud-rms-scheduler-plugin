package org.alien4cloud.rmsscheduler.dao;

import com.google.common.collect.Sets;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RuleDao {

    private Set<Rule> store = Sets.newConcurrentHashSet();

    public void create(final String environmentId, Collection<Rule> rules) {
        Iterator<Rule> stored = store.iterator();
        while (stored.hasNext()) {
            Rule rule = stored.next();
            if (rule.getEnvironmentId().equals(environmentId) && !rule.isHandled()) {
                stored.remove();
            }
        }
        rules.forEach(rule -> {
            store.add(rule);
        });
    }

    public Collection<Rule> handleRules(final String environmentId, final String deploymentId) {
        Collection<Rule> rules = store.stream()
                .filter(rule -> rule.getEnvironmentId().equals(environmentId) && !rule.isHandled())
                .collect(Collectors.toList());
        rules.forEach(rule -> {
            rule.setHandled(true);
            rule.setDeploymentId(deploymentId);
        });
        return rules;
    }

    public void deleteHandledRules(final String deploymentId) {
        Iterator<Rule> stored = store.iterator();
        while (stored.hasNext()) {
            Rule rule = stored.next();
            if (rule.getDeploymentId().equals(deploymentId) && rule.isHandled()) {
                stored.remove();
            }
        }
    }

    public void deleteHandledRule(final String ruleId) {
        Iterator<Rule> stored = store.iterator();
        while (stored.hasNext()) {
            Rule rule = stored.next();
            if (rule.getId().equals(ruleId) && rule.isHandled()) {
                stored.remove();
            }
        }
    }

    public Collection<Rule> list() {
        return store;
    }

}
