package com.sirius.agents_demo;

import com.sirius.agents_demo.a2a.AgentRegistration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChatController {

    private final ChatClient orchestratorChatClient;
    private final Map<String, AgentRegistration> agentRegistrationMap;

    public ChatController(
            @Lazy @Qualifier("orchestratorChatClient") ChatClient orchestratorChatClient,
            Map<String, AgentRegistration> agentRegistrationMap) {
        this.orchestratorChatClient = orchestratorChatClient;
        this.agentRegistrationMap = agentRegistrationMap;
    }

    /**
     * Main entry point: delegates to the orchestrator which runs the full
     * supplier evaluation pipeline (info-fetch → assessment → risk → synthesis).
     * <p>
     * Example: GET /chat?message=Evaluate supplier ACME GmbH
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestParam(defaultValue = "Hello, what can you do?") String message) {
        return orchestratorChatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * Human-friendly endpoint to query a single specialist agent directly
     * (bypasses the orchestrator; useful for testing individual agents).
     * <p>
     * Example: GET /agents/risk-assessment/chat?message=Score this profile: ...
     */
    @GetMapping(value = "/agents/{agentId}/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public String agentChat(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "Hello, what can you do?") String message) {

        AgentRegistration registration = agentRegistrationMap.get(agentId);
        if (registration == null) {
            return "Unknown agent: " + agentId
                    + ". Available agents: " + String.join(", ", agentRegistrationMap.keySet());
        }
        return registration.chatClient()
                .prompt()
                .user(message)
                .call()
                .content();
    }
}
