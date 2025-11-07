
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

### Prerequisites

Make sure you have:

1. **Conda/Miniconda** installed ([Download](https://docs.conda.io/en/latest/miniconda.html))
   ```bash
   conda --version  # Should show version number
   ```

2. **Docker** running
   ```bash
   docker ps  # Should not error
   ```

3. **Ollama** running with model
   ```bash
   ollama serve
   ```

4. **Python 3.9+**
   ```bash
   python3 --version
   ```

---

### Step 1: Start Python Embedding Service (Terminal 1)

⚠️ **Important:** You MUST use the Python embedding service (Ollama has a bug).

```bash
cd stage-4-agentic-rag/embedding-service
./start.sh
```

**First run:** Takes 2-3 minutes (downloads model, creates conda environment)

**Wait for this:**
```
✓ Model loaded successfully (768 dimensions)
INFO:     Uvicorn running on http://0.0.0.0:8001
```

**Subsequent runs:** Starts in seconds (everything cached)

**Troubleshooting:**
- `conda: command not found` → Run `conda init bash` then restart terminal
- Port 8001 in use → Change port in `server.py`
- More help: See [`embedding-service/README.md`](./embedding-service/README.md)

---

### Step 2: Run Ingestion Pipeline (Terminal 2)

```bash
cd stage-4-agentic-rag
./ingest.sh
```

**This will:**
1. ✓ Verify Python service is running
2. ✓ Start PostgreSQL with pgvector (Docker)
3. ✓ Load 5 Embabel repositories (committed files)
4. ✓ Chunk into ~800 token segments
5. ✓ Generate embeddings (768 dimensions)
6. ✓ Store 487 searchable chunks

**Takes 2-3 minutes.** You'll see:
```
🚀 Stage 4: RAG Ingestion Pipeline

🐍 Using Python embedding service
✓ Python service is ready at http://localhost:8001

Processing embabel-agent...
  → Chunks created: 98
  → Embeddings generated: 98/98
  → Stored: 98 documents

✅ Ingestion pipeline complete!
Total documents: 487
```

**Note:** Files are in git, no downloads needed. For fresh content: `./ingest.sh --refresh`

---

### Step 3: Start Chatting!

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

**Default Model:** `incept5/Jan-v1-2509:fp16`

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

### "No documents found"

**Fix:**
```bash
./ingest.sh  # Run ingestion

# Verify:
docker exec -it stage4-pgvector psql -U workshop -d workshop_rag \
  -c "SELECT COUNT(*) FROM documents;"
# Should show: 487 (or similar)
```

### Python service not running

**Fix:**
```bash
cd embedding-service
./start.sh

# Test:
curl http://localhost:8001/health
```

**More help:** [`embedding-service/README.md`](./embedding-service/README.md)

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
cd embedding-service && ./start.sh  # Terminal 1
./ingest.sh                         # Terminal 2
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
├── embedding-service/           # Python embedding service
│   ├── start.sh                 # Start service
│   ├── server.py                # FastAPI server
│   └── README.md                # Troubleshooting
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

**Last Updated:** 2025-11-07  
**Total Code:** ~1,800 lines across 15 files  
**Dependencies:** Docker, Ollama, PostgreSQL, Java 21+, Conda
