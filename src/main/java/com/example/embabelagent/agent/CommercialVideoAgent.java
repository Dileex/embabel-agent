package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Provided;
import com.embabel.agent.api.annotation.State;
import com.embabel.agent.api.common.Ai;
import com.example.embabelagent.config.CommercialVideoAgentProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Agent(description = "按商品资料、视频脚本、分镜和检查阶段生成商业视频制作方案")
public class CommercialVideoAgent {

    private final CommercialVideoAgentProperties properties;

    public CommercialVideoAgent(
            CommercialVideoAgentProperties properties) {
        this.properties = properties;
    }

    @Action(description = "整理商品事实、目标人群和商业视频要求")
    public ProductBriefReady prepareBrief(
            CommercialVideoRequest request,
            Ai ai) {
        ProductBrief brief = ai
                .withDefaultLlm()
                .createObject(
                        properties.prepareBrief()
                                .replace(
                                        "{productName}",
                                        request.productName())
                                .replace(
                                        "{productFacts}",
                                        request.productFacts())
                                .replace(
                                        "{audience}",
                                        request.audience())
                                .replace(
                                        "{platform}",
                                        request.platform())
                                .replace(
                                        "{durationSeconds}",
                                        Integer.toString(
                                                request.durationSeconds())),
                        ProductBrief.class);
        return new ProductBriefReady(
                request,
                brief,
                List.of("商品资料已整理"));
    }

