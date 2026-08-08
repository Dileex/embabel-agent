package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.support.AgentMetadataReader;
import com.embabel.agent.api.common.ActionContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Agent(description = "协调商品分析、内容生成和质检Agent完成商品内容方案")
public class ProductContentCoordinatorAgent {

    private final com.embabel.agent.core.Agent productBriefAgent;

    private final com.embabel.agent.core.Agent productCopyAgent;

    private final com.embabel.agent.core.Agent productContentReviewAgent;

    public ProductContentCoordinatorAgent(
            ProductBriefAgent productBriefAgent,
            ProductCopyAgent productCopyAgent,
            ProductContentReviewAgent productContentReviewAgent) {
        AgentMetadataReader metadataReader =
                new AgentMetadataReader();
        this.productBriefAgent = toAgent(
                metadataReader,
                productBriefAgent);
        this.productCopyAgent = toAgent(
                metadataReader,
                productCopyAgent);
        this.productContentReviewAgent = toAgent(
                metadataReader,
                productContentReviewAgent);
    }

    @Action(description = "把商品资料交给商品分析Agent")
    public ProductBrief delegateProductAnalysis(
            ProductContentRequest request,
            ActionContext context) {
        return context.asSubProcess(
                ProductBrief.class,
                productBriefAgent);
    }

    @Action(description = "把商品简报交给文案Agent")
    public ProductCopyWork delegateContentCreation(
            ProductContentRequest request,
            ProductBrief brief,
            ActionContext context) {
        return context.asSubProcess(
                ProductCopyWork.class,
                productCopyAgent);
    }

    @Action(description = "把商品资料和文案草稿交给质检Agent")
    public ProductContentReview delegateContentReview(
            ProductContentRequest request,
            ProductBrief brief,
            ProductCopyWork copyWork,
            ActionContext context) {
        return context.asSubProcess(
                ProductContentReview.class,
                productContentReviewAgent);
    }

    @AchievesGoal(description = "返回包含各子Agent产出的商品内容协作结果")
    @Action(description = "汇总商品分析、文案和质检结果")
    public ProductContentPlan assemblePlan(
            ProductContentRequest request,
            ProductBrief brief,
            ProductCopyWork copyWork,
            ProductContentReview review) {
        return new ProductContentPlan(
                request,
                brief,
                copyWork,
                review,
                review.ready(),
                List.of(
                        "商品分析Agent已完成",
                        "文案Agent已完成",
                        "质检Agent已完成"));
    }

    public static ProductContentRequest parseRequest(
            String rawMessage) {
        String message =
                rawMessage == null ? "" : rawMessage.strip();
        return new ProductContentRequest(
                readSection(
                        message,
                        "商品名称：",
                        "已确认信息："),
                readSection(
                        message,
                        "已确认信息：",
                        "目标平台："),
                readSection(
                        message,
                        "目标平台：",
                        "内容要求："),
                readSection(
                        message,
                        "内容要求：",
                        null));
    }

    private static com.embabel.agent.core.Agent toAgent(
            AgentMetadataReader metadataReader,
            Object annotatedAgent) {
        return (com.embabel.agent.core.Agent)
                metadataReader.createAgentMetadata(
                        annotatedAgent);
    }

    private static String readSection(
            String message,
            String startMarker,
            String endMarker) {
        int start = message.indexOf(startMarker);
        if (start < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "缺少字段：" + startMarker);
        }
        int contentStart = start + startMarker.length();
        int end = endMarker == null
                ? message.length()
                : message.indexOf(
                        endMarker,
                        contentStart);
        if (endMarker != null && end < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "缺少字段：" + endMarker);
        }
        String value = message
                .substring(contentStart, end)
                .strip()
                .replaceFirst("[；;]+$", "")
                .strip();
        if (value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "字段不能为空：" + startMarker);
        }
        return value;
    }

    public record ProductContentRequest(
            @NotBlank String productName,
            @NotBlank String confirmedFacts,
            @NotBlank String platform,
            @NotBlank String contentRequirement) {
    }

    public record ProductBrief(
            @NotBlank String productName,
            @NotEmpty List<@NotBlank String> confirmedFacts,
            @NotEmpty List<@NotBlank String> sellingPoints,
            @NotEmpty List<@NotBlank String> factualBoundaries,
            @NotNull List<@NotBlank String> rejectedRequests) {
    }

    public record ProductCopyDraft(
            @NotBlank String title,
            @NotEmpty List<@NotBlank String> sellingPoints,
            @NotBlank String videoScript,
            @NotEmpty List<@NotBlank String> onScreenTexts,
            @NotNull List<@NotBlank String> rejectedClaims,
            @NotBlank String skillName,
            @NotBlank String ruleVersion) {
    }

    public record ProductCopyWork(
            @Valid @NotNull ProductCopyDraft draft,
            @NotEmpty List<@NotBlank String> calledSkillTools) {
    }

    public record ProductContentReview(
            boolean ready,
            @NotEmpty List<@NotBlank String> checkedFacts,
            @NotNull List<@NotBlank String> unsupportedClaims,
            @NotEmpty List<@NotBlank String> issues) {
    }

    public record ProductContentPlan(
            @Valid @NotNull ProductContentRequest request,
            @Valid @NotNull ProductBrief brief,
            @Valid @NotNull ProductCopyWork copyWork,
            @Valid @NotNull ProductContentReview review,
            boolean ready,
            @NotEmpty List<@NotBlank String> collaborationSteps) {
    }
}
