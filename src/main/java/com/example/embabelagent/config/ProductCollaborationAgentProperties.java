package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.product-collaboration-agent.prompts")
public record ProductCollaborationAgentProperties(
        String analyzeProduct,
        String createContent,
        String reviewContent) {
}
