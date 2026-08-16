package com.example.embabelagent.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.hitl.ConfirmationRequest;
import com.embabel.agent.core.hitl.ConfirmationResponse;
import com.embabel.agent.core.hitl.ResponseImpact;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent;
import com.example.embabelagent.agent.ProductContentCoordinatorAgent.ProductContentRequest;
import com.example.embabelagent.agent.ProductContentHitlAgent.HitlPublishResult;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductContentHitlService {

    private final AgentPlatform agentPlatform;

    public ProductContentHitlService(
            AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    public HitlResponse start(String message) {
        ProductContentRequest request =
                ProductContentCoordinatorAgent.parseRequest(
                        message);

        AgentProcess process = AgentInvocation
                .create(
                        agentPlatform,
                        HitlPublishResult.class)
                .run(request);

        return response(process);
    }

    public HitlResponse confirm(
            String processId,
            boolean accepted) {
        AgentProcess process = find(processId);

        if (process.getStatus()
                != AgentProcessStatusCode.WAITING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "当前Agent进程不在等待确认状态");
        }

        Object lastResult = process.lastResult();
        if (!(lastResult
                instanceof ConfirmationRequest<?> request)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "当前进程没有等待中的确认请求");
        }

        ConfirmationResponse response =
                new ConfirmationResponse(
                        UUID.randomUUID().toString(),
                        request.getId(),
                        accepted,
                        false,
                        Instant.now());

        ResponseImpact impact =
                request.onResponse(
                        response,
                        process);

        if (!accepted) {
            process.terminateAgent(
                    "确认被拒绝");
            return response(
                    process,
                    "确认被拒绝，进程已结束");
        }

        if (impact != ResponseImpact.UPDATED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "确认没有更新Agent进程");
        }

        // 确认对象已回到当前进程，继续执行后面的Action。
        process.run();
        return response(process);
    }

    private AgentProcess find(String processId) {
        AgentProcess process =
                agentPlatform.getAgentProcess(processId);
        if (process == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "找不到Agent进程");
        }
        return process;
    }

    private HitlResponse response(
            AgentProcess process) {
        return response(
                process,
                process.getStatus().name(),
                switch (process.getStatus()) {
                    case WAITING -> "等待确认";
                    case COMPLETED -> "Agent进程已完成";
                    case TERMINATED -> "Agent进程已结束";
                    case FAILED -> "Agent进程执行失败";
                    case STUCK -> "Agent进程暂时无法继续";
                    default -> "Agent进程状态已更新";
                });
    }

    private HitlResponse response(
            AgentProcess process,
            String message) {
        return response(
                process,
                process.getStatus().name(),
                message);
    }

    private HitlResponse response(
            AgentProcess process,
            String status,
            String message) {
        Object lastResult = process.lastResult();
        String confirmationId = null;
        String confirmationMessage = null;
        Object payload = null;

        if (process.getStatus()
                == AgentProcessStatusCode.WAITING
                && lastResult
                instanceof ConfirmationRequest<?> request) {
            confirmationId = request.getId();
            confirmationMessage = request.getMessage();
            payload = request.getPayload();
        }

        return new HitlResponse(
                process.getId(),
                status,
                message,
                confirmationId,
                confirmationMessage,
                payload,
                process.getStatus()
                        == AgentProcessStatusCode.COMPLETED
                        ? process.lastResult()
                        : null);
    }

    public record HitlResponse(
            String processId,
            String status,
            String message,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String confirmationId,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            String confirmationMessage,
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Object confirmationPayload,
            Object output) {
    }
}
