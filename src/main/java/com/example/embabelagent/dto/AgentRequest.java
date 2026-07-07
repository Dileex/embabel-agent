package com.example.embabelagent.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentRequest(@NotBlank String message) {
}
