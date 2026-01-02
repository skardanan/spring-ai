package nl.gerimedica.controller;

import lombok.RequiredArgsConstructor;
import nl.gerimedica.domain.DocumentUploadRequest;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentController {

    private final VectorStore vectorStore;

    @PostMapping("/upload")
    public String uploadDocument(@RequestBody DocumentUploadRequest request) {
        Document doc = new Document(
                request.getText(),
                request.getMetadata() != null ? request.getMetadata() : Map.of("id", request.getId())
        );

        vectorStore.add(List.of(doc));
        return "Document uploaded successfully with id: " + request.getId();
    }
}
