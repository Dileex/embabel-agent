# Article 007: Agent 调用方式

对应文章：

```text
Embabel Agent 1.0.0 调用实战：任务已经明确，还要让模型再选一次 Agent 吗
```

对应分支：

```text
article/007-agent-platform
```

## 本篇代码

项目保留 `QuizAgent`，新增 `CodeReviewAgent`，对比三种调用入口：

```text
/agent/focused
-> AgentInvocation<QuizPack>

/agent/closed
-> Autonomy.chooseAndRunAgent(...)

/agent/open
-> Autonomy.chooseAndAccomplishGoal(...)
```

## 运行环境

```text
Java 21+
Spring Boot 3.5.14
Embabel Agent 1.0.0
DeepSeek deepseek-v4-flash
```

启动前设置模型密钥：

```bash
export DEEPSEEK_API_KEY=你的 DeepSeek API Key
```

## 编译和启动

```bash
mvn -DskipTests compile
mvn spring-boot:run
```

如果 8080 已被占用，可以临时改端口：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## 请求接口

按结果类型调用：

```bash
curl -sS -X POST "http://localhost:8080/agent/focused" \
  -H "Content-Type: application/json" \
  -d '{"message":"请根据下面文章生成 3 道单选题：Tool Calling 负责调用外部能力，Agent 还要管理目标、状态、动作和停止条件。"}'
```

让平台选择 Agent：

```bash
curl -sS -X POST "http://localhost:8080/agent/closed" \
  -H "Content-Type: application/json" \
  -d '{"message":"请审查这段 Java 代码：public int first(List<Integer> values) { return values.get(0); }"}'
```

让平台选择 Goal：

```bash
curl -sS -X POST "http://localhost:8080/agent/open" \
  -H "Content-Type: application/json" \
  -d '{"message":"请检查这段 Java 代码的边界问题并给出修改建议：return users.get(0).getName();"}'
```

响应中的 `mode`、`agentName`、`goalName` 和 `outputType` 用来确认本次调用走了哪条入口。

## 本篇相关文件

```text
src/main/java/com/example/embabelagent/agent/CodeReviewAgent.java
src/main/java/com/example/embabelagent/config/CodeReviewAgentProperties.java
src/main/java/com/example/embabelagent/controller/AgentController.java
src/main/java/com/example/embabelagent/service/AgentService.java
src/main/java/com/example/embabelagent/dto/AgentResponse.java
src/main/resources/application.yml
pom.xml
```
