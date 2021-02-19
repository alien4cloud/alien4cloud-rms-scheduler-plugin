package org.alien4cloud.rmsscheduler.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.rmsscheduler.RMSPluginConfiguration;
import org.alien4cloud.rmsscheduler.dao.DSLDao;
import org.alien4cloud.rmsscheduler.utils.KieUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * In charge of :
 * <ul>
 *     <li>Loading DSL from files</li>
 *     <li>Manage editable DSL from persistence</li>
 * </ul>
 */
@Service
@Slf4j
public class DSLLoader {

    @Resource
    private RMSPluginConfiguration pluginConfiguration;

    private List<String> dslFilesContent;

    @PostConstruct
    public void init() {
        if (pluginConfiguration.getDlsFiles() != null && !pluginConfiguration.getDlsFiles().isEmpty()) {
            loadDslFiles(pluginConfiguration.getDlsFiles());
        }
    }

    private void loadDslFiles(List<String> files) {
        dslFilesContent = Lists.newArrayList();
        files.forEach(filePath -> {
            log.info("Trying to read content from {}", filePath);
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Stream<String> lines = Files.lines(path);
                    StringBuilder sb = new StringBuilder();
                    lines.forEach(s -> {
                        sb.append(s);
                        sb.append("\r\n");
                    });
                    String content = sb.toString();
                    log.info("Adding DSL from file {} : {}", filePath, content);
                    dslFilesContent.add(content);
                } else {
                    // trying to load resource from path
                    String content = KieUtils.loadResource(filePath);
                    log.info("Adding DSL from classpath ressource {} : {}", filePath, content);
                    dslFilesContent.add(content);
                }
            } catch (IOException ioe) {
                log.error("Can't read DSL at {}, ignoring", filePath);
            }
        });
    }

    public List<String> getDSLs() {
        return dslFilesContent;
    }

}
