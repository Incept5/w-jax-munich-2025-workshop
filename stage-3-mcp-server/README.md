# Stage 2: MCP Server

## Overview

This stage introduces the **Model Context Protocol (MCP)**, a standardized protocol for connecting AI applications with external tools and data sources. You'll learn how to:
1. Expose tools through MCP as a server
2. Build an agent that uses MCP tools as a client
3. Connect the two to create a complete AI system

**Workshop Time**: 40 minutes (13:40-14:20)

## What You'll Learn

1. **MCP Protocol Fundamentals**
   - JSON-RPC 2.0 over STDIO
   - MCP initialization handshake
   - Tool discovery and execution
   - MCP message format

2. **Building an MCP Server**
   - Implementing the MCP protocol
   - Exposing tools via standard interface
   - Handling JSON-RPC requests
   - Returning structured responses

3. **Building an MCP Client**
   - Starting MCP server as subprocess
   - Communicating via STDIO
   - Discovering available tools
   - Calling tools via JSON-RPC

4. **Creating an MCP Agent**
   - Using AI backend for reasoning
   - Integrating MCP client for tool access
   - Implementing agent loop with MCP tools
   - Handling multi-step reasoning

5. **Testing with MCP Clients**
   - Using the MCP Inspector
   - Integrating with Claude Desktop
   - Running the agent interactively
   - Understanding client-server communication

## Key Concepts

### Model Context Protocol (MCP)

MCP is an open protocol that standardizes how applications provide context to Large Language Models (LLMs). It enables:

- **Interoperability**: Any MCP client can connect to any MCP server
- **Tool Discovery**: Clients can automatically discover available tools
- **Standardized Communication**: JSON-RPC 2.0 ensures consistent messaging
- **Type Safety**: JSON Schema validates tool parameters

### Architecture

**Server Mode** (for MCP Inspector, Claude Desktop, etc.):
```
┌─────────────────┐         JSON-RPC 2.0          ┌──────────────────┐
│   MCP Client    │◄──────── over STDIO ─────────►│   MCP Server     │
│  (Claude, etc)  │                                │  (Our Server)    │
└─────────────────┘                                └──────────────────┘
                                                            │
                                                            │ exposes
                                                            │
                                                            ▼
                                                   ┌──────────────────┐
                                                   │      Tools       │
                                                   │  - Weather       │
                                                   │  - Country Info  │
                                                   └──────────────────┘
```

**Agent Mode** (complete AI system):
```
┌─────────────────┐
│   MCP Agent     │
│  (AI Backend)   │
└────────┬────────┘
         │ uses
         │
         ▼
┌─────────────────┐         JSON-RPC 2.0          ┌──────────────────┐
│   MCP Client    │◄──────── over STDIO ─────────►│   MCP Server     │
│  (Our Client)   │                                │  (subprocess)    │
└─────────────────┘                                └──────────────────┘
                                                            │
                                                            │ exposes
                                                            │
                                                            ▼
                                                   ┌──────────────────┐
                                                   │      Tools       │
                                                   │  - Weather       │
                                                   │  - Country Info  │
                                                   └──────────────────┘
```

### MCP Messages

**1. Initialize**
```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {
      "name": "example-client",
      "version": "1.0.0"
    }
  }
}
```

**2. List Tools**
```json
{
  "jsonrpc": "2.0",
  "id": "2",
  "method": "tools/list",
  "params": {}
}
```

**3. Call Tool**
```json
{
  "jsonrpc": "2.0",
  "id": "3",
  "method": "tools/call",
  "params": {
    "name": "weather",
    "arguments": {
      "city": "Paris"
    }
  }
}
```

## Project Structure

```
stage-3-mcp-server/
├── src/main/java/com/incept5/workshop/stage3/
│   ├── SimpleMCPServer.java     # MCP server implementation
│   ├── MCPClient.java            # MCP client for connecting to server
│   ├── MCPAgent.java             # AI agent that uses MCP tools
│   ├── MCPDemo.java              # Demo application (server/agent/interactive)
│   └── tool/
│       ├── Tool.java             # Tool interface with JSON schema
│       ├── WeatherTool.java      # Weather tool implementation
│       └── CountryInfoTool.java  # Country info tool
├── pom.xml                       # Maven configuration
├── run.sh                        # Convenient runner script
└── README.md                     # This file
```

