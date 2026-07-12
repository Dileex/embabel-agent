package com.example.embabelagent.agent;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.example.embabelagent.config.QuizAgentProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Agent(description = "根据技术文章内容生成面向开发者学习复盘的测验题")
public class QuizAgent {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final int MAX_ARTICLE_CHARS = 12_000;

    private final QuizAgentProperties properties;

    public QuizAgent(QuizAgentProperties properties) {
        this.properties = properties;
    }

    @Action
    public ArticleInput extractArticle(UserInput userInput) {
        String rawInput = userInput.getContent();
        return extractFirstUrl(rawInput)
                .map(QuizAgent::fetchArticle)
                .orElseGet(() -> {
                    String content = limitLength(extractMarkdownArticle(rawInput));
                    return new ArticleInput(extractTitle(content), content, null);
                });
    }

    @Action
    public ConceptDigest extractConcepts(ArticleInput article, Ai ai) {
        String prompt = properties.extractConcepts()
                .replace("{title}", article.title())
                .replace("{content}", article.content());
        return ai.withDefaultLlm().createObject(prompt, ConceptDigest.class);
    }

    @Action
    public QuizDraft generateQuiz(ArticleInput article, ConceptDigest digest, Ai ai) {
        String prompt = properties.generateQuiz()
                .replace("{title}", article.title())
                .replace("{concepts}", renderConcepts(digest.concepts()))
                .replace("{content}", article.content());
        return ai.withDefaultLlm().createObject(prompt, QuizDraft.class);
    }

    @AchievesGoal(description = "生成一套可用于技术文章复盘的测验题")
    @Action
    public QuizPack reviewQuiz(ArticleInput article, QuizDraft draft, Ai ai) {
        String prompt = properties.reviewQuiz()
                .replace("{title}", article.title())
                .replace("{questions}", renderQuestions(draft.questions()));
        return ai.withDefaultLlm().createObject(prompt, QuizPack.class);
    }

    private static String renderConcepts(List<ConceptPoint> concepts) {
        return concepts.stream()
                .map(concept -> "- " + concept.name() + "：" + concept.explanation())
                .collect(Collectors.joining("\n"));
    }

    private static String renderQuestions(List<QuizQuestion> questions) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < questions.size(); index++) {
            QuizQuestion question = questions.get(index);
            builder.append(index + 1).append(". ").append(question.question()).append('\n')
                    .append("选项：").append(String.join("；", question.options())).append('\n')
                    .append("答案：").append(question.answer()).append('\n')
                    .append("解释：").append(question.explanation());
            if (index < questions.size() - 1) {
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }

    private static Optional<String> extractFirstUrl(String rawInput) {
        Matcher matcher = URL_PATTERN.matcher(rawInput == null ? "" : rawInput);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group().replaceAll("[，。,.!！?？）)]+$", ""));
    }

    private static ArticleInput fetchArticle(String url) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; EmbabelQuizzard/1.0)")
                    .timeout(15_000)
                    .get();

            String title = firstNonBlank(
                    document.select("meta[property=og:title]").attr("content"),
                    document.selectFirst("h1") != null ? document.selectFirst("h1").text() : "",
                    document.title(),
                    url);
            String content = limitLength(cleanArticleText(document));
            if (content.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Fetched article content is empty");
            }
            return new ArticleInput(title, content, url);
        }
        catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch article URL", ex);
        }
    }

    private static String cleanArticleText(Document document) {
        document.select("script, style, nav, header, footer, aside, form, noscript, svg").remove();
        Element article = firstElement(
                document.selectFirst("article"),
                document.selectFirst("main"),
                document.body());
        return article == null ? "" : article.text().replaceAll("\\s+", " ").strip();
    }

    private static Element firstElement(Element... elements) {
        for (Element element : elements) {
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return "未命名技术文章";
    }

    private static String limitLength(String content) {
        if (content == null) {
            return "";
        }
        String text = content.strip();
        return text.length() <= MAX_ARTICLE_CHARS ? text : text.substring(0, MAX_ARTICLE_CHARS);
    }

    private static String extractMarkdownArticle(String rawInput) {
        String text = rawInput == null ? "" : rawInput.strip();
        String[] lines = text.split("\\R");
        for (int index = 0; index < lines.length; index++) {
            if (lines[index].startsWith("# ")) {
                return joinLines(lines, index).strip();
            }
        }
        return text;
    }

    private static String extractTitle(String content) {
        String[] lines = content.split("\\R");
        for (String line : lines) {
            if (line.startsWith("# ")) {
                return line.substring(2).strip();
            }
            if (line.startsWith("标题：")) {
                return line.substring("标题：".length()).strip();
            }
        }
        for (String line : lines) {
            String candidate = line.strip();
            if (!candidate.isEmpty()) {
                return candidate.length() <= 60 ? candidate : candidate.substring(0, 60);
            }
        }
        return "未命名技术文章";
    }

    private static String joinLines(String[] lines, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < lines.length; index++) {
            if (index > startIndex) {
                builder.append('\n');
            }
            builder.append(lines[index]);
        }
        return builder.toString();
    }

    public record ArticleInput(
            @NotBlank String title,
            @NotBlank String content,
            String sourceUrl) {
    }

    public record ConceptDigest(
            @NotEmpty
            @Size(min = 3, max = 5)
            List<@Valid ConceptPoint> concepts) {
    }

    public record ConceptPoint(
            @NotBlank String name,
            @NotBlank String explanation) {
    }

    public record QuizDraft(
            @NotEmpty
            @Size(min = 3, max = 3)
            List<@Valid QuizQuestion> questions) {
    }

    public record QuizQuestion(
            @NotBlank String question,
            @NotEmpty
            @Size(min = 4, max = 4)
            List<@NotBlank String> options,
            @NotBlank String answer,
            @NotBlank String explanation) {
    }

    public record QuizPack(
            @NotBlank String title,
            @NotEmpty
            @Size(min = 3, max = 3)
            List<@Valid QuizQuestion> questions,
            @NotBlank String review) {
    }

}
