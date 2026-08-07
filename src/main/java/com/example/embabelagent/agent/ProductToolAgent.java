package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.example.embabelagent.config.ProductToolAgentProperties;
import com.example.embabelagent.tool.ProductBusinessTools;
import com.example.embabelagent.tool.ProductBusinessTools.ProductQueryTools;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Agent(description = "查询商品业务数据并判断短视频是否具备发布条件")
public class ProductToolAgent {

    private final ProductBusinessTools businessTools;

    private final ProductToolAgentProperties properties;

    public ProductToolAgent(
            ProductBusinessTools businessTools,
            ProductToolAgentProperties properties) {
        this.businessTools = businessTools;
        this.properties = properties;
    }

    @AchievesGoal(description = "给出有业务数据依据的短视频发布检查结果")
    @Action(description = "调用只读业务工具检查商品、库存、店铺和平台规则")
    public ProductPublishAssessment checkPublishReadiness(
            ProductPublishRequest request,
            Ai ai) {

        // tenantId由Java代码绑定，模型不能切换到其他租户。
        ProductQueryTools tools =
                businessTools.forTenant(request.tenantId());

        PublishDecision decision = ai
                .withDefaultLlm()
                // 只有显式加入的对象，里面的@LlmTool才会交给模型。
                .withToolObject(tools)
                .createObject(
                        buildPrompt(request),
                        PublishDecision.class);

        return new ProductPublishAssessment(
                request.productId(),
                request.storeId(),
                request.platform(),
                request.durationSeconds(),
                decision,
                tools.calledTools());
    }

    private String buildPrompt(
            ProductPublishRequest request) {
        return properties.checkPublishReadiness()
                .replace(
                        "{productId}",
                        request.productId())
                .replace(
                        "{storeId}",
                        request.storeId())
                .replace(
                        "{platform}",
                        request.platform())
                .replace(
                        "{durationSeconds}",
                        Integer.toString(
                                request.durationSeconds()));
    }

    public static ProductPublishRequest parseRequest(
            String rawMessage,
            String tenantId) {
        String message =
                rawMessage == null ? "" : rawMessage.strip();
        String duration = readSection(
                message,
                "视频时长：",
                null);
        try {
            return new ProductPublishRequest(
                    tenantId,
                    readSection(
                            message,
                            "商品ID：",
                            "店铺ID："),
                    readSection(
                            message,
                            "店铺ID：",
                            "发布平台："),
                    readSection(
                            message,
                            "发布平台：",
                            "视频时长："),
                    Integer.parseInt(
                            duration.replaceAll("[^0-9]", "")));
        }
        catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "视频时长必须包含整数秒数",
                    ex);
        }
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
                : message.indexOf(endMarker, contentStart);
        if (endMarker != null && end < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "缺少字段：" + endMarker);
        }
        String value =
                message.substring(contentStart, end).strip();
        value = value.replaceFirst("[；;]+$", "").strip();
        if (value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "字段不能为空：" + startMarker);
        }
        return value;
    }

    public record ProductPublishRequest(
            @NotBlank String tenantId,
            @NotBlank String productId,
            @NotBlank String storeId,
            @NotBlank String platform,
            @Min(5) int durationSeconds) {
    }

    public record PublishDecision(
            boolean publishable,
            @NotBlank String conclusion,
            @NotEmpty List<@NotBlank String> evidence,
            @NotEmpty List<@NotBlank String> blockers) {
    }

    public record ProductPublishAssessment(
            @NotBlank String productId,
            @NotBlank String storeId,
            @NotBlank String platform,
            @Min(5) int durationSeconds,
            @Valid @NotNull PublishDecision decision,
            @NotEmpty List<@NotBlank String> calledTools) {
    }
}
