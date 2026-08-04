package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.code-review-agent.prompts")
public record CodeReviewAgentProperties(String reviewCode) {
}
