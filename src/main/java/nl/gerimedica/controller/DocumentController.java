package nl.gerimedica.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.gerimedica.domain.DocumentUploadRequest;
import nl.gerimedica.service.DocumentParserService;
import nl.gerimedica.service.TextChunkingService;
import nl.gerimedica.service.WebCrawlerService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentController {

    private final VectorStore vectorStore;
    private final DocumentParserService documentParserService;
    private final TextChunkingService textChunkingService;
    private final WebCrawlerService webCrawlerService;

    @PostMapping("/upload")
    public String uploadDocument(@RequestBody DocumentUploadRequest request) {
        // Chunk the text if it's large
        List<String> chunks = textChunkingService.chunkText(request.getText());
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>(request.getMetadata() != null ? request.getMetadata() : Map.of());
            metadata.put("id", request.getId());
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());

            documents.add(new Document(chunks.get(i), metadata));
        }

        vectorStore.add(documents);
        return "Document uploaded successfully with id: " + request.getId() + " (" + chunks.size() + " chunks)";
    }

    @PostMapping("/upload-file")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "File is empty");
            return ResponseEntity.badRequest().body(response);
        }

        String filename = file.getOriginalFilename();
        if (!documentParserService.isSupportedFileType(filename)) {
            response.put("success", false);
            response.put("message", "Unsupported file type: " + filename);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String extractedText = documentParserService.extractText(file);

            if (extractedText == null || extractedText.isBlank()) {
                response.put("success", false);
                response.put("message", "Could not extract text from file: " + filename);
                return ResponseEntity.badRequest().body(response);
            }

            String contentType = documentParserService.detectContentType(file);
            String uploadTime = Instant.now().toString();

            // Chunk the text into smaller pieces
            List<String> chunks = textChunkingService.chunkText(extractedText);
            List<Document> documents = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("filename", filename);
                metadata.put("contentType", contentType);
                metadata.put("size", file.getSize());
                metadata.put("uploadedAt", uploadTime);
                metadata.put("chunkIndex", i);
                metadata.put("totalChunks", chunks.size());

                documents.add(new Document(chunks.get(i), metadata));
            }

            vectorStore.add(documents);

            log.info("Successfully uploaded and indexed file: {} ({} characters, {} chunks)",
                    filename, extractedText.length(), chunks.size());

            response.put("success", true);
            response.put("message", "File uploaded and indexed successfully");
            response.put("filename", filename);
            response.put("contentType", contentType);
            response.put("textLength", extractedText.length());
            response.put("chunks", chunks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing file: {}", filename, e);
            response.put("success", false);
            response.put("message", "Error processing file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/crawl")
    public ResponseEntity<Map<String, Object>> crawlUrl(@RequestBody CrawlRequest request) {
        Map<String, Object> response = new HashMap<>();

        String url = request.url();
        if (url == null || url.isBlank()) {
            response.put("success", false);
            response.put("message", "URL is required");
            return ResponseEntity.badRequest().body(response);
        }

        // Validate URL format
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            int maxPages = request.maxPages() != null ? request.maxPages() : 1;
            WebCrawlerService.CrawlResult result;

            if (maxPages > 1) {
                result = webCrawlerService.crawlUrlWithLinks(url, maxPages);
            } else {
                result = webCrawlerService.crawlUrl(url);
            }

            if (result.text() == null || result.text().isBlank()) {
                response.put("success", false);
                response.put("message", "Could not extract text from URL: " + url);
                return ResponseEntity.badRequest().body(response);
            }

            String uploadTime = Instant.now().toString();

            // Chunk the text into smaller pieces
            List<String> chunks = textChunkingService.chunkText(result.text());
            List<Document> documents = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", "web");
                metadata.put("url", result.url());
                metadata.put("title", result.title());
                metadata.put("domain", result.domain());
                metadata.put("crawledAt", uploadTime);
                metadata.put("chunkIndex", i);
                metadata.put("totalChunks", chunks.size());

                documents.add(new Document(chunks.get(i), metadata));
            }

            vectorStore.add(documents);

            log.info("Successfully crawled and indexed URL: {} ({} characters, {} chunks)",
                    result.url(), result.text().length(), chunks.size());

            response.put("success", true);
            response.put("message", "Website crawled and indexed successfully");
            response.put("url", result.url());
            response.put("title", result.title());
            response.put("domain", result.domain());
            response.put("textLength", result.text().length());
            response.put("chunks", chunks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error crawling URL: {}", url, e);
            response.put("success", false);
            response.put("message", "Error crawling URL: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    public record CrawlRequest(String url, Integer maxPages) {}
}
