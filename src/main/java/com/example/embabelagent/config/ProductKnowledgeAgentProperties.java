package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.product-knowledge-agent.prompts")
public record ProductKnowledgeAgentProperties(
        String answerWithEvidence) {
}
