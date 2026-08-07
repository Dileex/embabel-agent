# 第14篇：把商品内容规范装成Agent可以复用的Skill

分支：`article/014-agent-skills`

新增接口：

```text
POST /agent/skill-copy
```

启动：

```bash
export DEEPSEEK_API_KEY=<your-deepseek-api-key>
mvn spring-boot:run
```

验证：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/skill-copy" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请为轻量通勤双肩包写30秒抖音口播。已确认信息：重量约620克、容量18升、可以放14英寸笔记本、日常防泼水。希望写成全网第一、完全防水、销量第一。"
  }'
```

成功响应必须满足：

```text
calledSkillTools包含product_video_copy和readResource
skillName=product-video-copy
ruleVersion=copy-rule-2026-08
可发布文案不包含全网第一、完全防水、销量第一
可发布文案不包含用户没有提供的参照比较
```

再用一个没有高风险宣传词的商品验证Skill可以复用：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/skill-copy" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请为不锈钢保温杯写20秒短视频口播。已确认信息：容量500毫升、杯盖带提环、适合通勤携带。"
  }'
```

这条请求没有需要拒绝的宣传词，`rejectedClaims`可以返回空列表，文案也不能自行补充保温时长、材质等级、认证或优惠。

空白`message`应由请求校验直接返回`400 Bad Request`。
