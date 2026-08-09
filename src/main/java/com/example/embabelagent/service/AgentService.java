package com.example.embabelagent.service;

import com.embabel.agent.api.common.autonomy.AgentProcessExecution;
import com.embabel.agent.api.common.autonomy.Autonomy;
import com.embabel.agent.api.common.autonomy.GoalChoiceApprover;
import com.embabel.agent.api.common.autonomy.GoalSelectionOptions;
import com.embabel.agent.api.common.autonomy.ProcessExecutionException;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.example.embabelagent.agent.CodeReviewAgent.CodeFinding;
import com.example.embabelagent.agent.CodeReviewAgent.CodeReviewReport;
import com.example.embabelagent.agent.CommercialVideoAgent;
import com.example.embabelagent.agent.CommercialVideoAgent.CommercialVideoPlan;
import com.example.embabelagent.agent.CommercialVideoAgent.CommercialVideoRequest;
import com.example.embabelagent.agent.CommercialVideoAgent.Shot;
import com.example.embabelagent.agent.ParallelIncidentAgent;
import com.example.embabelagent.agent.ParallelIncidentAgent.AnalysisPart;
import com.example.embabelagent.agent.ParallelIncidentAgent.IncidentAnalysisReport;
import com.example.embabelagent.agent.ParallelIncidentAgent.IncidentRequest;
import com.example.embabelagent.agent.ProductToolAgent;
import com.example.embabelagent.agent.ProductToolAgent.ProductBusinessAnswer;
import com.example.embabelagent.agent.ProductToolAgent.ProductBusinessQuestion;
import com.example.embabelagent.agent.QuizAgent.QuizPack;
import com.example.embabelagent.agent.QuizAgent.QuizQuestion;
import com.example.embabelagent.dto.AgentResponse;
import com.example.embabelagent.tool.ProductBusinessTools;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgentService {

    private final Autonomy autonomy;

    private final AgentPlatform agentPlatform;

    private final ProductBusinessTools productBusinessTools;

    public AgentService(
            Autonomy autonomy,
            AgentPlatform agentPlatform,
            ProductBusinessTools productBusinessTools) {
        this.autonomy = autonomy;
        this.agentPlatform = agentPlatform;
        this.productBusinessTools = productBusinessTools;
    }

    public AgentResponse focused(String message) {
        AgentInvocation<QuizPack> invocation =
                AgentInvocation.create(
                        agentPlatform,
                        QuizPack.class);
        AgentProcess process =
                invocation.run(
                        new UserInput(message.strip()));
        QuizPack output =
                process.last(QuizPack.class);
        validateOutput(output);
        return response("focused", process, output);
    }

    public AgentResponse closed(String message) {
        AgentProcessExecution execution;
        try {
            execution = autonomy.chooseAndRunAgent(
                    message.strip(),
                    ProcessOptions.DEFAULT);
        }
        catch (ProcessExecutionException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent execution failed", ex);
        }
        Object output = execution.getOutput();
        validateOutput(output);
        return response("closed", execution.getAgentProcess(), output);
    }

    public AgentResponse open(String message) {
        AgentProcessExecution execution;
        try {
            execution = autonomy.chooseAndAccomplishGoal(
                    ProcessOptions.DEFAULT,
                    GoalChoiceApprover.Companion.getAPPROVE_ALL(),
                    agentPlatform,
                    Map.of(
                            "userInput",
                            new UserInput(message.strip())),
                    new GoalSelectionOptions());
        }
        catch (ProcessExecutionException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Goal execution failed", ex);
        }
        Object output = execution.getOutput();
        validateOutput(output);
        return response("open", execution.getAgentProcess(), output);
    }

    public AgentResponse parallel(String message) {
        IncidentRequest request =
                ParallelIncidentAgent.parseIncidentRequest(message);
        AgentInvocation<IncidentAnalysisReport> invocation =
                AgentInvocation.create(
                        agentPlatform,
                        IncidentAnalysisReport.class);
        AgentProcess process =
                invocation.run(
                        request);
        IncidentAnalysisReport output =
                process.last(IncidentAnalysisReport.class);
        validateOutput(output);
        return response("parallel", process, output);
    }

    public AgentResponse videoPlan(String message) {
        CommercialVideoRequest request =
                CommercialVideoAgent.parseRequest(message);
        AgentInvocation<CommercialVideoPlan> invocation =
                AgentInvocation.create(
                        agentPlatform,
                        CommercialVideoPlan.class);
        AgentProcess process = invocation.run(request);
        CommercialVideoPlan output =
                process.last(CommercialVideoPlan.class);
        validateOutput(output);
        return response("video-plan", process, output);
    }

    public AgentResponse productBusinessQuery(
            String tenantId,
            String message) {
        /*
         * 租户权限必须先由应用校验。模型只负责决定查哪些已授权工具，
         * 不能由模型决定自己能访问哪个租户的数据。
         */
        productBusinessTools.validateTenantAccess(tenantId);
        ProductBusinessQuestion request =
                new ProductBusinessQuestion(
                        tenantId,
                        message.strip());
        AgentInvocation<ProductBusinessAnswer> invocation =
                AgentInvocation.create(
                        agentPlatform,
                        ProductBusinessAnswer.class);
        AgentProcess process = invocation.run(request);
        ProductBusinessAnswer output =
                process.last(ProductBusinessAnswer.class);
        validateOutput(output);
        return response(
                "product-business-query",
                process,
                output);
    }

    private AgentResponse response(String mode, AgentProcess process, Object output) {
        String goalName = process.getGoal() == null ? null : process.getGoal().getName();
        return new AgentResponse(
                mode,
                process.getId(),
                process.getAgent().getName(),
                goalName,
                output.getClass().getSimpleName(),
                output);
    }

    private void validateOutput(Object output) {
        if (output instanceof QuizPack quizPack) {
            requireText(quizPack.title(), "QuizPack.title");
            requireQuestions(quizPack.questions());
            requireCompleteSentence(quizPack.review(), "QuizPack.review");
            return;
        }
        if (output instanceof CodeReviewReport report) {
            requireCompleteSentence(report.summary(), "CodeReviewReport.summary");
            requireCodeFindings(report.findings());
            return;
        }
        if (output instanceof IncidentAnalysisReport report) {
            requireCompleteSentence(
                    report.summary(),
                    "IncidentAnalysisReport.summary");
            requireCompleteSentence(
                    report.probableCause(),
                    "IncidentAnalysisReport.probableCause");
            requireTextList(
                    report.logFindings(),
                    "IncidentAnalysisReport.logFindings");
            requireTextList(
                    report.metricFindings(),
                    "IncidentAnalysisReport.metricFindings");
            requireTextList(
                    report.recentChangeFindings(),
                    "IncidentAnalysisReport.recentChangeFindings");
            requireTextList(
                    report.immediateActions(),
                    "IncidentAnalysisReport.immediateActions");
            requireTextList(
                    report.verificationSteps(),
                    "IncidentAnalysisReport.verificationSteps");
            requireTextList(
                    report.qualityIssues(),
                    "IncidentAnalysisReport.qualityIssues");
            requireAnalysisParts(report.parts());
            if (report.generationAttempt() <= 0
                    || report.qualityScore() < 0
                    || report.qualityScore() > 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Agent returned invalid quality evaluation");
            }
            if (report.execution().parallelWallClockMillis() <= 0
                    || report.execution().summedTaskMillis() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Agent returned invalid parallel timing");
            }
            return;
        }
        if (output instanceof CommercialVideoPlan plan) {
            requireText(
                    plan.brief().productName(),
                    "CommercialVideoPlan.brief.productName");
            requireTextList(
                    plan.brief().sellingPoints(),
                    "CommercialVideoPlan.brief.sellingPoints");
            requireTextList(
                    plan.brief().factualBoundaries(),
                    "CommercialVideoPlan.brief.factualBoundaries");
            requireText(
                    plan.script().hook(),
                    "CommercialVideoPlan.script.hook");
            requireText(
                    plan.script().voiceover(),
                    "CommercialVideoPlan.script.voiceover");
            requireTextList(
                    plan.script().onScreenTexts(),
                    "CommercialVideoPlan.script.onScreenTexts");
            requireShots(plan.storyboard().shots());
            requireTextList(
                    plan.review().issues(),
                    "CommercialVideoPlan.review.issues");
            requireTextList(
                    plan.completedStages(),
                    "CommercialVideoPlan.completedStages");
            List<String> expectedStages = List.of(
                    "商品资料已整理",
                    "视频脚本已生成",
                    "视频分镜已拆分",
                    "制作方案已检查");
            if (!expectedStages.equals(
                    plan.completedStages())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Agent returned invalid video stages");
            }
            int calculatedDuration =
                    plan.storyboard().shots().stream()
                            .mapToInt(Shot::durationSeconds)
                            .sum();
            if (calculatedDuration
                    != plan.totalDurationSeconds()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Agent returned invalid video duration");
            }
            return;
        }
        if (output instanceof ProductBusinessAnswer answer) {
            requireText(
                    answer.answer(),
                    "ProductBusinessAnswer.answer");
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unsupported agent output type");
    }

    private void requireShots(List<Shot> shots) {
        if (shots == null || shots.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Agent returned empty storyboard");
        }
        for (int index = 0; index < shots.size(); index++) {
            Shot shot = shots.get(index);
            String prefix =
                    "CommercialVideoPlan.storyboard.shots["
                            + index + "]";
            if (shot.sequence() <= 0
                    || shot.durationSeconds() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Agent returned invalid shot: " + prefix);
            }
            requireText(shot.visual(), prefix + ".visual");
            requireText(shot.voiceover(), prefix + ".voiceover");
            requireText(shot.caption(), prefix + ".caption");
            requireText(
                    shot.requiredMaterial(),
                    prefix + ".requiredMaterial");
        }
    }

    private void requireAnalysisParts(List<AnalysisPart> parts) {
        if (parts == null || parts.size() != 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Agent returned invalid parallel parts");
        }
        for (int index = 0; index < parts.size(); index++) {
            AnalysisPart part = parts.get(index);
            String prefix =
                    "IncidentAnalysisReport.parts[" + index + "]";
            if (part.task() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Agent returned null field: " + prefix + ".task");
            }
            requireTextList(part.items(), prefix + ".items");
            requireCompleteSentence(
                    part.conclusion(),
                    prefix + ".conclusion");
            requireText(part.threadName(), prefix + ".threadName");
            if (part.finishedAt() < part.startedAt()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Agent returned invalid timing: " + prefix);
            }
        }
    }

    private void requireCodeFindings(List<CodeFinding> findings) {
        if (findings == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Agent returned null field: CodeReviewReport.findings");
        }
        for (int index = 0; index < findings.size(); index++) {
            CodeFinding finding = findings.get(index);
            String prefix = "CodeReviewReport.findings[" + index + "]";
            requireText(finding.severity(), prefix + ".severity");
            requireText(finding.problem(), prefix + ".problem");
            requireText(finding.suggestion(), prefix + ".suggestion");
        }
    }

    private void requireQuestions(List<QuizQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent returned empty quiz questions");
        }
        for (int index = 0; index < questions.size(); index++) {
            QuizQuestion question = questions.get(index);
            String prefix = "QuizPack.questions[" + index + "]";
            requireText(question.question(), prefix + ".question");
            if (question.options() == null || question.options().size() != 4) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Agent returned invalid options: " + prefix + ".options");
            }
            requireOptions(question.options(), prefix + ".options");
            requireAnswerInOptions(question.answer(), question.options(), prefix + ".answer");
            requireCompleteSentence(question.explanation(), prefix + ".explanation");
        }
    }

    private void requireOptions(List<String> options, String fieldName) {
        Set<String> uniqueOptions = new HashSet<>();
        for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
            String option = options.get(optionIndex);
            requireText(option, fieldName + "[" + optionIndex + "]");
            if (!uniqueOptions.add(option.strip())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Agent returned duplicated option: " + fieldName + "[" + optionIndex + "]");
            }
        }
    }

    private void requireAnswerInOptions(String answer, List<String> options, String fieldName) {
        requireText(answer, fieldName);
        String normalizedAnswer = answer.strip();
        boolean matched = options.stream()
                .map(String::strip)
                .anyMatch(normalizedAnswer::equals);
        if (!matched) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Agent returned answer outside options: " + fieldName);
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent returned blank field: " + fieldName);
        }
    }

    private void requireCompleteSentence(String value, String fieldName) {
        requireText(value, fieldName);
        String text = value.strip();
        if (!text.matches(".*[。！？.!?]$")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent returned incomplete field: " + fieldName);
        }
    }

    private void requireTextList(
            List<String> values,
            String fieldName) {
        if (values == null || values.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Agent returned invalid list: " + fieldName);
        }
        for (int index = 0; index < values.size(); index++) {
            requireText(values.get(index), fieldName + "[" + index + "]");
        }
    }

}
