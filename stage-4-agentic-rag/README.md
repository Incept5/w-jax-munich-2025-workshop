
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

**Ideas to Extend:**
- Add your own repositories to `repos.yaml`
- Implement metadata filtering (by file type, date, author)
- Add hybrid search (vector + keyword)
- Build a web UI with streaming responses
- Create specialized agents for different codebases
- Add document re-ranking for better relevance

---

## Quick Start

### Prerequisites

Make sure you have:

1. **Docker** running (required for PostgreSQL)
   ```bash
   docker ps  # Should not error
   ```

2. **Ollama** running with model (required for LLM)
   ```bash
   ollama serve
   ollama pull incept5/Jan-v1-2509:fp16  # Or your preferred model
   ```

3. **Choose Your Embedding Provider:**

   **Option A: Python Service (Free, Local)** ✅ Recommended
   - Requires: Conda/Miniconda ([Download](https://docs.conda.io/en/latest/miniconda.html))
   - One-time setup: 2-3 minutes
   - Cost: FREE
   - Speed: ~2-3 minutes for ingestion

   **Option B: OpenAI API (Paid, Simple)**
   - Requires: OpenAI API key ([Get one](https://platform.openai.com/api-keys))
   - Setup: Just add API key to `.env` file
   - Cost: ~$0.008 (less than 1 cent for workshop)
   - Speed: ~1-2 minutes for ingestion

---

### Option A: Using Python Embedding Service (Free, Local)

**Step 1: Configure (one-time)**

```bash
cd stage-4-agentic-rag
cp .env.example .env
# Edit .env and ensure: EMBEDDING_PROVIDER=python
```

**Step 2: Start Python Service (Terminal 1)**

```bash
cd embedding-service
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

**Step 3: Run Ingestion (Terminal 2)**

```bash
cd stage-4-agentic-rag
./ingest.sh
```

---

### Option B: Using OpenAI Embeddings (Paid, Simple)

**Step 1: Configure**

```bash
cd stage-4-agentic-rag
cp .env.example .env
```

Edit `.env` and add:
```bash
EMBEDDING_PROVIDER=openai
OPENAI_API_KEY=sk-proj-your-key-here
```

**Step 2: Run Ingestion**

```bash
./ingest.sh
```

**This will:**
1. ✓ Verify OpenAI API key is set
2. ✓ Start PostgreSQL with pgvector (Docker)
3. ✓ Load 5 Embabel repositories (committed files)
4. ✓ Chunk into ~800 token segments
5. ✓ Generate embeddings via OpenAI (768 dimensions)
6. ✓ Store 487 searchable chunks

**Takes 1-2 minutes.** You'll see:
```
🚀 Stage 4: RAG Ingestion Pipeline

🤖 Using OpenAI embeddings
   text-embedding-3-small, 768 dimensions
✓ OpenAI API key configured: sk-proj...xyz
💰 Estimated cost: ~$0.008 (less than 1 cent)

Processing embabel-agent...
  → Chunks created: 98
  → Embeddings generated: 98/98
  → Stored: 98 documents

✅ Ingestion pipeline complete!
Total documents: 487
```

**Note:** Files are in git, no downloads needed. For fresh content: `./ingest.sh --refresh`

---

### Start Chatting!

```bash
./run.sh          # Standard mode
./run.sh --verbose  # See agent's thinking
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

## Embedding Provider Comparison

| Feature | Python Service | OpenAI API |
|---------|---------------|------------|
| **Setup Time** | 2-3 minutes (first time) | 30 seconds |
| **Cost** | FREE | ~$0.008 per ingestion |
| **Speed** | 2-3 minutes ingestion | 1-2 minutes ingestion |
| **Requirements** | Conda/Miniconda | API key |
| **Privacy** | 100% local | Data sent to OpenAI |
| **Model** | nomic-embed-text | text-embedding-3-small |
| **Dimensions** | 768 | 768 |
| **Quality** | Excellent for code | Excellent general purpose |
| **Internet** | Not required | Required |
| **Recommended For** | Privacy-conscious, local dev | Quick setup, workshops |

### Switching Providers

You can switch embedding providers at any time:

```bash
# Switch to OpenAI
echo "EMBEDDING_PROVIDER=openai" >> .env
echo "OPENAI_API_KEY=sk-..." >> .env
./ingest.sh

# Switch to Python
echo "EMBEDDING_PROVIDER=python" >> .env
cd embedding-service && ./start.sh  # Terminal 1
./ingest.sh                         # Terminal 2
```

**Note:** When switching providers, you need to re-run ingestion since embeddings are different.

---

## Configuration

### Change LLM Model

Edit `RAGAgentDemo.java`:
```java
private static final String LLM_MODEL = "qwen2.5:7b";  // Faster alternative
```

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

### OpenAI API Issues

**"OPENAI_API_KEY not set"**

```bash
# Check if .env file exists
cat .env

# Add API key
echo "OPENAI_API_KEY=sk-proj-your-key-here" >> .env

# Verify it's loaded
source .env && echo $OPENAI_API_KEY
```

**"OpenAI API connection failed"**

- Check your API key is valid at https://platform.openai.com/api-keys
- Verify you have credits: https://platform.openai.com/usage
- Check network/firewall isn't blocking api.openai.com

**"Rate limit exceeded"**

- You're making too many requests
- Wait a few seconds and try again
- Upgrade your OpenAI plan if needed

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
