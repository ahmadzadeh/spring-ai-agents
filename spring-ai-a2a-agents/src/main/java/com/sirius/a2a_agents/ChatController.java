package com.sirius.a2a_agents;

import com.sirius.a2a_agents.a2a.AgentRegistration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChatController {

    private final Map<String, AgentRegistration> agentRegistrationMap;

    public ChatController(Map<String, AgentRegistration> agentRegistrationMap) {
        this.agentRegistrationMap = agentRegistrationMap;
    }

    /**
     * Human-friendly endpoint to query a single specialist agent directly
     * (useful for testing individual agents).
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
