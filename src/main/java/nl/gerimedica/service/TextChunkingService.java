package nl.gerimedica.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkingService {

    private static final int DEFAULT_CHUNK_SIZE = 1000;  // characters per chunk
    private static final int DEFAULT_OVERLAP = 200;      // overlap between chunks

    public List<String> chunkText(String text) {
        return chunkText(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        // Clean up the text
        text = text.trim();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Try to find a good break point (sentence or paragraph end)
            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, start, end);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Move start position with overlap
            start = end - overlap;
            if (start < 0) start = 0;

            // Prevent infinite loop
            if (start >= text.length() - 1) break;
            if (end == text.length()) break;
        }

        return chunks;
    }

    private int findBreakPoint(String text, int start, int end) {
        // Look for paragraph break first
        int paragraphBreak = text.lastIndexOf("\n\n", end);
        if (paragraphBreak > start + (end - start) / 2) {
            return paragraphBreak + 2;
        }

        // Look for sentence end (. ! ?)
        for (int i = end - 1; i > start + (end - start) / 2; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) {
                return i + 1;
            }
        }

        // Look for line break
        int lineBreak = text.lastIndexOf('\n', end);
        if (lineBreak > start + (end - start) / 2) {
            return lineBreak + 1;
        }

        // Look for space
        int spaceBreak = text.lastIndexOf(' ', end);
        if (spaceBreak > start + (end - start) / 2) {
            return spaceBreak + 1;
        }

        return end;
    }
}