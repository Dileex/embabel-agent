package com.example.embabelagent.service;

import com.embabel.agent.api.common.autonomy.AgentProcessExecution;
import com.embabel.agent.api.common.autonomy.Autonomy;
import com.embabel.agent.api.common.autonomy.ProcessExecutionException;
import com.embabel.agent.core.ProcessOptions;
import com.example.embabelagent.agent.PolicyAgent.PolicyAnswer;
import com.example.embabelagent.agent.StarNewsAgent.Writeup;
import com.example.embabelagent.dto.AgentResponse;
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
        if (output instanceof Writeup writeup) {
            requireText(writeup.title(), "Writeup.title");
            requireCompleteSentence(writeup.summary(), "Writeup.summary");
            requireCompleteSentence(writeup.advice(), "Writeup.advice");
            return;
        }
        if (output instanceof PolicyAnswer answer) {
            requireText(answer.title(), "PolicyAnswer.title");
            requireCompleteSentence(answer.answer(), "PolicyAnswer.answer");
            requireText(answer.source(), "PolicyAnswer.source");
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unsupported agent output type");
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
