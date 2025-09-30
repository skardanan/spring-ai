package nl.gerimedica.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenAIService {
    private final ChatModel chatModel;

    private static final String RAG_PROMPT_TEMPLATE = """
        Answer the following question based on the provided context.
        If the context doesn't contain relevant information, say so clearly.
        
        Context:
        {context}
        
        Question: {question}
        
        Answer:
        """;

    public OpenAIService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String ask(String message) {
        ChatResponse response = chatModel.call(new Prompt(message));
        return response.getResult().getOutput().getText();
    }
}