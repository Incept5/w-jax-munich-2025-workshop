# Ollama Java Demo

A modern, feature-rich Java application demonstrating best practices for interacting with AI models. Supports multiple backends including **Ollama**, **LM Studio**, and **MLX-VLM**. This project showcases modern Java features including records, virtual threads, pattern matching, sealed classes, and more.

## Features

### Modern Java Features
- **Records** (Java 16+): Immutable data models with automatic getters, equals, hashCode, and toString
- **Virtual Threads** (Java 21+): Efficient concurrency using Project Loom
- **Pattern Matching** (Java 21+): Enhanced switch expressions for elegant code
- **Text Blocks** (Java 15+): Multi-line string literals for better readability
- **Sealed Classes** (Java 17+): Controlled exception hierarchy
- **var keyword** (Java 10+): Type inference for cleaner code
- **Try-with-resources** (Java 7+): Automatic resource management

### Application Features
- **Multiple Backend Support**: Unified interface for Ollama, LM Studio (OpenAI-compatible), and MLX-VLM
- **🆕 Image Support**: Vision model support across all backends with automatic format conversion
- **Streaming and Non-Streaming Support**: Choose between real-time streaming or batch responses
- **Raw Output Mode**: Clean output for scripting and piping (no headers, timing, or logging)
- **Model Information Display**: Automatic display of model details (family, parameters, quantization, format)
- **Model Parameter Control**: Customize temperature, context size, and system prompts
- **CLI Argument Parsing**: Flexible command-line interface with backend selection
- **Configuration Management**: Builder pattern for easy configuration
- **Comprehensive Logging**: SLF4J with Logback (WARN level by default for clean output)
- **Error Handling**: Type-safe exception handling with sealed classes
- **Async Support**: CompletableFuture for asynchronous operations
- **Performance Metrics**: Backend-compatible timing information with token rates

### Maven Best Practices
- **Version Management**: All dependency versions centralized in properties
- **Executable JAR**: Maven Shade plugin creates uber-JAR with all dependencies
- **Version Enforcement**: Maven Enforcer ensures correct Java and Maven versions
- **Modern Plugin Versions**: Latest stable versions of all Maven plugins
- **Comprehensive Metadata**: Proper project documentation

## Prerequisites

1. **Java 21** or later installed
2. **Maven 3.9.0** or later installed
3. At least one AI backend:
   - **Ollama** (default) running on port 11434 with models pulled
   - **LM Studio** (optional) running on port 1234/v1
   - **MLX-VLM** (optional) running on port 8000 (Apple Silicon only)

## Quick Start

### Setup Ollama

```bash
# Install Ollama (if not already installed)
# Visit: https://ollama.ai

# Pull the gemma3 model
ollama pull gemma3

# Verify Ollama is running
curl http://localhost:11434/api/tags
```

### Build and Run

```bash
# Clone or navigate to the project directory
cd stage-0-demo

# Build the project (creates executable JAR)
mvn clean package

# Run with default settings
java -jar target/ollama-java-demo.jar

# Or use Maven exec plugin
mvn exec:java
```



## Simple Example - Using OllamaClient in Your Code

The simplest way to use the OllamaClient in your own Java application:

```java
import client.com.incept5.ollama.OllamaClient;
import config.com.incept5.ollama.OllamaConfig;

public class SimpleExample {
    public static void main(String[] args) {
        // Create config with your preferred model
        var config = OllamaConfig.withModel("qwen3:4b");

        // Try-with-resources ensures proper cleanup
        try (var client = new OllamaClient(config)) {
            // Generate response and print it
            var response = client.generate("Hello");
            System.out.println(response.response());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

**Run it**:
```bash
java -cp target/ollama-java-demo.jar com.incept5.ollama.SimpleExample
```

This minimal example demonstrates:
- **Type inference** with `var` keyword
- **Try-with-resources** for automatic cleanup
- **Records** for immutable configuration
- **Clean API** with sensible defaults

For more advanced usage (async, streaming, parameters), see [SIMPLE_EXAMPLE.md](../SIMPLE_EXAMPLE.md).

## Backend Support

This application supports multiple AI backends through a unified interface. Switch between backends using the `-b` or `--backend` flag.

### Ollama (Default)
```bash
# Default - Ollama backend
java -jar target/ollama-java-demo.jar

