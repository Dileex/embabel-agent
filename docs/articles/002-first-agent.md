# Article 002: Embabel First Agent

对应文章：

```text
Embabel 上手：让 Java Agent 自己选择执行链路
```

对应分支：

```text
article/002-first-agent
```

这个示例演示的是通过一个 Web 入口，让 Embabel 根据用户输入选择不同 Agent，并在选中的 Agent 内部执行 Action 链路：

```text
POST /agent/ask
-> AgentService 调用 Autonomy.chooseAndRunAgent(...)
-> Embabel 根据 Agent description 选择 Agent
-> 在选中的 Agent 内部规划并执行 Action
-> 返回最终结构化对象
```

当前包含两个 Agent：

```text
星座文案：StarNewsAgent
制度问答：PolicyAgent
```

## 运行环境

```text
Java 21
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

## 请求接口

星座文案：

```bash
curl -X POST "http://localhost:8080/agent/ask" \
  -H "Content-Type: application/json" \
  -d '{"message":"给李白写一段白羊座今日运势文案"}'
```

返回结构类似：

```json
{
  "processId": "brave_bouman",
  "outputType": "Writeup",
  "output": {
    "title": "李白今日任务拆解指南",
    "summary": "今天适合像李白写诗那样，先把最关键的一句搞定，再慢慢推敲全篇。",
    "advice": "把今天最想做的事情拆成三步，走出第一步就够了。"
  }
}
```

制度问答：

```bash
curl -X POST "http://localhost:8080/agent/ask" \
  -H "Content-Type: application/json" \
  -d '{"message":"出差回来后报销需要哪些材料"}'
```

返回结构类似：

```json
{
  "processId": "sad_mendeleev",
  "outputType": "PolicyAnswer",
  "output": {
    "title": "差旅报销材料要求",
    "answer": "出差回来后报销需要提交出差审批单、交通票据、住宿发票、行程说明和费用明细。",
    "source": "差旅与报销制度"
  }
}
```

`processId` 和模型生成的表达会随请求变化，不要求逐字一致。关键是 `outputType`：

```text
星座文案 -> Writeup
制度问答 -> PolicyAnswer
```

## 关键代码

```text
src/main/java/com/example/embabelagent/EmbabelAgentApplication.java
src/main/java/com/example/embabelagent/agent/StarNewsAgent.java
src/main/java/com/example/embabelagent/agent/PolicyAgent.java
src/main/java/com/example/embabelagent/config/StarNewsAgentProperties.java
src/main/java/com/example/embabelagent/config/PolicyAgentProperties.java
src/main/java/com/example/embabelagent/controller/AgentController.java
src/main/java/com/example/embabelagent/dto/AgentRequest.java
src/main/java/com/example/embabelagent/dto/AgentResponse.java
src/main/java/com/example/embabelagent/service/AgentService.java
src/main/java/com/example/embabelagent/service/HoroscopeService.java
src/main/java/com/example/embabelagent/service/PolicyKnowledgeService.java
src/main/resources/application.yml
```

## 和文章的对应关系

```text
统一入口：AgentController -> POST /agent/ask
Embabel 调用入口：AgentService -> Autonomy.chooseAndRunAgent(...)
星座文案 Agent：StarNewsAgent
制度问答 Agent：PolicyAgent
星座资料服务：HoroscopeService
制度资料服务：PolicyKnowledgeService
提示词配置：application.yml -> demo.star-news-agent / demo.policy-agent
```

## 执行链路

星座文案：

```text
chooseAndRunAgent
-> StarNewsAgent
-> extractStarPerson
-> retrieveHoroscope
-> writeup
-> Writeup
```

制度问答：

```text
chooseAndRunAgent
-> PolicyAgent
-> extractPolicyQuestion
-> retrievePolicy
-> answer
-> PolicyAnswer
```
