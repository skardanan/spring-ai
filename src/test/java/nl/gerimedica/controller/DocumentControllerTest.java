package nl.gerimedica.controller;

import nl.gerimedica.service.DocumentParserService;
import nl.gerimedica.service.TextChunkingService;
import nl.gerimedica.service.WebCrawlerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private DocumentParserService documentParserService;

    @Mock
    private TextChunkingService textChunkingService;

    @Mock
    private WebCrawlerService webCrawlerService;

    private DocumentController documentController;

    @BeforeEach
    void setUp() {
        documentController = new DocumentController(
                vectorStore,
                documentParserService,
                textChunkingService,
                webCrawlerService
        );
    }

    @Test
    @DisplayName("Should upload file successfully")
    void uploadFile_validFile_returnsSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Test content".getBytes()
        );

        when(documentParserService.isSupportedFileType("test.txt")).thenReturn(true);
        when(documentParserService.extractText(any())).thenReturn("Extracted text content");
        when(documentParserService.detectContentType(any())).thenReturn("text/plain");
        when(textChunkingService.chunkText(anyString())).thenReturn(List.of("Chunk 1", "Chunk 2"));

        ResponseEntity<Map<String, Object>> response = documentController.uploadFile(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("test.txt", response.getBody().get("filename"));
        assertEquals(2, response.getBody().get("chunks"));
        verify(vectorStore).add(any());
    }

    @Test
    @DisplayName("Should reject empty file")
    void uploadFile_emptyFile_returnsBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        ResponseEntity<Map<String, Object>> response = documentController.uploadFile(file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("File is empty", response.getBody().get("message"));
        verify(vectorStore, never()).add(any());
    }

    @Test
    @DisplayName("Should reject unsupported file type")
    void uploadFile_unsupportedType_returnsBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "virus.exe",
                "application/octet-stream",
                "malicious content".getBytes()
        );

        when(documentParserService.isSupportedFileType("virus.exe")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = documentController.uploadFile(file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("Unsupported file type"));
        verify(vectorStore, never()).add(any());
    }

    @Test
    @DisplayName("Should reject file with no extractable text")
    void uploadFile_noExtractableText_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                "binary content".getBytes()
        );

        when(documentParserService.isSupportedFileType("empty.pdf")).thenReturn(true);
        when(documentParserService.extractText(any())).thenReturn("");

        ResponseEntity<Map<String, Object>> response = documentController.uploadFile(file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("Could not extract text"));
    }

    @Test
    @DisplayName("Should handle parsing exception gracefully")
    void uploadFile_parsingException_returnsServerError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "corrupted.pdf",
                "application/pdf",
                "corrupted content".getBytes()
        );

        when(documentParserService.isSupportedFileType("corrupted.pdf")).thenReturn(true);
        when(documentParserService.extractText(any())).thenThrow(new RuntimeException("Parse error"));

        ResponseEntity<Map<String, Object>> response = documentController.uploadFile(file);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("Error processing file"));
    }

    @Test
    @DisplayName("Should crawl URL successfully")
    void crawlUrl_validUrl_returnsSuccess() throws Exception {
        DocumentController.CrawlRequest request = new DocumentController.CrawlRequest("https://example.com", 1);

        WebCrawlerService.CrawlResult crawlResult = new WebCrawlerService.CrawlResult(
                "https://example.com",
                "Example Page",
                "This is the page content",
                "example.com"
        );

        when(webCrawlerService.crawlUrl(anyString())).thenReturn(crawlResult);
        when(textChunkingService.chunkText(anyString())).thenReturn(List.of("Chunk 1"));

        ResponseEntity<Map<String, Object>> response = documentController.crawlUrl(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("Example Page", response.getBody().get("title"));
        assertEquals("example.com", response.getBody().get("domain"));
        verify(vectorStore).add(any());
    }

    @Test
    @DisplayName("Should reject empty URL")
    void crawlUrl_emptyUrl_returnsBadRequest() {
        DocumentController.CrawlRequest request = new DocumentController.CrawlRequest("", null);

        ResponseEntity<Map<String, Object>> response = documentController.crawlUrl(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("URL is required", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Should reject null URL")
    void crawlUrl_nullUrl_returnsBadRequest() {
        DocumentController.CrawlRequest request = new DocumentController.CrawlRequest(null, null);

        ResponseEntity<Map<String, Object>> response = documentController.crawlUrl(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
    }

    @Test
    @DisplayName("Should add https prefix if missing")
    void crawlUrl_missingProtocol_addsHttps() throws Exception {
        DocumentController.CrawlRequest request = new DocumentController.CrawlRequest("example.com", 1);

        WebCrawlerService.CrawlResult crawlResult = new WebCrawlerService.CrawlResult(
                "https://example.com",
                "Example",
                "Content",
                "example.com"
        );

        when(webCrawlerService.crawlUrl("https://example.com")).thenReturn(crawlResult);
        when(textChunkingService.chunkText(anyString())).thenReturn(List.of("Chunk"));

        ResponseEntity<Map<String, Object>> response = documentController.crawlUrl(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(webCrawlerService).crawlUrl("https://example.com");
    }

    @Test
    @DisplayName("Should use crawlUrlWithLinks when maxPages > 1")
    void crawlUrl_multiplePages_usesCrawlWithLinks() throws Exception {
        DocumentController.CrawlRequest request = new DocumentController.CrawlRequest("https://example.com", 5);

        WebCrawlerService.CrawlResult crawlResult = new WebCrawlerService.CrawlResult(
                "https://example.com",
                "Example",
                "Multi-page content",
                "example.com"
        );

        when(webCrawlerService.crawlUrlWithLinks("https://example.com", 5)).thenReturn(crawlResult);
        when(textChunkingService.chunkText(anyString())).thenReturn(List.of("Chunk"));

        ResponseEntity<Map<String, Object>> response = documentController.crawlUrl(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(webCrawlerService).crawlUrlWithLinks("https://example.com", 5);
        verify(webCrawlerService, never()).crawlUrl(anyString());
    }

    @Test
    @DisplayName("Should reject crawl result with no text")
    void crawlUrl_noTextExtracted_returnsBadRequest() throws Exception {
        DocumentController.CrawlRequest request = new DocumentController.CrawlRequest("https://example.com", 1);

        WebCrawlerService.CrawlResult crawlResult = new WebCrawlerService.CrawlResult(
                "https://example.com",
                "Example",
                "",
                "example.com"
        );

        when(webCrawlerService.crawlUrl(anyString())).thenReturn(crawlResult);

        ResponseEntity<Map<String, Object>> response = documentController.crawlUrl(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("Could not extract text"));
    }

    @Test
    @DisplayName("Should handle crawl exception gracefully")
    void crawlUrl_crawlException_returnsServerError() throws Exception {
        DocumentController.CrawlRequest request = new DocumentController.CrawlRequest("https://invalid.com", 1);

        when(webCrawlerService.crawlUrl(anyString())).thenThrow(new RuntimeException("Connection refused"));

        ResponseEntity<Map<String, Object>> response = documentController.crawlUrl(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
        assertTrue(response.getBody().get("message").toString().contains("Error crawling URL"));
    }
}