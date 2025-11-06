package com.example.ollama.client;

import com.example.ollama.config.OllamaConfig;
import com.example.ollama.exception.AIBackendException;
import com.example.ollama.model.ModelInfo;
import com.example.ollama.model.OllamaRequest;
import com.example.ollama.model.OllamaResponse;
import com.example.ollama.util.ImageEncoder;
import com.example.ollama.util.ParameterMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Modern Java 25 HTTP client for Ollama API.
 * Uses virtual threads, pattern matching, and other modern Java features.
 */
public class OllamaClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);

    private final OllamaConfig config;
    private final HttpClient httpClient;
    private final Gson gson;
    private final ExecutorService virtualThreadExecutor;

    public OllamaClient(OllamaConfig config) {
        this.config = config;

        // Use virtual threads (Project Loom - Java 21+)
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Modern HttpClient with virtual thread executor
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .executor(virtualThreadExecutor)
                .build();

        this.gson = new GsonBuilder()
                .create();

        logger.info("OllamaClient initialized with config: {}", config);
    }

    /**
     * Generate a response synchronously using pattern matching for error handling
     */
    public OllamaResponse generate(String prompt) throws AIBackendException {
        return generate(prompt, null, null);
    }

    /**
     * Generate a response with optional system prompt and options
     */
    public OllamaResponse generate(String prompt, String systemPrompt, java.util.Map<String, Object> options) throws AIBackendException {
        var requestBuilder = OllamaRequest.builder(config.model(), prompt)
                .systemPrompt(systemPrompt)
                .stream(false);

        if (options != null && !options.isEmpty()) {
            requestBuilder.options(options);

            // Extract and process images from options
            var imagePaths = ParameterMapper.getImages(options);
            if (!imagePaths.isEmpty()) {
                try {
                    var encodedImages = ImageEncoder.processImagePaths(imagePaths);
                    requestBuilder.images(encodedImages);
                } catch (java.io.IOException e) {
                    throw new AIBackendException.ConnectionException(
                        "Failed to process images: " + e.getMessage(),
                        e
                    );
                }
            }
        }

        return sendRequest(requestBuilder.build());
    }

    /**
     * Generate a response asynchronously using CompletableFuture
     */
    public CompletableFuture<OllamaResponse> generateAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generate(prompt);
            } catch (AIBackendException e) {
                throw new CompletionException("Failed to generate response", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Generate a streaming response with callback for each chunk
     * Returns the final response containing timing information
     */
    public OllamaResponse generateStreaming(String prompt, String systemPrompt, java.util.Map<String, Object> options, Consumer<String> chunkConsumer) throws AIBackendException {
        var requestBuilder = OllamaRequest.builder(config.model(), prompt)
                .systemPrompt(systemPrompt)
                .stream(true);

        if (options != null && !options.isEmpty()) {
            requestBuilder.options(options);

            // Extract and process images from options
            var imagePaths = ParameterMapper.getImages(options);
            if (!imagePaths.isEmpty()) {
                try {
                    var encodedImages = ImageEncoder.processImagePaths(imagePaths);
                    requestBuilder.images(encodedImages);
                } catch (java.io.IOException e) {
                    throw new AIBackendException.ConnectionException(
                        "Failed to process images: " + e.getMessage(),
                        e
                    );
                }
            }
        }

        return sendStreamingRequest(requestBuilder.build(), chunkConsumer);
    }

    /**
     * Send a non-streaming request using modern pattern matching
     */
    private OllamaResponse sendRequest(OllamaRequest request) throws AIBackendException {
        try {
            String jsonBody = gson.toJson(request);
            logger.debug("Sending request to Ollama: {}", jsonBody);

            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(config.requestTimeout())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            logger.debug("Received response with status code: {}", response.statusCode());

            // Pattern matching with switch expression (Java 21+)
            return switch (response.statusCode()) {
                case 200 -> parseResponse(response.body());
                case 404 -> throw new AIBackendException.ModelNotFoundException(config.model());
                case int code when code >= 500 ->
                    throw new AIBackendException.ConnectionException(
                        "Server error: " + response.body(),
                        null
                    );
                default -> throw new AIBackendException.InvalidResponseException(
                    "Unexpected response: " + response.body(),
                    response.statusCode()
                );
            };

        } catch (Exception e) {
            // AIBackendException should propagate as-is, don't wrap it
            if (e instanceof AIBackendException ollamaEx) {
                throw ollamaEx;
            }
            logger.error("Error sending request to Ollama", e);
            throw new AIBackendException.ConnectionException(
                "Failed to connect to Ollama: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Send a streaming request and process chunks
     * Returns the final chunk containing timing information
     */
    private OllamaResponse sendStreamingRequest(OllamaRequest request, Consumer<String> chunkConsumer)
            throws AIBackendException {
        try {
            String jsonBody = gson.toJson(request);
            logger.debug("Sending streaming request to Ollama");

            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(config.requestTimeout())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new AIBackendException.InvalidResponseException(
                    "Streaming request failed",
                    response.statusCode()
                );
            }

            // Process streaming response line by line
            OllamaResponse finalChunk = null;
            try (var reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        var chunk = gson.fromJson(line, OllamaResponse.class);
                        if (chunk.response() != null && !chunk.response().isBlank()) {
                            chunkConsumer.accept(chunk.response());
                        }

                        if (chunk.done()) {
                            logger.debug("Streaming completed");
                            finalChunk = chunk;
                            break;
                        }
                    }
                }
            }

            if (finalChunk == null) {
                throw new AIBackendException.InvalidResponseException(
                    "Streaming ended without final chunk",
                    200
                );
            }

            return finalChunk;

        } catch (Exception e) {
            // AIBackendException should propagate as-is, don't wrap it
            if (e instanceof AIBackendException ollamaEx) {
                throw ollamaEx;
            }
            logger.error("Error in streaming request", e);
            throw new AIBackendException.ConnectionException(
                "Streaming request failed: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Parse the JSON response into OllamaResponse record
     */
    private OllamaResponse parseResponse(String jsonResponse) throws AIBackendException {
        try {
            return gson.fromJson(jsonResponse, OllamaResponse.class);
        } catch (Exception e) {
            logger.error("Failed to parse response", e);
            throw new AIBackendException.InvalidResponseException(
                "Invalid JSON response: " + e.getMessage(),
                200
            );
        }
    }

    /**
     * Close resources properly
     */
    @Override
    public void close() {
        logger.info("Closing OllamaClient");
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in time, forcing shutdown");
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor termination");
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the current configuration
     */
    public OllamaConfig getConfig() {
        return config;
    }

    /**
     * Get detailed information about a model
     */
    public ModelInfo getModelInfo(String modelName) throws AIBackendException {
        try {
            logger.debug("Fetching model info for: {}", modelName);

            // Create JSON request body
            var requestBody = new com.google.gson.JsonObject();
            requestBody.addProperty("name", modelName);
            String jsonBody = gson.toJson(requestBody);

            // Build HTTP request to /api/show endpoint
            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getModelInfoUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(config.requestTimeout())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Send request
            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            // Check status
            if (response.statusCode() != 200) {
                logger.warn("Failed to fetch model info: HTTP {}", response.statusCode());
                return null;
            }

            // Parse response
            return gson.fromJson(response.body(), ModelInfo.class);

        } catch (Exception e) {
            logger.warn("Error fetching model info: {}", e.getMessage());
            // Don't throw exception - model info is optional
            return null;
        }
    }
}
