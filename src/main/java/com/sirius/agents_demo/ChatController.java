package com.sirius.agents_demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient orchestratorChatClient;

    public ChatController(
            @Lazy @Qualifier("orchestratorChatClient") ChatClient orchestratorChatClient) {
        this.orchestratorChatClient = orchestratorChatClient;
    }

    /**
     * Main entry point: delegates to the orchestrator which runs the full
     * supplier evaluation pipeline via remote A2A agents on port 3030.
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
}
