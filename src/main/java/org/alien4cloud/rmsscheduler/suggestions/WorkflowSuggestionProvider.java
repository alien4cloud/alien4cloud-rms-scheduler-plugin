package org.alien4cloud.rmsscheduler.suggestions;

import alien4cloud.model.suggestion.SuggestionContextType;
import alien4cloud.model.suggestion.SuggestionRequestContext;
import alien4cloud.suggestions.ISimpleSuggestionPluginProvider;
import alien4cloud.topology.TopologyServiceCore;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.EditorService;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * In charge of providing DSL conditions as suggestions for the end-user
 * when s.he fill a <code>org.alien4cloud.rmsscheduling.policies.RMSScheduleWorkflowPolicy</code>.
 */
@Component("workflow-suggestion-provider")
@Slf4j
public class WorkflowSuggestionProvider implements ISimpleSuggestionPluginProvider {

    @Resource
    private EditorService editorService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Override
    public Collection<String> getSuggestions(String input, SuggestionRequestContext context) {
        String topologyId = context.getData().getTopologyId();
        if (StringUtils.isNotEmpty(topologyId)) {
            if (context.getType() == SuggestionContextType.TopologyEdit) {
                Topology topology = editorService.getTopology(topologyId);
                return topology.getWorkflows().keySet();
            } else {
                Topology topology = topologyServiceCore.getTopology(topologyId);
                return topology.getWorkflows().keySet();
            }
        } else {
            return Lists.newArrayList("run", "install", "uninstall");
        }
    }
}
