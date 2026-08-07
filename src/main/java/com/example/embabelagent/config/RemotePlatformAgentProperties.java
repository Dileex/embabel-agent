package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.remote-platform-agent.prompts")
public record RemotePlatformAgentProperties(
        String checkPublishReadiness) {
}
