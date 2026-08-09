package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.example.embabelagent.config.ProductToolAgentProperties;
import com.example.embabelagent.tool.ProductBusinessTools;
import com.example.embabelagent.tool.ProductBusinessTools.ProductQueryTools;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Agent(description = "根据用户问题调用商品业务工具并返回业务结论")
public class ProductToolAgent {

    private final ProductBusinessTools businessTools;

    private final ProductToolAgentProperties properties;

    public ProductToolAgent(
            ProductBusinessTools businessTools,
            ProductToolAgentProperties properties) {
        this.businessTools = businessTools;
        this.properties = properties;
    }

    @AchievesGoal(description = "给出有业务数据依据的商品业务问题答案")
    @Action(description = "调用只读业务工具检查商品、库存、店铺和平台规则")
    public ProductBusinessAnswer answerBusinessQuestion(
            ProductBusinessQuestion request,
            Ai ai) {

        /*
         * 每次请求都创建一个新的工具对象。
         * 这样工具调用记录不会混入其他请求，且模型只能拿到当前租户的数据访问入口。
         */
        ProductQueryTools tools =
                businessTools.forTenant(request.tenantId());

        BusinessAnswer answer = ai
                .withDefaultLlm()
                /*
                 * 只有这里显式传入的@LlmTool方法才会出现在本次模型调用中。
                 * 方法不会自动执行，模型会根据用户问题决定调用一个、多个，或在信息不足时不调用。
                 */
                .withToolObject(tools)
                .createObject(
                        buildPrompt(request),
                        BusinessAnswer.class);

        /*
         * calledTools由工具方法实际执行时记录，不能由模型伪造。
         * 当用户缺少商品ID等查询条件时，模型可以直接追问，此时两个列表允许为空。
         */
        return new ProductBusinessAnswer(
                request.question(),
                answer.answer(),
                answer.evidence(),
                tools.calledTools());
    }

    private String buildPrompt(
            ProductBusinessQuestion request) {
        return properties.answerBusinessQuestion()
                .replace(
                        "{question}",
                        request.question());
    }

    public record ProductBusinessQuestion(
            @NotBlank String tenantId,
            @NotBlank String question) {
    }

    public record BusinessAnswer(
            @NotBlank String answer,
            List<@NotBlank String> evidence) {
    }

    public record ProductBusinessAnswer(
            @NotBlank String question,
            @NotBlank String answer,
            // 信息不足时允许没有证据和工具调用，回答会要求用户补充查询条件。
            List<@NotBlank String> evidence,
            List<@NotBlank String> calledTools) {
    }
}
