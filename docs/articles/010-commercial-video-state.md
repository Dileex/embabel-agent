# 第10篇：商业视频任务如何按状态推进

分支：`article/010-commercial-video-state`

这份代码以第9篇为基线，保留已有示例，新增商业视频状态驱动流程：

```text
CommercialVideoRequest
->ProductBriefReady
->VideoScriptReady
->StoryboardReady
->CommercialVideoPlan
```

主要增量：

```text
CommercialVideoAgent.java
->使用@State record表示商业视频当前阶段
->每个状态只提供下一阶段需要的Action

CommercialVideoAgentProperties.java
->绑定资料整理、脚本、分镜和检查提示词

application.yml
->增加商业视频四个阶段的提示词

AgentController.java
->增加POST /agent/video-plan

AgentService.java
->调用CommercialVideoPlan目标并校验最终方案
```

启动：

```bash
export DEEPSEEK_API_KEY=<your-deepseek-api-key>
mvn spring-boot:run
```

请求：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/video-plan" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "商品名称：轻量通勤双肩包；\n商品资料：重量约620克，容量18升，支持放入14英寸笔记本，面料具有日常防泼水能力，包含独立电脑隔层和两个侧袋；\n目标人群：每天乘坐地铁通勤的上班族；\n发布平台：抖音；\n视频时长：30秒"
  }'
```

响应重点：

```text
brief
script
storyboard
review
completedStages
```
