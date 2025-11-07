# Stage 4: Agentic RAG - Retrieval-Augmented Generation Agent

**Status**: ✅ Complete (Phase 1: Ingestion Pipeline + Phase 2: Conversational Agent)

## ⚠️ Important: Use Python Embedding Service

**Due to an Ollama API bug, you MUST use the Python embedding service for this stage.**

The Python service provides:
- ✅ Reliable embedding generation
- ✅ Drop-in replacement for Ollama API
- ✅ Fast local inference
- ✅ No external dependencies once set up

### Setup Steps (Required)

**Step 1: Start the Python Embedding Service**

```bash
cd stage-4-agentic-rag/embedding-service
./start.sh
```

**What happens on first run:**
- Creates conda environment with Python 3.11
- Installs dependencies (fastapi, sentence-transformers, PyTorch, etc.)
- Downloads nomic-embed-text-v1.5 model (~500MB, one-time)
- Starts service on http://localhost:8001

**Subsequent runs:** Service starts immediately (environment and model cached)

**Wait for this output before proceeding:**
```
✓ Model loaded successfully (768 dimensions)
INFO:     Uvicorn running on http://0.0.0.0:8001
```

**Step 2: Run Ingestion (In a New Terminal)**

```bash
cd stage-4-agentic-rag
./ingest.sh
```

The ingestion script will:
- ✓ Verify Python service is running
- ✓ Start PostgreSQL with pgvector
- ✓ Process documents and generate embeddings
- ✓ Store everything in the database

### Prerequisites for Python Service

**Required:**
- **Conda** (Miniconda or Anaconda)
  - Install: https://docs.conda.io/en/latest/miniconda.html
- **8GB+ RAM** recommended
- **500MB disk space** for model download (one-time)

**Verify conda is installed:**
```bash
conda --version
# Should output: conda X.Y.Z
```

**If conda is not found:**
```bash
# Initialize conda for your shell
conda init bash  # or zsh, fish, etc.

# Restart your shell or source config
source ~/.bashrc  # or ~/.zshrc
```

### Troubleshooting Python Service

If you encounter issues, see [`embedding-service/README.md`](./embedding-service/README.md) for detailed troubleshooting.

**Quick verification:**
```bash
cd embedding-service
./verify-setup.sh
```

**Common issues:**
- **"conda: command not found"** → Run `conda init bash` and restart shell
- **Wrong Python version** → Environment will auto-create with Python 3.11
- **Port 8001 in use** → Change port in `server.py`
- **Module not found** → Run `./verify-setup.sh` to diagnose

---

## What This Stage Demonstrates

This stage showcases a production-ready RAG (Retrieval-Augmented Generation) system that combines:

### 1. **Document Ingestion Pipeline** (Shell Script)
- Uses pre-committed repository text files (no external dependencies by default)
- Optional fresh data fetching with `gitingest` (--refresh flag)
- Smart document chunking with overlap for context preservation
- Embedding generation via Ollama's `nomic-embed-text` model
- Vector storage in PostgreSQL with pgvector extension
- Idempotent operation (safe to re-run, only processes new/changed content)

### 2. **Conversational RAG Agent** (Java)
- Multi-turn conversations with memory retention
- JSON-based tool calling (Ollama native format)
- Context expansion via neighboring chunk retrieval
- Natural language Q&A over ingested documentation
- Full agent loop: Think → Act (search) → Observe → Answer

## How RAG Works in This Stage

### The Problem: Grounding AI in Your Documentation

Large Language Models (LLMs) are trained on general internet data, but they don't know about:
- Your internal documentation
- Recent project updates
- Proprietary frameworks (like Embabel)
- Specific implementation details

RAG solves this by giving the LLM **relevant context** from your documentation at query time.

### The RAG Pipeline: From Code to Conversation

