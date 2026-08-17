# 17.Embabel Agent1.0.0重试实战：素材服务偶尔失败，Action该不该再跑一次

分支：`codex/017-action-retry`

这篇在第16篇代码基础上，只增加Action级有限重试示例：

接口接收已经完成上游处理的商品任务，本次`AgentProcess`
从创建素材任务开始：

```text
POST /agent/media-task-retry
->AgentController
->ProductMediaRetryService创建MediaTaskRequest
->AgentInvocation启动AgentProcess
->ProductMediaRetryAgent.createMediaTask
->MockMediaService
->临时异常时重试同一个Action
->成功后COMPLETED，耗尽后由Service返回FAILED
```

重试沿用同一个`requestId`和`MediaTaskRequest`，不会重新进入Controller，
也不会重跑第16篇的内容生成与人工确认流程。

重试接口只使用本地 Java 逻辑。默认启动不会连接 Ollama，
也不会拉起 `npx`，直接启动即可：

```bash
mvn spring-boot:run
```

配置文件里已经给 `DEEPSEEK_API_KEY` 放了启动占位值 `demo-key`，
所以只测试本篇接口时不用配置 Key。
如果要调用第15、16篇里依赖大模型的接口，再设置真实的 Key：

```bash
export DEEPSEEK_API_KEY=<your-deepseek-api-key>
```

如果要重新打开第15、16篇的外部集成，可以显式传参数：

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.ai.mcp.client.enabled=true --demo.integrations.rag-enabled=true"
```

打开 RAG 后，需要本机 Ollama 已运行并准备 `bge-m3`；
打开 MCP 后，需要本机有 Node.js 和 `npx`。

前两次失败、第三次成功：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/media-task-retry?failuresBeforeSuccess=2" |
  jq .
```

重试三次后仍然失败：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/media-task-retry?failuresBeforeSuccess=99" |
  jq .
```

预期：

```text
failuresBeforeSuccess=2
->status=COMPLETED
->attempts=3

failuresBeforeSuccess=99
->status=FAILED
->attempts=3
```
