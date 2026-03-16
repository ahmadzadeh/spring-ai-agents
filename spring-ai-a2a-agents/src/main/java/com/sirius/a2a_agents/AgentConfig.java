package com.sirius.a2a_agents;

import com.sirius.a2a_agents.tools.TavilySearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.utils.AgentEnvironment;

@Configuration
public class AgentConfig {

	// -------------------------------------------------------------------------
	// InfoFetch agent: web search; minimal reasoning, no file write
	// -------------------------------------------------------------------------
	@Bean
	public ChatClient infoFetchChatClient(
			ChatClient.Builder builder,
			@Value("${agent.model}") String agentModel,
			@Value("classpath:/prompt/agents/info-fetch.md") Resource systemPrompt,
			@Value("classpath:/skills/supplier-info-fetch") Resource skillDir,
			@Value("${tavily.api-key:}") String tavilyApiKey) {

		var clientBuilder = builder.clone()
				.defaultSystem(p -> p.text(systemPrompt)
						.param(AgentEnvironment.AGENT_MODEL_KEY, agentModel))
				.defaultAdvisors(
						ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
						MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(100).build()).build())
				.defaultToolCallbacks(
						SkillsTool.builder().addSkillsResource(skillDir).build());

		if (tavilyApiKey != null && !tavilyApiKey.isBlank()) {
			clientBuilder.defaultTools(new TavilySearchTool(tavilyApiKey));
		}

		return clientBuilder.build();
	}

	// -------------------------------------------------------------------------
	// InitialAssessment agent: pure reasoning + template output
	// -------------------------------------------------------------------------
	@Bean
	public ChatClient initialAssessmentChatClient(
			ChatClient.Builder builder,
			@Value("${agent.model}") String agentModel,
			@Value("classpath:/prompt/agents/initial-assessment.md") Resource systemPrompt,
			@Value("classpath:/skills/supplier-initial-assessment") Resource skillDir) {

		return builder.clone()
				.defaultSystem(p -> p.text(systemPrompt)
						.param(AgentEnvironment.AGENT_MODEL_KEY, agentModel))
				.defaultAdvisors(
						ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
						MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(100).build()).build())
				.defaultToolCallbacks(
						SkillsTool.builder().addSkillsResource(skillDir).build())
				.build();
	}

	// -------------------------------------------------------------------------
	// RiskAssessment agent: risk rubric skill + structured scoring
	// -------------------------------------------------------------------------
	@Bean
	public ChatClient riskAssessmentChatClient(
			ChatClient.Builder builder,
			@Value("${agent.model}") String agentModel,
			@Value("classpath:/prompt/agents/risk-assessment.md") Resource systemPrompt,
			@Value("classpath:/skills/supplier-risk-assessment") Resource skillDir) {

		return builder.clone()
				.defaultSystem(p -> p.text(systemPrompt)
						.param(AgentEnvironment.AGENT_MODEL_KEY, agentModel))
				.defaultAdvisors(
						ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
						MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(100).build()).build())
				.defaultToolCallbacks(
						SkillsTool.builder().addSkillsResource(skillDir).build())
				.build();
	}

	// -------------------------------------------------------------------------
	// Synthesis agent: consolidation skill; can write the final report to disk
	// -------------------------------------------------------------------------
	@Bean
	public ChatClient synthesisChatClient(
			ChatClient.Builder builder,
			@Value("${agent.model}") String agentModel,
			@Value("classpath:/prompt/agents/synthesis.md") Resource systemPrompt,
			@Value("classpath:/skills/supplier-synthesis") Resource skillDir) {

		return builder.clone()
				.defaultSystem(p -> p.text(systemPrompt)
						.param(AgentEnvironment.AGENT_MODEL_KEY, agentModel))
				.defaultAdvisors(
						ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
						MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(100).build()).build())
				.defaultTools(
						FileSystemTools.builder().build())
				.defaultToolCallbacks(
						SkillsTool.builder().addSkillsResource(skillDir).build())
				.build();
	}

}
