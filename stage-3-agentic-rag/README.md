# Stage 3: Agentic RAG - Retrieval-Augmented Generation Agent

## Overview

This stage demonstrates a production-ready RAG (Retrieval-Augmented Generation) system with:

- **PostgreSQL + pgvector** for vector storage
- **Conversational memory** for multi-turn dialogues
- **JSON tool calling** (Ollama native format)
- **Context expansion** via neighboring chunk retrieval
- **Real documentation** from Embabel and Spring AI repositories

## What You've Built

### Phase 1: Ingestion Pipeline ✅ (Complete)
- Document chunking with overlap
- Embedding generation via Ollama
- Vector storage in PostgreSQL
- Idempotent ingestion with hash-based deduplication

### Phase 2: Conversational Agent ✅ (Complete)
- Full agent loop (think → act → observe)
- Multi-turn conversation support
- Vector search with context expansion
- Natural language responses

## Architecture

```
User Query → RAGAgent → ConversationMemory
                ↓
          Agent Loop (Think)
                ↓
          LLM (Ollama)
                ↓
          Tool Call? ──No──→ Final Answer
                ↓
               Yes
                ↓
      search_documentation
                ↓
         Vector Search
                ↓
    PostgreSQL + pgvector
                ↓
      Retrieved Documents
                ↓
    (Optional: Expand Context
     with Neighboring Chunks)
                ↓
         Format Results
                ↓
      Add to Conversation
                ↓
      Loop (Observe)
```

## Components Built

### Core Agent Components

1. **RAGAgent.java** (~300 lines)
   - Conversational agent with memory
   - JSON tool call parsing
   - Configurable iteration limits
   - Verbose mode for debugging

2. **ConversationMemory.java** (~150 lines)
   - Sliding window of recent messages
   - Token estimation and trimming
   - User/Assistant/System message types

3. **JsonToolCallParser.java** (~100 lines)
   - Parse Ollama's JSON tool format
   - Type-safe parameter extraction
   - Graceful error handling

### Tool Implementation

4. **RAGTool.java** (~200 lines)
   - Vector similarity search
   - **Neighboring chunk retrieval** (context expansion)
   - Smart result formatting

5. **ToolRegistry.java** (~80 lines)
   - Tool registration and lookup
   - JSON schema generation
   - Tool execution with error handling

### Database Components (Enhanced)

6. **PgVectorStore.java** (enhanced)
   - Added `getChunkByIndex()` - retrieve specific chunks
   - Added `getNeighboringChunks()` - expand context
   - Full vector search capabilities

7. **Document.java** (enhanced)
   - Added `fileHash` and `chunkIndex` fields
   - Enables neighbor retrieval

### Demo & Testing

8. **RAGAgentDemo.java** (~150 lines)
   - Interactive CLI interface
   - Multi-turn conversation loop
   - Command system (help, history, clear, exit)

9. **RAGAgentIntegrationTest.java** (~250 lines)
   - 7 comprehensive tests
   - Vector search validation
   - Multi-turn conversation testing
   - Context expansion verification

## Prerequisites

Before running:

1. **Ollama running**: `ollama serve`
2. **Models available**:
   ```bash
   ollama pull incept5/Jan-v1-2509:fp16
   ollama pull nomic-embed-text
   ```
3. **PostgreSQL + pgvector**: `docker-compose up -d`
4. **Documents ingested**: `./ingest.sh`

## Quick Start

### 1. Ingest Documentation (if not done)

```bash
cd stage-3-agentic-rag
./ingest.sh
```

This will:
- Download repositories using gitingest
- Chunk documents with overlap
- Generate embeddings
- Store in PostgreSQL

### 2. Run the Agent

```bash
# Standard mode
./run.sh

# Verbose mode (see agent reasoning)
./run.sh --verbose
```

### 3. Example Conversation

```
💬 You: What is Embabel?

🤖 Assistant: 
Embabel is an agent framework for the JVM created by Rod Johnson,
the founder of Spring Framework. It uses Goal-Oriented Action Planning
(GOAP) to build intelligent applications...

💬 You: Show me an example

🤖 Assistant:
Here's a basic Embabel agent:

@Agent(description = "Quiz generator")
public class QuizAgent {
    @Action(description = "Fetch web content")
    public WebContent fetchContent(String url) { ... }
    
    @Goal
    public Quiz createQuizFromUrl(String url) {
        return null;  // Framework fills this in
    }
}
...

💬 You: exit
👋 Goodbye!
```

## Key Features

### 1. Neighboring Chunk Expansion

When searching documentation, the agent can retrieve neighboring chunks:

```java
// In RAGTool.java
if (expandContext) {
    documents = expandWithNeighbors(documents);
}
```

This provides more complete context for:
- Code examples that span multiple chunks
- Explanations with dependencies on previous paragraphs
- Maintaining narrative flow

