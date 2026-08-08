package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductBrief;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductContentRequest;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductContentReview;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductCopyWork;
import com.example.embabelagent.config.ProductCollaborationAgentProperties;

@Agent(description = "检查商品文案是否超出已确认事实和平台内容要求")
public class ProductContentReviewAgent {

    private final ProductCollaborationAgentProperties properties;

    public ProductContentReviewAgent(
            ProductCollaborationAgentProperties properties) {
        this.properties = properties;
    }

    @AchievesGoal(description = "返回商品文案的事实和发布前质检结果")
    @Action(description = "核对商品事实、高风险说法和文案一致性")
    public ProductContentReview reviewContent(
            ProductContentRequest request,
            ProductBrief brief,
            ProductCopyWork copyWork,
            Ai ai) {
        return ai
                .withDefaultLlm()
                .createObject(
                        properties.reviewContent()
                                .replace(
                                        "{confirmedFacts}",
                                        String.join(
                                                "；",
                                                brief.confirmedFacts()))
                                .replace(
                                        "{factualBoundaries}",
                                        String.join(
                                                "；",
                                                brief.factualBoundaries()))
                                .replace(
                                        "{platform}",
                                        request.platform())
                                .replace(
                                        "{draft}",
                                        renderDraft(copyWork)),
                        ProductContentReview.class);
    }

    private static String renderDraft(
            ProductCopyWork copyWork) {
        return """
                标题：%s
                卖点：%s
                视频脚本：%s
                画面字幕：%s
                已拒绝说法：%s
                """.formatted(
                copyWork.draft().title(),
                String.join(
                        "；",
                        copyWork.draft().sellingPoints()),
                copyWork.draft().videoScript(),
                String.join(
                        "；",
                        copyWork.draft().onScreenTexts()),
                String.join(
                        "；",
                        copyWork.draft().rejectedClaims()));
    }
}
