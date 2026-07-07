package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.policy-agent.prompts")
public record PolicyAgentProperties(
        String extractPolicyQuestion,
        String answer) {
}
