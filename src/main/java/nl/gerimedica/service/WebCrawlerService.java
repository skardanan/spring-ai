package nl.gerimedica.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class WebCrawlerService {

    private static final int TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; HealthcareBot/1.0)";

    public CrawlResult crawlUrl(String url) throws IOException {
        log.info("Crawling URL: {}", url);

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();

        String title = doc.title();
        String text = extractText(doc);
        String domain = extractDomain(url);

        log.info("Successfully crawled: {} ({} characters)", title, text.length());

        return new CrawlResult(url, title, text, domain);
    }

    public CrawlResult crawlUrlWithLinks(String url, int maxPages) throws IOException {
        Set<String> visited = new HashSet<>();
        Set<String> toVisit = new HashSet<>();
        StringBuilder allText = new StringBuilder();
        String domain = extractDomain(url);
        String mainTitle = "";

        toVisit.add(url);

        while (!toVisit.isEmpty() && visited.size() < maxPages) {
            String currentUrl = toVisit.iterator().next();
            toVisit.remove(currentUrl);

            if (visited.contains(currentUrl)) {
                continue;
            }

            try {
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .get();

                visited.add(currentUrl);

                if (mainTitle.isEmpty()) {
                    mainTitle = doc.title();
                }

                String pageText = extractText(doc);
                allText.append("\n\n--- Page: ").append(doc.title()).append(" ---\n\n");
                allText.append(pageText);

                // Find links on the same domain
                if (visited.size() < maxPages) {
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String href = link.absUrl("href");
                        if (isSameDomain(href, domain) && !visited.contains(href) && !href.contains("#")) {
                            toVisit.add(href);
                        }
                    }
                }

                log.info("Crawled page {}/{}: {}", visited.size(), maxPages, currentUrl);

            } catch (Exception e) {
                log.warn("Failed to crawl: {} - {}", currentUrl, e.getMessage());
            }
        }

        return new CrawlResult(url, mainTitle, allText.toString().trim(), domain);
    }

    private String extractText(Document doc) {
        // Remove script and style elements
        doc.select("script, style, nav, footer, header, aside, .advertisement, .ads, .sidebar").remove();

        // Get main content
        Element main = doc.selectFirst("main, article, .content, .main-content, #content, #main");
        if (main != null) {
            return cleanText(main.text());
        }

        // Fallback to body
        Element body = doc.body();
        if (body != null) {
            return cleanText(body.text());
        }

        return cleanText(doc.text());
    }

    private String cleanText(String text) {
        if (text == null) return "";
        // Clean up whitespace
        return text.replaceAll("\\s+", " ").trim();
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    private boolean isSameDomain(String url, String domain) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null && (host.equals(domain) || host.endsWith("." + domain));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public record CrawlResult(String url, String title, String text, String domain) {}
}