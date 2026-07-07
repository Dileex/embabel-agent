package com.example.embabelagent.dto;

public record AgentResponse(
        String processId,
        String agentName,
        String outputType,
        Object output) {
}
