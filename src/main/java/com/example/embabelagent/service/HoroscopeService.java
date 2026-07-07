package com.example.embabelagent.service;

import java.util.Map;

import com.example.embabelagent.agent.StarNewsAgent.Horoscope;
import org.springframework.stereotype.Service;

@Service
public class HoroscopeService {

    private static final Map<String, String> HOROSCOPES = Map.ofEntries(
            Map.entry("白羊座", "适合把手头任务拆小，先完成最关键的一步。沟通时少绕弯，直接说结论会更顺。"),
            Map.entry("金牛座", "今天适合处理预算、排期和资源确认。别急着拍板，先把边界条件问清楚。"),
            Map.entry("双子座", "信息会比较多，适合做整理和表达。先抓主线，再补细节，效率会更高。"),
            Map.entry("巨蟹座", "适合修复关系和补齐遗漏。遇到卡点时，先确认对方真正担心的是什么。"),
            Map.entry("狮子座", "适合承担一个明确的推进角色。注意把目标拆成可执行动作，不要只停留在口号上。"),
            Map.entry("处女座", "适合做检查、复盘和质量改进。细节会帮你避开一个后面更麻烦的问题。"),
            Map.entry("天秤座", "适合协调多人意见。先把共同目标摆出来，再处理分歧，会少很多拉扯。"),
            Map.entry("天蝎座", "适合处理复杂问题。不要只看表面现象，顺着线索往下挖，会有新发现。"),
            Map.entry("射手座", "适合打开思路，但要给想法配一个落地动作。今天不缺方向，缺的是收束。"),
            Map.entry("摩羯座", "适合推进长期任务。把进度、风险和下一步写清楚，会让协作更稳。"),
            Map.entry("水瓶座", "适合尝试新方案。先做一个小验证，不要一开始就把方案铺得太大。"),
            Map.entry("双鱼座", "适合做创意表达和用户体验相关的判断。注意把感受转成具体改动。"));

    public Horoscope dailyHoroscope(String sign) {
        String summary = HOROSCOPES.getOrDefault(sign, "今天适合先把目标说清楚，再决定下一步动作。");
        return new Horoscope(sign, summary);
    }

}
