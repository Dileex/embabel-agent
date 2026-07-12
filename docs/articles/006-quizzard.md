# Article 006: Quizzard

对应文章：

```text
Quizzard 实战：根据技术文章生成测验题
```

对应分支：

```text
article/006-quizzard
```

这个分支只保留第六章相关代码。示例目标是把一篇技术文章转换成一套复盘测验题：

```text
UserInput
-> ArticleInput
-> ConceptDigest
-> QuizDraft
-> QuizPack
```

## 运行环境

```text
Java 21+
Spring Boot 3.5.14
Embabel Agent 0.5.0
DeepSeek deepseek-v4-flash
```

启动前配置 DeepSeek Key：

```bash
export DEEPSEEK_API_KEY=你的 DeepSeek API Key
```

## 编译

```bash
mvn -DskipTests compile
```

## 启动

```bash
mvn spring-boot:run
```

如果 8080 已被占用，可以临时改端口：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## 请求接口

URL 输入：

```bash
curl -X POST "http://localhost:8080/agent/ask" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请根据这篇技术文章生成 3 道单选测验题：https://docs.spring.io/spring-ai/reference/api/chatclient.html"
  }'
```

直接粘贴正文也可以：

```bash
curl -X POST "http://localhost:8080/agent/ask" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请根据下面技术文章生成 3 道单选测验题。标题：为什么 Spring AI 的 Tool Calling 不等于完整 Agent。正文：Tool Calling 解决的是模型如何请求外部工具，以及应用侧如何执行这些工具。一次工具调用循环可以让模型先查资料、再继续回答，但它通常只发生在一次模型调用上下文里。Agent 更关注任务目标、状态、动作链路和停止条件。工程里如果只把工具暴露给模型，却没有校验结果、保存状态和处理失败路径，任务很容易停在半路。"
  }'
```

返回结构类似：

```json
{
  "processId": "blissful_mclean",
  "agentName": "QuizAgent",
  "outputType": "QuizPack",
  "output": {
    "title": "Spring AI Tool Calling 与 Agent 的区别",
    "questions": [
      {
        "question": "根据文章，Tool Calling 和 Agent 在架构上的最根本区别是什么？",
        "options": [
          "Tool Calling 支持多工具，Agent 只支持单个工具",
          "Tool Calling 是单次交互，Agent 是带有目标、状态和停止条件的决策循环",
          "Tool Calling 依赖模型自己处理错误，Agent 由开发者处理错误",
          "Tool Calling 只能调用 API，Agent 可以调用数据库"
        ],
        "answer": "Tool Calling 是单次交互，Agent 是带有目标、状态和停止条件的决策循环",
        "explanation": "文章指出 Tool Calling 通常在一次模型调用上下文中完成，而 Agent 关注任务目标、运行状态、动作决策链路以及停止条件。"
      }
    ],
    "review": "这套题主要考察对 Spring AI 中 Tool Calling 与 Agent 在架构、工程保障和作用域上的核心区别的理解。"
  }
}
```

`processId` 和模型生成的表达会随请求变化。关键是：

```text
agentName = QuizAgent
outputType = QuizPack
questions 非空
每道题包含 question / options / answer / explanation
```

## 关键代码

```text
src/main/java/com/example/embabelagent/EmbabelAgentApplication.java
src/main/java/com/example/embabelagent/agent/QuizAgent.java
src/main/java/com/example/embabelagent/config/QuizAgentProperties.java
src/main/java/com/example/embabelagent/controller/AgentController.java
src/main/java/com/example/embabelagent/dto/AgentRequest.java
src/main/java/com/example/embabelagent/dto/AgentResponse.java
src/main/java/com/example/embabelagent/service/AgentService.java
src/main/resources/application.yml
```

## 执行链路

```text
POST /agent/ask
-> AgentService 调用 Autonomy.chooseAndRunAgent(...)
-> QuizAgent.extractArticle
-> QuizAgent.extractConcepts
-> QuizAgent.generateQuiz
-> QuizAgent.reviewQuiz
-> QuizPack
```

## 结果校验

`AgentService` 会对最终输出做一层轻量校验：

```text
QuizPack.title 不能为空
questions 不能为空
每道题必须有题干
options 必须正好 4 个
options 不能重复
answer 必须命中 options 里的某个选项
explanation 必须是完整句子
```

校验不通过时返回 `502`。
