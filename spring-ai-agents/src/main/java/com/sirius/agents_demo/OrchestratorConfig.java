package com.sirius.agents_demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;
import org.springaicommunity.agent.common.task.subagent.SubagentType;
import org.springaicommunity.agent.subagent.a2a.A2ASubagentDefinition;
import org.springaicommunity.agent.subagent.a2a.A2ASubagentExecutor;
import org.springaicommunity.agent.subagent.a2a.A2ASubagentResolver;
import org.springaicommunity.agent.common.task.subagent.SubagentExecutor;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.tools.task.TaskOutputTool;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springaicommunity.agent.tools.task.repository.DefaultTaskRepository;
import org.springaicommunity.agent.tools.task.repository.TaskRepository;
import org.springaicommunity.agent.utils.AgentEnvironment;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * Builds the orchestratorChatClient with a TaskTool wired to remote A2A subagent endpoints.
 * <p>
 * The orchestrator delegates to specialist agents running on the A2A agents service (port 3030):
 * <ol>
 *   <li>info-fetch</li>
 *   <li>initial-assessment</li>
 *   <li>risk-assessment</li>
 *   <li>synthesis</li>
 * </ol>
 */
@Configuration
public class OrchestratorConfig {

    private static final String A2A_AGENTS_BASE_URL = "http://localhost:3030/agents/";

    private static final List<String> AGENT_IDS = List.of(
            "info-fetch",
            "initial-assessment",
            "risk-assessment",
            "synthesis"
    );

    private final PipelineProgressTracker progressTracker;

    public OrchestratorConfig(PipelineProgressTracker progressTracker) {
        this.progressTracker = progressTracker;
    }

    @Bean
    @Lazy
    public ChatClient orchestratorChatClient(
            ChatClient.Builder builder,
            @Value("${agent.model}") String agentModel,
            @Value("classpath:/prompt/agents/orchestrator.md") Resource systemPrompt,
            @Value("classpath:/skills/supplier-synthesis") Resource skillDir) {

        TaskRepository taskRepository = new DefaultTaskRepository();
        ToolCallback taskTool = buildTaskTool(taskRepository);
        ToolCallback taskOutputTool = TaskOutputTool.builder()
                .taskRepository(taskRepository)
                .build();

        return builder.clone()
                .defaultSystem(p -> p.text(systemPrompt)
                        .param(AgentEnvironment.ENVIRONMENT_INFO_KEY, AgentEnvironment.info())
                        .param(AgentEnvironment.GIT_STATUS_KEY, AgentEnvironment.gitStatus())
                        .param(AgentEnvironment.AGENT_MODEL_KEY, agentModel))
                .defaultAdvisors(
                        ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(500).build()).build())
                .defaultTools(
                        ShellTools.builder().build(),
                        FileSystemTools.builder().build(),
                        GrepTool.builder().build(),
                        GlobTool.builder().build(),
                        TodoWriteTool.builder().build())
                .defaultToolCallbacks(
                        SkillsTool.builder().addSkillsResource(skillDir).build())
                .defaultToolCallbacks(taskTool, taskOutputTool)
                .build();
    }

    private ToolCallback buildTaskTool(TaskRepository taskRepository) {
        SubagentExecutor trackedExecutor = new TrackedSubagentExecutor(
                new A2ASubagentExecutor(), progressTracker);
        TaskTool.Builder taskToolBuilder = TaskTool.builder()
                .taskRepository(taskRepository)
                .subagentTypes(new SubagentType(new A2ASubagentResolver(), trackedExecutor));

        for (String agentId : AGENT_IDS) {
            String agentUrl = A2A_AGENTS_BASE_URL + agentId;
            taskToolBuilder.subagentReferences(
                    new SubagentReference(agentUrl, A2ASubagentDefinition.KIND));
        }

        return taskToolBuilder.build();
    }
}
