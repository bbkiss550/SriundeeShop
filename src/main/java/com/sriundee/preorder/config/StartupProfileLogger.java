package com.sriundee.preorder.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

@Component
public class StartupProfileLogger {

    private static final Logger logger = LoggerFactory.getLogger(StartupProfileLogger.class);

    private final Environment environment;

    public StartupProfileLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logRuntimeDatabase() {
        String profiles = Arrays.toString(environment.getActiveProfiles());
        if (profiles.equals("[]")) {
            profiles = Arrays.toString(environment.getDefaultProfiles());
        }

        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        logger.info("Runtime database profile={} database={}", profiles, extractDatabaseName(datasourceUrl));
    }

    private String extractDatabaseName(String datasourceUrl) {
        if (!datasourceUrl.startsWith("jdbc:postgresql://")) {
            return "unknown";
        }

        String uriValue = datasourceUrl.substring("jdbc:".length());
        try {
            String path = URI.create(uriValue).getPath();
            if (path == null || path.length() <= 1) {
                return "unknown";
            }
            return path.substring(1);
        } catch (IllegalArgumentException ex) {
            return "unknown";
        }
    }
}
