package com.example.embabelagent.dto;

public record AgentResponse(
        String mode,
        String processId,
        String agentName,
        String goalName,
        String outputType,
        Object output) {
}