```
┌─────────────────────────────────────────────────────────────┐
│ PHASE 1: INGESTION (One-Time Setup via ./ingest.sh)        │
└─────────────────────────────────────────────────────────────┘

1. Repository Content (committed text files)
   ┌──────────────────────────────────┐
   │  Embabel Agent Repository        │
   │  • Java source files             │
   │  • Documentation (*.md)          │
   │  • Configuration files           │
   │  • Example code                  │
   │  (Pre-processed via gitingest)   │
   └────────────┬─────────────────────┘
                │
                ▼
2. Document Chunking (DocumentChunker.java)
   ┌──────────────────────────────────┐
   │  Split into ~800 token chunks    │
   │  with 200 token overlap          │
   │  ────────────────────            │
   │  Chunk 1: [tokens 0-800]         │
   │  Chunk 2: [tokens 600-1400] ←─┐  │
   │  Chunk 3: [tokens 1200-2000]   │  │
   │           ↑         ↑          │  │
   │           └─────────┴──────────┘  │
   │           200 token overlap       │
   └────────────┬─────────────────────┘
                │
                ▼
3. Embedding Generation (EmbeddingService.java)
   ┌──────────────────────────────────┐
   │  For each chunk:                 │
   │  "@Agent annotation defines..."  │
   │           ↓                      │
   │  [Ollama: nomic-embed-text]      │
   │           ↓                      │
   │  [0.234, -0.891, 0.456, ...]     │
   │   ↑                              │
   │   1024-dimensional vector        │
   │   (semantic meaning encoded)     │
   └────────────┬─────────────────────┘
                │
                ▼
4. Vector Storage (PgVectorStore.java)
   ┌──────────────────────────────────────────┐
   │  PostgreSQL + pgvector Extension         │
   │  ┌────────────────────────────────────┐  │
   │  │ documents table                    │  │
   │  ├────────────────────────────────────┤  │
   │  │ id | source | chunk_index | ...   │  │
   │  │ embedding (vector[1024])           │  │
   │  │ content (text)                     │  │
   │  └────────────────────────────────────┘  │
   │                                           │
   │  IVFFlat Index for fast similarity search│
   └──────────────────────────────────────────┘

   Result: 487 searchable document chunks

┌─────────────────────────────────────────────────────────────┐
│ PHASE 2: QUERY TIME (Interactive via ./run.sh)             │
└─────────────────────────────────────────────────────────────┘

1. User Question
   "What is Embabel and how do I use it?"
                │
                ▼
2. RAG Agent Decides to Search
   ┌──────────────────────────────────┐
   │  RAGAgent.java                   │
   │  • Analyzes question             │
   │  • Decides tool needed           │
   │  • Calls: search_documentation   │
   └────────────┬─────────────────────┘
                │
                ▼
3. Query Embedding
   "What is Embabel and how do I use it?"
                │
                ▼
   [Ollama: nomic-embed-text]
                │
                ▼
   [0.123, -0.567, 0.890, ...]
    ↑
    Query vector (1024 dimensions)
                │
                ▼
4. Similarity Search
   ┌──────────────────────────────────────────┐
   │  PgVectorStore.search()                  │
   │  SELECT *, embedding <=> $1 AS distance  │
   │  FROM documents                          │
   │  ORDER BY distance                       │
   │  LIMIT 5;                                │
   │                                           │
   │  Cosine similarity finds closest vectors:│
   │  • Chunk 142: 0.89 similarity            │
   │  • Chunk 87:  0.85 similarity            │
   │  • Chunk 201: 0.82 similarity            │
   │  • Chunk 56:  0.79 similarity            │
   │  • Chunk 345: 0.76 similarity            │
   └────────────┬─────────────────────────────┘
                │
                ▼
5. Context Expansion (Optional)
   ┌──────────────────────────────────┐
   │  For each matched chunk:         │
   │  • Get previous chunk (N-1)      │
   │  • Get current chunk (N)         │
   │  • Get next chunk (N+1)          │
   │                                  │
   │  Ensures complete code examples  │
   │  and maintains narrative flow    │
   └────────────┬─────────────────────┘
                │
                ▼
6. Augmented Prompt
   ┌──────────────────────────────────────────┐
   │  SYSTEM: You are a helpful assistant... │
   │                                          │
   │  CONTEXT (from vector search):           │
   │  ---                                     │
   │  [Chunk 142: Embabel is a framework...]  │
   │  [Chunk 87: To create an agent, use...] │
   │  [Chunk 201: Example code: @Agent...]    │
   │  ---                                     │
   │                                          │
   │  USER: What is Embabel and how...       │
   └────────────┬─────────────────────────────┘
                │
                ▼
7. LLM Generation
   [Ollama: incept5/Jan-v1-2509:fp16]
                │
                ▼
   "Embabel is an agent framework for the JVM
    created by Rod Johnson. Here's how to use it:
    
    1. Annotate your class with @Agent
    2. Define @Action methods for atomic tasks
    3. Define @Goal methods for high-level objectives
    
    Example:
    @Agent
    public class MyAgent {
        @Action
        public Data fetchData() { ... }
        
        @Goal
        public Report generateReport() { ... }
    }"
                │
                ▼
8. Response to User
   [Answer shown in terminal]
```

