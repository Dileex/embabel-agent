package com.example.embabelagent.config;

import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.common.ai.model.EmbeddingService;
import com.embabel.common.ai.model.SpringAiEmbeddingService;
import java.util.List;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "demo.integrations",
        name = "rag-enabled",
        havingValue = "true")
public class RagConfiguration {

    private static final List<String> KNOWLEDGE_FILES = List.of(
            "classpath:knowledge/product-P-1001.md",
            "classpath:knowledge/brand-guide.md",
            "classpath:knowledge/douyin-rules.md");

    @Bean
    EmbeddingService productEmbeddingService(
            OllamaEmbeddingModel ollamaEmbeddingModel,
            @Value("${spring.ai.ollama.embedding.model}")
            String modelName) {

        // Spring AI调用本地Ollama，
        // 这里把它适配成Embabel需要的EmbeddingService。
        return new SpringAiEmbeddingService(
                modelName,
                "Ollama",
                ollamaEmbeddingModel,
                1024,
                null);
    }

    @Bean(destroyMethod = "close")
    LuceneSearchOperations productKnowledgeSearch(
            EmbeddingService productEmbeddingService) {

        // 不设置indexPath时，Lucene索引只保存在当前应用内存中。
        return LuceneSearchOperations
                .withName("product-knowledge")
                .withEmbeddingService(productEmbeddingService)
                .build();
    }

    @Bean
    ApplicationRunner loadProductKnowledge(
            LuceneSearchOperations productKnowledgeSearch) {
        return arguments -> {
            TikaHierarchicalContentReader reader =
                    new TikaHierarchicalContentReader();

            // 启动时读取资料，
            // 每个Chunk通过Ollama生成向量后写入Lucene。
            for (String resource : KNOWLEDGE_FILES) {
                productKnowledgeSearch
                        .writeAndChunkDocument(
                                reader.parseResource(resource));
            }
        };
    }
}
