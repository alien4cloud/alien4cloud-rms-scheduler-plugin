package org.alien4cloud.rmsscheduler.rest;

import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.dao.RuleDao;
import org.alien4cloud.rmsscheduler.dao.SessionDao;
import org.alien4cloud.rmsscheduler.dao.SessionHandler;
import org.alien4cloud.rmsscheduler.model.*;
import org.apache.lucene.util.NamedThreadFactory;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Just stuff to play with drools and experiment rules.
 * TODO: remove
 */
@Slf4j
//@RestController
//@RequestMapping({ "/rest/rules" })
public class POCControler {

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    private SessionDao sessionDao;

    private String ruleCompileTemplate;
    private String ruleCompileDsl;

    ThreadLocal<Random> random = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };

    ExecutorService executorService = Executors.newCachedThreadPool(new NamedThreadFactory("rules"));

    ScheduledExecutorService schedulerService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("rules-scheduler"));

    private int getRandomBeetween(int min, int max) {
        return random.get().nextInt((max - min) + 1) + min;
    }

    private String loadResource(String path) throws IOException {
        Resource resource = ResourceFactory.newClassPathResource(path);
        Scanner sc = new Scanner(resource.getInputStream());
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) {
            sb.append(sc.nextLine());
            sb.append("\r\n");
        }
        return sb.toString();
    }

    @PostConstruct
    public void init() throws IOException {
        this.ruleCompileTemplate = loadResource("com/rule/drools-poc.dsrlt");
        this.ruleCompileDsl = loadResource("com/rule/drools-poc.dsl");
        log.info("Rule Compile template : {}", this.ruleCompileTemplate);

        this.schedulerService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                log.info("Iterating over sessions");
                sessionDao.list().forEach(sessionHandler -> {
                    KieSession kieSession = sessionHandler.getSession();
                    TickTocker tickTocker = (TickTocker)kieSession.getObject(sessionHandler.getTicktockerHandler());
                    tickTocker.setNow(new Date());
                    kieSession.update(sessionHandler.getTicktockerHandler(), tickTocker);
                    kieSession.fireAllRules();
                });
            }
        },0, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        this.schedulerService.shutdown();
    }

    private KieSession getSession() {
        //get the stateful session
        //KieSession kieSession = kieContainer.newKieSession("rulesSession");


        KieSession kieSession = kieContainer.newKieSession();
        //kieSession.insert(product);
/*        log.info("about to fireUntilHalt");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                log.info("Launching Kies session in Stream mode");
                kieSession.fireUntilHalt();
            }
        });
        log.info("fired");*/
        //kieSession.dispose();
        return kieSession;
    }

    @RequestMapping(value = "/createSesssion/{sessionId}", method = RequestMethod.PUT, produces = "application/json")
    public void createSession(@PathVariable String sessionId) {
        log.info("creating session " + sessionId);
        KieSession kieSession = getSession();
        //kieSession.addEventListener(new DebugRuleRuntimeEventListener());
        kieSession.addEventListener(new DefaultRuleRuntimeEventListener() {
            @Override
            public void objectUpdated(final ObjectUpdatedEvent event) {
                super.objectUpdated(event);
                Object o = event.getObject();

                if (o instanceof RuleTrigger) {
                    log.info("object updated: {}", o.toString());
                    final RuleTrigger r = (RuleTrigger)o;
                    if (r.getStatus() == RuleTriggerStatus.TRIGGERED) {
                        executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    long sleepTime = getRandomBeetween(2, 4) * 1000;
                                    log.info("A4C will now '{}' for '{}' ({}) ... delay {}ms", r.getAction(), r.getRuleId(), r.getEnvironmentId(), sleepTime);
                                    Thread.sleep(sleepTime);
                                    r.setStatus(RuleTriggerStatus.RUNNING);
                                    kieSession.update(event.getFactHandle(), r);

                                    sleepTime = getRandomBeetween(120, 300) * 1000;
                                    log.info("Doing '{}' for '{}' ... duration {}ms", r.getAction(), r.getRuleId(), sleepTime);
                                    Thread.sleep(sleepTime);
                                    if (r.getRuleId().endsWith("Err")) {
                                        r.setStatus(RuleTriggerStatus.ERROR);
                                    } else {
                                        r.setStatus(RuleTriggerStatus.SUCCESS);
                                    }
                                    kieSession.update(event.getFactHandle(), r);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void objectInserted(ObjectInsertedEvent event) {
                super.objectInserted(event);
                Object o = event.getObject();
                log.info("object inserted: {}", o.toString());
            }

            @Override
            public void objectDeleted(ObjectDeletedEvent event) {
                super.objectDeleted(event);
                log.info("Object deleted: {}", event.getOldObject().toString());
            }
        });
        log.info("Session created");
        TickTocker tickTocker = new TickTocker();
        SessionHandler sh = new SessionHandler();
        sh.setId(sessionId);
        sh.setSession(kieSession);
        sh.setTicktockerHandler(kieSession.insert(tickTocker));

        sessionDao.create(sh);
    }

    @RequestMapping(value = "/stopSesssion/{sessionId}", method = RequestMethod.DELETE, produces = "application/json")
    public void stopSession(@PathVariable String sessionId) {
        log.info("stopping session " + sessionId);
        SessionHandler kieSessionHandler = sessionDao.get(sessionId);

        if (kieSessionHandler != null) {
            KieSession kieSession = kieSessionHandler.getSession();
            kieSession.halt();
            kieSession.dispose();
            sessionDao.delete(kieSessionHandler);
        }
    }

    @RequestMapping(value = "/facts/{sessionId}", method = RequestMethod.GET, produces = "text/html")
    public String listFacts(@PathVariable String sessionId) {
        StringBuilder sb = new StringBuilder();
        SessionHandler kieSessionHandler = sessionDao.get(sessionId);
        if (kieSessionHandler != null) {
            KieSession kieSession = kieSessionHandler.getSession();
            sb.append(String.format("<h3>Session %s</h3>", sessionId));
            sb.append(String.format("%d facts<br>", kieSession.getFactCount()));
            log.info("Session {} has {} facts.", sessionId, kieSession.getFactCount());
            for (Object sessionObject : kieSession.getObjects()) {
                log.info(" - {}", sessionObject.toString());
            }
            log.info("Listing all RuleTriggers (using factHandles) :");
            Collection<FactHandle> rules = kieSession.getFactHandles(o -> o.getClass().equals(RuleTrigger.class));
            sb.append(String.format("%d RuleTriggers<br>", rules.size()));
            rules.forEach(factHandle -> {
                log.info(" - {}", kieSession.getObject(factHandle).toString());
            });
//            kieSession.getFactHandles().forEach(factHandle -> {
//                String fact = factHandle.toExternalForm();
//                sb.append(String.format(" - %s\\r\\n", fact));
//                log.info(" - {}", fact);
//            });
        }
        return sb.toString();
    }

    @RequestMapping(value = "/addEvent/{sessionId}/{eventLabel}/{eventValue}", method = RequestMethod.PUT, produces = "application/json")
    public void addEvent(@PathVariable String sessionId, @PathVariable String eventLabel, @PathVariable Long eventValue) {
        SessionHandler sessionHandler = sessionDao.get(sessionId);
        if (sessionHandler != null) {
            MetricEvent m = new MetricEvent(eventLabel, eventValue);
            sessionHandler.getSession().insert(m);
        }
    }

    @RequestMapping(value = "/verify", method = RequestMethod.POST, produces = "application/json")
    public RestResponse<Results> verify(@RequestBody String sentence) {
        log.info("Sentence: " + sentence);
        KieHelper kieHelper = new KieHelper();
        String rule = String.format(this.ruleCompileTemplate, sentence);
        kieHelper.addContent(this.ruleCompileDsl, ResourceType.DSL);
        kieHelper.addContent(rule, ResourceType.DSLR);
        Results results = kieHelper.verify();

        RestResponseBuilder<Results> builder = RestResponseBuilder.<Results>builder();
        builder.data(results);
        if (results.hasMessages(Message.Level.ERROR)) {
            builder.error(new RestError(0, "DSLR Compilation failed"));
        }
        return builder.build();
    }



}
