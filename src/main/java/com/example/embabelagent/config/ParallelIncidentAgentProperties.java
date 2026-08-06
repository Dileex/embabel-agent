package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.parallel-incident-agent.prompts")
public record ParallelIncidentAgentProperties(
        String analyzeLogs,
        String analyzeMetrics,
        String analyzeRecentChange,
        String consolidateReport) {
}
