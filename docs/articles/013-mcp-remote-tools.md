# 第13篇：业务工具不在当前项目里，Agent怎么远程调用

分支：`article/013-mcp-remote-tools`

进程：

```text
主应用：8080
平台规则MCP服务：8081
```

接口：

```text
POST /agent/mcp-publish-check
```

启动MCP服务：

```bash
mvn -f platform-rules-mcp-server/pom.xml \
  spring-boot:run
```

启动主应用：

```bash
export DEEPSEEK_API_KEY=<your-deepseek-api-key>
mvn spring-boot:run
```

请求：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/mcp-publish-check" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-demo" \
  -d '{
    "message": "商品ID：P-1001；店铺ID：S-2001；发布平台：抖音；视频时长：30秒"
  }'
```

本地工具查询商品、库存和店铺，远程MCP工具`query_platform_rule`查询平台规则。

成功响应应包含：

```text
localCalledTools=
query_product、query_inventory_price、query_store

platformRuleSource=platform-rules-mcp
platformRuleVersion=demo-2026-08
```

平台规则服务日志应出现：

```text
MCP tool query_platform_rule called, platform=抖音
```

停止8081后再次调用，接口应返回`502`，不会改用第11篇的本地平台规则。