# Explicit Ollama backend
java -jar target/ollama-java-demo.jar -b ollama

# Ollama with custom URL
java -jar target/ollama-java-demo.jar -b ollama -u http://localhost:11434
```

**Features:**
- Full support for model information display
- Streaming and non-streaming modes
- Custom system prompts and parameters
- Native Ollama API format

### LM Studio
```bash
# LM Studio backend with local model
java -jar target/ollama-java-demo.jar -b lmstudio -m "local-model"

# LM Studio with custom URL
java -jar target/ollama-java-demo.jar -b lmstudio -u http://localhost:1234/v1

# LM Studio with streaming
java -jar target/ollama-java-demo.jar -b lmstudio -m "llama-3.2-3b" -s
```

**Features:**
- OpenAI-compatible API format
- Streaming support via Server-Sent Events (SSE)
- Temperature and context size parameters
- Token usage tracking
- Default URL: `http://localhost:1234/v1`

**Note:** Model information display is not available for LM Studio (API limitation).

### MLX-VLM
```bash
# MLX-VLM backend (Apple Silicon optimized)
java -jar target/ollama-java-demo.jar -b mlx_vlm -m "mlx-community/nanoLLaVA-1.5-8bit"

# MLX-VLM with custom URL
java -jar target/ollama-java-demo.jar -b mlx_vlm -u http://localhost:8000

# MLX-VLM with streaming
java -jar target/ollama-java-demo.jar -b mlx_vlm -m "mlx-community/nanoLLaVA-1.5-8bit" -s -p "Describe this"
```

**Features:**
- Apple Silicon optimized with MLX framework
- Vision Language Model support (multimodal)
- Streaming support via Server-Sent Events (SSE)
- Temperature and token limit parameters
- Token usage tracking
- Default URL: `http://localhost:8000`

**Note:** Model information display is not available for MLX-VLM (API limitation). MLX-VLM may be slower for inference compared to other backends.

## Usage Examples

### Basic Usage

```bash
# Default prompt and model (shows model information by default)
java -jar target/ollama-java-demo.jar

# Display help message
java -jar target/ollama-java-demo.jar --help
```

### Custom Prompts

```bash
# Custom prompt
java -jar target/ollama-java-demo.jar -p "Explain quantum computing in simple terms"

# Custom prompt with specific model
java -jar target/ollama-java-demo.jar -m llama2 -p "What is artificial intelligence?"
```

### Streaming Mode

```bash
# Enable streaming for real-time responses
java -jar target/ollama-java-demo.jar -s

# Streaming with custom prompt
java -jar target/ollama-java-demo.jar -s -p "Write a short story about space exploration"
```

### Raw Output Mode

```bash
# Raw mode - only the response (perfect for piping)
java -jar target/ollama-java-demo.jar -r -p "What is 2+2?"

# Raw mode with streaming
java -jar target/ollama-java-demo.jar -r -s -p "Generate code"

# Pipe to other commands
java -jar target/ollama-java-demo.jar -r -p "List programming languages" | grep Java
```

### Model Parameters

```bash
# Set system prompt
java -jar target/ollama-java-demo.jar \
  --system "You are a helpful coding assistant" \
  -p "How do I reverse a string?"

# Adjust temperature for more creative output
java -jar target/ollama-java-demo.jar \
  --temp 0.8 -p "Write a poem"

# Increase context size for longer responses
java -jar target/ollama-java-demo.jar \
  --ctx 8192 -p "Explain quantum computing in detail"

# Combine multiple parameters
java -jar target/ollama-java-demo.jar \
  --system "Be concise" --temp 0.3 --ctx 4096 \
  -p "Explain virtual threads"
```

### Advanced Configuration

```bash
# Custom Ollama server URL
java -jar target/ollama-java-demo.jar -u http://remote-server:11434

# Custom timeout (in seconds)
java -jar target/ollama-java-demo.jar -t 600

# Combine multiple options
java -jar target/ollama-java-demo.jar \
  -m llama2 -p "Explain machine learning" -s -t 300 \
  --temp 0.5 --ctx 4096
```

## Command-Line Options