### What Makes This "Semantic" Search?

Traditional keyword search:
- **Query**: "How to make an agent"
- **Matches**: Exact words "make" and "agent"
- **Misses**: "create", "build", "construct" variations

Semantic vector search:
- **Query**: "How to make an agent"
- **Vector**: [0.123, -0.567, 0.890, ...]
- **Matches**: Documents with similar **meaning**:
  - "Creating your first agent"
  - "Agent construction guide"
  - "Build intelligent agents with @Agent"
- **Why**: Embeddings encode **semantic meaning**, not just words

### The Math: Cosine Similarity

```
Query Vector:    [0.8, 0.3, -0.5]
Document Vector: [0.7, 0.4, -0.4]

Cosine Similarity = (A · B) / (||A|| × ||B||)
                  = (0.8×0.7 + 0.3×0.4 + -0.5×-0.4) / (sqrt(...) × sqrt(...))
                  = 0.89  ← High similarity (0-1 scale)

0.9-1.0 = Very similar (likely relevant)
0.7-0.9 = Similar (probably relevant)
0.5-0.7 = Somewhat similar (maybe relevant)
<0.5    = Different (likely not relevant)
```

### Why This Approach?

**Ingested Repositories** (configured in `repos.yaml`):
1. **Embabel Agent Framework** - Core agent patterns and @Agent/@Action/@Goal usage
2. **Embabel Examples** - Real-world agent implementations
3. **Embabel Java Template** - Starter project structure
4. **Embabel Kotlin Template** - Kotlin-specific patterns
5. **Tripper Application** - Production multi-agent system

**Result**: The agent can answer questions about:
- "How do I create an agent?" → Finds @Agent examples
- "What's the difference between Action and Goal?" → Finds conceptual docs
- "Show me a real example" → Finds Tripper code
- "How does Spring AI integration work?" → Finds integration guides

### Key Technical Details

**Embedding Model**: `nomic-embed-text`
- **Dimensions**: 1024 (higher = more nuanced meaning)
- **Context Length**: 8192 tokens (handles large chunks)
- **Speed**: ~50ms per embedding via Ollama
- **Why this model?**: Optimized for semantic search tasks

**Chunking Strategy**:
- **Size**: ~800 tokens (balance between context and precision)
- **Overlap**: 200 tokens (prevents splitting concepts)
- **Why overlap?**: Ensures code examples aren't cut mid-function

**Database**: PostgreSQL + pgvector
- **Storage**: Native vector column type
- **Indexing**: IVFFlat for fast approximate nearest neighbor search
- **Scale**: Handles millions of vectors efficiently
- **Why pgvector?**: Production-ready, ACID compliant, standard SQL

**Similarity Threshold**: 0.7 (configurable)
- **Higher (0.8-0.9)**: More precise, fewer results
- **Lower (0.5-0.7)**: More results, may include tangential info
- **Default (0.7)**: Good balance for documentation search

## Prerequisites

**IMPORTANT**: You must complete these steps before running the agent:

### 1. Docker Running
```bash
# Verify Docker is running
docker ps
```

### 2. Python 3.9+ Installed
```bash
# Check Python version
python3 --version  # Should be 3.9 or higher
```

### 4. Verify Python Embedding Service is Running

The Python service should already be running from the setup steps above.

**Check the service:**
```bash
# In the terminal where you ran ./start.sh, you should see:
# ✓ Model loaded successfully (768 dimensions)
# INFO:     Uvicorn running on http://0.0.0.0:8001

# Test from another terminal:
curl http://localhost:8001/health
# Should return: {"status":"healthy","model":"nomic-ai/nomic-embed-text-v1.5","dimensions":768}
```

**If not running:**
```bash
cd stage-4-agentic-rag/embedding-service
./start.sh
```

**For troubleshooting**, see [`embedding-service/README.md`](./embedding-service/README.md)

### 5. Run the Ingestion Script (REQUIRED)
```bash
# In a new terminal
cd stage-4-agentic-rag

# This MUST be run before the agent will work
./ingest.sh
```

