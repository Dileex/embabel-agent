package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.SupplierActionContext;
import com.embabel.agent.api.common.TransformationActionContext;
import com.embabel.agent.api.common.workflow.control.ResultList;
import com.embabel.agent.api.common.workflow.control.ScatterGatherBuilder;
import com.embabel.agent.api.common.workflow.loop.RepeatUntilActionContext;
import com.embabel.agent.api.common.workflow.loop.RepeatUntilBuilder;
import com.example.embabelagent.config.ParallelIncidentAgentProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Agent(description = "并行分析线上故障的日志、监控指标和最近变更，再生成处理建议")
public class ParallelIncidentAgent {

    private final ParallelIncidentAgentProperties properties;

    public ParallelIncidentAgent(
            ParallelIncidentAgentProperties properties) {
        this.properties = properties;
    }

    @AchievesGoal(description = "生成一份包含三类证据和处理建议的线上故障分析报告")
    @Action
    public IncidentAnalysisReport analyze(
            IncidentRequest request,
            ActionContext context) {
        List<Function<
                SupplierActionContext<AnalysisPart>,
                AnalysisPart>> tasks = List.of(
                taskContext -> analyzePart(
                        AnalysisTask.LOG,
                        properties.analyzeLogs(),
                        request,
                        taskContext),
                taskContext -> analyzePart(
                        AnalysisTask.METRICS,
                        properties.analyzeMetrics(),
                        request,
                        taskContext),
                taskContext -> analyzePart(
                        AnalysisTask.RECENT_CHANGE,
                        properties.analyzeRecentChange(),
                        request,
                        taskContext));

        IncidentEvidence evidence = ScatterGatherBuilder
                .returning(IncidentEvidence.class)
                .fromElements(AnalysisPart.class)
                .withGenerators(tasks)
                .consolidatedBy(joinContext ->
                        collectEvidence(
                                joinContext.getInput().getResults(),
                                joinContext))
                .asSubProcess(context);

        ReviewedIncidentReport reviewed = RepeatUntilBuilder
                .returning(ReviewedIncidentReport.class)
                .consuming(IncidentRequest.class)
                .withMaxIterations(3)
                .repeating(loopContext ->
                        generateAndEvaluate(
                                request,
                                evidence,
                                loopContext))
                .until(loopContext ->
                        loopContext.lastAttempt() != null
                                && loopContext
                                        .lastAttempt()
                                        .qualityScore() >= 0.9)
                .asSubProcess(context);
        return reviewed.report();
    }

    public static IncidentRequest parseIncidentRequest(String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.strip();
        return new IncidentRequest(
                readSection(message, "故障现象：", "监控指标："),
                readSection(message, "监控指标：", "最近变更："),
                readSection(message, "最近变更：", "日志片段："),
                readSection(
                        message,
                        "日志片段：",
                        message.contains("质量要求：")
                                ? "质量要求："
                                : null),
                message.contains("质量要求：")
                        ? readSection(message, "质量要求：", null)
                        : "没有额外质量要求");
    }

    private AnalysisPart analyzePart(
            AnalysisTask task,
            String promptTemplate,
            IncidentRequest request,
            SupplierActionContext<AnalysisPart> context) {
        long startedAt = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        AnalysisContent content = context.ai()
                .withDefaultLlm()
                .createObject(
                        fillRequest(promptTemplate, request),
                        AnalysisContent.class);
        long finishedAt = System.currentTimeMillis();
        return new AnalysisPart(
                task,
                content.items(),
                content.conclusion(),
                content.flagged(),
                startedAt,
                finishedAt,
                threadName);
    }

    private IncidentEvidence collectEvidence(
            List<AnalysisPart> parts,
            TransformationActionContext<
                    ResultList<AnalysisPart>,
                    IncidentEvidence> context) {
        Map<AnalysisTask, AnalysisPart> byTask =
                new EnumMap<>(AnalysisTask.class);
        for (AnalysisPart part : parts) {
            byTask.put(part.task(), part);
        }

        AnalysisPart logs = requirePart(byTask, AnalysisTask.LOG);
        AnalysisPart metrics = requirePart(byTask, AnalysisTask.METRICS);
        AnalysisPart recentChange =
                requirePart(byTask, AnalysisTask.RECENT_CHANGE);

        return new IncidentEvidence(
                List.of(logs, metrics, recentChange),
                summarizeExecution(parts));
    }

