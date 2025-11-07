
# Stage 1 - Simple Agent with Tool Calling

A simple but powerful AI agent that can reason about tasks and use real-world API tools to accomplish them. This stage introduces the fundamental agent architecture: **Think → Act → Observe**.

## Overview

This agent demonstrates:
- **Agent Loop**: The classic think-act-observe pattern
- **Tool Abstraction**: Clean interface for external capabilities
- **Real API Integration**: Weather (wttr.in) and country info (REST Countries)
- **Multi-step Reasoning**: Composing tool calls to solve complex queries
- **Simple Protocol**: XML-like format for tool calling

## Architecture

```
┌─────────────┐
│  User Task  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────┐
│      Simple Agent           │
│  ┌──────────────────────┐   │
│  │   Think (LLM)        │   │
│  │   ↓                  │   │
│  │   Act (Tool Call?)   │   │
│  │   ↓                  │   │
│  │   Observe (Result)   │   │
│  └──────────────────────┘   │
└──────┬──────────────────────┘
       │
       ├─► WeatherTool (wttr.in API)
       │
       └─► CountryInfoTool (REST Countries API)
```

## Quick Start

### Prerequisites

1. **Java 21+** installed
2. **Maven 3.9.0+** installed
3. **Ollama** running with a model (e.g., `ollama pull qwen3:4b`)

### Run with Default Task

```bash
./run.sh
```

This will run the default task: *"What's the weather like in the capital of France?"*

### Run with Custom Task

```bash
./run.sh "What's the weather in Tokyo?"
```

### Run with Verbose Output

See each step of the agent loop:

```bash
./run.sh --verbose "Tell me about Brazil"
```

## Example Tasks

### Simple Weather Query
```bash
./run.sh "What's the weather in Berlin?"
```

**Agent behavior:**
1. Think: "I need to check the weather"
2. Act: Call `weather` tool with city="Berlin"
3. Observe: "Berlin: 12°C (feels like 10°C), Cloudy..."
4. Respond: Final answer with weather info

### Multi-step Reasoning
```bash
./run.sh "What's the weather in Japan's capital?"
```

**Agent behavior:**
1. Think: "I need to find Japan's capital first"
2. Act: Call `country_info` tool with country="Japan"
3. Observe: "Japan - Capital: Tokyo, Population: 125,584,838..."
4. Think: "Now I know the capital is Tokyo, check weather"
5. Act: Call `weather` tool with city="Tokyo"
6. Observe: "Tokyo: 18°C (feels like 18°C), Clear..."
7. Respond: Final answer combining both pieces of information

### Comparison Task
```bash
./run.sh "Compare the weather in Paris and London"
```

**Agent behavior:**
1. Act: Call `weather` for Paris
2. Observe: Paris weather data
3. Act: Call `weather` for London
4. Observe: London weather data
5. Respond: Comparison of both cities

## Available Tools

### 1. WeatherTool
- **API**: wttr.in (free, no authentication)
- **Function**: Gets real-time weather for any city
- **Parameters**: `city` (e.g., "Paris", "Tokyo")
- **Returns**: Temperature, conditions, humidity, wind speed

### 2. CountryInfoTool
- **API**: REST Countries (free, no authentication)
- **Function**: Gets comprehensive country information
- **Parameters**: `country` (e.g., "France", "Japan")
- **Returns**: Capital, population, region, languages

## Tool Call Format

The agent teaches the LLM to use tools with a simple XML-like format:

```json
{
        "tool": "country_info",
        "parameters": {
        "country": "France"
        }
}
```

For country info:
```json
{
        "tool": "country_info",
        "parameters": {
        "country": "France"
        }
}
```

## Project Structure

```
stage-2-simple-agent/
├── pom.xml                           # Maven configuration
├── README.md                         # This file
├── run.sh                            # Quick run script
└── src/main/java/com/incept5/workshop/stage2/
    ├── SimpleAgent.java              # Core agent loop (~200 lines)
    ├── SimpleAgentDemo.java          # CLI runner (~150 lines)
    ├── tool/
    │   ├── Tool.java                 # Tool interface
    │   ├── ToolRegistry.java         # Tool management
    │   ├── WeatherTool.java          # Real weather API
    │   └── CountryInfoTool.java      # Real country API
    └── util/
        ├── ToolCallParser.java       # Parse XML tool calls
        └── HttpHelper.java           # HTTP client wrapper
```