**What `ingest.sh` does**:
- ✓ Verifies Python embedding service is running (REQUIRED)
- ✓ Starts PostgreSQL with pgvector extension (Docker)
- ✓ Runs database migrations (Flyway)
- ✓ Loads repository text files (committed to git)
- ✓ Chunks documents into searchable segments
- ✓ Generates embeddings via Python service (768 dimensions)
- ✓ Stores everything in PostgreSQL

**Optional refresh mode** (`./ingest.sh --refresh`):
- ✓ Checks for `gitingest` installation (required for refresh)
- ✓ Downloads fresh documentation from configured repositories
- ✓ Processes as above with updated content

**Expected output**:
```
🚀 Stage 4: RAG Ingestion Pipeline

🐍 Using Python embedding service (REQUIRED)
✓ Python service is ready at http://localhost:8001

📄 Using committed repository files from git
   (Use --refresh to fetch fresh data with gitingest)
🐘 Starting PostgreSQL with pgvector...
✓ PostgreSQL is ready
🔧 Running database migrations...
✓ Migrations complete
📚 Starting ingestion pipeline...

Processing embabel-agent...
  → Using committed file: data/gitingest-output/embabel-agent.txt
  → Chunks created: 98
  → Embeddings generated: 98/98
  → Stored: 98 documents

Processing embabel-examples...
  → Using committed file: data/gitingest-output/embabel-examples.txt
  → Chunks created: 67
  → Embeddings generated: 67/67
  → Stored: 67 documents
...

✅ Ingestion pipeline complete!
Total documents: 487
```

**First-time setup takes**: 
- Python service first run: 2-3 minutes (download model, create environment)
- Ingestion: 2-3 minutes (generating embeddings from committed files)
- Total: ~5 minutes for complete setup

**Note**: The repository text files are already committed to git, so no external downloads are needed. To fetch fresh content, use `./ingest.sh --refresh` (requires `gitingest` installation).

## Repository Data Source

**Default Mode** (No External Dependencies):
- Repository text files are **committed to git** in `data/gitingest-output/`
- No need to install `gitingest` or download repositories
- Faster setup (2-3 minutes vs 5-10 minutes)
- Deterministic content (everyone gets identical files)

**Refresh Mode** (Optional):
```bash
./ingest.sh --refresh
```
- Fetches fresh repository content from GitHub
- Requires `gitingest` to be installed
- Useful for updating documentation or adding new repositories
- Takes 5-10 minutes (downloads + processes repositories)

**When to use refresh mode:**
- ✅ You want the latest documentation updates
- ✅ You've modified `repos.yaml` to add new repositories
- ✅ You're developing/testing the ingestion pipeline itself

**When to use default mode:**
- ✅ First-time workshop setup (faster, no dependencies)
- ✅ You just want to try the RAG agent
- ✅ Offline environment or slow network

## Quick Start

### Step 1: Setup (One-Time)

**Terminal 1 - Start Python Service:**
```bash
cd stage-4-agentic-rag/embedding-service
./start.sh

# Wait for:
# ✓ Model loaded successfully (768 dimensions)
# INFO:     Uvicorn running on http://0.0.0.0:8001
```

**Terminal 2 - Run Ingestion:**
```bash
cd stage-4-agentic-rag

# Install dependencies and ingest documentation
./ingest.sh

# Verify ingestion succeeded
docker exec -it stage4-pgvector psql -U workshop -d workshop_rag -c "SELECT COUNT(*) FROM documents;"
# Should show: count: 487 (or similar number > 0)
```

### Step 2: Run the Agent

```bash
# Standard mode
./run.sh

# Verbose mode (see agent reasoning and tool calls)
./run.sh --verbose
```

### Step 3: Chat with the Agent

