package com.sirius.agents_demo.a2a;

import io.a2a.spec.AgentCard;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Holds the A2A identity (AgentCard) and the Spring AI ChatClient for one agent.
 */
public record AgentRegistration(AgentCard agentCard, ChatClient chatClient) {
}
