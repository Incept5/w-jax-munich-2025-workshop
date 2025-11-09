
# Stage 4: Agentic RAG - Chat with Your Codebase

**Status**: ✅ Complete

---

## What Is This?

This is **example code** showing a 2-stage RAG (Retrieval-Augmented Generation) implementation that lets you chat with documentation about the **Embabel framework codebase**.

**The Two Stages:**

1. **Ingestion** (`./ingest.sh`) - One-time setup:
   - Reads 5 Embabel repositories (committed text files)
   - Breaks them into ~800 token chunks
   - Converts to vector embeddings (768 dimensions)
   - Stores in PostgreSQL with pgvector

2. **RAG Agent** (`./run.sh`) - Interactive chat:
   - Ask questions in plain English
   - Agent searches vector database for relevant docs
   - Uses those docs to give accurate answers
   - Remembers conversation context

**What You Can Ask:**

```
💬 You: What is Embabel?
🤖 Agent: Embabel is a Goal-Oriented Action Planning framework...

💬 You: Show me a simple example
🤖 Agent: Here's a basic @Agent with @Action and @Goal...

💬 You: How does it integrate with Spring AI?
🤖 Agent: Embabel uses Spring AI's ChatClient for...
```

**Why This Matters:**
- ✅ Agent answers based on actual docs (not hallucinations)
- ✅ Works with any codebase (just change `repos.yaml`)
- ✅ Production-ready PostgreSQL with pgvector
- ✅ Natural conversation with context memory

**Want to Learn by Doing?**
👉 See **[EXERCISES.md](./EXERCISES.md)** for 10+ hands-on exercises that teach you how RAG works:
- Visualize embeddings and understand vector search
- Experiment with chunk sizes and search parameters
- Build custom chunking strategies for code
- Add metadata filtering and hybrid search
- Create a web UI with streaming responses
- Measure and improve search quality

---

## Quick Start

### 🎯 Workshop Participants: Two Options!

**Option A: Use Shared Database** (Recommended - No Docker needed!)
```bash
./run.sh --shared  # That's it! Connects to 172.20.15.241:5432
```
- See **[WORKSHOP_CONNECTION.md](./WORKSHOP_CONNECTION.md)** for quick start
- See **[WORKSHOP_SETUP.md](./WORKSHOP_SETUP.md)** for detailed setup
- Skip to "Step 3: Start Chatting" below

**Option B: Run Everything Locally**
- Follow full setup below (Docker + Ingestion required)
- Complete independence, no network dependencies

---

### 🔧 Workshop Instructor: Firewall Setup

If you're hosting the shared database, you need to open port 5432 on your macOS firewall:

**Quick Start:**
```bash
./firewall-setup.sh open    # Before workshop
./firewall-setup.sh verify  # Check everything works
./firewall-setup.sh close   # After workshop
```

**Documentation:**
- **[FIREWALL_QUICKSTART.md](./FIREWALL_QUICKSTART.md)** - 5-minute setup guide
- **[FIREWALL_SETUP.md](./FIREWALL_SETUP.md)** - Complete reference with troubleshooting

---

### Prerequisites

Make sure you have:

1. **Docker** running
   ```bash
   docker ps  # Should not error
   ```

2. **Ollama** with embedding model
   ```bash
   ollama serve
   ollama pull qwen3-embedding:0.6b
   ```

3. **Java 21+** and **Maven**
   ```bash
   java --version
   mvn --version
   ```

---

### Step 1: Run Ingestion Pipeline

```bash
cd stage-4-agentic-rag
./ingest.sh
```

**What the script does:**
1. ✓ Verify Ollama is running
2. ✓ Check/pull qwen3-embedding:0.6b model
3. ✓ Build the Java project (automatically builds shared module dependencies)
4. ✓ Start PostgreSQL with pgvector (Docker)
5. ✓ Load 5 Embabel repositories (committed files)
6. ✓ Chunk into ~512 token segments
7. ✓ Generate embeddings (1024 dimensions with qwen3-embedding:0.6b)
8. ✓ Store ~3,200 searchable chunks

**Note:** The script automatically handles multi-module Maven builds, building both the shared module and stage-4 module in the correct order.

**Takes 15-20 minutes** (rate-limited to avoid overwhelming Ollama). You'll see:
```
🚀 Stage 4: RAG Ingestion Pipeline

🦙 Using Ollama for embeddings
Using default model: qwen3-embedding:0.6b
✓ Ollama is ready

🔨 Building project (including shared module)...
✓ Build complete

🐘 Starting PostgreSQL with pgvector...
✓ PostgreSQL is ready

📚 Starting ingestion pipeline...
Processing embabel-agent...
  → Chunks created: 2798
  → Embeddings generated: 2798/2798
  → Stored: 2798 documents

✅ Ingestion pipeline complete!
Total documents: ~3,200
```