```
╔════════════════════════════════════════════════════╗
║  RAG Agent - Embabel & Spring AI Assistant        ║
║  Type 'help' for commands, 'exit' to quit         ║
╚════════════════════════════════════════════════════╝

💬 You: What is Embabel?

[Agent thinking...]
[Tool: search_documentation]
[Retrieved 5 documents]

🤖 Assistant: 
Embabel is an agent framework for the JVM created by Rod Johnson,
the founder of Spring Framework. It uses Goal-Oriented Action Planning
(GOAP) to build intelligent applications.

Key features:
- @Agent annotation to define agent behavior
- Actions: Atomic steps an agent can take
- Goals: High-level objectives the agent works towards
- Automatic planning: Framework determines action sequences
- Integration with Spring AI

💬 You: Show me a simple example

[Using conversation context...]
[Tool: search_documentation]

🤖 Assistant:
Here's a basic Embabel agent example:

@Agent(description = "Quiz generator agent")
public class QuizAgent {
    
    @Action(description = "Fetch web content")
    public WebContent fetchContent(String url) {
        // Implementation
    }
    
    @Action(description = "Generate quiz from content")
    public Quiz generateQuiz(WebContent content) {
        // Implementation
    }
    
    @Goal
    public Quiz createQuizFromUrl(String url) {
        // Embabel will automatically plan:
        // 1. Call fetchContent(url)
        // 2. Call generateQuiz(content)
        return null;  // Placeholder for framework
    }
}

The framework automatically determines which actions to call
and in what order to achieve the goal.

💬 You: How does it integrate with Spring AI?

[Continuing conversation...]
```

## Architecture Overview

```
┌─────────────┐
│ User Query  │
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────────┐
│         RAGAgent.chat()              │
│  • Add to ConversationMemory         │
│  • Build prompt with history         │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│      Agent Loop (Max 10 iterations)  │
│  1. Think: LLM processes prompt      │
│  2. Act: Calls tool (if needed)      │
│  3. Observe: Processes tool result   │
└──────┬───────────────────────────────┘
       │
       ├──→ Tool Call? ──No──→ Final Answer
       │                            ↓
       Yes                   Return to User
       │
       ▼
┌──────────────────────────────────────┐
│      search_documentation            │
│  • Parse JSON tool call              │
│  • Execute vector search             │
│  • Expand with neighbors (optional)  │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│    PgVectorStore.search()            │
│  • Generate query embedding          │
│  • Cosine similarity search          │
│  • Return top K results              │
└──────┬───────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│  PostgreSQL + pgvector               │
│  • 487 documents ingested            │
│  • 1024-dim embeddings               │
│  • IVFFlat index for fast search     │
└──────┬───────────────────────────────┘
       │
       ▼
   Retrieved Documents
       │
       ▼
   Add to Memory & Continue Loop
```

## Key Features

### 1. Two-Phase Architecture

**Phase 1: Ingestion (Shell Script)**
- Runs once to populate the database
- Can be re-run to update documentation
- Hash-based change detection (only processes new content)

**Phase 2: Conversational Agent (Java)**
- Interactive Q&A over ingested docs
- Maintains conversation context
- Retrieves relevant information on-demand

### 2. Multi-Turn Conversations

The agent remembers previous exchanges:

```
Q1: "What is Embabel?"
A1: [Explains Embabel framework]

Q2: "Who created it?"          ← Uses context from Q1
A2: "Rod Johnson, founder of Spring Framework"

Q3: "Show me an example"       ← Understands "it" refers to Embabel
A3: [Provides code example]
```

### 3. Context Expansion

When retrieving documents, the agent can fetch neighboring chunks:

```java
// If document is part of a larger explanation
if (expandContext) {
    // Get previous and next chunks from same file
    documents = expandWithNeighbors(documents);
}
```

This ensures complete code examples and maintains narrative flow.

### 4. JSON Tool Calling

Cleaner than XML format (used in Stage 1):

```json
{
  "tool": "search_documentation",
  "parameters": {
    "query": "how to create an agent",
    "topK": 5,
    "expandContext": true
  }
}
```

## CLI Commands

Once the agent is running, you can use these commands:

- `help` - Show available commands
- `history` - Display full conversation history
- `clear` - Clear conversation memory (start fresh)
- `exit` or `quit` - End conversation

## Testing

### Quick Tool Calling Test

Verify that tool calling works correctly:

```bash
./test-tool-calling.sh
```

This script will:
1. Check all prerequisites (Ollama, models, PostgreSQL, documents)
2. Run a simple query with verbose output
3. Verify that the agent correctly parses and executes tool calls

**Look for:**
- `✓ Parsed as tool call: ToolCall{...}` ← Tool calling works!
- `✗ Not a tool call` ← May indicate parsing issue

### Integration Tests

```bash
# Build and run all tests
mvn test

# Run specific test
mvn test -Dtest=RAGAgentIntegrationTest#testVectorSearch
```

