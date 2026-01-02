# Spring AI Healthcare Sandbox
A personal learning project for exploring AI integration in healthcare applications using Spring AI, OpenAI, and vector databases.

## Tech Stack

- **Java 21**
- **Spring Boot 3.3.0**
- **Spring AI 1.0.2**
- **OpenAI** (gpt-3.5-turbo, text-embedding-ada-002)
- **Qdrant** - Vector database for semantic search
- **Gradle** - Build tool
- **Lombok** - Boilerplate reduction
- **SpringDoc OpenAPI** - API documentation

## Project Structure

```
src/main/java/nl/gerimedica/
├── StartApplication.java       # Spring Boot entry point
├── config/
│   ├── ChatClientConfig.java   # ChatClient & VectorStore setup with QA advisor
│   └── OpenApiConfig.java      # Swagger/OpenAPI configuration
├── controller/
│   ├── ChatController.java     # POST /api/chat endpoint
│   └── DocumentController.java # POST /api/documents/upload endpoint
├── domain/
│   └── DocumentUploadRequest.java  # Document upload DTO
└── service/
    └── OpenAIService.java      # OpenAI chat service
```

## Build & Run Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Clean build
./gradlew clean build
```

## Testing

```bash
./gradlew test
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chat` | Send a message to the AI chatbot |
| POST | `/api/documents/upload` | Upload documents for RAG indexing |

Swagger UI available at: `http://localhost:8080/swagger-ui.html`

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `OPENAI_API_KEY` | OpenAI API key for chat and embeddings | Yes |

## External Services

### Qdrant Vector Database

Must be running locally before starting the application:

```bash
# Run Qdrant with Docker
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant
```

- HTTP port: 6333
- gRPC port: 6334 (used by Spring AI)
- Collection: `docs`

## Architecture

This project implements **RAG (Retrieval-Augmented Generation)**:

1. Documents are uploaded and converted to embeddings via OpenAI
2. Embeddings are stored in Qdrant vector database
3. When a user asks a question, relevant documents are retrieved via semantic search
4. Retrieved context is passed to the LLM along with the question
5. LLM generates an answer based on the provided context

## Configuration

Key settings in `application.yml`:

- **Model**: gpt-3.5-turbo
- **Embedding Model**: text-embedding-ada-002
- **Temperature**: 1.0
- **Vector Search**: Top 3 results with 0.7 similarity threshold

## Code Conventions

- Package base: `nl.gerimedica`
- Use Lombok `@Getter`/`@Setter` for DTOs
- REST controllers return simple types or ResponseEntity
- Configuration classes use `@Configuration` with `@Bean` methods

## Common Development Tasks

### Adding a new endpoint
1. Create/update controller in `controller/` package
2. Add any required DTOs in `domain/` package
3. Update service layer if needed

### Adding documents to vector store
Use the `/api/documents/upload` endpoint with:
```json
{
  "id": "unique-id",
  "text": "Document content to be indexed",
  "metadata": {"key": "value"}
}
```

### Testing chat functionality
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Your question here"}'
```