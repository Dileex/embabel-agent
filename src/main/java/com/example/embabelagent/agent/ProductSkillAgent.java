package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.example.embabelagent.config.ProductSkillAgentProperties;
import com.example.embabelagent.skill.ProductContentSkills;
import com.example.embabelagent.skill.ProductContentSkills.SkillSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Agent(description = "按商品内容Skill生成可继续审核的短视频文案")
public class ProductSkillAgent {

    private final ProductContentSkills productContentSkills;

    private final ProductSkillAgentProperties properties;

    public ProductSkillAgent(
            ProductContentSkills productContentSkills,
            ProductSkillAgentProperties properties) {
        this.productContentSkills =
                productContentSkills;
        this.properties = properties;
    }

    @AchievesGoal(
            description = "返回遵循商品内容Skill和参考规则的短视频文案")
    @Action(
            description = "按需激活商品短视频Skill并生成文案")
    public SkillProductCopyResult createCopy(
            ProductCopyRequest request,
            Ai ai) {

        SkillSession skillSession =
                productContentSkills.newSession();

        ProductCopyDraft draft = ai
                .withDefaultLlm()
                // 每个Skill先暴露名称和用途，完整说明按需加载。
                .withReferences(
                        skillSession.references())
                .createObject(
                        buildPrompt(request),
                        ProductCopyDraft.class);

        return new SkillProductCopyResult(
                draft,
                skillSession.calledTools());
    }

    private String buildPrompt(
            ProductCopyRequest request) {
        return properties.prompts()
                .createCopy()
                .replace(
                        "{request}",
                        request.message());
    }

    public record ProductCopyRequest(
            @NotBlank String message) {
    }

    public record ProductCopyDraft(
            @NotBlank String title,
            @NotBlank String opening,
            @NotBlank String voiceover,
            @NotEmpty List<@NotBlank String> onScreenTexts,
            @NotNull List<@NotBlank String> rejectedClaims,
            @NotBlank String skillName,
            @NotBlank String ruleVersion) {
    }

    public record SkillProductCopyResult(
            @Valid @NotNull ProductCopyDraft draft,
            @NotEmpty List<@NotBlank String> calledSkillTools) {
    }
}
