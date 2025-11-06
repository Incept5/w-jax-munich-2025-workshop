package com.example.ollama.backend;

import com.example.ollama.exception.AIBackendException;
import com.example.ollama.model.AIResponse;
import com.example.ollama.model.ModelInfo;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Interface defining the contract for AI backend implementations.
 * Supports multiple backend providers (Ollama, LM Studio, MLX-VLM, etc.)
 *
 * Implementations must be thread-safe and support both synchronous and
 * asynchronous operations.
 */
public interface AIBackend extends AutoCloseable {

    /**
     * Generate a response synchronously
     *
     * @param prompt the input prompt
     * @param systemPrompt optional system prompt to guide behavior
     * @param options backend-specific options (temperature, context size, etc.)
     * @return the AI response
     * @throws AIBackendException if generation fails
     */
    AIResponse generate(String prompt, String systemPrompt, Map<String, Object> options)
            throws AIBackendException;

    /**
     * Generate a response with streaming output
     *
     * @param prompt the input prompt
     * @param systemPrompt optional system prompt to guide behavior
     * @param options backend-specific options
     * @param chunkConsumer callback for each response chunk
     * @return the final response with timing information
     * @throws AIBackendException if generation fails
     */
    AIResponse generateStreaming(
            String prompt,
            String systemPrompt,
            Map<String, Object> options,
            Consumer<String> chunkConsumer
    ) throws AIBackendException;

    /**
     * Generate a response asynchronously
     *
     * @param prompt the input prompt
     * @param systemPrompt optional system prompt to guide behavior
     * @param options backend-specific options
     * @return future containing the response
     */
    CompletableFuture<AIResponse> generateAsync(
            String prompt,
            String systemPrompt,
            Map<String, Object> options
    );

    /**
     * Get information about a model (if supported by backend)
     *
     * @param modelName the model identifier
     * @return model information, or null if not available
     * @throws AIBackendException if request fails
     */
    ModelInfo getModelInfo(String modelName) throws AIBackendException;

    /**
     * Get the backend type
     *
     * @return the type of this backend
     */
    BackendType getBackendType();

    /**
     * Check if this backend supports model info queries
     *
     * @return true if getModelInfo() is supported
     */
    default boolean supportsModelInfo() {
        return true;
    }

    /**
     * Get the base URL for this backend
     *
     * @return the base URL
     */
    String getBaseUrl();

    /**
     * Get the model name being used
     *
     * @return the model name
     */
    String getModelName();

    /**
     * Close and cleanup resources
     */
    @Override
    void close();
}
