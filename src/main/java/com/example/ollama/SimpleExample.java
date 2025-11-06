package com.example.ollama;

import com.example.ollama.client.OllamaClient;
import com.example.ollama.config.OllamaConfig;

/**
 * Simplest possible example demonstrating modern Java usage with OllamaClient.
 * This class shows the minimal code needed to call Ollama and display a response.
 *
 * Features demonstrated:
 * - var keyword for type inference (Java 10+)
 * - try-with-resources for automatic cleanup (Java 7+)
 * - Records for configuration (Java 16+)
 *
 * To run this example:
 * 1. Ensure Ollama is running: ollama serve
 * 2. Pull model: ollama pull qwen3:4b
 * 3. Build: mvn clean package
 * 4. Run: java --enable-preview -cp target/ollama-java-demo.jar com.example.ollama.SimpleExample
 */
public class SimpleExample {
    public static void main(String[] args) {
        // Create config - uses qwen3:4b model
        var config = OllamaConfig.withModel("qwen3:4b");

        // Try-with-resources ensures client is closed properly
        try (var client = new OllamaClient(config)) {
            // Generate response and print it
            System.out.println("Sending user input: \"Hello\" to Ollama...");
            var response = client.generate("Hello");
            System.out.println(response.response());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