### 2. JSON Tool Calling

Cleaner than XML (used in Stage 1):

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

### 3. Conversation Memory

The agent maintains context across turns:

```java
memory.addUserMessage("What is Embabel?");
memory.addAssistantMessage("Embabel is...");
memory.addUserMessage("Show me an example");  // Uses previous context
```

### 4. Smart Document Retrieval

Vector search with configurable:
- **topK**: Number of results (1-10)
- **threshold**: Similarity score (0-1)
- **expandContext**: Include neighbors

## CLI Commands

- `help` - Show available commands
- `history` - Display conversation history
- `clear` - Clear conversation history
- `exit` or `quit` - End conversation

## Running Tests

```bash
# Run all tests (requires Ollama + PostgreSQL + ingested data)
mvn test

# Run specific test
mvn test -Dtest=RAGAgentIntegrationTest#testVectorSearch
```

**Note**: Tests require:
- Ollama running with models loaded
- PostgreSQL with ingested documents
- First test run may be slow (model loading)

## Project Structure

```
stage-3-agentic-rag/
├── src/main/java/com/incept5/workshop/stage3/
│   ├── agent/
│   │   ├── ConversationMemory.java      # ✅ Multi-turn context
│   │   ├── RAGAgent.java                # ✅ Main conversational agent
│   │   └── RAGAgentDemo.java            # ✅ Interactive CLI
│   ├── tool/
│   │   ├── Tool.java                    # ✅ Tool interface
│   │   ├── ToolRegistry.java            # ✅ Tool management
│   │   └── RAGTool.java                 # ✅ Document search + expansion
│   ├── util/
│   │   └── JsonToolCallParser.java      # ✅ JSON tool parsing
│   ├── db/
│   │   ├── Document.java                # ✅ Enhanced with chunk info
│   │   └── PgVectorStore.java           # ✅ Enhanced with neighbor retrieval
│   └── ingestion/
│       └── ...                          # ✅ Already complete
├── src/test/java/com/incept5/workshop/stage3/
│   └── RAGAgentIntegrationTest.java     # ✅ Comprehensive tests
├── run.sh                               # ✅ Run script
├── ingest.sh                            # ✅ Ingestion script
├── docker-compose.yml                   # ✅ PostgreSQL + pgvector
└── README.md                            # ✅ This file
```

## What Makes This Different from Stage 1?

| Aspect | Stage 1 | Stage 3 |
|--------|---------|---------|
| **Memory** | None | Full conversation history |
| **Tool Format** | XML | JSON (Ollama native) |
| **Data Source** | External APIs | Vector database |
| **Context** | Single-turn | Multi-turn with memory |
| **Expansion** | N/A | Neighboring chunk retrieval |
| **Scale** | 2 tools | RAG with 1000+ documents |

## Configuration

Edit `RAGAgentDemo.java` to change:

```java
private static final String LLM_MODEL = "incept5/Jan-v1-2509:fp16";
private static final String EMBEDDING_MODEL = "nomic-embed-text";
private static final String DB_URL = "jdbc:postgresql://localhost:5432/workshop_rag";
```

## Troubleshooting

### No documents found
```bash
# Run ingestion
./ingest.sh
```

### Ollama connection refused
```bash
# Start Ollama
ollama serve

# Pull models
ollama pull incept5/Jan-v1-2509:fp16
ollama pull nomic-embed-text
```

### PostgreSQL connection refused
```bash
# Start database
docker-compose up -d

# Check status
docker-compose ps
```

### Slow responses
- First query loads the model (30-60 seconds)
- Subsequent queries are faster
- Use smaller model if needed: `qwen2.5:7b`

## Next Steps

This completes Phase 2 of Stage 3! You now have:

✅ Complete ingestion pipeline  
✅ Conversational RAG agent  
✅ Context expansion with neighbors  
✅ Multi-turn conversations  
✅ Interactive CLI demo  
✅ Integration tests  

**What's Next:**
- Stage 4: Multi-agent systems (orchestration)
- Stage 5: Enterprise patterns (monitoring, resilience)

## Resources

- **Root Architecture**: [/architecture.md](../architecture.md)
- **Stage 1 (Simple Agent)**: [/stage-1-simple-agent/README.md](../stage-1-simple-agent/README.md)
- **Stage 3 Architecture**: [architecture.md](./architecture.md)

### External Resources

- [Spring AI RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [pgvector](https://github.com/pgvector/pgvector)
- [Ollama Embeddings](https://github.com/ollama/ollama/blob/main/docs/api.md#generate-embeddings)

---

*Stage 3 Phase 2 Implementation Complete: 2025-11-06*  
*Total New Code: ~1,200 lines across 9 files*  
*Status: Fully functional RAG conversational agent*
