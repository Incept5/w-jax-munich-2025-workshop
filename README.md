
# W-JAX Munich 2025 Workshop - AI Agents with Java 21+

A comprehensive hands-on workshop building modern AI agents using Java 21+ and local LLMs. Learn to create intelligent agents that can reason, use tools, and collaborate - all running locally with privacy-first, open-source technologies.

## Workshop Overview

**Duration**: 7 hours (09:00-16:30)  
**Level**: Intermediate Java developers  
**Focus**: Practical, hands-on AI agent development  
**Philosophy**: Privacy-first, open-source, no vendor lock-in

### What You'll Build

By the end of this workshop, you'll have built:

1. ✅ **Simple AI Agent** - Tool-calling agent with real API integrations
2. ✅ **MCP Server** - Model Context Protocol server exposing tools
3. ✅ **RAG Agent** - Retrieval-augmented generation with PostgreSQL vector search
4. 🔍 **Enterprise Multi-Agent** - Explore production Embabel Tripper system

### Key Learning Outcomes

- Understand the agent reasoning loop: **Think → Act → Observe**
- Implement tool-calling with real-world APIs
- Build MCP servers and clients for tool interoperability
- Add retrieval-augmented generation (RAG) with pgvector
- Explore production multi-agent architecture (Embabel Tripper)
- Learn enterprise patterns: Spring Boot, OAuth2, distributed tracing

## Prerequisites

### Required

1. **Java 21+** installed and configured
   ```bash
   java -version  # Should show 21 or higher
   ```

2. **Maven 3.9.0+** installed
   ```bash
   mvn -version
   ```

3. **Ollama** running locally
   ```bash
   # Install Ollama from https://ollama.ai
   ollama serve
   ```

4. **Default Models** downloaded
   ```bash
   ollama pull incept5/Jan-v1-2509:fp16
   ollama pull qwen3:4b
   ```

### Optional (for advanced stages)

- **LM Studio** for OpenAI-compatible inference
- **MLX-VLM** for Apple Silicon vision models
- **Alternative Models**: Qwen 2.5 7B, Mistral 7B, Gemma 2 9B

### For Stage 4 (Enterprise Reference)

- **Docker Desktop** with Model Runner for MCP Gateway
- **API Keys** (for exploration, not required for stages 1-3):
  - OpenAI API key (GPT-4.1 models)
  - Brave Search API key (web search)
  - Google Maps API key (location services)
- See `stage-4-embabel-tripper/API_KEYS.md` for setup details

## Quick Start

### 1. Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd w-jax-munich-2025-workshop

