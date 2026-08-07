package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.product-tool-agent.prompts")
public record ProductToolAgentProperties(
        String checkPublishReadiness) {
}