| Option | Short | Description | Default |
|--------|-------|-------------|---------|
| `--backend` | `-b` | Backend type (ollama, lmstudio, mlx_vlm) | ollama |
| `--model` | `-m` | Model name | gemma3 |
| `--url` | `-u` | Backend server URL | backend-specific |
| `--prompt` | `-p` | Prompt text | "What is the capital of France?" |
| `--timeout` | `-t` | Request timeout (seconds) | 300 |
| `--stream` | `-s` | Enable streaming mode | false |
| `--raw` | `-r` | Raw output mode (response only) | false |
| `--system` | `--sys` | System prompt for the model | - |
| `--temperature` | `--temp` | Temperature (0.0-2.0) | model default |
| `--context` | `--ctx` | Context size in tokens | model default |
| `--images` | `-i` | 🆕 Image file paths or URLs (space-separated) | - |
| `--help` | `-h` | Show help message | - |

**Backend Default URLs:**
- Ollama: `http://localhost:11434`
- LM Studio: `http://localhost:1234/v1`
- MLX-VLM: `http://localhost:8000`

## Raw Output Mode

Use the `-r` or `--raw` flag to get clean output perfect for scripting and piping:

```bash
# Get just the response text
java -jar target/ollama-java-demo.jar -r -p "What is 2+2?"
# Output: 2 + 2 = 4

# Pipe to other commands
java -jar target/ollama-java-demo.jar -r -p "List 3 colors" | grep blue

# Save to file without headers
java -jar target/ollama-java-demo.jar -r -p "Write a poem" > poem.txt

# Raw mode with streaming
java -jar target/ollama-java-demo.jar -r -s -p "Generate code"
```

**Raw mode features:**
- ✅ No headers or decorative output
- ✅ No timing information
- ✅ No logging to console
- ✅ Only the model's response text
- ✅ Works with both streaming and non-streaming

## Model Information

By default, the application displays detailed model information before each generation:

```
Model Information:
------------------------------------------------------------
Family:               gemma3
Parameters:           4.3B
Quantization:         Q4_K_M
Format:               gguf
Architecture:         gemma3
------------------------------------------------------------
```

This information is retrieved from Ollama's `/api/show` endpoint and includes:
- **Family**: Model family/type
- **Parameters**: Number of parameters (e.g., 4.3B, 7B, 13B)
- **Quantization**: Quantization level (e.g., Q4_K_M, Q8_0)
- **Format**: Model format (gguf, safetensors, etc.)
- **Architecture**: Model architecture details

Model information is hidden in raw mode (`-r`).

## Model Parameters

You can customize the model's behavior using the following parameters:

### System Prompt (`--system` or `--sys`)

Sets a system prompt that guides the model's behavior and persona:

```bash
# Make the model more concise
java -jar target/ollama-java-demo.jar \
  --system "You are a helpful assistant. Be concise." \
  -p "What is Java?"

# Set a specific role
java -jar target/ollama-java-demo.jar \
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
java -jar target/ollama-java-demo.jar \
  --temp 0.1 -p "What is 2+2?"

# Creative output
java -jar target/ollama-java-demo.jar \
  --temp 0.8 -p "Write a poem about coding"
```

### Context Size (`--context` or `--ctx`)

Sets the context window size in tokens. A larger context allows the model to handle longer prompts and generate longer responses:

```bash
# Increase context for longer conversations
java -jar target/ollama-java-demo.jar \
  --ctx 8192 -p "Explain quantum computing in detail"

# Standard context
java -jar target/ollama-java-demo.jar \
  --ctx 4096 -p "Explain Java records"
```

### Combining Parameters

You can combine multiple parameters for fine-grained control:

```bash
# All three parameters together
java -jar target/ollama-java-demo.jar \
  --system "You are a helpful coding assistant" \
  --temp 0.3 \
  --ctx 4096 \
  -p "How do I reverse a string in Java?"

# Parameters work in raw mode too
java -jar target/ollama-java-demo.jar \
  -r --temp 0.5 --system "Answer in one sentence" \
  -p "What is AI?"

# Parameters work with streaming
java -jar target/ollama-java-demo.jar \
  -s --temp 0.7 --ctx 8192 \
  -p "Write a short story"
```

