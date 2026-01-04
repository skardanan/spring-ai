package nl.gerimedica.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpenAIService {
    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    private static final String RAG_PROMPT_TEMPLATE = """
        Answer the following question based on the provided context from uploaded documents.
        If the context doesn't contain relevant information, say so clearly and answer based on your general knowledge.

        Context from uploaded documents:
        %s

        Question: %s

        Answer:
        """;

    public OpenAIService(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public String ask(String message) {
        // Search for relevant documents in the vector store
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(message)
                        .topK(5)
                        .build()
        );

        String response;
        if (relevantDocs != null && !relevantDocs.isEmpty()) {
            // Build context from retrieved documents
            String context = relevantDocs.stream()
                    .map(doc -> {
                        String filename = doc.getMetadata().getOrDefault("filename", "Unknown").toString();
                        return "[From: " + filename + "]\n" + doc.getText();
                    })
                    .collect(Collectors.joining("\n\n---\n\n"));

            log.info("Found {} relevant documents for query: {}", relevantDocs.size(), message);
            log.info("Context: {}", context);

            // Use RAG prompt with context
            String ragPrompt = String.format(RAG_PROMPT_TEMPLATE, context, message);
            ChatResponse chatResponse = chatModel.call(new Prompt(ragPrompt));
            response = chatResponse.getResult().getOutput().getText();
        } else {
            // No relevant documents found, use direct query
            log.info("No relevant documents found for query: {}", message);
            ChatResponse chatResponse = chatModel.call(new Prompt(message));
            response = chatResponse.getResult().getOutput().getText();
        }

        return response;
    }
}