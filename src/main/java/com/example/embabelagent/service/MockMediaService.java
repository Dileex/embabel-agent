package com.example.embabelagent.service;

import com.embabel.agent.core.ActionException;
import com.example.embabelagent.agent.ProductMediaRetryAgent.MediaTaskRequest;
import com.example.embabelagent.agent.ProductMediaRetryAgent.MediaTaskResult;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class MockMediaService {

    private final ConcurrentHashMap<String, AtomicInteger> attempts =
            new ConcurrentHashMap<>();

    public MediaTaskResult createTask(MediaTaskRequest request) {
        int attempt = attempts
                .computeIfAbsent(
                        request.requestId(),
                        ignored -> new AtomicInteger())
                .incrementAndGet();

        if (attempt <= request.failuresBeforeSuccess()) {
            throw new ActionException.Transient(
                    "模拟素材服务超时，第" + attempt + "次调用失败",
                    null);
        }

        return new MediaTaskResult(
                request.requestId(),
                "MEDIA-" + request.requestId(),
                "CREATED",
                attempt);
    }

    public int attempts(String requestId) {
        AtomicInteger counter = attempts.get(requestId);
        return counter == null ? 0 : counter.get();
    }
}
