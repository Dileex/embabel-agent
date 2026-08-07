---
name: product-video-copy
description: 为电商商品生成或检查短视频标题、开场口播、正文和画面字幕；当任务涉及商品短视频文案、卖点表达或合规改写时使用。
license: Apache-2.0
compatibility: Embabel Agent 1.0.0
metadata:
  owner: content-platform
  version: "2026-08"
---

# 商品短视频文案

处理商品短视频文案时，按下面的顺序执行：

1.读取[商品视频文案规则](references/copy-rules.md)。
2.只提取用户已经确认的商品信息，不补充销量、排名、认证、功效、优惠或参照比较。
3.把用户要求使用但不符合规则的说法放进`rejectedClaims`，不要继续写进可发布文案。
4.标题写清商品和使用场景，开场先说用户能感知的真实特点。
5.口播适合用户要求的时长，画面字幕保持简短。
6.`skillName`填写`product-video-copy`，`ruleVersion`填写参考规则中的版本。

最终返回结构化的商品短视频文案，不解释执行过程。
