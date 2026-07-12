package com.example.embabelagent.service;

import com.embabel.agent.api.common.autonomy.AgentProcessExecution;
import com.embabel.agent.api.common.autonomy.Autonomy;
import com.embabel.agent.api.common.autonomy.ProcessExecutionException;
import com.embabel.agent.core.ProcessOptions;
import com.example.embabelagent.agent.QuizAgent.QuizPack;
import com.example.embabelagent.agent.QuizAgent.QuizQuestion;
import com.example.embabelagent.dto.AgentResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgentService {

    private final Autonomy autonomy;

    public AgentService(Autonomy autonomy) {
        this.autonomy = autonomy;
    }

    public AgentResponse ask(String message) {
        AgentProcessExecution execution;
        try {
            execution = autonomy.chooseAndRunAgent(message.strip(), new ProcessOptions());
        }
        catch (ProcessExecutionException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent execution failed", ex);
        }
        Object output = execution.getOutput();
        validateOutput(output);
        return new AgentResponse(
                execution.getAgentProcess().getId(),
                execution.getAgentProcess().getAgent().getName(),
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
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unsupported agent output type");
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

}
