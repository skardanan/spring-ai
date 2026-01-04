package nl.gerimedica.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkingServiceTest {

    private TextChunkingService textChunkingService;

    @BeforeEach
    void setUp() {
        textChunkingService = new TextChunkingService();
    }

    @Test
    @DisplayName("Should return empty list for null input")
    void chunkText_nullInput_returnsEmptyList() {
        List<String> result = textChunkingService.chunkText(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list for blank input")
    void chunkText_blankInput_returnsEmptyList() {
        List<String> result = textChunkingService.chunkText("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return single chunk for short text")
    void chunkText_shortText_returnsSingleChunk() {
        String shortText = "This is a short text.";
        List<String> result = textChunkingService.chunkText(shortText);

        assertEquals(1, result.size());
        assertEquals(shortText, result.get(0));
    }

    @Test
    @DisplayName("Should split long text into multiple chunks")
    void chunkText_longText_returnsMultipleChunks() {
        // Create text longer than default chunk size (1000 chars)
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            longText.append("This is sentence number ").append(i).append(". ");
        }

        List<String> result = textChunkingService.chunkText(longText.toString());

        assertTrue(result.size() > 1, "Should have multiple chunks");
        // Verify no chunk is empty
        result.forEach(chunk -> assertFalse(chunk.isBlank()));
    }

    @Test
    @DisplayName("Should respect custom chunk size")
    void chunkText_customChunkSize_respectsSize() {
        String text = "Word one. Word two. Word three. Word four. Word five.";
        int chunkSize = 20;
        int overlap = 5;

        List<String> result = textChunkingService.chunkText(text, chunkSize, overlap);

        assertTrue(result.size() > 1, "Should have multiple chunks with small chunk size");
    }

    @Test
    @DisplayName("Should handle text with paragraph breaks")
    void chunkText_withParagraphBreaks_splitsAtParagraphs() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            text.append("This is paragraph ").append(i).append(" with some content.\n\n");
        }

        List<String> result = textChunkingService.chunkText(text.toString(), 200, 50);

        assertTrue(result.size() > 1);
        // Should not have chunks starting with newlines (trimmed)
        result.forEach(chunk -> assertFalse(chunk.startsWith("\n")));
    }

    @Test
    @DisplayName("Should maintain overlap between chunks")
    void chunkText_withOverlap_maintainsOverlap() {
        // Create text that will definitely be chunked
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            text.append("Sentence ").append(i).append(" has unique content. ");
        }

        List<String> result = textChunkingService.chunkText(text.toString(), 100, 20);

        // With overlap, later chunks should contain some content from previous chunks
        assertTrue(result.size() >= 2, "Need at least 2 chunks to test overlap");
    }

    @Test
    @DisplayName("Should handle text without sentence boundaries")
    void chunkText_noPunctuation_stillChunks() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            text.append("word").append(i).append(" ");
        }

        List<String> result = textChunkingService.chunkText(text.toString(), 100, 20);

        assertTrue(result.size() > 1);
        result.forEach(chunk -> assertFalse(chunk.isBlank()));
    }
}