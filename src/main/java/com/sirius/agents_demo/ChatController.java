package com.sirius.agents_demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

	private final ChatClient agentChatClient;

	public ChatController(@Qualifier("agentChatClient") ChatClient agentChatClient) {
		this.agentChatClient = agentChatClient;
	}

	@GetMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
	public String chat(@RequestParam(defaultValue = "Hello, what can you do?") String message) {
		return agentChatClient.prompt()
				.user(message)
				.call()
				.content();
	}
}
