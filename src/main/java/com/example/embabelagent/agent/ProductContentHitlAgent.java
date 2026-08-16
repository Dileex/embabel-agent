package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.support.AgentMetadataReader;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.core.hitl.WaitFor;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductContentPlan;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductContentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Agent(description = "生成商品内容并在模拟发布前等待确认")
public class ProductContentHitlAgent {

    private final com.embabel.agent.core.Agent contentCoordinatorAgent;

    public ProductContentHitlAgent(
            ProductContentCoordinatorAgent contentCoordinatorAgent) {
        AgentMetadataReader metadataReader =
                new AgentMetadataReader();
        this.contentCoordinatorAgent =
                (com.embabel.agent.core.Agent)
                        metadataReader.createAgentMetadata(
                                contentCoordinatorAgent);
    }

    @Action(description = "运行商品内容协作流程")
    public ProductContentPlan createPlan(
            ProductContentRequest request,
            ActionContext context) {
        return context.asSubProcess(
                ProductContentPlan.class,
                contentCoordinatorAgent);
    }

    @Action(description = "发布前等待确认")
    public PublishApproval waitForApproval(
            ProductContentPlan plan) {
        if (!plan.ready()) {
            throw new IllegalStateException(
                    "内容质检未通过，不能请求确认");
        }

        // 这里会让当前AgentProcess进入WAITING。
        return WaitFor.confirmation(
                new PublishApproval(plan),
                "商品内容已经生成，是否继续执行模拟发布？");
    }

    @AchievesGoal(description = "返回确认后的模拟发布结果")
    @Action(description = "确认通过后执行模拟发布")
    public HitlPublishResult publish(
            PublishApproval approval) {
        return new HitlPublishResult(
                "PUBLISHED",
                "确认通过，模拟发布成功");
    }

    public record PublishApproval(
            @Valid @NotNull ProductContentPlan plan) {
    }

    public record HitlPublishResult(
            @NotBlank String status,
            @NotBlank String message) {
    }
}
