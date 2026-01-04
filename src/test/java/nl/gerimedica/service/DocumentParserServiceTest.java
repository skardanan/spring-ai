package nl.gerimedica.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserServiceTest {

    private DocumentParserService documentParserService;

    @BeforeEach
    void setUp() {
        documentParserService = new DocumentParserService();
    }

    @Test
    @DisplayName("Should extract text from plain text file")
    void extractText_plainTextFile_returnsContent() throws Exception {
        String content = "This is a test document with some content.";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                content.getBytes()
        );

        String result = documentParserService.extractText(file);

        assertEquals(content, result);
    }

    @Test
    @DisplayName("Should extract text from HTML file")
    void extractText_htmlFile_returnsTextContent() throws Exception {
        String htmlContent = "<html><body><h1>Title</h1><p>Paragraph content.</p></body></html>";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.html",
                "text/html",
                htmlContent.getBytes()
        );

        String result = documentParserService.extractText(file);

        assertTrue(result.contains("Title"));
        assertTrue(result.contains("Paragraph content"));
    }

    @Test
    @DisplayName("Should extract text from JSON file")
    void extractText_jsonFile_returnsContent() throws Exception {
        String jsonContent = "{\"name\": \"Test\", \"value\": 123}";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.json",
                "application/json",
                jsonContent.getBytes()
        );

        String result = documentParserService.extractText(file);

        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("Should extract text from CSV file")
    void extractText_csvFile_returnsContent() throws Exception {
        String csvContent = "name,age,city\nJohn,30,Amsterdam\nJane,25,Rotterdam";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes()
        );

        String result = documentParserService.extractText(file);

        assertTrue(result.contains("John"));
        assertTrue(result.contains("Amsterdam"));
    }

    @Test
    @DisplayName("Should extract text from Markdown file")
    void extractText_markdownFile_returnsContent() throws Exception {
        String mdContent = "# Header\n\nThis is **bold** and *italic* text.";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.md",
                "text/markdown",
                mdContent.getBytes()
        );

        String result = documentParserService.extractText(file);

        assertTrue(result.contains("Header"));
        assertTrue(result.contains("bold"));
    }

    @Test
    @DisplayName("Should detect content type for text file")
    void detectContentType_textFile_returnsTextPlain() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        String contentType = documentParserService.detectContentType(file);

        assertNotNull(contentType);
        assertTrue(contentType.contains("text"));
    }

    @ParameterizedTest
    @DisplayName("Should support common file types")
    @ValueSource(strings = {".pdf", ".doc", ".docx", ".txt", ".rtf", ".md", ".csv", ".json", ".xml", ".html", ".htm", ".xls", ".xlsx", ".ppt", ".pptx", ".odt", ".ods", ".odp"})
    void isSupportedFileType_supportedExtensions_returnsTrue(String extension) {
        assertTrue(documentParserService.isSupportedFileType("document" + extension));
    }

    @ParameterizedTest
    @DisplayName("Should reject unsupported file types")
    @ValueSource(strings = {".exe", ".dll", ".bat", ".sh", ".zip", ".rar", ".mp3", ".mp4", ".avi", ".jpg", ".png", ".gif"})
    void isSupportedFileType_unsupportedExtensions_returnsFalse(String extension) {
        assertFalse(documentParserService.isSupportedFileType("file" + extension));
    }

    @Test
    @DisplayName("Should return false for null filename")
    void isSupportedFileType_nullFilename_returnsFalse() {
        assertFalse(documentParserService.isSupportedFileType(null));
    }

    @Test
    @DisplayName("Should handle case-insensitive file extensions")
    void isSupportedFileType_uppercaseExtension_returnsTrue() {
        assertTrue(documentParserService.isSupportedFileType("document.PDF"));
        assertTrue(documentParserService.isSupportedFileType("document.DOCX"));
        assertTrue(documentParserService.isSupportedFileType("document.TXT"));
    }

    @Test
    @DisplayName("Should throw exception for empty file")
    void extractText_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // Tika throws ZeroByteFileException for empty files
        assertThrows(Exception.class, () -> documentParserService.extractText(file));
    }
}