package com.example.ollama.backend;

import com.example.ollama.client.OllamaClient;
import com.example.ollama.config.OllamaConfig;
import com.example.ollama.exception.AIBackendException;
import com.example.ollama.model.AIResponse;
import com.example.ollama.model.ModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Ollama backend implementation using the existing OllamaClient
 */
public class OllamaBackend implements AIBackend {
    private static final Logger logger = LoggerFactory.getLogger(OllamaBackend.class);

    private final OllamaClient client;
    private final OllamaConfig config;

    public OllamaBackend(OllamaConfig config) {
        this.config = config;
        this.client = new OllamaClient(config);
        logger.info("Initialized Ollama backend with base URL: {}", config.baseUrl());
    }

    @Override
    public AIResponse generate(String prompt, String systemPrompt, Map<String, Object> options)
            throws AIBackendException {
        var ollamaResponse = client.generate(prompt, systemPrompt, options);
        return AIResponse.fromOllamaResponse(ollamaResponse);
    }

    @Override
    public AIResponse generateStreaming(
            String prompt,
            String systemPrompt,
            Map<String, Object> options,
            Consumer<String> chunkConsumer
    ) throws AIBackendException {
        var ollamaResponse = client.generateStreaming(prompt, systemPrompt, options, chunkConsumer);
        return AIResponse.fromOllamaResponse(ollamaResponse);
    }

    @Override
    public CompletableFuture<AIResponse> generateAsync(
            String prompt,
            String systemPrompt,
            Map<String, Object> options
    ) {
        return client.generateAsync(prompt)
                .thenApply(AIResponse::fromOllamaResponse);
    }

    @Override
    public ModelInfo getModelInfo(String modelName) throws AIBackendException {
        return client.getModelInfo(modelName);
    }

    @Override
    public BackendType getBackendType() {
        return BackendType.OLLAMA;
    }

    @Override
    public boolean supportsModelInfo() {
        return true;
    }

    @Override
    public String getBaseUrl() {
        return config.baseUrl();
    }

    @Override
    public String getModelName() {
        return config.model();
    }

    @Override
    public void close() {
        client.close();
    }
}
