# Quick Start Guide

## Build

```bash
mvn clean package
```

This creates an executable JAR at `target/ollama-java-demo.jar` (1.3MB with all dependencies).

## Run

### Prerequisites

1. Ollama must be running: `ollama serve`
2. A model must be pulled: `ollama pull gemma3`

### Basic Usage

```bash
# Default run (requires --enable-preview for Java 25 features)
java --enable-preview -jar target/ollama-java-demo.jar

# Show help
java --enable-preview -jar target/ollama-java-demo.jar --help
```

## Common Commands

```bash
# Custom prompt (includes model info by default)
java --enable-preview -jar target/ollama-java-demo.jar -p "Explain quantum computing"

# Streaming mode (real-time token output)
java --enable-preview -jar target/ollama-java-demo.jar -s

# Raw mode - output ONLY the response (no headers, no timing, no logging)
java --enable-preview -jar target/ollama-java-demo.jar -r -p "Hello"

# Raw mode with streaming (perfect for piping)
java --enable-preview -jar target/ollama-java-demo.jar -r -s -p "Generate code"

# Different model
java --enable-preview -jar target/ollama-java-demo.jar -m llama2

# Custom Ollama server
java --enable-preview -jar target/ollama-java-demo.jar -u http://remote-server:11434

# Combine options
java --enable-preview -jar target/ollama-java-demo.jar -m gemma3 -p "Write a haiku" -s
```

## Options

| Flag | Description | Default |
|------|-------------|---------|
| `-m, --model` | Model name | gemma3 |
| `-u, --url` | Ollama server URL | http://localhost:11434 |
| `-p, --prompt` | Prompt text | "What is the capital of France?" |
| `-t, --timeout` | Timeout in seconds | 300 |
| `-s, --stream` | Enable streaming | false |
| `-r, --raw` | Raw output mode (response only) | false |
| `--system, --sys` | System prompt | - |
| `--temperature, --temp` | Temperature (0.0-2.0) | model default |
| `--context, --ctx` | Context size in tokens | model default |
| `-h, --help` | Show help | - |

## Model Parameters

You can customize the model's behavior using the following parameters:

### System Prompt (`--system` or `--sys`)

Sets a system prompt that guides the model's behavior and persona.

```bash
# Make the model more concise
java --enable-preview -jar target/ollama-java-demo.jar \
  --system "You are a helpful assistant. Be concise." \
  -p "What is Java?"

# Set a specific role
java --enable-preview -jar target/ollama-java-demo.jar \
  --sys "You are a helpful coding assistant" \
  -p "How do I reverse a string in Java?"
```

### Temperature (`--temperature` or `--temp`)

Controls randomness in the output (0.0-2.0):
- **Lower values (0.0-0.5)**: More focused and deterministic responses
- **Medium values (0.5-1.0)**: Balanced creativity and coherence
- **Higher values (1.0-2.0)**: More creative and varied responses

```bash
# Focused, deterministic output
java --enable-preview -jar target/ollama-java-demo.jar \
  --temp 0.1 -p "What is 2+2?"

# Creative output
java --enable-preview -jar target/ollama-java-demo.jar \
  --temp 0.8 -p "Write a poem about coding"
```

### Context Size (`--context` or `--ctx`)

Sets the context window size in tokens. A larger context allows the model to handle longer prompts and generate longer responses.

```bash
# Increase context for longer conversations
java --enable-preview -jar target/ollama-java-demo.jar \
  --ctx 8192 -p "Explain quantum computing in detail"

# Standard context
java --enable-preview -jar target/ollama-java-demo.jar \
  --ctx 4096 -p "Explain Java records"
```

### Combining Parameters

You can combine multiple parameters for fine-grained control:

```bash
# All three parameters together
java --enable-preview -jar target/ollama-java-demo.jar \
  --system "You are a helpful coding assistant" \
  --temp 0.3 \
  --ctx 4096 \
  -p "How do I reverse a string in Java?"

# Parameters work in raw mode too
java --enable-preview -jar target/ollama-java-demo.jar \
  -r --temp 0.5 --system "Answer in one sentence" \
  -p "What is AI?"

# Parameters work with streaming
java --enable-preview -jar target/ollama-java-demo.jar \
  -s --temp 0.7 --ctx 8192 \
  -p "Write a short story"
```

