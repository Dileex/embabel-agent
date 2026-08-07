package com.example.embabelagent.config;

import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfiguration {

    private static final List<String> KNOWLEDGE_FILES = List.of(
            "classpath:knowledge/product-P-1001.md",
            "classpath:knowledge/brand-guide.md",
            "classpath:knowledge/douyin-rules.md");

    @Bean(destroyMethod = "close")
    LuceneSearchOperations productKnowledgeSearch() {
        // 不设置indexPath时，Lucene索引只保存在当前应用内存中。
        return LuceneSearchOperations
                .withName("product-knowledge")
                .build();
    }

    @Bean
    ApplicationRunner loadProductKnowledge(
            LuceneSearchOperations productKnowledgeSearch) {
        return arguments -> {
            TikaHierarchicalContentReader reader =
                    new TikaHierarchicalContentReader();
            for (String resource : KNOWLEDGE_FILES) {
                productKnowledgeSearch.writeAndChunkDocument(
                        reader.parseResource(resource));
            }
        };
    }
}
