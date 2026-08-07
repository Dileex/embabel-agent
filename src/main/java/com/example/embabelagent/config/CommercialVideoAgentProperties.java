package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.commercial-video-agent.prompts")
public record CommercialVideoAgentProperties(
        String prepareBrief,
        String writeScript,
        String createStoryboard,
        String reviewPlan) {
}