When parameters are set, they are displayed in the output (except in raw mode):

```
Model Parameters:
------------------------------------------------------------
System Prompt:        You are a helpful coding assistant
Temperature:          0.3
Context Size:         4096 tokens
------------------------------------------------------------
```

## 🆕 Image Support (Vision Models)

All three backends now support vision models for analyzing images. The application automatically handles the different image formats required by each backend:

- **Ollama**: Images encoded to base64 strings
- **LM Studio**: Images encoded to data URLs (OpenAI format)
- **MLX-VLM**: File paths passed directly (most efficient)

### Ollama with Vision Models

```bash
# Analyze a single image with llava
java -jar target/ollama-java-demo.jar \
  -m llava \
  -i photo.jpg \
  -p "What's in this image?"

# Multiple images
java -jar target/ollama-java-demo.jar \
  -m llava \
  -i image1.jpg image2.png \
  -p "Compare these two images"

# With streaming for real-time response
java -jar target/ollama-java-demo.jar \
  -m llava -s \
  -i landscape.jpg \
  -p "Describe this landscape in detail"

# Combine with other parameters
java -jar target/ollama-java-demo.jar \
  -m llava --temp 0.7 --ctx 8192 \
  -i photo.jpg \
  -p "Write a detailed description"
```

### LM Studio with Vision Models

```bash
# Analyze image with LM Studio (qwen2-vl or similar)
java -jar target/ollama-java-demo.jar \
  -b lmstudio \
  -m "qwen2-vl-2b" \
  -i document.png \
  -p "Extract the text from this document"

# Multiple images with LM Studio
java -jar target/ollama-java-demo.jar \
  -b lmstudio \
  -m "qwen2-vl-2b" \
  -i chart1.png chart2.png \
  -p "Compare these charts and summarize"

# Raw output mode with images
java -jar target/ollama-java-demo.jar \
  -b lmstudio -m "qwen2-vl-2b" -r \
  -i receipt.jpg \
  -p "Extract the total amount"
```

### MLX-VLM with Vision Models

```bash
# Analyze image with MLX-VLM (remember to specify the model!)
java -jar target/ollama-java-demo.jar \
  -b mlx_vlm \
  -m "mlx-community/nanoLLaVA-1.5-8bit" \
  -i screenshot.png \
  -p "What's shown in this screenshot?"

# Multiple images
java -jar target/ollama-java-demo.jar \
  -b mlx_vlm \
  -m "mlx-community/nanoLLaVA-1.5-8bit" \
  -i before.jpg after.jpg \
  -p "What changed between these images?"

# With streaming
java -jar target/ollama-java-demo.jar \
  -b mlx_vlm -s \
  -m "mlx-community/nanoLLaVA-1.5-8bit" \
  -i diagram.png \
  -p "Explain this diagram"
```

### Image Support Features

- ✅ **Local files**: `/path/to/image.jpg`
- ✅ **URLs**: `https://example.com/image.png`
- ✅ **Multiple images**: Space-separated list
- ✅ **Multiple formats**: jpg, jpeg, png, gif, webp
- ✅ **Automatic conversion**: Base64, data URLs, or paths based on backend
- ✅ **Size limit**: 100MB per image
- ✅ **Works with streaming**: Real-time image analysis

### Example Output

```
╔══════════════════════════════════════════════════════════╗
║           Ollama Java Demo Application               ║
╚══════════════════════════════════════════════════════════╝

Model: llava
(Model information not available)

Model Parameters:
------------------------------------------------------------
Images:               2 image(s)
  [1] /path/to/photo1.jpg
  [2] /path/to/photo2.png
------------------------------------------------------------

Prompt: Compare these images
Mode: Standard
============================================================

Generating response...

Response:
------------------------------------------------------------
The first image shows a sunset over mountains...
------------------------------------------------------------
```

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

Timing information is hidden in raw mode (`-r`).

## Project Structure

