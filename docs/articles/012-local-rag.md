# 第12篇：商品资料很多，Agent怎么只找这次需要的内容

分支：`article/012-local-rag`

接口：

```text
POST /agent/product-knowledge
```

处理过程：

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
ollama pull bge-m3
ollama serve

export DEEPSEEK_API_KEY=<your-deepseek-api-key>
mvn spring-boot:run
```

请求：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/product-knowledge" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "商品ID：P-1001\n发布平台：抖音\n问题：背包遇到小雨还能正常使用吗？视频里能不能说完全不怕水？"
  }'
```

Ollama的`bge-m3`负责把资料和问题转换成向量，DeepSeek负责根据检索片段生成回答。响应中的`citations`来自真实检索文件，`retrievedChunks`保留命中的片段和Lucene向量检索分数。商品ID或平台资料缺失时，接口返回`found=false`，不会继续调用模型生成答案。

空结果请求：

```bash
curl -sS -X POST \
  "http://localhost:8080/agent/product-knowledge" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "商品ID：P-9009\n发布平台：火星商城\n问题：量子纠缠咖啡机的十年保修政策是什么？"
  }'
```