**Note:** Files are in git, no downloads needed. For fresh content: `./ingest.sh --refresh`

---

### Step 2: Start Chatting!

```bash
./run.sh                        # Standard mode (default model)
./run.sh --verbose              # See agent's thinking
./run.sh --model qwen2.5:7b     # Use specific model
./run.sh -m mistral:7b -v       # Model + verbose mode
```

**Try these:**
```
💬 What is Embabel and who created it?
💬 Show me a simple @Agent example
💬 How do Actions differ from Goals?
💬 What's the Tripper application?
💬 How does Spring AI integration work?
```

**Chat commands:**
- `help` - Show commands
- `history` - View conversation
- `clear` - Fresh start
- `exit` - Quit

**Example:**
```
╔════════════════════════════════════════════════════╗
║  RAG Agent - Embabel & Spring AI Assistant        ║
╚════════════════════════════════════════════════════╝

💬 You: What is Embabel?

[Tool: search_documentation]
[Retrieved 5 documents]

🤖 Assistant: 
Embabel is an agent framework for the JVM created by Rod Johnson.
It uses Goal-Oriented Action Planning (GOAP)...

💬 You: Show me an example

[Using conversation context...]

🤖 Assistant:
Here's a basic agent:

@Agent(description = "Quiz generator")
public class QuizAgent {
    @Action
    public WebContent fetchContent(String url) { ... }
    
    @Goal
    public Quiz createQuiz(String url) { ... }
}
```

---

## How It Works

### High-Level Architecture

**Ingestion (One-time):**
```
Docs → Chunk → Embed → PostgreSQL+pgvector
       ↓       ↓       ↓
     800 tok  768 dim  487 chunks
```

**Query (Every question):**
```
Question → Embed → Search → LLM+Context → Answer
           ↓       ↓        ↓
         768 dim  Top 5    Grounded response
```

### Agent Loop (Simplified)

```java
public String chat(String userMessage) {
    memory.addUserMessage(userMessage);
    
    for (int i = 0; i < MAX_ITERATIONS; i++) {
        String response = llm.generate(
            memory.getMessages(),
            SYSTEM_PROMPT,
            List.of(ragTool.getDefinition())
        );
        
        if (isToolCall(response)) {
            // Search documentation
            List<Document> docs = ragTool.execute(
                parseToolCall(response)
            );
            memory.addToolResult(docs);
            // Loop continues with new context
        } else {
            // Final answer!
            return response;
        }
    }
}
```

### Why Vector Search?

**Traditional keyword search:**
- Query: "How to make an agent"
- Matches: Exact words "make" and "agent"
- Misses: "create", "build", "construct"

**Semantic vector search:**
- Query: "How to make an agent"
- Embedding: `[0.123, -0.567, 0.890, ...]` (768 numbers)
- Matches similar **meaning**:
  - "Creating your first agent" ← Different words, same meaning!
  - "Agent construction guide"
  - "Build agents with @Agent"

**The magic:** Embeddings encode semantic meaning, not just words.

---

## Key Code Files

Explore these to understand the implementation:

- **[`RAGAgent.java`](./src/main/java/com/incept5/workshop/stage4/agent/RAGAgent.java)** - Main agent loop
- **[`RAGTool.java`](./src/main/java/com/incept5/workshop/stage4/tool/RAGTool.java)** - Vector search tool
- **[`PgVectorStore.java`](./src/main/java/com/incept5/workshop/stage4/db/PgVectorStore.java)** - Database operations
- **[`ConversationMemory.java`](./src/main/java/com/incept5/workshop/stage4/agent/ConversationMemory.java)** - Context tracking
- **[`IngestionService.java`](./src/main/java/com/incept5/workshop/stage4/ingestion/IngestionService.java)** - Ingestion pipeline
- **[`DocumentChunker.java`](./src/main/java/com/incept5/workshop/stage4/ingestion/DocumentChunker.java)** - Smart chunking

**Full architecture:** See [`architecture.md`](./architecture.md)

---

## Configuration

### Changing the Model

The RAG agent uses **two different models**:

1. **LLM Model** (for reasoning and chat) - Configurable via multiple methods
2. **Embedding Model** (for vector generation) - Fixed at `nomic-embed-text`

#### Override LLM Model

You can override the LLM model (used for reasoning) in three ways:

**Method 1: Command-Line Flag** (recommended for testing)
```bash
./run.sh --model qwen2.5:7b
./run.sh -m mistral:7b --verbose
```

**Method 2: Environment Variable** (recommended for persistent config)
```bash
export OLLAMA_MODEL="qwen2.5:7b"
./run.sh
```