```
ollama-java-demo/
├── pom.xml                          # Maven configuration
├── README.md                        # This file
├── QUICKSTART.md                    # Quick reference guide
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── ollama/
        │               ├── OllamaDemo.java          # Main application
        │               ├── SimpleExample.java       # Simple usage example
        │               ├── backend/
        │               │   ├── AIBackend.java           # Backend interface
        │               │   ├── AbstractHttpBackend.java # Base class for HTTP backends
        │               │   ├── BackendType.java         # Backend type enum
        │               │   ├── BackendFactory.java      # Factory for backends
        │               │   ├── OllamaBackend.java       # Ollama implementation
        │               │   ├── LMStudioBackend.java     # LM Studio implementation
        │               │   └── MLXVLMBackend.java       # MLX-VLM implementation
        │               ├── client/
        │               │   └── OllamaClient.java    # HTTP client for Ollama API
        │               ├── config/
        │               │   ├── OllamaConfig.java    # Ollama configuration
        │               │   └── BackendConfig.java   # Backend configuration
        │               ├── model/
        │               │   ├── AIResponse.java          # Unified response record
        │               │   ├── ModelInfo.java           # Model information record
        │               │   ├── OllamaRequest.java       # Ollama request record
        │               │   ├── OllamaResponse.java      # Ollama response record
        │               │   ├── LMStudioRequest.java     # LM Studio request record
        │               │   ├── LMStudioResponse.java    # LM Studio response record
        │               │   ├── MLXVLMRequest.java       # MLX-VLM request record
        │               │   └── MLXVLMResponse.java      # MLX-VLM response record
        │               ├── util/
        │               │   ├── ImageEncoder.java        # Image encoding utilities
        │               │   ├── ParameterMapper.java     # Parameter extraction
        │               │   └── SSEStreamProcessor.java  # Server-Sent Events processing
        │               └── exception/
        │                   └── AIBackendException.java  # Sealed exception hierarchy
        └── resources/
            ├── logback.xml              # Logging configuration
            └── logback-raw.xml          # Raw mode logging (OFF)
```

## Architecture Overview

### Backend Abstraction (Strategy Pattern)
The project uses a strategy pattern for backend abstraction:
- `AIBackend`: Interface defining backend contract
- `AbstractHttpBackend`: Base class with common HTTP functionality (reduces ~248 lines of duplication)
- `BackendType`: Enum for available backends (Ollama, LM Studio, MLX-VLM)
- `BackendFactory`: Factory for creating backend instances
- `OllamaBackend`, `LMStudioBackend`, `MLXVLMBackend`: Concrete implementations
- `AIResponse`: Unified response format across all backends

This design allows easy addition of new backends without modifying existing code.

### Image Support Architecture
Multi-modal support with automatic format conversion:
- `ImageEncoder`: Handles image encoding (base64, data URLs) with validation
- `ParameterMapper`: Extracts parameters including images from options map
- Backend-specific handling:
  - **Ollama**: Base64 string array
  - **LM Studio**: Data URLs in OpenAI message format
  - **MLX-VLM**: File paths (most efficient, no encoding)

### Records (Java 16+)
The project uses records for immutable data transfer objects:
- `OllamaRequest`, `LMStudioRequest`: Backend-specific request models
- `OllamaResponse`, `LMStudioResponse`: Backend-specific response models
- `AIResponse`: Unified response model with timing information
- `OllamaConfig`, `BackendConfig`: Configuration models with sensible defaults
- `ModelInfo`: Model metadata with nested `ModelDetails` record

### Virtual Threads (Java 21+)
The `OllamaClient` uses virtual threads for efficient concurrency:
```java
ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
```

### Pattern Matching (Java 21+)
Enhanced switch expressions for HTTP status code handling:
```java
return switch (response.statusCode()) {
    case 200 -> parseResponse(response.body());
    case 404 -> throw new ModelNotFoundException(model);
    case int code when code >= 500 -> throw new ServerException();
    default -> throw new InvalidResponseException();
};
```

### Sealed Classes (Java 17+)
Controlled exception hierarchy for type-safe error handling:
```java
public sealed class AIBackendException extends Exception
        permits ConnectionException, ModelNotFoundException, InvalidResponseException
```
This sealed exception hierarchy provides type-safe error handling across all backends.

## Development

### Build

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package into JAR
mvn package

