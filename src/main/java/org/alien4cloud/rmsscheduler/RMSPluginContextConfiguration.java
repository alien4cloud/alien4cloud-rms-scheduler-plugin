package org.alien4cloud.rmsscheduler;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.alien4cloud.rmsscheduler")
public class RMSPluginContextConfiguration {

    //@Bean
    public KieContainer kieContainer() {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/drools-poc.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/drools-poc.dsl"));
        //kieFileSystem.write(ResourceFactory.newClassPathResource("rules/drools-poc.dslr"));
        //kieFileSystem.write(ResourceFactory.newClassPathResource("rules/drools-test.dslr"));
/*        kieFileSystem.write(ResourceFactory.newClassPathResource("com/rule/drools-poc.dsl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource("com/rule/drools-poc-main.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource("com/rule/drools-poc-dep1.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource("com/rule/drools-poc-dep1.dslr"));*/
        KieModuleModel kieModuleModel = kieServices.newKieModuleModel();
        KieBaseModel baseModel = kieModuleModel.newKieBaseModel("defaultKBase")
                .setDefault(true)
                .setEventProcessingMode(EventProcessingOption.STREAM);
        baseModel.newKieSessionModel("defaultKSession")
                .setDefault(true);
        kieFileSystem.writeKModuleXML(kieModuleModel.toXML());
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

}
