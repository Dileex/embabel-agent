# Article 008: 并行执行

对应文章：

```text
Embabel Agent 1.0.0并行执行实战：一场线上故障，三条线索怎么一起查
```

对应分支：

```text
article/008-parallelization
```

## 本篇代码

项目保留第7篇的两个Agent，新增`ParallelIncidentAgent`：

```text
POST /agent/parallel
->解析故障输入
->并行分析日志、指标和最近变更
->汇总三份证据
->生成IncidentAnalysisReport
```

处置建议不会和三项证据分析同时执行。它要等三份结果都返回后，再根据已有证据生成。

## 运行环境

```text
Java 21+
Spring Boot 3.5.14
Embabel Agent 1.0.0
DeepSeek deepseek-v4-flash
```

启动前设置模型密钥：

```bash
export DEEPSEEK_API_KEY=你的DeepSeek API Key
```

## 编译和启动

```bash
mvn -DskipTests compile
mvn spring-boot:run
```

## 请求接口

```bash
curl -sS -X POST "http://localhost:8080/agent/parallel" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "故障现象：订单查询接口最近10分钟大量超时；\n监控指标：数据库连接池使用率92%，接口P95从300ms升到4s；\n最近变更：刚发布过查询条件改动；\n日志片段：query timeout after 3000ms，connection pool exhausted。"
  }'
```

响应中的`logFindings`、`metricFindings`和`recentChangeFindings`是三路分析结果。`immediateActions`和`verificationSteps`是在三项分析都完成后生成的。

`parts`记录每项任务的开始时间、结束时间和线程名，`execution.overlapObserved`用于确认三个任务是否出现执行重叠。

缺少任意一段输入时，接口返回`400`：

```bash
curl -sS -o /tmp/embabel-parallel-error.json -w "%{http_code}\n" \
  -X POST "http://localhost:8080/agent/parallel" \
  -H "Content-Type: application/json" \
  -d '{"message":"故障现象：订单查询接口超时；监控指标：P95升高；最近变更：无。"}'
```

## 本篇相关文件

```text
src/main/java/com/example/embabelagent/agent/ParallelIncidentAgent.java
src/main/java/com/example/embabelagent/config/ParallelIncidentAgentProperties.java
src/main/java/com/example/embabelagent/controller/AgentController.java
src/main/java/com/example/embabelagent/service/AgentService.java
src/main/resources/application.yml
```
