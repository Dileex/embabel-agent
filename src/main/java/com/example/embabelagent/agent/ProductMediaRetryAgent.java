package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.example.embabelagent.service.MockMediaService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Component;

@Agent(description = "调用素材服务创建商品素材任务")
public class ProductMediaRetryAgent {

    private final MockMediaService mediaService;

    public ProductMediaRetryAgent(
            MockMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @AchievesGoal(description = "返回创建成功的商品素材任务")
    @Action(
            description = "创建商品素材任务",
            actionRetryPolicyExpression =
                    "${demo.media-task-retry}")
    public MediaTaskResult createMediaTask(
            MediaTaskRequest request) {
        return mediaService.createTask(request);
    }

    public record MediaTaskRequest(
            @NotBlank String requestId,
            @NotBlank String productName,
            @Min(0) int failuresBeforeSuccess) {
    }

    public record MediaTaskResult(
            @NotBlank String requestId,
            @NotBlank String taskId,
            @NotBlank String status,
            @Min(1) int attempts) {
    }
}
