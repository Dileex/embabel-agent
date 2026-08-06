# 第9篇：结果不合格时继续修改

分支：`article/009-repeat-until-acceptable`

这份代码以第8篇为基线，保留三路并行故障分析，在报告汇总后增加质量评估循环：

```text
并行分析日志、指标和最近变更
->生成故障报告
->评估报告
->评分不足0.9时带着反馈重新生成
->最多执行3轮
```

主要增量：

```text
ParallelIncidentAgent.java
->使用RepeatUntilBuilder组织生成和评估循环
->报告增加generationAttempt、qualityScore和qualityIssues

ParallelIncidentAgentProperties.java
->增加evaluateReport提示词配置

application.yml
->增加质量要求、上一轮反馈和报告评估提示词

AgentService.java
->校验评估分数、轮次和问题列表
```

启动：

```bash
export DEEPSEEK_API_KEY=<your-deepseek-api-key>
mvn spring-boot:run
```

请求：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/parallel" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "故障现象：订单查询接口最近10分钟大量超时；\n监控指标：数据库连接池使用率92%，接口P95从300ms升到4s；\n最近变更：刚发布过查询条件改动；\n日志片段：query timeout after 3000ms，connection pool exhausted。\n质量要求：可能原因必须明确写出仍需验证；立即处理必须包含回滚查询条件改动、缓解数据库连接池压力和观察接口P95；验证步骤至少4项。"
  }'
```

响应重点：

```text
generationAttempt
revisionFeedback
qualityScore
qualityIssues
```
