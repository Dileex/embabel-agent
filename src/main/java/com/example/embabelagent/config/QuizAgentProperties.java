package com.example.embabelagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.quiz-agent.prompts")
public record QuizAgentProperties(
        String extractConcepts,
        String generateQuiz,
        String reviewQuiz) {
}
