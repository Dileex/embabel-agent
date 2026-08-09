# 第11篇：模型怎么选择本地业务工具

分支：`article/011-local-business-tools`

接口：

```text
POST /agent/product-query
```

调用链：

```text
ProductBusinessQuestion
->ProductToolAgent
->ProductBusinessTools
->ProductBusinessAnswer
```

本篇提供四个只读工具，由模型按用户问题选择：

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
  "http://localhost:8080/agent/product-query" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-demo" \
  -d '{
    "message": "商品P-1001还有多少库存？当前售价是多少？"
  }'
```

响应中的`calledTools`记录模型实际选择了哪些工具；信息不足时，它也可以是空列表。
