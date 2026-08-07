package com.example.platformrules;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class PlatformRuleTools {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PlatformRuleTools.class);

    private final Map<String, PlatformRule> rules = Map.of(
            "抖音",
            new PlatformRule(
                    "抖音",
                    300,
                    "9:16",
                    List.of(
                            "商品名称",
                            "商品卖点",
                            "价格信息",
                            "素材来源"),
                    List.of(
                            "不能把日常防泼水写成完全防水",
                            "不能虚构销量、排名、认证和效果承诺"),
                    "platform-rules-mcp",
                    "demo-2026-08"));

    @Tool(
            name = "query_platform_rule",
            description = "按平台中文名称查询商品视频发布规则")
    public PlatformRule queryPlatformRule(
            @ToolParam(
                    description = "发布平台中文名称")
            String platform) {

        log.info(
                "MCP tool query_platform_rule called, platform={}",
                platform);

        PlatformRule rule = rules.get(
                platform == null
                        ? ""
                        : platform.strip());
        if (rule == null) {
            throw new IllegalArgumentException(
                    "没有找到平台规则：" + platform);
        }
        return rule;
    }

    public record PlatformRule(
            String platform,
            int maxVideoDurationSeconds,
            String preferredAspectRatio,
            List<String> requiredInformation,
            List<String> prohibitedClaims,
            String source,
            String ruleVersion) {
    }
}
