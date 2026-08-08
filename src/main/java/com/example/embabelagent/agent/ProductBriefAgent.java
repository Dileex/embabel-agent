package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductBrief;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductContentRequest;
import com.example.embabelagent.config.ProductCollaborationAgentProperties;

@Agent(description = "从已确认的商品资料中整理卖点和事实边界")
public class ProductBriefAgent {

    private final ProductCollaborationAgentProperties properties;

    public ProductBriefAgent(
            ProductCollaborationAgentProperties properties) {
        this.properties = properties;
    }

    @AchievesGoal(description = "返回只包含已确认商品事实的商品简报")
    @Action(description = "分析商品资料并整理真实卖点")
    public ProductBrief analyzeProduct(
            ProductContentRequest request,
            Ai ai) {
        return ai
                .withDefaultLlm()
                .createObject(
                        properties.analyzeProduct()
                                .replace(
                                        "{productName}",
                                        request.productName())
                                .replace(
                                        "{confirmedFacts}",
                                        request.confirmedFacts())
                                .replace(
                                        "{platform}",
                                        request.platform())
                                .replace(
                                        "{contentRequirement}",
                                        request.contentRequirement()),
                        ProductBrief.class);
    }
}
