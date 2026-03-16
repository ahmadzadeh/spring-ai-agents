package com.sirius.agents_demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks the progress of the supplier evaluation pipeline and pushes
 * Server-Sent Events to connected clients.
 */
@Component
public class PipelineProgressTracker {

    private static final Logger log = LoggerFactory.getLogger(PipelineProgressTracker.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 min timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void emitStepStarted(String agentId, String agentName) {
        send("step-started", "{\"agentId\":\"" + escapeJson(agentId) + "\",\"agentName\":\"" + escapeJson(agentName) + "\"}");
    }

    public void emitStepCompleted(String agentId, String agentName) {
        send("step-completed", "{\"agentId\":\"" + escapeJson(agentId) + "\",\"agentName\":\"" + escapeJson(agentName) + "\"}");
    }

    public void emitPipelineDone() {
        send("pipeline-done", "{}");
    }

    private void send(String eventName, String data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.debug("Removing broken SSE emitter: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
