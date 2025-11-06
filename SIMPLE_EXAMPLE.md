# Simple Example - Minimal Ollama Client Usage

This document demonstrates the **simplest possible way** to use the OllamaClient from this project in your own code.

## The Code

The complete working example is in `SimpleExample.java`:

```java
package com.example.ollama;

import client.com.incept5.ollama.OllamaClient;
import config.com.incept5.ollama.OllamaConfig;

public class SimpleExample {
   public static void main(String[] args) {
      // Create config - uses qwen3:4b model
      var config = OllamaConfig.withModel("qwen3:4b");

      // Try-with-resources ensures client is closed properly
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

## What This Demonstrates

This minimal example shows:

1. **Type Inference (`var`)** - Java 10+ feature for cleaner code
2. **Try-with-resources** - Automatic resource cleanup (Java 7+)
3. **Records** - `OllamaConfig` is a record (Java 16+)
4. **Immutability** - Configuration and responses are immutable
5. **Fluent API** - Simple factory method for configuration

## How to Run

### Prerequisites

1. **Java 21+** installed
2. **Ollama** running: `ollama serve`
3. **Model pulled**: `ollama pull qwen3:4b`

### Steps

1. **Build the project**:
   ```bash
   mvn clean package
   ```

2. **Run the example**:
   ```bash
   java --enable-preview -cp target/ollama-java-demo.jar com.incept5.ollama.SimpleExample
   ```

### Expected Output

```
Hello! How can I assist you today? 😊
```

## Using Different Models

Simply change the model name in the configuration:

```java
var config = OllamaConfig.withModel("gemma3");      // Use gemma3
var config = OllamaConfig.withModel("llama2");      // Use llama2
var config = OllamaConfig.withModel("mistral");     // Use mistral
```

## Adding Custom Configuration

For more control, use the builder pattern:

```java
var config = OllamaConfig.builder()
    .model("qwen3:4b")
    .baseUrl("http://localhost:11434")
    .requestTimeout(Duration.ofMinutes(2))
    .build();
```

## Error Handling

The example catches all exceptions. For production code, you might want to handle specific exception types:

```java
try (var client = new OllamaClient(config)) {
    var response = client.generate("Hello");
    System.out.println(response.response());
} catch (OllamaException.ModelNotFoundException e) {
    System.err.println("Model not found: " + e.getModelName());
} catch (OllamaException.ConnectionException e) {
    System.err.println("Connection failed: " + e.getMessage());
} catch (Exception e) {
    System.err.println("Unexpected error: " + e.getMessage());
}
```

## Adding Parameters

You can pass system prompts and options:

```java
try (var client = new OllamaClient(config)) {
    // With system prompt
    var response = client.generate(
        "What is Java?",
        "You are a helpful coding assistant",
        null
    );
    System.out.println(response.response());

    // With temperature and context
    var options = Map.of(
        "temperature", 0.7,
        "num_ctx", 4096
    );
    var response2 = client.generate(
        "Write a poem",
        null,
        options
    );
    System.out.println(response2.response());
}
```

## Async Usage

For non-blocking calls:

```java
try (var client = new OllamaClient(config)) {
    CompletableFuture<OllamaResponse> future = client.generateAsync("Hello");

    // Do other work...

    // Wait for result
    var response = future.get();
    System.out.println(response.response());
}
```

## Streaming Responses

For real-time token streaming:

```java
try (var client = new OllamaClient(config)) {
    client.generateStreaming(
        "Tell me a story",
        null,
        null,
        chunk -> System.out.print(chunk)  // Print each token as it arrives
    );
}
```

## Reusing as a Library

To use this code in your own project:

1. **Copy the classes you need**:
   - `com/example/ollama/client/OllamaClient.java`
   - `com/example/ollama/config/OllamaConfig.java`
   - `com/example/ollama/model/OllamaRequest.java`
   - `com/example/ollama/model/OllamaResponse.java`
   - `com/example/ollama/exception/OllamaException.java`

2. **Add dependencies** to your `pom.xml`:
   ```xml
   <dependencies>
       <dependency>
           <groupId>com.google.code.gson</groupId>
           <artifactId>gson</artifactId>
           <version>2.11.0</version>
       </dependency>
       <dependency>
           <groupId>org.slf4j</groupId>
           <artifactId>slf4j-api</artifactId>
           <version>2.0.16</version>
       </dependency>
       <dependency>
           <groupId>ch.qos.logback</groupId>
           <artifactId>logback-classic</artifactId>
           <version>1.5.12</version>
       </dependency>
   </dependencies>
   ```

3. **Set Java version**:
   ```xml
   <properties>
       <maven.compiler.source>21</maven.compiler.source>
       <maven.compiler.target>21</maven.compiler.target>
   </properties>
   ```

## Why This Design?

This client library was designed with modern Java best practices:

- ✅ **Immutable by default** - Using records prevents accidental mutation
- ✅ **Resource management** - AutoCloseable ensures proper cleanup
- ✅ **Type safety** - Sealed exception hierarchy provides compile-time safety
- ✅ **Modern concurrency** - Virtual threads for efficient I/O
- ✅ **Minimal dependencies** - Only Gson for JSON and SLF4J for logging
- ✅ **Clean API** - Fluent builders and sensible defaults

## Next Steps

- See `README.md` for the full CLI application
- See `QUICKSTART.md` for more examples
- See `OPTIONS.md` for all available options
- Check the source code for advanced features like streaming and async
