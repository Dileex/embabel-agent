# 第13篇：Agent怎么调用MCP文件工具

接口：

```text
POST /agent/mcp-publish-check
```

执行链：

```text
ProductPublishRequest
->RemotePlatformToolAgent
->本地商品、库存和店铺工具
->filesystem MCP读取平台规则
->RemotePlatformPublishAssessment
```

平台规则文件：

```text
mcp-files/douyin-rules.md
```

启动：

```bash
export DEEPSEEK_API_KEY=<your-deepseek-api-key>
mvn spring-boot:run
```

主应用会通过stdio启动：

```text
npx -y @modelcontextprotocol/server-filesystem@2026.7.10 \
  <项目目录>/mcp-files
```

这里使用stdio只是为了让示例少启动一个服务。实际项目可以把MCP Server独立部署，再通过Streamable HTTP等方式连接远程服务。

请求：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/mcp-publish-check" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-demo" \
  -d '{
    "message": "商品ID：P-1001；店铺ID：S-2001；发布平台：抖音；视频时长：360秒"
  }'
```

三个本地工具查询商品、库存和店铺，MCP工具`read_text_file`读取平台规则。

响应中的`localCalledTools`应包含：

```text
query_product
query_inventory_price
query_store
```

规则字段应包含：

```text
platformRuleSource=platform-rules-mcp
platformRuleVersion=demo-2026-08
publishable=false
```

主应用日志应出现：

```text
calling tool read_text_file
```
