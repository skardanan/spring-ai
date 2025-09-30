package nl.gerimedica.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class OpenAIService {
    private final ChatClient chatClient;

    public OpenAIService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String ask(String message) {
        return chatClient.prompt().user(message).call().content();
    }
}