package org.alien4cloud.rmsscheduler.utils;

import org.alien4cloud.rmsscheduler.model.RuleTrigger;
import org.alien4cloud.rmsscheduler.model.RuleTriggerStatus;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.io.ResourceFactory;

import java.io.IOException;
import java.util.Scanner;

public class KieUtils {

    public static String loadResource(String path) throws IOException {
        Resource resource = ResourceFactory.newClassPathResource(path);
        Scanner sc = new Scanner(resource.getInputStream());
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) {
            sb.append(sc.nextLine());
            sb.append("\r\n");
        }
        return sb.toString();
    }

    public static void updateRuleTrigger(KieSession session, RuleTrigger ruleTrigger, FactHandle factHandle, RuleTriggerStatus status) {
        ruleTrigger.setStatus(status);
        session.update(factHandle, ruleTrigger);
    }

}