## Running the Project

### Prerequisites

**IMPORTANT**: You must build the project before running any commands:

```bash
cd stage-3-mcp-server
mvn clean package
```

This creates the executable JAR file at `target/stage-3-mcp-server.jar`.

**Having issues?** See [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) for common problems and solutions.

### Quick Start

```bash
# After building (mvn clean package), you can run:

# Run in different modes:

# 1. Server mode (for MCP Inspector/Claude Desktop)
./run.sh server
# or: java -jar target/stage-3-mcp-server.jar server

# 2. Agent mode (single task)
./run.sh agent "What's the weather in Tokyo?"
# or: java -jar target/stage-3-mcp-server.jar agent "What's the weather in Tokyo?"

# 3. Interactive mode (chat with the agent)
./run.sh interactive
# or: java -jar target/stage-3-mcp-server.jar interactive
```

### Testing with MCP Inspector

The MCP Inspector is a tool for testing MCP servers.

**First, make sure you've built the project** (see Prerequisites above).

Then run:

```bash
# Install and run the inspector (requires Node.js)
npx @modelcontextprotocol/inspector java -jar target/stage-3-mcp-server.jar server
```

**Troubleshooting**: If you see "Unable to access jarfile", you need to build first with `mvn clean package`. See [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) for details.

This will:
1. Start your MCP server
2. Open a web interface for testing
3. Allow you to call tools interactively

### Running the Agent

The agent mode lets you use AI to reason about and use the MCP tools:

```bash
# Simple query
./run.sh agent "What's the weather in Paris?"

# Complex multi-step query
./run.sh agent "What's the weather in the capital of France?"

# With verbose output to see the reasoning
./run.sh agent "Tell me about Japan" --verbose
```

### Interactive Mode

Chat with the agent in an interactive session:

```bash
./run.sh interactive

# Then type your questions:
You: What's the weather in Munich?
Agent: The weather in Munich is...

You: Tell me about Germany
Agent: Germany is a country in...

You: exit
```

### Integrating with Claude Desktop

Add your server to Claude Desktop's configuration:

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "workshop-tools": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/stage-3-mcp-server.jar",
        "server"
      ]
    }
  }
}
```

Restart Claude Desktop, and your tools will be available!

## Implementation Details

### SimpleMCPServer

The server implements the core MCP protocol:

```java
public class SimpleMCPServer {
    private final Map<String, Tool> tools;
    
    // Registers tools
    public SimpleMCPServer() {
        registerTool(new WeatherTool());
        registerTool(new CountryInfoTool());
    }
    
    // Handles JSON-RPC requests
    private void handleRequest(String requestJson) {
        // Parse JSON-RPC request
        // Route to appropriate handler
        // Return JSON-RPC response
    }
    
    // Protocol methods
    private JsonObject handleInitialize(Object id, JsonObject params);
    private JsonObject handleToolsList(Object id);
    private JsonObject handleToolCall(Object id, JsonObject params);
}
```

### MCPClient

The client connects to an MCP server and provides a Java API:

```java
public class MCPClient implements AutoCloseable {
    // Start server as subprocess and connect via STDIO
    public MCPClient(String serverCommand, String... serverArgs) 
            throws IOException;
    
    // Discover available tools
    public List<MCPTool> getAvailableTools();
    
    // Call a tool
    public String callTool(String toolName, Map<String, String> arguments) 
            throws IOException;
    
    // Get formatted tool descriptions for LLM
    public String getToolDescriptions();
}
```

### MCPAgent

The agent uses AI to reason about when and how to use tools:

```java
public class MCPAgent implements AutoCloseable {
    private final AIBackend backend;
    private final MCPClient mcpClient;
    
