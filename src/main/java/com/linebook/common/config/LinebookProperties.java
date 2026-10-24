package com.linebook.common.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "linebook")
public record LinebookProperties(
        @DefaultValue("Asia/Kolkata") String zone,
        @DefaultValue("Surya Cable Network") String operatorName,
        @DefaultValue("ag-01") String defaultAgentId,
        @DefaultValue("true") boolean currentCycleOnly,
        @DefaultValue Cors cors) {

    public record Cors(
            @DefaultValue({"http://localhost:8080", "http://localhost:5173"}) List<String> allowedOrigins,
            @DefaultValue("1h") Duration maxAge) {
    }
}
