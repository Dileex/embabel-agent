package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.product-skill-agent")
public record ProductSkillAgentProperties(
        String skillPath,
        Prompts prompts) {

    public record Prompts(String createCopy) {
    }
}