    // Run agent on a task
    public AgentResult run(String task, boolean verbose) 
            throws AIBackendException, IOException {
        // THINK: Ask LLM what to do
        // ACT: Execute tool via MCP if requested
        // OBSERVE: Add result to context
        // Repeat until complete
    }
}
```

The agent implements the classic agent loop, but with tools accessed via MCP instead of directly.

### Tool Interface

Tools now include a JSON schema for parameter validation:

```java
public interface Tool {
    String getName();
    String getDescription();
    String getParameterSchema();  // New: JSON Schema for parameters
    String execute(Map<String, String> parameters) throws ToolExecutionException;
}
```

Example schema:

```json
{
  "type": "object",
  "properties": {
    "city": {
      "type": "string",
      "description": "Name of the city"
    }
  },
  "required": ["city"]
}
```

## Exercises

### Exercise 1: Test the Server (5 minutes)

1. Build and run the server:
   ```bash
   ./run.sh server
   ```

2. In another terminal, use the MCP Inspector:
   ```bash
   npx @modelcontextprotocol/inspector java -jar target/stage-3-mcp-server.jar server
   ```

3. Test the tools through the web interface

### Exercise 2: Test the Agent (10 minutes)

1. Run the agent with a simple query:
   ```bash
   ./run.sh agent "What's the weather in Paris?"
   ```

2. Try a multi-step query:
   ```bash
   ./run.sh agent "What's the weather in the capital of France?" --verbose
   ```

3. Try interactive mode:
   ```bash
   ./run.sh interactive
   ```

Observe how the agent:
- Discovers it needs to find the capital first
- Uses country_info tool to get the capital
- Then uses weather tool for that city
- Provides a final answer

### Exercise 3: Add a New Tool (15 minutes)

Create a calculator tool that performs basic math operations:

```java
public class CalculatorTool implements Tool {
    @Override
    public String getName() {
        return "calculator";
    }
    
    @Override
    public String getDescription() {
        return "Performs basic math operations (add, subtract, multiply, divide)";
    }
    
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "operation": {
                  "type": "string",
                  "enum": ["add", "subtract", "multiply", "divide"],
                  "description": "The operation to perform"
                },
                "a": {
                  "type": "number",
                  "description": "First number"
                },
                "b": {
                  "type": "number",
                  "description": "Second number"
                }
              },
              "required": ["operation", "a", "b"]
            }
            """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) 
            throws ToolExecutionException {
        // Implement the calculator logic
        // Parse numbers and perform operation
        // Return result
    }
}
```

**Tasks**:
1. Implement the calculator tool
2. Register it in SimpleMCPServer
3. Test it with the MCP Inspector
4. Test it with the agent:
   ```bash
   ./run.sh agent "What is 15 times 23?"
   ```

## Extension Ideas: Additional Tools

Once you've mastered the basics, try adding these tools to extend your MCP server's capabilities. Each tool demonstrates different aspects of MCP design:

### 1. Time Zone Tool (Simple, No External API)

**Difficulty**: ⭐ Easy  
**Time**: ~10 minutes  
**What You'll Learn**: Working with Java time APIs, returning formatted data

```java
public class TimeZoneTool implements Tool {
    @Override
    public String getName() {
        return "get_time";
    }
    
    @Override
    public String getDescription() {
        return "Get the current time in a specific timezone (e.g., 'America/New_York', 'Europe/London', 'Asia/Tokyo')";
    }
    
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "timezone": {
                  "type": "string",
                  "description": "Timezone name (e.g., 'America/New_York', 'Europe/London')"
                }
              },
              "required": ["timezone"]
            }
            """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) 
            throws ToolExecutionException {
        String timezone = parameters.get("timezone");
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
            return "Current time in " + timezone + ": " + now.format(formatter);
        } catch (Exception e) {
            throw new ToolExecutionException(
                "Invalid timezone: " + timezone + ". Use format like 'America/New_York'");
        }
    }
}
```

**Test Queries**:
- "What time is it in Tokyo?"
- "What's the current time in New York?"
- "Tell me the time in London and Paris"

### 2. Currency Converter Tool (Medium, Free API)

**Difficulty**: ⭐⭐ Medium  
**Time**: ~15 minutes  
**What You'll Learn**: Working with external APIs, handling numeric parameters, error handling  
**API**: [Exchange Rate API](https://www.exchangerate-api.com/) (free, no auth needed)

```java
public class CurrencyConverterTool implements Tool {
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";
    
