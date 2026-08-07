# 第11篇：Agent怎么调用本地业务工具

分支：`article/011-local-business-tools`

接口：

```text
POST /agent/product-publish-check
```

调用链：

```text
ProductPublishRequest
->ProductToolAgent
->ProductBusinessTools
->ProductPublishAssessment
```

本篇增加四个只读工具：

```text
query_product
query_inventory_price
query_store
query_platform_spec
```

启动：

```bash
export DEEPSEEK_API_KEY=<your-deepseek-api-key>
mvn spring-boot:run
```

请求：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/product-publish-check" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-demo" \
  -d '{
    "message": "商品ID：P-1001；店铺ID：S-2001；发布平台：抖音；视频时长：30秒"
  }'
```

响应中的`calledTools`用于确认模型实际调用过哪些工具。
