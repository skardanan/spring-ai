package nl.gerimedica.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebCrawlerServiceTest {

    private WebCrawlerService webCrawlerService;

    @BeforeEach
    void setUp() {
        webCrawlerService = new WebCrawlerService();
    }

    @Test
    @DisplayName("Should crawl URL and extract text content")
    void crawlUrl_validUrl_returnsContent() throws IOException {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            // Setup mock document
            Document mockDocument = mock(Document.class);
            Connection mockConnection = mock(Connection.class);
            Element mockBody = mock(Element.class);

            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.get()).thenReturn(mockDocument);

            when(mockDocument.title()).thenReturn("Test Page Title");
            when(mockDocument.select(anyString())).thenReturn(new Elements());
            when(mockDocument.selectFirst(anyString())).thenReturn(null);
            when(mockDocument.body()).thenReturn(mockBody);
            when(mockBody.text()).thenReturn("This is the page content.");
            when(mockDocument.text()).thenReturn("This is the page content.");

            // Execute
            WebCrawlerService.CrawlResult result = webCrawlerService.crawlUrl("https://example.com");

            // Verify
            assertNotNull(result);
            assertEquals("https://example.com", result.url());
            assertEquals("Test Page Title", result.title());
            assertEquals("example.com", result.domain());
            assertFalse(result.text().isBlank());
        }
    }

    @Test
    @DisplayName("Should extract domain from URL correctly")
    void crawlUrl_extractsDomainCorrectly() throws IOException {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            Document mockDocument = mock(Document.class);
            Connection mockConnection = mock(Connection.class);
            Element mockBody = mock(Element.class);

            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.get()).thenReturn(mockDocument);

            when(mockDocument.title()).thenReturn("Test");
            when(mockDocument.select(anyString())).thenReturn(new Elements());
            when(mockDocument.selectFirst(anyString())).thenReturn(null);
            when(mockDocument.body()).thenReturn(mockBody);
            when(mockBody.text()).thenReturn("Content");

            WebCrawlerService.CrawlResult result = webCrawlerService.crawlUrl("https://www.test-site.org/path/page");

            assertEquals("www.test-site.org", result.domain());
        }
    }

    @Test
    @DisplayName("Should handle IOException when crawling fails")
    void crawlUrl_connectionFails_throwsIOException() throws Exception {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            Connection mockConnection = mock(Connection.class);

            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            doThrow(new IOException("Connection refused")).when(mockConnection).get();

            assertThrows(IOException.class, () -> webCrawlerService.crawlUrl("https://invalid-url.com"));
        }
    }

    @Test
    @DisplayName("Should extract main content when main element exists")
    void crawlUrl_withMainElement_extractsMainContent() throws IOException {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            Document mockDocument = mock(Document.class);
            Connection mockConnection = mock(Connection.class);
            Element mockMain = mock(Element.class);

            jsoupMock.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.get()).thenReturn(mockDocument);

            when(mockDocument.title()).thenReturn("Test");
            when(mockDocument.select(anyString())).thenReturn(new Elements());
            when(mockDocument.selectFirst(anyString())).thenReturn(mockMain);
            when(mockMain.text()).thenReturn("Main content here");

            WebCrawlerService.CrawlResult result = webCrawlerService.crawlUrl("https://example.com");

            assertEquals("Main content here", result.text());
        }
    }

    @Test
    @DisplayName("Should crawl multiple pages with links")
    void crawlUrlWithLinks_multiplePages_aggregatesContent() throws IOException {
        try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
            Document mockDocument1 = mock(Document.class);
            Document mockDocument2 = mock(Document.class);
            Connection mockConnection = mock(Connection.class);
            Element mockBody1 = mock(Element.class);
            Element mockBody2 = mock(Element.class);
            Elements mockLinks = mock(Elements.class);
            Element mockLink = mock(Element.class);

            jsoupMock.when(() -> Jsoup.connect("https://example.com")).thenReturn(mockConnection);
            jsoupMock.when(() -> Jsoup.connect("https://example.com/page2")).thenReturn(mockConnection);

            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.get())
                    .thenReturn(mockDocument1)
                    .thenReturn(mockDocument2);

            // First page
            when(mockDocument1.title()).thenReturn("Page 1");
            when(mockDocument1.select("script, style, nav, footer, header, aside, .advertisement, .ads, .sidebar")).thenReturn(new Elements());
            when(mockDocument1.selectFirst(anyString())).thenReturn(null);
            when(mockDocument1.body()).thenReturn(mockBody1);
            when(mockBody1.text()).thenReturn("Content of page 1");
            when(mockDocument1.select("a[href]")).thenReturn(mockLinks);

            // Mock links
            when(mockLinks.iterator()).thenReturn(java.util.List.of(mockLink).iterator());
            when(mockLink.absUrl("href")).thenReturn("https://example.com/page2");

            // Second page
            when(mockDocument2.title()).thenReturn("Page 2");
            when(mockDocument2.select("script, style, nav, footer, header, aside, .advertisement, .ads, .sidebar")).thenReturn(new Elements());
            when(mockDocument2.selectFirst(anyString())).thenReturn(null);
            when(mockDocument2.body()).thenReturn(mockBody2);
            when(mockBody2.text()).thenReturn("Content of page 2");
            when(mockDocument2.select("a[href]")).thenReturn(new Elements());

            WebCrawlerService.CrawlResult result = webCrawlerService.crawlUrlWithLinks("https://example.com", 2);

            assertNotNull(result);
            assertTrue(result.text().contains("Content of page 1"));
        }
    }

    @Test
    @DisplayName("CrawlResult record should have correct values")
    void crawlResult_createsCorrectRecord() {
        WebCrawlerService.CrawlResult result = new WebCrawlerService.CrawlResult(
                "https://test.com",
                "Test Title",
                "Test content",
                "test.com"
        );

        assertEquals("https://test.com", result.url());
        assertEquals("Test Title", result.title());
        assertEquals("Test content", result.text());
        assertEquals("test.com", result.domain());
    }
}