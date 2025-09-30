package nl.gerimedica.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel, VectorStore vectorStore) {
        vectorStore.add(List.of(
                Document.builder().text("Group A: Alice, Bob, Charlie").build(),
                Document.builder().text("Group B: David, Eve, Frank").build(),
                Document.builder().text("Group C: Grace, Heidi, Ivan").build(),
                Document.builder().text("Group D: Judy, Mallory, Nathan").build(),
                Document.builder().text("Group E: Olivia, Peggy, Quentin").build(),
                Document.builder().text("Group F: Rupert, Sybil, Trent").build(),
                Document.builder().text("Group G: Uma, Victor, Wendy").build(),
                Document.builder().text("Group H: Xavier, Yvonne, Zach").build()
        ));

        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.7)
                        .topK(3)
                        .build())
                .build();

        return ChatClient.builder(chatModel)
                .defaultAdvisors(qaAdvisor)
                .build();
    }

}