**Method 3: System Property**
```bash
java -Dollama.model="mistral:7b" -jar target/stage-4-agentic-rag-1.0-SNAPSHOT.jar
```

**Default Model:** `qwen3:4b`

**Note:** The embedding model (`nomic-embed-text`) remains unchanged regardless of LLM model override. This is intentional - embeddings must be generated with the same model used during ingestion for semantic search to work correctly.

### Add Your Repositories

Edit `repos.yaml`:
```yaml
repositories:
  - name: my-repo
    url: https://github.com/user/my-repo
    branch: main
    description: "My custom codebase"
```

Then: `./ingest.sh --refresh`

### Adjust Chunking

Edit `repos.yaml`:
```yaml
settings:
  chunk_size: 800          # Tokens per chunk
  chunk_overlap: 200       # Overlap for context
  similarity_threshold: 0.7  # Minimum similarity
```

---

## Troubleshooting

### Build Error: "Could not find artifact com.incept5:shared:jar"

**Problem:** Maven can't find the shared module dependency.

**Fix:** The `ingest.sh` script now automatically handles this. If you're building manually:
```bash
# Build from parent directory
cd /path/to/w-jax-munich-2025-workshop
mvn clean install -DskipTests

# Or use the script (recommended)
cd stage-4-agentic-rag
./ingest.sh
```

The script builds both the shared module and stage-4 module in the correct order using Maven's reactor build.

### "No documents found"

**Fix:**
```bash
./ingest.sh  # Run ingestion

# Verify:
docker exec -it stage4-pgvector psql -U workshop -d workshop_rag \
  -c "SELECT COUNT(*) FROM documents;"
# Should show: 487 (or similar)
```

### Ollama not running

**Fix:**
```bash
ollama serve

# Test:
curl http://localhost:11434/api/tags
```

### PostgreSQL connection refused

**Fix:**
```bash
docker-compose up -d

# Check:
docker-compose ps
```

### Slow first response (30-60s)

**Normal!** Ollama loads model into memory on first request.

**Subsequent queries:** 1-5 seconds

**Faster alternative:**
```bash
ollama pull qwen2.5:7b  # Smaller model
```

### Reset Everything

```bash
./cleanup.sh  # Removes database and data

# Then restart:
./ingest.sh
```

---

## Testing

**Quick test:**
```bash
./test-tool-calling.sh
```

**Integration tests:**
```bash
mvn test
```

---

## Project Structure

```
stage-4-agentic-rag/
├── README.md                    # This file
├── architecture.md              # Detailed docs
├── ingest.sh                    # ⚠️ RUN THIS FIRST
├── run.sh                       # Run the agent
├── docker-compose.yml           # PostgreSQL + pgvector
├── repos.yaml                   # What to ingest
│
└── src/main/java/.../stage4/
    ├── agent/
    │   ├── RAGAgent.java        # Main agent
    │   ├── ConversationMemory.java
    │   └── RAGAgentDemo.java    # CLI
    ├── tool/
    │   └── RAGTool.java         # Vector search
    ├── db/
    │   └── PgVectorStore.java   # Database
    └── ingestion/
        ├── IngestionService.java
        ├── EmbeddingService.java # Ollama embeddings
        └── DocumentChunker.java
```

---

## What You'll Learn

✅ RAG fundamentals (Retrieval → Augmentation → Generation)  
✅ Vector databases (PostgreSQL + pgvector)  
✅ Semantic embeddings (768-dimensional vectors)  
✅ Conversational AI (multi-turn memory)  
✅ Document chunking (with overlap)  
✅ JSON tool calling (Ollama native)  
✅ Production patterns (connection pooling, migrations)

---

## Differences from Stage 2 (Simple Agent)

| Aspect | Stage 2 | Stage 4 (RAG) |
|--------|---------|---------------|
| **Memory** | None | Full conversation history |
| **Tool Format** | XML | JSON |
| **Data Source** | External APIs | Vector database (487 docs) |
| **Context** | Single question | Multi-turn dialogue |
| **Setup** | None | Ingestion required |

---

## Resources

**Internal:**
- [Root Architecture](../architecture.md)
- [Stage 4 Architecture](./architecture.md)
- [Stage 2 README](../stage-2-simple-agent/README.md)

**External:**
- [pgvector](https://github.com/pgvector/pgvector)
- [gitingest](https://github.com/cyclotruc/gitingest)
- [Ollama Embeddings API](https://github.com/ollama/ollama/blob/main/docs/api.md#generate-embeddings)
- [nomic-embed-text Model](https://ollama.com/library/nomic-embed-text)

---

**Last Updated:** 2025-11-09
**Total Code:** ~1,800 lines across 15 files
**Dependencies:** Docker, Ollama, PostgreSQL, Java 21+, Maven
