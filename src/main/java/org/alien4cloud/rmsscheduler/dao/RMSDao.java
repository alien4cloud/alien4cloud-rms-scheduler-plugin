package org.alien4cloud.rmsscheduler.dao;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.IndexingServiceException;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.model.Dsl;
import org.alien4cloud.rmsscheduler.model.Rule;
import org.alien4cloud.rmsscheduler.model.timeline.TimelineAction;
import org.alien4cloud.rmsscheduler.model.timeline.TimelineRuleConditionEvent;
import org.alien4cloud.rmsscheduler.model.timeline.TimelineWindow;
import org.alien4cloud.rmsscheduler.model.timeline.TriggerEvent;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.*;

/**
 *
 */
@Component
@Slf4j
public class RMSDao extends ESGenericSearchDAO {

    public static final String ADMIN_DSL_ID = "admin";

    @PostConstruct
    public void init() throws IOException {
        try {
            getMappingBuilder().initialize("org.alien4cloud.rmsscheduler.model");
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }
        // Audit trace index
        initIndices("rms_dsl", null, Dsl.class);
        initIndices("rms_rule", "1M", Rule.class);
        initIndices("rms_timeline_rule_condition_event", "1M", TimelineRuleConditionEvent.class);
        initIndices("rms_timeline_window", "1M", TimelineWindow.class);
        initIndices("rms_timeline_action", "1M", TimelineAction.class);
        initIndices("rms_trigger_event", "1M", TriggerEvent.class);
        initCompleted();

        if (this.count(Dsl.class, "", Collections.EMPTY_MAP) == 0) {
            Dsl dsl = new Dsl();
            dsl.setId(ADMIN_DSL_ID);
            dsl.setContent(KieUtils.loadResource("rules/schedule-editor.dsl"));
            //dsl.setDslr(KieUtils.loadResource("rules/schedule-editor.dslr"));
            this.save(dsl);
        }
    }

    public Dsl getAdminDsl() {
        return this.findById(Dsl.class, ADMIN_DSL_ID);
    }

    public String getConcatenatedDsl() {
        GetMultipleDataResult<Dsl> dsls = this.find(Dsl.class, Collections.emptyMap(), Integer.MAX_VALUE);
        StringBuilder sb = new StringBuilder();
        for (Dsl dsl : dsls.getData()) {
            sb.append(dsl.getContent()).append("\r\n");
        }
        return sb.toString();
    }

    public void saveAdminDsl(String content) {
        Dsl dsl = new Dsl();
        dsl.setId(ADMIN_DSL_ID);
        dsl.setContent(content);
        this.save(dsl);
    }

}