# Build all modules
mvn clean package
```

### 2. Verify Setup (Stage 0)

```bash
cd stage-0-demo
./run.sh "Hello from W-JAX Munich!"
```

Expected output: Response from your local LLM with timing information.

### 3. Run Your First Agent (Stage 1)

```bash
cd stage-1-simple-agent
./run.sh "What's the weather in Munich?"
```

Expected: Agent uses tools to look up weather information and responds.

## Workshop Structure

### Stage 0: Foundation (35 min) ✅

**Time**: 09:15-09:50  
**Module**: `stage-0-demo/`  
**Status**: Complete

Verify your environment and understand the backend abstraction.

```bash
cd stage-0-demo
./run.sh "Test my setup"
```

**What You'll Learn**:
- Backend abstraction (Ollama, LM Studio, MLX-VLM)
- Streaming vs non-streaming responses
- Model parameter control
- Multi-modal support (images)

### Stage 1: Simple Agent (1h 50min) ✅

**Time**: 10:50-12:30  
**Module**: `stage-1-simple-agent/`  
**Status**: Complete

Build your first working AI agent with tool-calling capabilities.

```bash
cd stage-1-simple-agent
./run.sh "What's the weather in the capital of Japan?"
./run.sh --verbose "Tell me about Brazil"
```

**What You'll Build**:
- Complete agent loop (think → act → observe)
- Tool interface and registry
- Real API integrations (weather, country info)
- Multi-step reasoning
- Integration tests

**Key Files**:
- `SimpleAgent.java` - Core agent loop (~200 lines)
- `tool/WeatherTool.java` - Real wttr.in API
- `tool/CountryInfoTool.java` - Real REST Countries API
- `SimpleAgentIntegrationTest.java` - Integration tests

### Stage 2: MCP Server (40 min) ✅

**Time**: 13:40-14:20  
**Module**: `stage-2-mcp-server/`  
**Status**: Complete

Build a Model Context Protocol server that exposes tools.

```bash
cd stage-2-mcp-server
./run.sh server  # Run as MCP server
./run.sh agent "What's the weather in Tokyo?"  # Run as agent
```

**What You'll Build**:
- MCP protocol server (JSON-RPC 2.0)
- MCP client for tool discovery
- Agent integration with MCP
- Three operating modes

**Key Files**:
- `SimpleMCPServer.java` - JSON-RPC 2.0 server
- `MCPClient.java` - Client with subprocess management
- `MCPAgent.java` - Agent using MCP tools

### Stage 3: Agentic RAG (35 min) ✅

**Time**: 14:20-14:55  
**Module**: `stage-3-agentic-rag/`  
**Status**: Complete

Add retrieval-augmented generation with PostgreSQL + pgvector.

```bash
cd stage-3-agentic-rag
docker-compose up -d  # Start PostgreSQL
./ingest.sh           # Ingest documents
./run.sh              # Run RAG agent
```

**What You'll Build**:
- PostgreSQL + pgvector database
- Document chunking and embedding
- RAG-enabled agent
- Context retrieval with similarity search

**Key Files**:
- `PgVectorStore.java` - Vector database integration
- `RAGAgent.java` - Agent with RAG capabilities
- `RAGTool.java` - Document retrieval tool

### Stage 4: Enterprise Multi-Agent (65 min) 🔍

**Time**: 15:15-16:20  
**Module**: `stage-4-embabel-tripper/` (External Reference)  
**Repository**: https://github.com/Incept5/tripper  
**Local Path**: `/Users/adam/dev/opensource/explore-embabel/tripper`  
**Status**: Exploration & Discussion

Explore a production multi-agent travel planning system built with Spring Boot and Embabel framework.

**What You'll Explore**:
- **Production Architecture**: Spring Boot + Embabel agent framework
- **Multiple LLMs**: GPT-4.1 (planner) + GPT-4.1-mini (researcher)
- **MCP at Scale**: 6+ MCP servers (Brave, Wikipedia, Google Maps, Airbnb, Puppeteer, GitHub)
- **Enterprise Security**: Spring Security with OAuth2 (Google)
- **Infrastructure**: Docker Compose, MCP Gateway, Zipkin tracing
- **Real Domain**: Travel planning with structured itineraries

**Learning Goals**:
- Understand production agent architecture with Embabel
- See multi-LLM orchestration patterns
- Learn enterprise security (OAuth2, API key management)
- Explore infrastructure as code
- Understand monitoring and observability
- See MCP gateway pattern for tool management
- Compare action-based vs loop-based agent design

**Setup Requirements**:
- Docker Desktop with Model Runner
- API keys: OpenAI, Brave Search, Google Maps
- See `stage-4-embabel-tripper/README.md` for detailed setup
- See `stage-4-embabel-tripper/API_KEYS.md` for API key instructions

**Exploration Guide**: See `stage-4-embabel-tripper/EXPLORATION.md`

**Note**: This stage focuses on reading and discussing production code rather than building from scratch. The goal is to understand how the patterns from stages 1-3 scale to enterprise applications.

## Project Structure

```
w-jax-munich-2025-workshop/
├── README.md                    # This file
├── AGENDA.md                    # Detailed schedule
├── architecture.md              # System architecture
├── pom.xml                      # Parent POM
│
├── shared/                      # ✅ Shared backend libraries
│   ├── backend/                 # Multi-backend support
│   ├── client/                  # HTTP client
│   ├── config/                  # Configuration
│   ├── model/                   # Request/response records
│   ├── exception/               # Sealed exceptions
│   └── util/                    # Utilities
│
├── stage-0-demo/                # ✅ Foundation demo
├── stage-1-simple-agent/        # ✅ First working agent
├── stage-2-mcp-server/          # ✅ MCP server
├── stage-3-agentic-rag/         # ✅ RAG agent
└── stage-4-embabel-tripper/     # 🔍 Enterprise reference (docs only)
```

## Technologies

### Core Stack

- **Java 21+**: Virtual threads, records, pattern matching, sealed classes
- **Maven 3.9.0+**: Multi-module build
- **Gson 2.11.0**: JSON processing
- **SLF4J + Logback**: Logging
- **JUnit 5.11.3**: Testing

### AI Backends

- **Ollama** (default): Local LLM inference on localhost:11434
- **LM Studio** (optional): OpenAI-compatible API
- **MLX-VLM** (optional): Apple Silicon optimized

### Default Model

- **Primary**: `incept5/Jan-v1-2509:fp16`
- **Alternatives**: Qwen 2.5 7B, Mistral 7B, Gemma 2 9B
- **Easy Switching**: Change via config or CLI

## Modern Java Features

This workshop showcases Java 21+ features:

- ✅ **Records** - Immutable data models
- ✅ **Virtual Threads** - Efficient concurrency (Project Loom)
- ✅ **Pattern Matching** - Enhanced switch expressions
- ✅ **Sealed Classes** - Type-safe exception hierarchy
- ✅ **Text Blocks** - Multi-line strings for prompts
- ✅ **var** - Type inference for cleaner code

## Running the Workshop

### Build Everything

```bash
mvn clean package
```

### Run Specific Stage

```bash
cd stage-1-simple-agent
./run.sh "Your task here"
```

### Run Tests

```bash
# All tests
mvn test

