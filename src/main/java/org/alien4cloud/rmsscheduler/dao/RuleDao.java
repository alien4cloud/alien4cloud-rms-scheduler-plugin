package org.alien4cloud.rmsscheduler.dao;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.exception.IndexingServiceException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TODO: Should store handled rules in persistence in order to manage recovery
 */
@Component
public class RuleDao extends ESGenericSearchDAO {

    private Set<Rule> store = Sets.newConcurrentHashSet();

    @PostConstruct
    public void init() {
        try {
            getMappingBuilder().initialize("alien4cloud.paas.yorc.model");
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // Audit trace index
        initIndices("rule", null, Rule.class);
        initCompleted();
    }

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
        // TODO: serialize handled rules
        this.save(rules.toArray());
        return rules;
    }

    public void deleteHandledRules(final String deploymentId) {
        Iterator<Rule> stored = store.iterator();
        while (stored.hasNext()) {
            Rule rule = stored.next();
            if (rule.getDeploymentId().equals(deploymentId) && rule.isHandled()) {
                stored.remove();
                this.delete(Rule.class, rule.getId());
            }
        }
    }

    public Collection<Rule> listHandledRules() {
        Map<String, String[]> filters = Maps.newHashMap();
        return Lists.newArrayList(this.find(Rule.class, filters, Integer.MAX_VALUE).getData());
    }

    public Collection<Rule> list() {
        return store;
    }

}
