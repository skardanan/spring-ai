package nl.gerimedica.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Service
public class DocumentParserService {

    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();

    public String extractText(MultipartFile file) throws IOException, TikaException, SAXException {
        try (InputStream inputStream = file.getInputStream()) {
            BodyContentHandler handler = new BodyContentHandler(-1); // -1 means no limit
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
            ParseContext context = new ParseContext();

            parser.parse(inputStream, handler, metadata, context);

            return handler.toString().trim();
        }
    }

    public String detectContentType(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        }
    }

    public boolean isSupportedFileType(String filename) {
        if (filename == null) return false;
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".pdf") ||
               lowerName.endsWith(".doc") ||
               lowerName.endsWith(".docx") ||
               lowerName.endsWith(".txt") ||
               lowerName.endsWith(".rtf") ||
               lowerName.endsWith(".md") ||
               lowerName.endsWith(".csv") ||
               lowerName.endsWith(".json") ||
               lowerName.endsWith(".xml") ||
               lowerName.endsWith(".html") ||
               lowerName.endsWith(".htm") ||
               lowerName.endsWith(".xls") ||
               lowerName.endsWith(".xlsx") ||
               lowerName.endsWith(".ppt") ||
               lowerName.endsWith(".pptx") ||
               lowerName.endsWith(".odt") ||
               lowerName.endsWith(".ods") ||
               lowerName.endsWith(".odp");
    }
}