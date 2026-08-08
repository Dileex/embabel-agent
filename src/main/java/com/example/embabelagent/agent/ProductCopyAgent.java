package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductBrief;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductContentRequest;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductCopyDraft;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductCopyWork;
import com.example.embabelagent.config.ProductCollaborationAgentProperties;
import com.example.embabelagent.skill.ProductContentSkills;
import com.example.embabelagent.skill.ProductContentSkills.SkillSession;

@Agent(description = "根据商品简报和商品内容Skill生成短视频文案")
public class ProductCopyAgent {

    private final ProductContentSkills productContentSkills;

    private final ProductCollaborationAgentProperties properties;

    public ProductCopyAgent(
            ProductContentSkills productContentSkills,
            ProductCollaborationAgentProperties properties) {
        this.productContentSkills =
                productContentSkills;
        this.properties = properties;
    }

    @AchievesGoal(description = "返回遵循商品内容Skill的文案草稿")
    @Action(description = "根据商品简报生成标题、卖点和视频脚本")
    public ProductCopyWork createContent(
            ProductContentRequest request,
            ProductBrief brief,
            Ai ai) {
        SkillSession skillSession =
                productContentSkills.newSession();
        ProductCopyDraft draft = ai
                .withDefaultLlm()
                .withReferences(
                        skillSession.references())
                .createObject(
                        properties.createContent()
                                .replace(
                                        "{productName}",
                                        request.productName())
                                .replace(
                                        "{platform}",
                                        request.platform())
                                .replace(
                                        "{contentRequirement}",
                                        request.contentRequirement())
                                .replace(
                                        "{brief}",
                                        renderBrief(brief)),
                        ProductCopyDraft.class);
        return new ProductCopyWork(
                draft,
                skillSession.calledTools());
    }

    private static String renderBrief(
            ProductBrief brief) {
        return """
                商品名称：%s
                已确认事实：%s
                可用卖点：%s
                事实边界：%s
                已拒绝要求：%s
                """.formatted(
                brief.productName(),
                String.join("；", brief.confirmedFacts()),
                String.join("；", brief.sellingPoints()),
                String.join("；", brief.factualBoundaries()),
                String.join("；", brief.rejectedRequests()));
    }
}
