package com.sirius.agents_demo;

import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentExecutor;
import org.springaicommunity.agent.common.task.subagent.TaskCall;

/**
 * Wraps an existing {@link SubagentExecutor} and emits pipeline-progress
 * SSE events before and after each agent execution.
 */
public class TrackedSubagentExecutor implements SubagentExecutor {

    private final SubagentExecutor delegate;
    private final PipelineProgressTracker tracker;

    public TrackedSubagentExecutor(SubagentExecutor delegate, PipelineProgressTracker tracker) {
        this.delegate = delegate;
        this.tracker = tracker;
    }

    @Override
    public String getKind() {
        return delegate.getKind();
    }

    @Override
    public String execute(TaskCall taskCall, SubagentDefinition subagentDefinition) {
        String agentSlug = extractAgentSlug(subagentDefinition);
        String agentName = subagentDefinition.getName();
        tracker.emitStepStarted(agentSlug, agentName);
        try {
            String result = delegate.execute(taskCall, subagentDefinition);
            tracker.emitStepCompleted(agentSlug, agentName);
            return result;
        } catch (RuntimeException e) {
            tracker.emitStepCompleted(agentSlug, agentName);
            throw e;
        }
    }

    /**
     * Extracts the last path segment from the agent reference URI.
     * e.g. "http://localhost:3030/agents/info-fetch" → "info-fetch"
     */
    private static String extractAgentSlug(SubagentDefinition def) {
        String uri = def.getReference().uri();
        int lastSlash = uri.lastIndexOf('/');
        return lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
    }
}