## Learning Objectives

By studying this stage, you'll understand:

1. **Agent Loop Pattern**
   - Think: Query the LLM for next action
   - Act: Execute tools when needed
   - Observe: Feed results back to the LLM
   - Iterate until task is complete

2. **Tool Abstraction**
   - Simple `Tool` interface
   - Registry pattern for tool management
   - Loose coupling between agent and tools

3. **Real API Integration**
   - Making HTTP GET requests
   - Parsing JSON responses
   - Error handling for network issues

4. **Multi-step Reasoning**
   - Agent can chain multiple tool calls
   - Context builds up across iterations
   - LLM decides when enough info is gathered

5. **Prompt Engineering**
   - Teaching LLM the tool format
   - System prompt with tool descriptions
   - Guiding LLM behavior through examples

## Command-Line Options

```
Usage: ./run.sh [OPTIONS] [TASK]

Options:
  -v, --verbose    Show detailed step-by-step execution
  -h, --help       Show help message

Examples:
  ./run.sh                                    # Use default task
  ./run.sh "What's the weather in Tokyo?"     # Custom task
  ./run.sh --verbose "Tell me about Brazil"   # Verbose mode
```

## Building

```bash
# Build with Maven
mvn clean package

# Run tests
mvn test

# Run directly with Maven
mvn exec:java -Dexec.args="What's the weather in Paris?"
```

## Configuration

The agent uses the same backend configuration as Stage 0:
- Default backend: Ollama
- Default model: qwen2.5:3b
- Default URL: http://localhost:11434

To use a different backend or model, modify `BackendConfig` in `SimpleAgentDemo.java`.

## Error Handling

The agent gracefully handles:
- **Network failures**: Timeouts, connection refused
- **Invalid cities/countries**: API returns appropriate errors
- **API rate limits**: Though these APIs are very generous
- **Max iterations**: Stops after 10 iterations to prevent infinite loops

## Verbose Mode

Use `--verbose` to see each iteration of the agent loop:

```
============================================================
Iteration 1
============================================================

[THINKING]
LLM Response:
------------------------------------------------------------
I need to find out what the capital of France is first.
<tool_use>
<tool_name>country_info</tool_name>
<country>France</country>
</tool_use>
------------------------------------------------------------

[ACTING]
Tool call: ToolCall{tool='country_info', params={country=France}}

[OBSERVING]
Tool result:
------------------------------------------------------------
France - Capital: Paris, Population: 67,391,582, Region: Europe...
------------------------------------------------------------
```

## Extending with New Tools

Adding a new tool is simple:

1. Implement the `Tool` interface:
```java
public class MyTool implements Tool {
    public String getName() { return "my_tool"; }
    public String getDescription() { return "..."; }
    public String execute(Map<String, String> params) { ... }
}
```

2. Register it in `SimpleAgentDemo`:
```java
toolRegistry.register(new MyTool());
```

3. The agent automatically learns about it through the system prompt!

## Limitations

This is a **simple** agent for educational purposes. It does not include:
- ❌ Advanced error recovery
- ❌ Tool result validation
- ❌ Parallel tool execution
- ❌ Long-term memory
- ❌ Cost tracking
- ❌ Streaming responses

These features will be added in later stages!

## Next Steps

- **Stage 2**: MCP Server - Expose tools via Model Context Protocol
- **Stage 3**: Agent with MCP - Connect to external MCP servers
- **Stage 4**: Agentic RAG - Add retrieval and vector search
- **Stage 5**: Multi-Agent - Multiple specialized agents working together
- **Stage 6**: Enterprise - Production-ready patterns

## Troubleshooting

**Problem**: `Connection refused to localhost:11434`
```bash
# Solution: Start Ollama
ollama serve
```

**Problem**: `Model not found: qwen2.5:3b`
```bash
# Solution: Pull the model
ollama pull qwen2.5:3b
```

**Problem**: API timeouts
```bash
# Solution: Check internet connection or increase timeout
# Edit HttpHelper.java and change Duration.ofSeconds(10) to higher value
```

## Resources

- [wttr.in API](https://github.com/chubin/wttr.in)
- [REST Countries API](https://restcountries.com/)
- [Ollama Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)

---

**Workshop Time**: 09:15-12:30 (3h 15min)  
**Difficulty**: Beginner  
**Key Concepts**: Agent loop, tool calling, multi-step reasoning
