# 第12篇：Agent怎么先查资料再回答

分支：`article/012-local-rag`

接口：

```text
POST /agent/product-knowledge
```

执行链：

```text
ProductKnowledgeQuestion
->retrieveKnowledge(...)
->RetrievedKnowledge
->answerWithEvidence(...)
->ProductKnowledgeAnswer
```

知识资料：

```text
src/main/resources/knowledge/product-P-1001.md
src/main/resources/knowledge/brand-guide.md
src/main/resources/knowledge/douyin-rules.md
```

启动：

```bash
export DEEPSEEK_API_KEY=<your-deepseek-api-key>
mvn spring-boot:run
```

请求：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/product-knowledge" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "商品ID：P-1001\n发布平台：抖音\n问题：这款双肩包做30秒通勤视频时，哪些卖点可以说，哪些内容不能说？"
  }'
```

响应中的`citations`来自真实检索文件，`retrievedChunks`保留命中的片段和Lucene排序分数。商品ID或平台资料缺失时，接口返回`found=false`，不会继续调用模型生成答案。