# Specific stage
mvn -pl stage-1-simple-agent test
```

### Skip Tests (if Ollama not available)

```bash
mvn package -DskipTests
```

## Configuration

### Switching Models

Edit `BackendConfig` in any demo/agent class:

```java
BackendConfig config = BackendConfig.builder()
    .backendType(BackendType.OLLAMA)
    .baseUrl("http://localhost:11434")
    .model("incept5/Jan-v1-2509:fp16")  // ← Change here
    .requestTimeout(Duration.ofSeconds(300))
    .build();
```

Or use CLI (Stage 0):

```bash
java -jar target/stage-0-demo.jar -m "qwen2.5:7b" -p "Hello"
```

### Switching Backends

```bash
# Use LM Studio
./run.sh -b lmstudio -m "local-model" -p "Hello"

# Use MLX-VLM (Apple Silicon)
./run.sh -b mlx_vlm -m "mlx-community/nanoLLaVA-1.5-8bit" -p "Hello"
```

## Testing Strategy

Each stage includes a **single comprehensive integration test** that:

- ✅ Tests the complete happy path
- ✅ Uses real Ollama backend (no mocks)
- ✅ Calls real external APIs
- ✅ Verifies end-to-end functionality
- ✅ Includes verbose output for debugging

**Example**: `stage-1-simple-agent/src/test/java/.../SimpleAgentIntegrationTest.java`

### Test Requirements

1. **Ollama running**: `ollama serve`
2. **Model available**: `ollama pull incept5/Jan-v1-2509:fp16`
3. **Network access**: For external API calls

## Troubleshooting

### Connection Issues

**Problem**: `Connection refused to localhost:11434`

```bash
# Solution: Start Ollama
ollama serve
```

**Problem**: `Model not found`

```bash
# Solution: Pull the model
ollama pull incept5/Jan-v1-2509:fp16
```

### Build Issues

**Problem**: `Unsupported class file major version`

```bash
# Solution: Ensure Java 21+
java -version
```

**Problem**: Maven version too old

```bash
# Solution: Update Maven to 3.9.0+
mvn -version
```

### API Timeouts

**Problem**: External API calls timing out

```bash
# Solution: Check internet connection
# Or increase timeout in HttpHelper.java
```

## Architecture Highlights

### Backend Abstraction

Unified interface for multiple AI backends:

```java
AIBackend backend = BackendFactory.createBackend(
    BackendType.OLLAMA,
    "http://localhost:11434",
    "incept5/Jan-v1-2509:fp16",
    Duration.ofSeconds(300)
);

