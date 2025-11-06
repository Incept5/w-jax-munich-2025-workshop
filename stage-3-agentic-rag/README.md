# Stage 3: Agentic RAG - Getting Started

## Overview

This stage implements a production-ready RAG (Retrieval-Augmented Generation) pipeline using PostgreSQL with pgvector extension. Phase 1 focuses on document ingestion - extracting code from repositories, chunking, generating embeddings, and storing them for semantic search.

## What You'll Build (Phase 1)

1. **Infrastructure**: PostgreSQL 17 + pgvector in Docker
2. **Ingestion Pipeline**: Extract → Chunk → Embed → Store
3. **Vector Storage**: Efficient similarity search with cosine distance
4. **Idempotent Processing**: Hash-based deduplication

## Prerequisites

- Java 21+
- Maven 3.9.0+
- Docker and Docker Compose
- Ollama running with `nomic-embed-text` model
- pipx (for gitingest installation)

### Install Prerequisites

```bash
# Ollama
# (Assuming already installed from previous stages)

# Pull embedding model
ollama pull nomic-embed-text

# pipx (if not already installed)
# macOS:
brew install pipx

# Ubuntu/Debian:
sudo apt install pipx

# Ensure pipx is in PATH
pipx ensurepath
```

## Quick Start

### One-Command Setup

```bash
cd stage-3-agentic-rag
./ingest.sh
```

This script will:
1. ✓ Check/install gitingest via pipx
2. ✓ Start PostgreSQL + pgvector with Docker
3. ✓ Run Flyway database migrations
4. ✓ Process all repositories from repos.yaml
5. ✓ Generate embeddings via Ollama
6. ✓ Store in PostgreSQL with vector indexes

### What Gets Ingested

By default, the pipeline ingests 6 repositories (configured in `repos.yaml`):
- **spring-ai**: Spring AI framework documentation
- **embabel-agent**: Core Embabel framework
- **embabel-examples**: Practical Embabel examples
- **embabel-java-template**: Java project template
- **embabel-kotlin-template**: Kotlin project template
- **tripper**: Advanced travel planner example

Total documents: ~400-500 chunks depending on repository sizes.

## Manual Steps (For Learning)

If you want to understand each step:

### 1. Start PostgreSQL

```bash
docker-compose up -d

# Verify it's running
docker ps
docker exec -it stage3-pgvector pg_isready -U workshop -d workshop_rag
```

### 2. Build the Project

```bash
mvn clean package
```

### 3. Install gitingest

```bash
pipx install gitingest
gitingest --help
```

### 4. Run Ingestion

```bash
java -jar target/stage-3-agentic-rag.jar repos.yaml
```

Watch the progress:
- Repository download progress
- Chunking statistics
- Embedding generation (with progress)
- Storage confirmation

## Verify the Results

### Check Document Count

```bash
docker exec -it stage3-pgvector psql -U workshop -d workshop_rag -c "SELECT COUNT(*) FROM documents;"
```

### View Documents by Source

```bash
docker exec -it stage3-pgvector psql -U workshop -d workshop_rag -c "SELECT source, COUNT(*) FROM documents GROUP BY source;"
```

### Test Vector Search

```bash
docker exec -it stage3-pgvector psql -U workshop -d workshop_rag
```

Then in psql:

```sql
-- Get a sample embedding to test with
\x
SELECT id, content, source, 
       1 - (embedding <=> (SELECT embedding FROM documents LIMIT 1)) as similarity
FROM documents
WHERE 1 - (embedding <=> (SELECT embedding FROM documents LIMIT 1)) > 0.7
ORDER BY similarity DESC
LIMIT 5;
```

## Configuration

### Customize Repositories (repos.yaml)

Add or remove repositories:

```yaml
repositories:
  - name: my-repo
    url: https://github.com/user/repo
    branch: main
    description: "My custom repository"
```

### Adjust Chunking Settings

```yaml
settings:
  chunk_size: 800          # tokens per chunk (increase for larger context)
  chunk_overlap: 200       # overlap for continuity (20-25% of chunk_size)
  similarity_threshold: 0.7  # minimum cosine similarity (0.0-1.0)
```

### Change Embedding Model

```yaml
settings:
  embedding_model: nomic-embed-text  # or 'all-minilm-l6-v2' for faster/smaller
  ollama_base_url: http://localhost:11434
```

**Note**: If you change the embedding model, you must:
1. Update the vector dimension in `V1__Create_documents_table.sql`
2. Drop and recreate the database
3. Re-run ingestion

## Architecture Components

### Database Schema

```sql
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    source VARCHAR(255) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    chunk_index INTEGER NOT NULL,
    metadata JSONB,
    embedding vector(768),  -- nomic-embed-text dimension
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(source, file_hash, chunk_index)  -- Idempotency
);

-- Vector similarity index (IVFFlat)
CREATE INDEX idx_documents_embedding ON documents 
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
```

### Java Components

1. **DatabaseConfig**: HikariCP connection pooling
2. **PgVectorStore**: Vector storage and similarity search
3. **EmbeddingService**: Generate embeddings via Ollama
4. **DocumentChunker**: Split documents with overlap
5. **IngestionService**: Orchestrate the pipeline

### Pipeline Flow

```
repos.yaml → IngestionService
              ↓
         gitingest (clone & extract)
              ↓
         DocumentChunker (split with overlap)
              ↓
         EmbeddingService (Ollama)
              ↓
         PgVectorStore (PostgreSQL + pgvector)
```

## Troubleshooting

### Ollama Connection Error

```bash
# Check Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama if needed
ollama serve
```

### PostgreSQL Connection Refused

```bash
# Check container status
docker ps

# View logs
docker logs stage3-pgvector

# Restart if needed
docker-compose restart
```

### gitingest Not Found

```bash
# Install via pipx
pipx install gitingest

# Verify installation
which gitingest
gitingest --help
```

### Embedding Model Not Found

```bash
# Pull the model
ollama pull nomic-embed-text

# Verify it's available
ollama list
```

### Out of Memory During Ingestion

Increase Java heap size:

```bash
java -Xmx4g -jar target/stage-3-agentic-rag.jar repos.yaml
```

## Clean Up

### Stop PostgreSQL

```bash
docker-compose down
```

### Remove All Data (including volumes)

```bash
docker-compose down -v
```

### Clean gitingest Outputs

```bash
rm -rf data/
```

## Next Steps

Phase 1 (Ingestion) is now complete! The next phases will add:

- **Phase 2**: Conversational RAG agent with JSON tool calling
- **Phase 3**: Multi-turn conversations with memory
- **Phase 4**: Advanced features (re-ranking, hybrid search)

For now, you have a production-ready vector store with ~500 documents ready for semantic search!

## Performance Notes

### Ingestion Speed

- **gitingest**: ~5-10 seconds per repository (depends on size)
- **Chunking**: Very fast (~1000 chunks/second)
- **Embeddings**: ~2-3 embeddings/second (Ollama on M3 Max)
- **Storage**: Very fast (batched inserts)

**Total time**: ~5-10 minutes for all 6 repositories

### Vector Search Speed

- **Exact search** (no index): ~50ms for 500 documents
- **IVFFlat index**: ~5-10ms for 500 documents
- Scales to millions of vectors with proper tuning

## Learning Resources

- **pgvector**: https://github.com/pgvector/pgvector
- **Ollama Embeddings**: https://github.com/ollama/ollama/blob/main/docs/api.md#generate-embeddings
- **gitingest**: https://github.com/cyclotruc/gitingest
- **RAG Overview**: https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html

---

For full architectural details, see [architecture.md](./architecture.md)