**Note**: When parameters are set, they are displayed in the output (except in raw mode):

```
Model Parameters:
------------------------------------------------------------
System Prompt:        You are a helpful coding assistant
Temperature:          0.3
Context Size:         4096 tokens
------------------------------------------------------------
```

## Development

```bash
# Compile only
mvn clean compile

# Run directly with Maven
mvn exec:java

# Run with custom arguments
mvn exec:java -Dexec.args="-p 'Hello' -s"
```

## Java 25 Features Used

✨ **Records** - Immutable data models with automatic methods
✨ **Virtual Threads** - Efficient concurrency with Project Loom
✨ **Pattern Matching** - Enhanced switch expressions
✨ **Text Blocks** - Multi-line strings with better formatting
✨ **Sealed Classes** - Controlled exception hierarchy

## Troubleshooting

**Problem**: `Unable to access jarfile`
**Solution**: Run `mvn clean package` first to build the JAR

**Problem**: `Connection refused`
**Solution**: Ensure Ollama is running with `ollama serve`

**Problem**: `Model not found`
**Solution**: Pull the model first with `ollama pull gemma3`

**Problem**: Preview features warning
**Solution**: Always use `--enable-preview` flag with Java 25

## Project Structure

```
src/main/java/com/example/ollama/
├── OllamaDemo.java           # Main application with CLI
├── client/
│   └── OllamaClient.java     # HTTP client (virtual threads)
├── config/
│   └── OllamaConfig.java     # Configuration (record + builder)
├── model/
│   ├── OllamaRequest.java    # Request DTO (record)
│   └── OllamaResponse.java   # Response DTO (record)
└── exception/
    └── OllamaException.java  # Sealed exception hierarchy
```

## Performance

- **Virtual Threads**: Efficient I/O with minimal memory overhead
- **Streaming**: Lower latency, tokens arrive as generated
- **Connection Pool**: HTTP client reuses connections

## Performance Metrics

Both streaming and non-streaming modes display detailed timing information in Ollama-compatible format:

```
total duration:       1.2573s
load duration:        126.29ms
prompt eval count:    16 token(s)
prompt eval duration: 78.12ms
prompt eval rate:     204.81 tokens/s
eval count:           123 token(s)
eval duration:        1.0224s
eval rate:            120.31 tokens/s
```

Metrics include:
- **Total Duration**: Complete request time (4 decimal places for seconds)
- **Load Duration**: Model loading time (2 decimal places for ms/µs)
- **Prompt Eval**: Token count and rate for processing the prompt
- **Eval**: Token count and rate for generating the response

Precision: 5 significant figures (e.g., 1.2573s, 126.29ms)

## Model Information

**By default**, the demo displays detailed model information:

```
Model Information:
------------------------------------------------------------
Family:               gemma3
Parameters:           4.3B
Quantization:         Q4_K_M
Format:               gguf
Model Size:           2.85 GB
Architecture:         gemma3
------------------------------------------------------------
```

Information displayed:
- **Family**: Model family/type
- **Parameters**: Number of parameters (e.g., 7B, 13B)
- **Quantization**: Quantization level (e.g., Q4_K_M, Q8_0)
- **Format**: Model format (gguf, safetensors, etc.)
- **Model Size**: File size on disk
- **Architecture**: Model architecture details

## Raw Output Mode

Use `-r` or `--raw` flag for clean output (perfect for scripting and piping):

```bash
# Get just the response text
java --enable-preview -jar target/ollama-java-demo.jar -r -p "What is 2+2?"
# Output: 2 + 2 = 4

# Pipe to other commands
java --enable-preview -jar target/ollama-java-demo.jar -r -p "List 3 colors" | grep blue

# Save to file without headers
java --enable-preview -jar target/ollama-java-demo.jar -r -p "Write a poem" > poem.txt
```

**Raw mode features:**
- ✅ No headers or decorative output
- ✅ No timing information
- ✅ No logging to console
- ✅ Only the model's response text
- ✅ Works with both streaming and non-streaming

## Logs

- **Console**: Real-time output with timestamps
- **File**: `logs/ollama-java-demo.log` (30-day rotation)

Configure in `src/main/resources/logback.xml`