try (backend) {
    AIResponse response = backend.generate("Hello, world!");
    System.out.println(response.response());
}
```

### Tool Interface

Simple, extensible tool abstraction:

```java
public interface Tool {
    String getName();
    String getDescription();
    String execute(Map<String, String> params);
}
```

### Agent Loop

Classic think-act-observe pattern:

```java
while (!completed && iterations < maxIterations) {
    // Think: Ask LLM what to do next
    String llmResponse = backend.generate(prompt);
    
    // Act: Parse and execute tool calls
    if (hasToolCall(llmResponse)) {
        String result = executeTool(toolCall);
        prompt += "\nTool result: " + result;
    } else {
        completed = true;
    }
    
    iterations++;
}
```

## Resources

### Workshop Materials

- **Agenda**: [AGENDA.md](./AGENDA.md) - Detailed schedule
- **Architecture**: [architecture.md](./architecture.md) - System design
- **Stage 1 Guide**: [stage-1-simple-agent/README.md](./stage-1-simple-agent/README.md)
- **Stage 1 Notes**: [stage-1-simple-agent/IMPLEMENTATION_NOTES.md](./stage-1-simple-agent/IMPLEMENTATION_NOTES.md)

### External Resources

- [Java 21 Documentation](https://openjdk.org/projects/jdk/21/)
- [Project Loom (Virtual Threads)](https://openjdk.org/projects/loom/)
- [Ollama](https://ollama.ai/) - Local LLM inference
- [Ollama API Docs](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)

## Workshop Philosophy

### Privacy-First

- ✅ All models run locally
- ✅ No data sent to external servers (except tool APIs)
- ✅ Full control over your data

### Open Source

- ✅ No vendor lock-in
- ✅ Use any compatible model
- ✅ Switch backends freely

### Practical Focus

- ✅ Real code, not slides
- ✅ Working examples
- ✅ Integration tests with real services
- ✅ Production-ready patterns

## What Makes This Workshop Different

1. **Real Integration Tests**: Every stage includes tests with real APIs and real LLMs
2. **Modern Java**: Showcases Java 21+ features in practical AI scenarios
3. **Multiple Backends**: Not locked to one provider or API
4. **Privacy-First**: Everything runs locally by default (stages 1-3)
5. **Progressive Complexity**: Build from scratch (stages 1-3), then explore production code (stage 4)
6. **Production Reference**: Learn from a real-world multi-agent system (Embabel Tripper)
7. **Enterprise Patterns**: See Spring Boot, OAuth2, distributed tracing, and MCP at scale

## Contributing

This is a workshop project. Contributions are welcome after the workshop:

- Bug fixes and improvements
- Additional stages or examples
- Documentation enhancements
- Alternative model configurations

## License

[To be determined - likely MIT or Apache 2.0]

## Acknowledgments

Built for W-JAX Munich 2025 to demonstrate modern Java AI agent development patterns.

---

**Ready to build intelligent agents with Java?**

Start with Stage 0 to verify your setup, then dive into Stage 1 to build your first working agent!

```bash
cd stage-0-demo && ./run.sh "Let's begin!"
```