    @Override
    public String getName() {
        return "convert_currency";
    }
    
    @Override
    public String getDescription() {
        return "Convert amount between currencies using current exchange rates (e.g., USD to EUR)";
    }
    
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "amount": {
                  "type": "number",
                  "description": "Amount to convert"
                },
                "from_currency": {
                  "type": "string",
                  "description": "Source currency code (e.g., USD, EUR, GBP)"
                },
                "to_currency": {
                  "type": "string",
                  "description": "Target currency code (e.g., USD, EUR, GBP)"
                }
              },
              "required": ["amount", "from_currency", "to_currency"]
            }
            """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) 
            throws ToolExecutionException {
        // Parse parameters
        double amount = Double.parseDouble(parameters.get("amount"));
        String fromCurrency = parameters.get("from_currency").toUpperCase();
        String toCurrency = parameters.get("to_currency").toUpperCase();
        
        // Call API and convert
        // See WeatherTool for HTTP client example
        // Return formatted result
    }
}
```

**Test Queries**:
- "Convert 100 USD to EUR"
- "How much is 50 euros in Japanese yen?"
- "What's 1000 GBP in dollars?"

### 3. Random Joke Tool (Easy, Free API)

**Difficulty**: ⭐ Easy  
**Time**: ~10 minutes  
**What You'll Learn**: Optional parameters, enum types, simple API calls  
**API**: [JokeAPI](https://v2.jokeapi.dev/) (free, no auth needed)

```java
public class JokeTool implements Tool {
    private static final String API_URL = "https://v2.jokeapi.dev/joke/";
    
    @Override
    public String getName() {
        return "get_joke";
    }
    
    @Override
    public String getDescription() {
        return "Get a random joke from a specific category (Programming, Misc, Dark, Pun, Spooky, Christmas)";
    }
    
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "category": {
                  "type": "string",
                  "enum": ["Programming", "Misc", "Dark", "Pun", "Spooky", "Christmas", "Any"],
                  "description": "Joke category",
                  "default": "Any"
                }
              }
            }
            """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) 
            throws ToolExecutionException {
        String category = parameters.getOrDefault("category", "Any");
        
        // Call https://v2.jokeapi.dev/joke/{category}
        // Parse JSON response
        // Handle both single and two-part jokes
        // Return formatted joke
    }
}
```

**Test Queries**:
- "Tell me a programming joke"
- "I need a joke to lighten the mood"
- "Give me a pun"

### 4. Unit Converter Tool (Medium)

**Difficulty**: ⭐⭐ Medium  
**Time**: ~20 minutes  
**What You'll Learn**: Complex parameter validation, multiple conversion types

```java
public class UnitConverterTool implements Tool {
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "value": {
                  "type": "number",
                  "description": "Value to convert"
                },
                "from_unit": {
                  "type": "string",
                  "enum": ["celsius", "fahrenheit", "kelvin", 
                           "meters", "feet", "miles", "kilometers",
                           "kilograms", "pounds"],
                  "description": "Source unit"
                },
                "to_unit": {
                  "type": "string",
                  "enum": ["celsius", "fahrenheit", "kelvin",
                           "meters", "feet", "miles", "kilometers",
                           "kilograms", "pounds"],
                  "description": "Target unit"
                }
              },
              "required": ["value", "from_unit", "to_unit"]
            }
            """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) 
            throws ToolExecutionException {
        // Implement conversion logic for:
        // - Temperature: C, F, K
        // - Distance: m, ft, mi, km
        // - Weight: kg, lb
        // Validate unit compatibility
    }
}
```

**Test Queries**:
- "Convert 100 fahrenheit to celsius"
- "How many kilometers is 50 miles?"
- "Convert 70 kilograms to pounds"

### 5. Quote of the Day Tool (Easy, Free API)

**Difficulty**: ⭐ Easy  
**Time**: ~10 minutes  
**What You'll Learn**: Tools with optional parameters, handling API responses  
**API**: [Quotable API](https://api.quotable.io/) (free, no auth needed)

```java
public class QuoteTool implements Tool {
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "author": {
                  "type": "string",
                  "description": "Author name (optional, e.g., 'Einstein', 'Shakespeare')"
                },
                "tag": {
                  "type": "string",
                  "description": "Quote category (optional, e.g., 'wisdom', 'technology', 'life')"
                }
              }
            }
            """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) 
            throws ToolExecutionException {
        // Call https://api.quotable.io/random
        // Optional: filter by author or tag
        // Return quote with author
    }
}
```

**Test Queries**:
- "Give me an inspirational quote"
- "Share a quote from Einstein"
- "Tell me a quote about technology"

### 6. UUID Generator Tool (Very Easy)

**Difficulty**: ⭐ Very Easy  
**Time**: ~5 minutes  
**What You'll Learn**: Tools with no required parameters

```java
public class UuidGeneratorTool implements Tool {
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "count": {
                  "type": "integer",
                  "description": "Number of UUIDs to generate (default: 1)",
                  "default": 1,
                  "minimum": 1,
                  "maximum": 10
                }
              }
            }
            """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) {
        int count = Integer.parseInt(parameters.getOrDefault("count", "1"));
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(UUID.randomUUID().toString());
            if (i < count - 1) result.append("\n");
        }
        return result.toString();
    }
}
```

### 7. IP Address Info Tool (Medium, Free API)

**Difficulty**: ⭐⭐ Medium  
**Time**: ~15 minutes  
**What You'll Learn**: Handling optional vs required parameters  
**API**: [ipapi.co](https://ipapi.co/) (free, no auth, 1000 requests/day)

```java
public class IpInfoTool implements Tool {
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "ip_address": {
                  "type": "string",
                  "description": "IP address to look up (optional, defaults to requester's IP)"
                }
              }
            }
            """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) 
            throws ToolExecutionException {
        // Call https://ipapi.co/{ip}/json/ or https://ipapi.co/json/ for own IP
        // Return location, ISP, timezone info
    }
}
```

## Step-by-Step: Adding Your Tool

### Step 1: Create Tool Class

Create a new file in `src/main/java/com/incept5/workshop/stage3/tool/`:

```java
package com.incept5.workshop.stage3.tool;

