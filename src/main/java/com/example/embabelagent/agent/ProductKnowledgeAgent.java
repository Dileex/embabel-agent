package com.example.embabelagent.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import com.embabel.agent.rag.model.Chunk;
import com.embabel.agent.rag.service.RagRequest;
import com.embabel.common.core.types.SimilarityResult;
import com.example.embabelagent.config.ProductKnowledgeAgentProperties;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Agent(description = "从商品资料、品牌规范和平台规则中检索证据并回答问题")
public class ProductKnowledgeAgent {

    private final LuceneSearchOperations productKnowledgeSearch;

    private final ProductKnowledgeAgentProperties properties;

    public ProductKnowledgeAgent(
            LuceneSearchOperations productKnowledgeSearch,
            ProductKnowledgeAgentProperties properties) {
        this.productKnowledgeSearch = productKnowledgeSearch;
        this.properties = properties;
    }

    @Action(description = "从本地知识资料中查找与商品问题有关的片段")
    public RetrievedKnowledge retrieveKnowledge(
            ProductKnowledgeQuestion question) {

        // 商品ID和平台一起参与向量化，
        // 让问题保留当前业务上下文。
        String query = String.join(
                " ",
                question.productId(),
                question.platform(),
                question.question());

        List<KnowledgeMatch> matches;
        // 同一时间只执行一次问题向量生成和Lucene检索，
        // 后面的DeepSeek回答生成不在锁内。
        synchronized (productKnowledgeSearch) {
            matches = productKnowledgeSearch
                    .vectorSearch(
                            RagRequest.query(query)
                                    .withTopK(6)
                                    .withSimilarityThreshold(0.5),
                            Chunk.class)
                    .stream()
                    .map(ProductKnowledgeAgent::toMatch)
                    .toList();
        }

        // 含义接近不代表资料属于当前商品和平台，
        // 再做一次业务字段校验。
        boolean hasProduct = matches.stream()
                .anyMatch(match -> match.text()
                        .contains(question.productId()));
        boolean hasPlatform = matches.stream()
                .anyMatch(match -> match.text()
                        .contains(question.platform()));
        if (!hasProduct || !hasPlatform) {
            matches = List.of();
        }

        return new RetrievedKnowledge(question, matches);
    }

    @AchievesGoal(description = "回答商品内容问题，并返回检索到的资料依据")
    @Action(description = "只根据已经检索到的资料片段组织回答")
    public ProductKnowledgeAnswer answerWithEvidence(
            RetrievedKnowledge knowledge,
            Ai ai) {

        // 没有可靠片段时直接返回，不让模型根据常识补写答案。
        if (knowledge.matches().isEmpty()) {
            return new ProductKnowledgeAnswer(
                    false,
                    "本地资料中没有找到足够依据，暂时不能回答这个问题。",
                    List.of(),
                    List.of());
        }

        // 只把已经检索到的片段放进提示词。
        KnowledgeDraft draft = ai
                .withDefaultLlm()
                .createObject(
                        buildPrompt(knowledge),
                        KnowledgeDraft.class);

        // 引用由Java根据检索结果生成，不交给模型填写。
        List<String> citations = knowledge.matches()
                .stream()
                .map(KnowledgeMatch::source)
                .distinct()
                .toList();

        return new ProductKnowledgeAnswer(
                true,
                draft.answer(),
                citations,
                knowledge.matches());
    }

    private String buildPrompt(
            RetrievedKnowledge knowledge) {
        String evidence = knowledge.matches()
                .stream()
                .map(match -> """
                        来源：%s
                        小节：%s
                        内容：%s
                        """.formatted(
                        match.source(),
                        match.section(),
                        match.text()))
                .reduce(
                        "",
                        (left, right) -> left + "\n" + right);

        return properties.answerWithEvidence()
                .replace(
                        "{productId}",
                        knowledge.question().productId())
                .replace(
                        "{platform}",
                        knowledge.question().platform())
                .replace(
                        "{question}",
                        knowledge.question().question())
                .replace(
                        "{evidence}",
                        evidence);
    }

    private static KnowledgeMatch toMatch(
            SimilarityResult<Chunk> result) {
        Chunk chunk = result.getMatch();
        Map<String, Object> metadata = chunk.getMetadata();
        String text = chunk.getUrtext().strip();
        return new KnowledgeMatch(
                sourceName(metadata),
                metadataText(
                        metadata,
                        ContentChunker.LEAF_SECTION_TITLE,
                        firstLine(text)),
                text,
                result.getScore());
    }

    private static String firstLine(String text) {
        int lineBreak = text.indexOf('\n');
        return lineBreak < 0
                ? text
                : text.substring(0, lineBreak).strip();
    }

    private static String sourceName(
            Map<String, Object> metadata) {
        String uri = metadataText(
                metadata,
                ContentChunker.LEAF_SECTION_URL,
                "");
        if (uri.isBlank()) {
            uri = metadataText(
                    metadata,
                    ContentChunker.CONTAINER_SECTION_URL,
                    "unknown");
        }
        int separator = Math.max(
                uri.lastIndexOf('/'),
                uri.lastIndexOf(':'));
        return separator >= 0
                ? uri.substring(separator + 1)
                : uri;
    }

    private static String metadataText(
            Map<String, Object> metadata,
            String key,
            String fallback) {
        return Objects.toString(
                metadata.get(key),
                fallback);
    }

    public static ProductKnowledgeQuestion parseRequest(
            String rawMessage) {
        String message =
                rawMessage == null ? "" : rawMessage.strip();
        return new ProductKnowledgeQuestion(
                readSection(
                        message,
                        "商品ID：",
                        "发布平台："),
                readSection(
                        message,
                        "发布平台：",
                        "问题："),
                readSection(
                        message,
                        "问题：",
                        null));
    }

    private static String readSection(
            String message,
            String startMarker,
            String endMarker) {
        int start = message.indexOf(startMarker);
        if (start < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "缺少字段：" + startMarker);
        }
        int contentStart = start + startMarker.length();
        int end = endMarker == null
                ? message.length()
                : message.indexOf(
                        endMarker,
                        contentStart);
        if (endMarker != null && end < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "缺少字段：" + endMarker);
        }
        String value = message
                .substring(contentStart, end)
                .strip()
                .replaceFirst("[；;]+$", "")
                .strip();
        if (value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "字段不能为空：" + startMarker);
        }
        return value;
    }

    public record ProductKnowledgeQuestion(
            @NotBlank String productId,
            @NotBlank String platform,
            @NotBlank String question) {
    }

    public record RetrievedKnowledge(
            ProductKnowledgeQuestion question,
            List<KnowledgeMatch> matches) {
    }

    public record KnowledgeMatch(
            @NotBlank String source,
            @NotBlank String section,
            @NotBlank String text,
            double score) {
    }

    public record KnowledgeDraft(
            @NotBlank String answer) {
    }

    public record ProductKnowledgeAnswer(
            boolean found,
            @NotBlank String answer,
            List<String> citations,
            List<KnowledgeMatch> retrievedChunks) {
    }
}
