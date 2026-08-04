package com.example.embabelagent.agent;

import java.util.List;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.example.embabelagent.config.CodeReviewAgentProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Agent(description = "检查 Java 代码中的缺陷、边界问题和可维护性风险")
public class CodeReviewAgent {

    private final CodeReviewAgentProperties properties;

    public CodeReviewAgent(CodeReviewAgentProperties properties) {
        this.properties = properties;
    }

    @Action
    public CodeReviewRequest extractCodeReviewRequest(UserInput userInput) {
        return new CodeReviewRequest(userInput.getContent().strip());
    }

    @AchievesGoal(description = "返回一份 Java 代码审查报告")
    @Action
    public CodeReviewReport reviewCode(CodeReviewRequest request, Ai ai) {
        String prompt = properties.reviewCode()
                .replace("{request}", request.content());
        return ai.withDefaultLlm().createObject(prompt, CodeReviewReport.class);
    }

    public record CodeReviewRequest(@NotBlank String content) {
    }

    public record CodeReviewReport(
            @NotBlank String summary,
            @NotNull List<@Valid CodeFinding> findings) {
    }

    public record CodeFinding(
            @NotBlank String severity,
            @NotBlank String problem,
            @NotBlank String suggestion) {
    }

}
