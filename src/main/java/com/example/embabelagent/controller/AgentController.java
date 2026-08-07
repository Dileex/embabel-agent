package com.example.embabelagent.controller;

import com.example.embabelagent.dto.AgentRequest;
import com.example.embabelagent.dto.AgentResponse;
import com.example.embabelagent.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/ask")
    public AgentResponse ask(@Valid @RequestBody AgentRequest request) {
        return agentService.closed(request.message());
    }

    @PostMapping("/focused")
    public AgentResponse focused(@Valid @RequestBody AgentRequest request) {
        return agentService.focused(request.message());
    }

    @PostMapping("/closed")
    public AgentResponse closed(@Valid @RequestBody AgentRequest request) {
        return agentService.closed(request.message());
    }

    @PostMapping("/open")
    public AgentResponse open(@Valid @RequestBody AgentRequest request) {
        return agentService.open(request.message());
    }

    @PostMapping("/parallel")
    public AgentResponse parallel(
            @Valid @RequestBody AgentRequest request) {
        return agentService.parallel(request.message());
    }

    @PostMapping("/video-plan")
    public AgentResponse videoPlan(
            @Valid @RequestBody AgentRequest request) {
        return agentService.videoPlan(request.message());
    }

    @PostMapping("/product-query")
    public AgentResponse productQuery(
            @RequestHeader(
                    name = "X-Tenant-Id",
                    defaultValue = "tenant-demo")
            String tenantId,
            @Valid @RequestBody AgentRequest request) {
        return agentService.productBusinessQuery(
                tenantId,
                request.message());
    }

    @PostMapping("/product-knowledge")
    public AgentResponse productKnowledge(
            @Valid @RequestBody AgentRequest request) {
        return agentService.productKnowledge(
                request.message());
    }

    @PostMapping("/mcp-publish-check")
    public AgentResponse mcpPublishCheck(
            @RequestHeader(
                    name = "X-Tenant-Id",
                    defaultValue = "tenant-demo")
            String tenantId,
            @Valid @RequestBody AgentRequest request) {
        return agentService.mcpPublishCheck(
                tenantId,
                request.message());
    }

}
