package com.example.embabelagent.service;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.example.embabelagent.agent.ProductMediaRetryAgent.MediaTaskRequest;
import com.example.embabelagent.agent.ProductMediaRetryAgent.MediaTaskResult;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProductMediaRetryService {

    private final AgentPlatform agentPlatform;

    private final MockMediaService mediaService;

    public ProductMediaRetryService(
            AgentPlatform agentPlatform,
            MockMediaService mediaService) {
        this.agentPlatform = agentPlatform;
        this.mediaService = mediaService;
    }

    public RetryResponse create(
            String productName,
            int failuresBeforeSuccess) {
        String requestId = UUID.randomUUID().toString();
        MediaTaskRequest request = new MediaTaskRequest(
                requestId,
                productName,
                failuresBeforeSuccess);

        try {
            AgentProcess process = AgentInvocation
                    .create(
                            agentPlatform,
                            MediaTaskResult.class)
                    .run(request);

            return new RetryResponse(
                    process.getId(),
                    process.getStatus().name(),
                    mediaService.attempts(requestId),
                    (MediaTaskResult) process.lastResult(),
                    null);
        } catch (Exception ex) {
            return new RetryResponse(
                    null,
                    "FAILED",
                    mediaService.attempts(requestId),
                    null,
                    rootMessage(ex));
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    public record RetryResponse(
            String processId,
            String status,
            int attempts,
            MediaTaskResult output,
            String error) {
    }
}
