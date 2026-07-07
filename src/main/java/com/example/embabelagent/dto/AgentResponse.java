package com.example.embabelagent.dto;

public record AgentResponse(
        String processId,
        String outputType,
        Object output) {
}