**Test Coverage:**
- ✅ Vector search with similarity threshold
- ✅ Single-turn conversation
- ✅ Multi-turn conversation with context
- ✅ Tool invocation and parsing
- ✅ Context expansion with neighbors
- ✅ Conversation memory management
- ✅ Database connection and queries

**Note**: First test run may be slow (30-60 seconds) while Ollama loads the model into memory.

## Project Structure

```
stage-4-agentic-rag/
├── README.md                        # This file
├── architecture.md                  # Detailed architecture documentation
├── ingest.sh                        # ⚠️ RUN THIS FIRST
├── run.sh                           # Run the agent
├── test-tool-calling.sh             # Quick verification script
├── cleanup.sh                       # Remove database and generated files
├── docker-compose.yml               # PostgreSQL + pgvector setup
├── repos.yaml                       # Configure what to ingest
│
├── db/migration/
│   └── V1__Create_documents_table.sql   # Database schema (Flyway)
│
├── data/                            # Generated by ingest.sh (gitignored)
│   └── gitingest-output/
│       ├── spring-ai.txt
│       ├── embabel-agent.txt
│       └── ...
│
└── src/
    ├── main/java/com/incept5/workshop/stage4/
    │   ├── agent/
    │   │   ├── RAGAgent.java                # Main conversational agent
    │   │   ├── ConversationMemory.java      # Multi-turn context tracking
    │   │   └── RAGAgentDemo.java            # Interactive CLI
    │   ├── tool/
    │   │   ├── Tool.java                    # Tool interface
    │   │   ├── ToolRegistry.java            # Tool management
    │   │   └── RAGTool.java                 # Document search with expansion
    │   ├── util/
    │   │   └── JsonToolCallParser.java      # Parse JSON tool calls
    │   ├── db/
    │   │   ├── Document.java                # Document record with chunk info
    │   │   ├── PgVectorStore.java           # Vector database operations
    │   │   └── DatabaseConfig.java          # HikariCP connection pool
    │   └── ingestion/
    │       ├── IngestionService.java        # Main ingestion orchestration
    │       ├── DocumentChunker.java         # Split docs into chunks
    │       ├── EmbeddingService.java        # Generate embeddings
    │       ├── IngestionConfig.java         # Configuration
    │       └── RepoConfig.java              # Repository metadata
    │
    └── test/java/com/incept5/workshop/stage4/
        ├── RAGAgentIntegrationTest.java     # Comprehensive tests
        └── VectorSearchIntegrationTest.java # Database-specific tests
```

## What Makes This Different from Stage 1?

| Aspect | Stage 1 (Simple Agent) | Stage 3 (RAG Agent) |
|--------|------------------------|---------------------|
| **Memory** | None (single turn) | Full conversation history |
| **Tool Format** | XML (custom) | JSON (Ollama native) |
| **Data Source** | External APIs (weather, country) | Vector database (docs) |
| **Context** | Single question | Multi-turn dialogue |
| **Scale** | 2 tools, instant calls | 1 RAG tool, 487+ documents |
| **Expansion** | N/A | Neighboring chunk retrieval |
| **Setup** | None required | Ingestion pipeline required |

## Configuration

### Changing Models

Edit `RAGAgentDemo.java`:

```java
private static final String LLM_MODEL = "incept5/Jan-v1-2509:fp16";
private static final String EMBEDDING_MODEL = "nomic-embed-text";
```

### Adding More Repositories

Edit `repos.yaml`:

```yaml
repositories:
  - name: my-repo
    url: https://github.com/user/my-repo
    branch: main
    description: "My custom repository"
```

Then re-run `./ingest.sh` to add the new content.

### Adjusting Chunk Size

Edit `repos.yaml`:

```yaml
settings:
  chunk_size: 800          # tokens per chunk (default)
  chunk_overlap: 200       # overlap for context (default)
  similarity_threshold: 0.7  # minimum cosine similarity (default)
```

## Troubleshooting

### "No documents found" error

**Solution**: Run the ingestion script
```bash
./ingest.sh
```

**Verify**:
```bash
docker exec -it stage4-pgvector psql -U workshop -d workshop_rag -c "SELECT COUNT(*) FROM documents;"
```

Should show count > 0 (typically ~487 documents).

### Ollama connection refused (for LLM)

**Solution**: Start Ollama and pull the LLM model
```bash
# Start Ollama
ollama serve

# Pull required LLM model (for agent reasoning)
ollama pull incept5/Jan-v1-2509:fp16

# Verify
curl http://localhost:11434/api/tags
```

