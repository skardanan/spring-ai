# Knowledge Base Chat Bot

An AI-powered chatbot that answers questions using your own documents and web content as a knowledge base. Built with Spring AI, OpenAI, and Qdrant vector database.

## Features

- **Multi-format Document Upload** - Upload PDF, Word, Excel, PowerPoint, TXT, Markdown, CSV, JSON, XML, HTML, and OpenDocument formats
- **Web Crawling** - Crawl and index website content directly from URLs
- **Semantic Search** - AI-powered vector similarity search finds the most relevant information
- **RAG Architecture** - Responses are grounded in your actual documents, reducing hallucinations
- **Text Chunking** - Large documents are automatically split into optimal chunks for better retrieval
- **Modern Dashboard** - Clean Angular UI for document management and chat interface

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 4.0, Spring AI 2.0 |
| AI Model | OpenAI GPT-4o-mini |
| Embeddings | OpenAI text-embedding-ada-002 |
| Vector Database | Qdrant |
| Document Parsing | Apache Tika |
| Web Crawling | Jsoup |
| Frontend | Angular 19, TypeScript |
| Build Tool | Gradle 8.14 |

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Angular UI    │────▶│  Spring Boot    │────▶│     OpenAI      │
│   (Dashboard)   │     │   REST API      │     │   (Chat/Embed)  │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │     Qdrant      │
                        │  Vector Store   │
                        └─────────────────┘
```

### How It Works

1. **Document Ingestion**: Upload files or crawl URLs → Text extraction via Tika/Jsoup → Chunking → Embedding via OpenAI → Store in Qdrant
2. **Query Processing**: User question → Semantic search in Qdrant → Retrieve relevant chunks → Augment prompt with context → Generate answer via OpenAI

## Prerequisites

- Java 21+
- Docker (for Qdrant)
- Node.js 20+ (for frontend build)
- OpenAI API Key

## Quick Start

### 1. Start Qdrant Vector Database

```bash
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant
```

### 2. Set Environment Variables

```bash
export OPENAI_API_KEY=your-api-key-here
```

### 3. Build and Run

```bash
# Build the project (includes frontend)
./gradlew build

# Run the application
./gradlew bootRun
```

### 4. Access the Application

- **Web UI**: http://localhost:8080
- **Swagger API Docs**: http://localhost:8080/swagger-ui.html
- **Qdrant Dashboard**: http://localhost:6333/dashboard

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chat` | Send a message to the AI chatbot |
| POST | `/api/documents/upload` | Upload document via JSON (text + metadata) |
| POST | `/api/documents/upload-file` | Upload document file (multipart) |
| POST | `/api/documents/crawl` | Crawl a website URL |

### Example: Chat with the Bot

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the main topic of the uploaded documents?"}'
```

### Example: Upload a Document

```bash
curl -X POST http://localhost:8080/api/documents/upload-file \
  -F "file=@/path/to/document.pdf"
```

### Example: Crawl a Website

```bash
curl -X POST http://localhost:8080/api/documents/crawl \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com", "maxPages": 1}'
```

## Project Structure

```
src/
├── main/
│   ├── java/nl/gerimedica/
│   │   ├── StartApplication.java          # Spring Boot entry point
│   │   ├── config/
│   │   │   ├── ChatClientConfig.java      # AI client configuration
│   │   │   └── OpenApiConfig.java         # Swagger configuration
│   │   ├── controller/
│   │   │   ├── ChatController.java        # Chat endpoint
│   │   │   └── DocumentController.java    # Document upload/crawl endpoints
│   │   ├── domain/
│   │   │   └── DocumentUploadRequest.java # DTOs
│   │   └── service/
│   │       ├── OpenAIService.java         # RAG chat service
│   │       ├── DocumentParserService.java # Tika document parsing
│   │       ├── TextChunkingService.java   # Text chunking logic
│   │       └── WebCrawlerService.java     # Website crawling
│   ├── resources/
│   │   ├── application.yml                # App configuration
│   │   └── static/                        # Angular build output
│   └── frontend/                          # Angular source code
│       └── src/app/
│           └── components/
│               ├── chat/                  # Chat interface
│               └── dashboard/             # Document management
└── test/
    └── java/nl/gerimedica/
        ├── controller/
        │   └── DocumentControllerTest.java
        └── service/
            ├── TextChunkingServiceTest.java
            ├── DocumentParserServiceTest.java
            └── WebCrawlerServiceTest.java
```

## Configuration

Key settings in `application.yml`:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 1.0
      embedding:
        options:
          model: text-embedding-ada-002
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        collection-name: docs
        initialize-schema: true
```

## Supported File Types

| Category | Extensions |
|----------|------------|
| Documents | PDF, DOC, DOCX, RTF, ODT |
| Spreadsheets | XLS, XLSX, CSV, ODS |
| Presentations | PPT, PPTX, ODP |
| Text | TXT, MD, JSON, XML |
| Web | HTML, HTM |

## Development

### Run Tests

```bash
./gradlew test
```

### Build Frontend Only

```bash
cd src/main/frontend
npm install
npm run build
```

### Run in Development Mode

```bash
# Terminal 1: Backend
./gradlew bootRun -x buildFrontend

# Terminal 2: Frontend (with hot reload)
cd src/main/frontend
npm start
```

## Use Cases

- **Enterprise Knowledge Base** - Index company documents for employee Q&A
- **Documentation Assistant** - Query technical documentation naturally
- **Research Tool** - Upload papers and ask questions about findings
- **Customer Support** - Build a support bot from your help articles
- **Legal Document Analysis** - Search through contracts and policies

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request