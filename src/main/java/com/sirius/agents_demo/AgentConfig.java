package com.sirius.agents_demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.GlobTool;
import org.springaicommunity.agent.tools.GrepTool;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.utils.AgentEnvironment;

import java.util.List;

@Configuration
public class AgentConfig {

	@Bean
	public ChatClient agentChatClient(ChatClient.Builder builder,
			@Value("${agent.model:gpt-4o}") String agentModel,
			@Value("${agent.model.knowledge.cutoff:2025-01-01}") String knowledgeCutoff,
			@Value("classpath:/prompt/agent-system-prompt.md") Resource systemPromptResource,
			@Value("${agent.skills.paths:#{null}}") List<Resource> skillPaths) {

		var chatClientBuilder = builder
				.defaultSystem(p -> p.text(systemPromptResource)
						.param(AgentEnvironment.ENVIRONMENT_INFO_KEY, AgentEnvironment.info())
						.param(AgentEnvironment.GIT_STATUS_KEY, AgentEnvironment.gitStatus())
						.param(AgentEnvironment.AGENT_MODEL_KEY, agentModel)
						.param(AgentEnvironment.AGENT_MODEL_KNOWLEDGE_CUTOFF_KEY, knowledgeCutoff))
				.defaultTools(
						ShellTools.builder().build(),
						FileSystemTools.builder().build(),
						GrepTool.builder().build(),
						GlobTool.builder().build(),
						TodoWriteTool.builder().build())
				.defaultAdvisors(
						ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
						MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(500).build()).build());

		if (skillPaths != null && !skillPaths.isEmpty()) {
			chatClientBuilder.defaultToolCallbacks(
					SkillsTool.builder().addSkillsResources(skillPaths).build());
		}

		return chatClientBuilder.build();
	}
}
