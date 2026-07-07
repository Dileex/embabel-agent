package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.star-news-agent.prompts")
public record StarNewsAgentProperties(
        String extractStarPerson,
        String writeup) {
}