**Note**: You do NOT need `nomic-embed-text` in Ollama. The Python service handles embeddings.

### PostgreSQL connection refused

**Solution**: Start Docker Compose
```bash
cd stage-4-agentic-rag
docker-compose up -d

# Check status
docker-compose ps

# View logs if needed
docker-compose logs
```

### Slow first response (30-60 seconds)

**Explanation**: This is normal! Ollama loads the model into memory on the first request.

**Subsequent queries are much faster** (1-5 seconds typically).

**Alternative**: Use a smaller/faster model:
```bash
ollama pull qwen2.5:7b
# Update LLM_MODEL in RAGAgentDemo.java
```

### Python embedding service not running

**Error**: "Connection refused" or "Service not available" during ingestion

**Solution**: Start the Python service
```bash
cd embedding-service
./start.sh

# Wait for:
# ✓ Model loaded successfully (768 dimensions)
# INFO:     Uvicorn running on http://0.0.0.0:8001
```

**Verify it's working:**
```bash
curl http://localhost:8001/health
# Should return: {"status":"healthy",...}
```

### Python service setup issues

**Problem**: Service won't start or shows errors

**Solution 1**: Run verification script
```bash
cd embedding-service
./verify-setup.sh
```

**Solution 2**: Recreate conda environment
```bash
conda env remove -n embedding-service
./start.sh  # Will recreate environment
```

**Solution 3**: Check detailed troubleshooting
See [`embedding-service/README.md`](./embedding-service/README.md) for:
- Conda installation issues
- Python version problems
- Module not found errors
- Port conflicts

### "Repository file not found" error

**Cause**: Text files missing from `data/gitingest-output/`

**Solution 1**: Ensure files are checked out from git
```bash
git status
git checkout data/gitingest-output/*.txt
```

**Solution 2**: Fetch fresh content with gitingest
```bash
# Install gitingest if not available
brew install pipx && pipx install gitingest  # macOS
# or
pipx install gitingest  # if pipx already installed

# Run ingestion in refresh mode
./ingest.sh --refresh
```

### Clean Slate (Reset Everything)

```bash
# Stop database and remove all data
./cleanup.sh

# Or manually:
docker-compose down -v
rm -rf data/

# Stop Python service (Ctrl+C in the terminal where it's running)
# Or:
pkill -f "python.*server.py"
```

Then restart:
1. Python service: `cd embedding-service && ./start.sh`
2. Ingestion: `./ingest.sh`

## What You'll Learn

By completing this stage, you'll understand:

✅ **RAG Fundamentals**: Retrieval → Augmentation → Generation flow  
✅ **Vector Databases**: PostgreSQL + pgvector for production use  
✅ **Embeddings**: How to generate and use semantic vectors  
✅ **Conversational AI**: Multi-turn dialogues with memory  
✅ **Document Processing**: Chunking strategies and overlap  
✅ **Tool Integration**: JSON-based tool calling with Ollama  
✅ **Production Patterns**: Connection pooling, migrations, idempotency  

## Next Steps

After completing Stage 3, you're ready for:

- **Stage 4**: Multi-Agent Teams (orchestration, specialized agents)
- **Stage 5**: Enterprise Patterns (monitoring, resilience, security)

Or explore enhancements:
- Add metadata filtering to vector search
- Implement hybrid search (vector + keyword)
- Add document re-ranking for better results
- Create a web UI with streaming responses
- Add persistent conversation storage

## Resources

### Internal Documentation
- **Root Architecture**: [/architecture.md](../architecture.md)
- **Stage 3 Architecture**: [architecture.md](./architecture.md)
- **Stage 1 (Simple Agent)**: [/stage-1-simple-agent/README.md](../stage-1-simple-agent/README.md)

### External Resources
- [Spring AI RAG Documentation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [gitingest GitHub](https://github.com/cyclotruc/gitingest)
- [Ollama Embeddings API](https://github.com/ollama/ollama/blob/main/docs/api.md#generate-embeddings)
- [nomic-embed-text Model](https://ollama.com/library/nomic-embed-text)

---

**Stage 3 Status**: ✅ Complete  
**Last Updated**: 2025-11-06  
**Total Code**: ~1,800 lines across 15 files  
**Dependencies**: Docker, Ollama, PostgreSQL, Java 21+

---