    private ReviewedIncidentReport generateAndEvaluate(
            IncidentRequest request,
            IncidentEvidence evidence,
            RepeatUntilActionContext<
                    IncidentRequest,
                    ReviewedIncidentReport> context) {
        int attempt = context.getHistory().attemptCount() + 1;
        String previousFeedback = context.lastAttempt() == null
                ? "首次生成"
                : String.join(
                        "；",
                        context.lastAttempt().qualityIssues());
        IncidentConclusion conclusion = context.ai()
                .withDefaultLlm()
                .createObject(
                        properties.consolidateReport()
                                .replace("{symptom}", request.symptom())
                                .replace(
                                        "{evidence}",
                                        renderEvidence(evidence.parts()))
                                .replace(
                                        "{qualityRequirement}",
                                        request.qualityRequirement())
                                .replace(
                                        "{feedback}",
                                        previousFeedback),
                        IncidentConclusion.class);

        IncidentAnalysisReport draft = new IncidentAnalysisReport(
                request.symptom(),
                conclusion.summary(),
                conclusion.probableCause(),
                requirePart(
                        evidence.parts(),
                        AnalysisTask.LOG).items(),
                requirePart(
                        evidence.parts(),
                        AnalysisTask.METRICS).items(),
                requirePart(
                        evidence.parts(),
                        AnalysisTask.RECENT_CHANGE).items(),
                conclusion.immediateActions(),
                conclusion.verificationSteps(),
                evidence.parts(),
                evidence.execution(),
                attempt,
                previousFeedback,
                0.0,
                List.of("等待评估"));
        ReportEvaluation evaluation = context.ai()
                .withDefaultLlm()
                .createObject(
                        properties.evaluateReport()
                                .replace("{symptom}", request.symptom())
                                .replace(
                                        "{qualityRequirement}",
                                        request.qualityRequirement())
                                .replace(
                                        "{evidence}",
                                        renderEvidence(evidence.parts()))
                                .replace(
                                        "{report}",
                                        renderReport(draft)),
                        ReportEvaluation.class);
        double score = Math.max(0.0, Math.min(1.0, evaluation.score()));
        IncidentAnalysisReport evaluatedReport =
                new IncidentAnalysisReport(
                        draft.symptom(),
                        draft.summary(),
                        draft.probableCause(),
                        draft.logFindings(),
                        draft.metricFindings(),
                        draft.recentChangeFindings(),
                        draft.immediateActions(),
                        draft.verificationSteps(),
                        draft.parts(),
                        draft.execution(),
                        draft.generationAttempt(),
                        draft.revisionFeedback(),
                        score,
                        evaluation.issues());
        return new ReviewedIncidentReport(
                evaluatedReport,
                score,
                evaluation.issues());
    }

    private static String fillRequest(
            String promptTemplate,
            IncidentRequest request) {
        return promptTemplate
                .replace("{symptom}", request.symptom())
                .replace("{metrics}", request.metrics())
                .replace("{recentChange}", request.recentChange())
                .replace("{logs}", request.logs());
    }

    private static String renderEvidence(List<AnalysisPart> parts) {
        return parts.stream()
                .map(part -> """
                        [%s]
                        证据：%s
                        小结：%s
                        是否发现异常：%s
                        """.formatted(
                        part.task().displayName(),
                        String.join("；", part.items()),
                        part.conclusion(),
                        part.flagged()))
                .collect(Collectors.joining("\n"));
    }

    private static String renderReport(IncidentAnalysisReport report) {
        return """
                总结：%s
                可能原因：%s
                立即处理：%s
                验证步骤：%s
                """.formatted(
                report.summary(),
                report.probableCause(),
                String.join("；", report.immediateActions()),
                String.join("；", report.verificationSteps()));
    }

    private static ExecutionSummary summarizeExecution(
            List<AnalysisPart> parts) {
        long firstStartedAt = parts.stream()
                .mapToLong(AnalysisPart::startedAt)
                .min()
                .orElseThrow();
        long lastFinishedAt = parts.stream()
                .mapToLong(AnalysisPart::finishedAt)
                .max()
                .orElseThrow();
        long summedTaskMillis = parts.stream()
                .mapToLong(AnalysisPart::durationMillis)
                .sum();
        return new ExecutionSummary(
                lastFinishedAt - firstStartedAt,
                summedTaskMillis,
                hasOverlap(parts));
    }

