# 第16篇：商品内容生成后，先等人工确认

分支：`article/016-native-hitl`
基线：`article/015-multi-agent-collaboration`

本分支只增加第16篇的HITL代码：

```text
src/main/java/com/example/embabelagent/agent/ProductContentHitlAgent.java
src/main/java/com/example/embabelagent/service/ProductContentHitlService.java
src/main/java/com/example/embabelagent/controller/AgentController.java中的两个接口
docs/articles/016-native-hitl.md
```

这篇使用Embabel Agent1.0.0原生HITL能力：

```text
WaitFor.confirmation(...)
->ConfirmationRequest
->AgentProcess进入WAITING
->ConfirmationResponse
->恢复同一个AgentProcess
```

新增接口：

```text
POST /agent/content-plan-hitl
POST /agent/content-plan-hitl/{processId}/confirm?accepted=true
POST /agent/content-plan-hitl/{processId}/confirm?accepted=false
```

启动：

```text
Ollama已经运行，并已准备bge-m3模型
Node.js和npx命令可用
curl和jq命令可用
DEEPSEEK_API_KEY已经配置
```

```bash
export DEEPSEEK_API_KEY=<your-deepseek-api-key>
mvn spring-boot:run
```

先提交商品内容任务：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/content-plan-hitl" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "商品名称：轻量通勤双肩包；已确认信息：商品重量约620克，整体轻便好背，容量适合上下班和上学等日常通勤，可放入14英寸笔记本电脑，面料支持日常防泼水，日常通勤途中遇到少量水滴可以及时擦干，但不能宣传为完全防水；目标平台：抖音；内容要求：生成一份30秒商品短视频内容方案，只使用这些已确认信息，不要增加销量、排名、认证、材质、电脑保护功能或完全防水等说法。"
  }'
```

质检通过后，响应应满足：

```text
status=WAITING
confirmationMessage=商品内容已经生成，是否继续执行模拟发布？
output=null
```

使用响应中的`processId`确认继续：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/content-plan-hitl/<processId>/confirm?accepted=true"
```

响应应满足：

```text
processId与等待时相同
status=COMPLETED
output.status=PUBLISHED
```

改为`accepted=false`时，不会执行发布Action。示例调用
`terminateAgent(...)`结束等待中的进程，因此接口返回的进程状态是
`TERMINATED`。

2026-08-16使用文章中的接口完成HTTP验证：

```text
启动任务：200，status=WAITING，ready=true
确认通过：200，status=COMPLETED，output.status=PUBLISHED
再次启动：200，status=WAITING，ready=true
确认拒绝：200，status=TERMINATED，output=null
```

当前示例使用框架默认的内存进程仓库。应用重启后，等待中的`processId`会失效。