# Run without packaging
mvn exec:java
```

### Code Quality

The project follows modern Java best practices:
- Immutable data structures using records
- Proper resource management with try-with-resources
- Type-safe error handling with sealed classes
- Comprehensive logging at appropriate levels
- Builder pattern for complex object construction
- Single Responsibility Principle in class design

### Logging

Logs are written to:
- **Console**: Real-time output with timestamps (WARN level by default)
- **File**: `logs/ollama-demo.log` with daily rotation (30-day retention)

Configure logging levels in `src/main/resources/logback.xml`

Default logging level is WARN for clean output. INFO and DEBUG messages are suppressed unless configured otherwise.

## API Integration

### Ollama Backend

Endpoints used:
- **Generate**: `POST /api/generate` - Generate text completions
  - Non-streaming: Returns complete response
  - Streaming: Returns chunks in real-time
- **Show**: `POST /api/show` - Get model information
  - Returns model metadata (family, parameters, quantization, etc.)

### LM Studio Backend

Uses OpenAI-compatible API:
- **Chat Completions**: `POST /v1/chat/completions`
  - Supports streaming via Server-Sent Events (SSE)
  - Messages format with system and user roles
  - Returns token usage statistics

### Ollama Request Format

```json
{
  "model": "gemma3",
  "prompt": "Your prompt here",
  "stream": false,
  "system": "Optional system prompt",
  "options": {
    "temperature": 0.8,
    "num_ctx": 4096
  }
}
```

### Ollama Response Format

```json
{
  "model": "gemma3",
  "created_at": "2024-01-01T00:00:00Z",
  "response": "The generated text...",
  "done": true,
  "total_duration": 5000000000,
  "load_duration": 1000000000,
  "prompt_eval_count": 10,
  "prompt_eval_duration": 2000000000,
  "eval_count": 50,
  "eval_duration": 2000000000
}
```

### LM Studio Request Format

```json
{
  "model": "local-model",
  "messages": [
    {"role": "system", "content": "You are helpful"},
    {"role": "user", "content": "Your prompt here"}
  ],
  "temperature": 0.8,
  "max_tokens": 4096,
  "stream": false
}
```

### LM Studio Response Format

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "model": "local-model",
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "The generated text..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 50,
    "total_tokens": 60
  }
}
```

## Performance Considerations

### Virtual Threads
The application uses virtual threads for I/O operations, allowing thousands of concurrent requests with minimal memory overhead.

### Streaming
Streaming mode provides:
- Lower perceived latency (first tokens arrive faster)
- Better user experience for long responses
- Real-time feedback

### Connection Pooling
The HttpClient maintains a connection pool for efficient resource usage.

## Troubleshooting

### Connection Issues

**Problem**: `Connection refused`
```
Solution: Ensure Ollama is running
$ ollama serve
```

**Problem**: `Model not found`
```
Solution: Pull the model first
$ ollama pull gemma3
```

### Timeout Issues

**Problem**: `Request timeout`
```
Solution: Increase timeout or use streaming
$ java -jar ollama-java-demo.jar -t 600 -s
```

### Memory Issues

**Problem**: `OutOfMemoryError`
```
Solution: Increase JVM heap size
$ java -Xmx2g -jar ollama-java-demo.jar
```

### Java Version

**Problem**: `Unsupported class file major version`
```
Solution: Ensure Java 21 is installed and active
$ java -version
```

## Dependencies

- **Gson 2.11.0**: JSON serialization/deserialization
- **SLF4J 2.0.16**: Logging API
- **Logback 1.5.12**: Logging implementation
- **JUnit 5.11.3**: Testing framework

## License

This is a demonstration project for educational purposes.

## Contributing

Contributions are welcome! Please ensure:
- Code follows Java 21 best practices
- All tests pass
- Logging is appropriate
- Documentation is updated

## Resources

### AI Backends
- [Ollama Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [LM Studio](https://lmstudio.ai/)
- [MLX-VLM on GitHub](https://github.com/Blaizzy/mlx-vlm)

### Java
- [Java Documentation](https://openjdk.org/)
- [Maven Documentation](https://maven.apache.org/)
- [Project Loom (Virtual Threads)](https://openjdk.org/projects/loom/)
- [Java Records](https://openjdk.org/jeps/395)
- [Pattern Matching](https://openjdk.org/jeps/441)

## Acknowledgments

This project demonstrates modern Java development practices and serves as a reference for integrating with AI models through REST APIs.