    private static AnalysisPart requirePart(
            Map<AnalysisTask, AnalysisPart> parts,
            AnalysisTask task) {
        AnalysisPart part = parts.get(task);
        if (part == null) {
            throw new IllegalStateException(
                    "Missing parallel incident part: " + task);
        }
        return part;
    }

    private static AnalysisPart requirePart(
            List<AnalysisPart> parts,
            AnalysisTask task) {
        return parts.stream()
                .filter(part -> part.task() == task)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Missing parallel incident part: " + task));
    }

    private static boolean hasOverlap(List<AnalysisPart> parts) {
        for (int left = 0; left < parts.size(); left++) {
            for (int right = left + 1; right < parts.size(); right++) {
                AnalysisPart first = parts.get(left);
                AnalysisPart second = parts.get(right);
                if (first.startedAt() < second.finishedAt()
                        && second.startedAt() < first.finishedAt()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String readSection(
            String message,
            String label,
            String nextLabel) {
        int start = message.indexOf(label);
        if (start < 0) {
            throw badRequest("缺少请求内容：" + label);
        }
        start += label.length();
        int end = nextLabel == null
                ? message.length()
                : message.indexOf(nextLabel, start);
        if (end < 0) {
            throw badRequest("缺少请求内容：" + nextLabel);
        }
        String value = message.substring(start, end).strip();
        if (value.isBlank()) {
            throw badRequest("请求内容不能为空：" + label);
        }
        return value;
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    public enum AnalysisTask {
        LOG("日志"),
        METRICS("监控指标"),
        RECENT_CHANGE("最近变更");

        private final String displayName;

        AnalysisTask(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public record IncidentRequest(
            @NotBlank String symptom,
            @NotBlank String metrics,
            @NotBlank String recentChange,
            @NotBlank String logs,
            @NotBlank String qualityRequirement) {
    }

    public record AnalysisContent(
            @NotEmpty List<@NotBlank String> items,
            @NotBlank String conclusion,
            boolean flagged) {
    }

    public record AnalysisPart(
            @NotNull AnalysisTask task,
            @NotEmpty List<@NotBlank String> items,
            @NotBlank String conclusion,
            boolean flagged,
            long startedAt,
            long finishedAt,
            @NotBlank String threadName) {

        public long durationMillis() {
            return finishedAt - startedAt;
        }
    }

    public record IncidentConclusion(
            @NotBlank String summary,
            @NotBlank String probableCause,
            @NotEmpty List<@NotBlank String> immediateActions,
            @NotEmpty List<@NotBlank String> verificationSteps) {
    }

    public record ReportEvaluation(
            double score,
            @NotEmpty List<@NotBlank String> issues) {
    }

    public record ReviewedIncidentReport(
            @Valid @NotNull IncidentAnalysisReport report,
            double qualityScore,
            @NotEmpty List<@NotBlank String> qualityIssues) {
    }

    public record IncidentEvidence(
            @NotEmpty List<@Valid AnalysisPart> parts,
            @Valid @NotNull ExecutionSummary execution) {
    }

    public record ExecutionSummary(
            long parallelWallClockMillis,
            long summedTaskMillis,
            boolean overlapObserved) {
    }

    public record IncidentAnalysisReport(
            @NotBlank String symptom,
            @NotBlank String summary,
            @NotBlank String probableCause,
            @NotEmpty List<@NotBlank String> logFindings,
            @NotEmpty List<@NotBlank String> metricFindings,
            @NotEmpty List<@NotBlank String> recentChangeFindings,
            @NotEmpty List<@NotBlank String> immediateActions,
            @NotEmpty List<@NotBlank String> verificationSteps,
            @NotEmpty
            @Size(min = 3, max = 3)
            List<@Valid AnalysisPart> parts,
            @Valid @NotNull ExecutionSummary execution,
            int generationAttempt,
            @NotBlank String revisionFeedback,
            double qualityScore,
            @NotEmpty List<@NotBlank String> qualityIssues) {
    }
}
