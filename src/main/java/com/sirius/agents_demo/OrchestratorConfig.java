package com.sirius.agents_demo;

import com.sirius.agents_demo.a2a.AgentRegistration;
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
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springaicommunity.agent.utils.AgentEnvironment;
import org.springframework.ai.tool.ToolCallback;

import java.util.Map;

/**
 * Builds the orchestratorChatClient with a TaskTool wired to A2A subagent endpoints.
 * <p>
 * The orchestrator delegates to specialist agents over A2A:
 * <ol>
 *   <li>info-fetch</li>
 *   <li>initial-assessment</li>
 *   <li>risk-assessment</li>
 *   <li>synthesis</li>
 * </ol>
 */
@Configuration
public class OrchestratorConfig {

    //https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/SkillsTool.md

    @Bean
    @Lazy
    public ChatClient orchestratorChatClient(
            ChatClient.Builder builder,
            @Value("${agent.model}") String agentModel,
            @Value("${server.port:2020}") int serverPort,
            @Value("classpath:/prompt/agents/orchestrator.md") Resource systemPrompt,
            @Value("classpath:/skills/supplier-synthesis") Resource skillDir,
            Map<String, AgentRegistration> agentRegistrationMap) {

        ToolCallback taskTool = buildTaskTool(serverPort, agentRegistrationMap);

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
                .defaultToolCallbacks(taskTool)
                .build();
    }

    private ToolCallback buildTaskTool(int serverPort, Map<String, AgentRegistration> registrations) {
        TaskTool.Builder taskToolBuilder = TaskTool.builder()
                .subagentTypes(new SubagentType(new A2ASubagentResolver(), new A2ASubagentExecutor()));

        // Register each specialist agent as an A2A subagent reference
        for (String agentId : registrations.keySet()) {
            String agentUrl = "http://localhost:" + serverPort + "/agents/" + agentId;
            taskToolBuilder.subagentReferences(
                    new SubagentReference(agentUrl, A2ASubagentDefinition.KIND));
        }

        return taskToolBuilder.build();
    }
}
