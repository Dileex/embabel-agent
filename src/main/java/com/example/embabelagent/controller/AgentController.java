package com.example.embabelagent.controller;

import com.example.embabelagent.dto.AgentRequest;
import com.example.embabelagent.dto.AgentResponse;
import com.example.embabelagent.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        return agentService.ask(request.message());
    }

}
