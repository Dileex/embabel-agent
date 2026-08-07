package com.example.embabelagent.skill;

import com.embabel.agent.api.reference.LlmReference;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.api.tool.ToolCallContext;
import com.embabel.agent.skills.Skills;
import com.example.embabelagent.config.ProductSkillAgentProperties;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProductContentSkills {

    private final List<LlmReference> references;

    public ProductContentSkills(
            ProductSkillAgentProperties properties) {
        Path skillPath = Path.of(
                        properties.skillPath())
                .toAbsolutePath()
                .normalize();

        Skills skills = new Skills(
                "product-content-skills",
                "商品内容生产可使用的业务Skill")
                .withLocalSkill(
                        skillPath.toString());

        this.references = List.copyOf(
                skills.asIndividualReferences());
    }

    public SkillSession newSession() {
        Set<String> calledTools =
                Collections.synchronizedSet(
                        new LinkedHashSet<>());

        List<LlmReference> trackedReferences =
                references.stream()
                        .map(reference ->
                                trackReference(
                                        reference,
                                        calledTools))
                        .toList();

        return new SkillSession(
                trackedReferences,
                calledTools);
    }

    private static LlmReference trackReference(
            LlmReference reference,
            Set<String> calledTools) {
        List<Tool> tools = reference.tools()
                .stream()
                .map(tool -> (Tool) new TrackingTool(
                        tool,
                        calledTools))
                .toList();

        return LlmReference.of(
                reference.getName(),
                reference.getDescription(),
                tools,
                reference.notes());
    }

    public static final class SkillSession {

        private final List<LlmReference> references;

        private final Set<String> calledTools;

        private SkillSession(
                List<LlmReference> references,
                Set<String> calledTools) {
            this.references = references;
            this.calledTools = calledTools;
        }

        public List<LlmReference> references() {
            return references;
        }

        public List<String> calledTools() {
            synchronized (calledTools) {
                return new ArrayList<>(
                        calledTools);
            }
        }
    }

    private static final class TrackingTool
            implements Tool {

        private final Tool delegate;

        private final Set<String> calledTools;

        private TrackingTool(
                Tool delegate,
                Set<String> calledTools) {
            this.delegate = delegate;
            this.calledTools = calledTools;
        }

        @Override
        public Definition getDefinition() {
            return delegate.getDefinition();
        }

        @Override
        public Metadata getMetadata() {
            return delegate.getMetadata();
        }

        @Override
        public Result call(String input) {
            calledTools.add(
                    delegate.getDefinition()
                            .getName());
            return delegate.call(input);
        }

        @Override
        public Result call(
                String input,
                ToolCallContext context) {
            calledTools.add(
                    delegate.getDefinition()
                            .getName());
            return delegate.call(
                    input,
                    context);
        }
    }
}