import java.util.Map;

public class YourTool implements Tool {
    @Override
    public String getName() {
        return "your_tool_name";  // Use snake_case
    }
    
    @Override
    public String getDescription() {
        return "Clear description of what your tool does";
    }
    
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "param_name": {
                  "type": "string",
                  "description": "Parameter description"
                }
              },
              "required": ["param_name"]
            }
            """;
    }
    
    @Override
    public String execute(Map<String, String> parameters) 
            throws ToolExecutionException {
        // Your implementation here
        return "Tool result";
    }
}
```

### Step 2: Register Tool

In `SimpleMCPServer.java`, add your tool to the constructor:

```java
public SimpleMCPServer() {
    // Existing tools
    registerTool(new WeatherTool());
    registerTool(new CountryInfoTool());
    
    // Your new tool
    registerTool(new YourTool());
}
```

### Step 3: Build

```bash
mvn clean package
```

### Step 4: Test with MCP Inspector

```bash
npx @modelcontextprotocol/inspector java -jar target/stage-3-mcp-server.jar server
```

Verify:
1. Your tool appears in the tools list
2. The parameter schema looks correct
3. You can call the tool successfully
4. Error handling works as expected

### Step 5: Test with Agent

```bash
./run.sh agent "Use my new tool to..."
```

## JSON Schema Quick Reference

### Basic Types

```json
{
  "properties": {
    "string_param": { "type": "string" },
    "number_param": { "type": "number" },
    "integer_param": { "type": "integer" },
    "boolean_param": { "type": "boolean" }
  }
}
```

### With Constraints

```json
{
  "properties": {
    "age": {
      "type": "integer",
      "minimum": 0,
      "maximum": 120
    },
    "email": {
      "type": "string",
      "format": "email"
    },
    "category": {
      "type": "string",
      "enum": ["A", "B", "C"]
    }
  }
}
```

### Optional vs Required

```json
{
  "properties": {
    "required_param": { "type": "string" },
    "optional_param": {
      "type": "string",
      "default": "default_value"
    }
  },
  "required": ["required_param"]
}
```

## Tips for Tool Development

### 1. Start Simple
- Begin with tools that need no external APIs
- Add complexity gradually
- Test each component before combining

### 2. Good Parameter Names
- Use snake_case for consistency
- Be descriptive: `target_city` not `city`
- Provide clear descriptions in schema

### 3. Error Handling
- Validate parameters early
- Throw `ToolExecutionException` with clear messages
- Handle API failures gracefully
- Log errors to stderr (not stdout!)

### 4. Testing Strategy
- Test with MCP Inspector first (isolated testing)
- Then test with agent (integration testing)
- Try edge cases (empty strings, invalid formats)
- Test error conditions

### 5. API Integration
- Use free APIs with no auth when possible
- Handle rate limits gracefully
- Cache responses when appropriate
- Set reasonable timeouts

### 6. Response Format
- Return human-readable strings
- Include units in numeric results
- Format dates/times clearly
- Be consistent across tools

## Challenge: Build a Tool Chain

Once you have multiple tools, try creating queries that require tool chaining:

```bash
# Example: Currency + Weather
./run.sh agent "What's the weather in Paris and how much is 50 EUR in USD?"

# Example: Time + Weather
./run.sh agent "What time is it in Tokyo and what's the weather there?"

# Example: Country + Currency + Weather
./run.sh agent "Tell me about Japan: what's the weather in its capital, \
                 what time is it there, and how much is 10000 JPY in USD?"
```

Observe how the agent:
1. Plans which tools to use
2. Determines the correct order
3. Passes results between tools
4. Synthesizes a final answer

## Helpful Resources

### Free APIs (No Auth Required)
- [JokeAPI](https://v2.jokeapi.dev/) - Random jokes
- [Quotable](https://api.quotable.io/) - Random quotes
- [Exchange Rate API](https://www.exchangerate-api.com/) - Currency conversion
- [ipapi.co](https://ipapi.co/) - IP geolocation
- [Open-Meteo](https://open-meteo.com/) - Weather data
- [REST Countries](https://restcountries.com/) - Country information

### JSON Schema Validators
- [JSON Schema Validator](https://www.jsonschemavalidator.net/)
- [JSON Schema Lint](https://jsonschemalint.com/)

### HTTP Clients in Java
- Java 11+ HttpClient (see `WeatherTool` for example)
- Handle timeouts and retries
- Parse JSON responses with Gson

---

### Exercise 4: Integrate with Claude Desktop (5 minutes)

1. Configure Claude Desktop with your server
2. Restart Claude
3. Ask Claude to:
   - Get weather for your city
   - Get information about your country
   - Use your calculator tool (if added)

### Exercise 5: Understanding the Architecture (5 minutes)

Read through the code and answer:

1. **SimpleMCPServer.java**: How does the server handle JSON-RPC requests?
2. **MCPClient.java**: How does the client start the server and communicate with it?
3. **MCPAgent.java**: How does the agent decide when to use tools?
4. How does the agent loop work in MCP context?

## Comparison with Stage 1

| Aspect | Stage 1 | Stage 2 (Server) | Stage 2 (Agent) |
|--------|---------|------------------|-----------------|
| **Protocol** | Custom JSON format | Standard JSON-RPC 2.0 | Uses MCP client |
| **Transport** | Direct method calls | STDIO (stdin/stdout) | STDIO to subprocess |
| **Discovery** | Hardcoded in prompt | Dynamic via tools/list | Dynamic from MCP |
| **Interop** | Agent-specific | Any MCP client | Uses MCP protocol |
| **Validation** | Manual parsing | JSON Schema | JSON Schema |
| **Process** | Single process | Server process | Server as subprocess |

## Key Takeaways

1. **MCP standardizes tool integration** - Any MCP client can use your tools
2. **JSON-RPC 2.0 provides structure** - Clear request/response format
3. **STDIO enables process isolation** - Server runs in separate process
4. **JSON Schema validates inputs** - Type-safe parameter passing
5. **Tool discovery is dynamic** - Clients learn about tools at runtime
6. **Agents can use MCP tools** - Build AI systems that leverage MCP
7. **Client-server architecture** - Clean separation of concerns
8. **Subprocess management** - Server runs as independent process

## Next Steps

In Stage 3, we'll add:
- **RAG (Retrieval Augmented Generation)** - Using PostgreSQL with pgvector
- **Document processing** - Chunking and embedding
- **Vector search** - Similarity-based retrieval

## Resources

- [MCP Specification](https://modelcontextprotocol.io/docs)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)
- [MCP Inspector](https://github.com/modelcontextprotocol/inspector)
- [Claude Desktop](https://claude.ai/download)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- [JSON Schema](https://json-schema.org/)

## Important: STDIO and Logging

### Critical Configuration

When using MCP over STDIO transport, **all logging MUST go to stderr, not stdout**. This is because:

1. **STDOUT is reserved for JSON-RPC messages** - The MCP protocol expects clean JSON on stdout
2. **Any non-JSON output breaks the protocol** - Log messages on stdout cause parse errors
3. **STDERR is for diagnostics** - All logging, errors, and debug output goes here

Our `logback.xml` configuration ensures this:

```xml
<appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
    <target>System.err</target>  <!-- ALL logs go to STDERR -->
    <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

**Common Error**: If you see errors like "Unexpected non-whitespace character after JSON", it means something is writing to stdout. Check:
- System.out.println() calls (remove or redirect to stderr)
- Logger configuration (must target stderr)
- Third-party libraries that may log to stdout

### NDJSON Format

MCP over STDIO uses **Newline Delimited JSON (NDJSON)**:
- Each JSON-RPC message must be on a single line
- Messages are separated by newlines
- No pretty-printing or multi-line formatting

This is why we use `new Gson()` instead of `new GsonBuilder().setPrettyPrinting().create()`.

### JSON-RPC 2.0 ID Handling

JSON-RPC 2.0 requires that the `id` field be present in all responses (both success and error), matching the `id` from the request:

- **Request ID present**: Response includes the same `id` (string or number)
- **Request ID is null**: Response includes `"id": null`
- **Request has no ID (notification)**: No response is sent

Our implementation handles all three cases correctly:

```java
// ID must always be present in response, matching request ID
if (id == null) {
    response.add("id", gson.toJsonTree(null));  // Explicit null
} else if (id instanceof Number) {
    response.add("id", gson.toJsonTree(id));     // Preserve number type
} else {
    response.addProperty("id", id.toString());    // String ID
}
```

This ensures compatibility with strict JSON-RPC clients like the MCP Inspector.

## Troubleshooting

### Server won't start
- Check Java version (requires Java 21+)
- Verify the JAR was built: `mvn clean package`
- Check for port conflicts if using HTTP transport

### JSON parse errors in MCP Inspector
- **"Unexpected token '}'"** - Pretty-printing is enabled (should use compact JSON)
- **"Unexpected non-whitespace character"** - Logging is going to stdout (should go to stderr)
- **"Unexpected end of JSON"** - Incomplete JSON message (check for truncation)
- **"ZodError: Invalid input" with missing 'id' field** - Fixed in v1.0.1: JSON-RPC responses now always include the `id` field, even when null, to comply with the JSON-RPC 2.0 specification

### Tools not appearing in Claude
- Verify the config file path
- Check JSON syntax in claude_desktop_config.json
- Restart Claude Desktop completely
- Check Claude Desktop logs

### Tool execution fails
- Verify parameter names match the schema
- Check for network issues (weather/country APIs)
- Look at server logs (on stderr!) for detailed errors

### MCP Inspector issues
- Ensure Node.js is installed
- Try clearing npm cache: `npm cache clean --force`
- Check that the server JAR path is correct
- Look for error messages in the browser console

## Further Reading

- [Building MCP Servers](https://modelcontextprotocol.io/docs/develop/build-server)
- [MCP Best Practices](https://modelcontextprotocol.io/docs/develop/best-practices)
- [JSON-RPC 2.0](https://www.jsonrpc.org/specification)