    public static CommercialVideoRequest parseRequest(
            String rawMessage) {
        String message =
                rawMessage == null ? "" : rawMessage.strip();
        String duration = readSection(
                message,
                "视频时长：",
                null);
        try {
            return new CommercialVideoRequest(
                    readSection(
                            message,
                            "商品名称：",
                            "商品资料："),
                    readSection(
                            message,
                            "商品资料：",
                            "目标人群："),
                    readSection(
                            message,
                            "目标人群：",
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

    @State
    public record ProductBriefReady(
            @Valid @NotNull CommercialVideoRequest request,
            @Valid @NotNull ProductBrief brief,
            @NotEmpty List<@NotBlank String> completedStages) {

        @Action(description = "根据商品简报生成商业视频口播脚本")
        public VideoScriptReady writeScript(
                Ai ai,
                @Provided
                CommercialVideoAgentProperties properties) {
            VideoScript script = ai
                    .withDefaultLlm()
                    .createObject(
                            properties.writeScript()
                                    .replace(
                                            "{brief}",
                                            renderBrief(brief)),
                            VideoScript.class);
            return new VideoScriptReady(
                    request,
                    brief,
                    script,
                    appendStage(
                            completedStages,
                            "视频脚本已生成"));
        }
    }

    @State
    public record VideoScriptReady(
            @Valid @NotNull CommercialVideoRequest request,
            @Valid @NotNull ProductBrief brief,
            @Valid @NotNull VideoScript script,
            @NotEmpty List<@NotBlank String> completedStages) {

        @Action(description = "把视频脚本拆成可执行分镜")
        public StoryboardReady createStoryboard(
                Ai ai,
                @Provided
                CommercialVideoAgentProperties properties) {
            Storyboard storyboard = ai
                    .withDefaultLlm()
                    .createObject(
                            properties.createStoryboard()
                                    .replace(
                                            "{brief}",
                                            renderBrief(brief))
                                    .replace(
                                            "{script}",
                                            renderScript(script))
                                    .replace(
                                            "{durationSeconds}",
                                            Integer.toString(
                                                    request.durationSeconds())),
                            Storyboard.class);
            return new StoryboardReady(
                    request,
                    brief,
                    script,
                    storyboard,
                    appendStage(
                            completedStages,
                            "视频分镜已拆分"));
        }
    }

    @State
    public record StoryboardReady(
            @Valid @NotNull CommercialVideoRequest request,
            @Valid @NotNull ProductBrief brief,
            @Valid @NotNull VideoScript script,
            @Valid @NotNull Storyboard storyboard,
            @NotEmpty List<@NotBlank String> completedStages) {

        @AchievesGoal(description = "生成一份经过检查的商业视频制作方案")
        @Action(description = "检查商品事实、镜头时长和素材要求")
        public CommercialVideoPlan finish(
                Ai ai,
                @Provided
                CommercialVideoAgentProperties properties) {
            int totalDurationSeconds =
                    storyboard.shots().stream()
                            .mapToInt(Shot::durationSeconds)
                            .sum();
            PlanReview review = ai
                    .withDefaultLlm()
                    .createObject(
                            properties.reviewPlan()
                                    .replace(
                                            "{brief}",
                                            renderBrief(brief))
                                    .replace(
                                            "{script}",
                                            renderScript(script))
                                    .replace(
                                            "{storyboard}",
                                            renderStoryboard(
                                                    storyboard))
                                    .replace(
                                            "{expectedDuration}",
                                            Integer.toString(
                                                    request.durationSeconds()))
                                    .replace(
                                            "{actualDuration}",
                                            Integer.toString(
                                                    totalDurationSeconds)),
                            PlanReview.class);
            return new CommercialVideoPlan(
                    request,
                    brief,
                    script,
                    storyboard,
                    review,
                    totalDurationSeconds,
                    appendStage(
                            completedStages,
                            "制作方案已检查"));
        }
    }

    private static List<String> appendStage(
            List<String> stages,
            String nextStage) {
        List<String> result = new ArrayList<>(stages);
        result.add(nextStage);
        return List.copyOf(result);
    }

    private static String renderBrief(ProductBrief brief) {
        return """
                商品名称：%s
                目标人群：%s
                发布平台：%s
                目标时长：%d秒
                卖点：%s
                事实边界：%s
                """.formatted(
                brief.productName(),
                brief.audience(),
                brief.platform(),
                brief.durationSeconds(),
                String.join("；", brief.sellingPoints()),
                String.join("；", brief.factualBoundaries()));
    }

    private static String renderScript(VideoScript script) {
        return """
                开头：%s
                口播：%s
                收尾：%s
                屏幕文字：%s
                """.formatted(
                script.hook(),
                script.voiceover(),
                script.closing(),
                String.join("；", script.onScreenTexts()));
    }

    private static String renderStoryboard(
            Storyboard storyboard) {
        return storyboard.shots().stream()
                .map(shot -> """
                        镜头%d（%d秒）
                        画面：%s
                        口播：%s
                        字幕：%s
                        素材：%s
                        """.formatted(
                        shot.sequence(),
                        shot.durationSeconds(),
                        shot.visual(),
                        shot.voiceover(),
                        shot.caption(),
                        shot.requiredMaterial()))
                .collect(Collectors.joining("\n"));
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

    public record CommercialVideoRequest(
            @NotBlank String productName,
            @NotBlank String productFacts,
            @NotBlank String audience,
            @NotBlank String platform,
            @Min(5) int durationSeconds) {
    }

    public record ProductBrief(
            @NotBlank String productName,
            @NotBlank String audience,
            @NotBlank String platform,
            @Min(5) int durationSeconds,
            @NotEmpty List<@NotBlank String> sellingPoints,
            @NotEmpty List<@NotBlank String> factualBoundaries) {
    }

    public record VideoScript(
            @NotBlank String hook,
            @NotBlank String voiceover,
            @NotBlank String closing,
            @NotEmpty List<@NotBlank String> onScreenTexts) {
    }

    public record Shot(
            @Min(1) int sequence,
            @Min(1) int durationSeconds,
            @NotBlank String visual,
            @NotBlank String voiceover,
            @NotBlank String caption,
            @NotBlank String requiredMaterial) {
    }

    public record Storyboard(
            @NotEmpty List<@Valid Shot> shots) {
    }

    public record PlanReview(
            boolean ready,
            @NotEmpty List<@NotBlank String> issues) {
    }

    public record CommercialVideoPlan(
            @Valid @NotNull CommercialVideoRequest request,
            @Valid @NotNull ProductBrief brief,
            @Valid @NotNull VideoScript script,
            @Valid @NotNull Storyboard storyboard,
            @Valid @NotNull PlanReview review,
            @Min(1) int totalDurationSeconds,
            @NotEmpty List<@NotBlank String> completedStages) {
    }
}
