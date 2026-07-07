package com.example.embabelagent.service;

import com.embabel.agent.api.common.autonomy.AgentProcessExecution;
import com.embabel.agent.api.common.autonomy.Autonomy;
import com.embabel.agent.api.common.autonomy.ProcessExecutionException;
import com.embabel.agent.core.ProcessOptions;
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
        return new AgentResponse(
                execution.getAgentProcess().getId(),
                output.getClass().getSimpleName(),
                output);
    }

}
