package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.example.embabelagent.config.RemotePlatformAgentProperties;
import com.example.embabelagent.tool.ProductBusinessTools;
import com.example.embabelagent.tool.ProductBusinessTools.LocalProductQueryTools;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Agent(description = "组合本地业务查询和MCP规则文件检查商品视频")
public class RemotePlatformToolAgent {

    private final ProductBusinessTools businessTools;

    private final RemotePlatformAgentProperties properties;

    public RemotePlatformToolAgent(
            ProductBusinessTools businessTools,
            RemotePlatformAgentProperties properties) {
        this.businessTools = businessTools;
        this.properties = properties;
    }

    @AchievesGoal(description = "给出同时包含本地业务数据和MCP规则文件的发布检查结果")
    @Action(description = "调用本地工具和MCP文件工具检查商品视频")
    public RemotePlatformPublishAssessment checkPublishReadiness(
            ProductPublishRequest request,
            Ai ai) {

        LocalProductQueryTools localTools =
                businessTools.localForTenant(
                        request.tenantId());

        RemotePublishDecision decision = ai
                .withDefaultLlm()
                .withToolObject(localTools)
                .withToolGroup(
                        "platform-rules",
                        "read_text_file")
                .createObject(
                        buildPrompt(request),
                        RemotePublishDecision.class);

        return new RemotePlatformPublishAssessment(
                request.productId(),
                request.storeId(),
                request.platform(),
                request.durationSeconds(),
                decision,
                localTools.calledTools());
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
                            duration.replaceAll(
                                    "[^0-9]",
                                    "")));
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
        int contentStart =
                start + startMarker.length();
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

    public record ProductPublishRequest(
            @NotBlank String tenantId,
            @NotBlank String productId,
            @NotBlank String storeId,
            @NotBlank String platform,
            @Min(5) int durationSeconds) {
    }

    public record RemotePublishDecision(
            boolean publishable,
            @NotBlank String conclusion,
            @NotEmpty List<@NotBlank String> evidence,
            @NotEmpty List<@NotBlank String> blockers,
            @NotBlank String platformRuleSource,
            @NotBlank String platformRuleVersion) {
    }

    public record RemotePlatformPublishAssessment(
            @NotBlank String productId,
            @NotBlank String storeId,
            @NotBlank String platform,
            int durationSeconds,
            @Valid @NotNull RemotePublishDecision decision,
            @NotEmpty List<@NotBlank String> localCalledTools) {
    }
}
