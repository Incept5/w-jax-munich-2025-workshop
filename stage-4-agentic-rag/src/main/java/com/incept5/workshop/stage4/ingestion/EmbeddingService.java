    package com.incept5.workshop.stage4.ingestion;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Embedding generation service using Ollama.
 *
 * Uses Ollama's API at http://localhost:11434 to generate embeddings.
 * Default model: qwen3-embedding:0.6b (1024 dimensions)
 *
 * Supports multiple embedding models with automatic dimension detection.
 */
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;
    private final boolean isPythonBackend;
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 500; // 500ms between requests to avoid overwhelming Ollama

    /**
     * Create embedding service with Ollama backend (default).
     * Uses qwen3-embedding:0.6b which produces 1024-dimensional embeddings.
     */
    public EmbeddingService() {
        this("http://localhost:11434", "qwen3-embedding:0.6b");
    }
    
    /**
     * Create embedding service with custom backend.
     *
     * @param baseUrl Backend URL (e.g., "http://localhost:11434" for Ollama)
     * @param model Model name (e.g., "qwen3-embedding:0.6b")
     */
    public EmbeddingService(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        // Force HTTP/1.1 for better compatibility
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
        this.isPythonBackend = detectBackend();

        logger.info("📊 EmbeddingService initialized:");
        logger.info("   Backend: {} ({})", baseUrl, isPythonBackend ? "Python" : "Ollama");
        logger.info("   Model: {}", model);
    }

    /**
     * Detect backend type based on environment variable or URL.
     *
     * Checks EMBEDDING_BACKEND env var first, falls back to URL detection.
     *
     * @return true if Python backend, false if Ollama
     */
    private boolean detectBackend() {
        // Check EMBEDDING_BACKEND env var first
        String backendType = System.getenv("EMBEDDING_BACKEND");
        if (backendType != null && !backendType.isEmpty()) {
            boolean isPython = "python".equalsIgnoreCase(backendType);
            logger.info("🔧 Backend type from env EMBEDDING_BACKEND={}: {}",
                backendType, isPython ? "Python" : "Ollama");
            return isPython;
        }

        // Fallback: Detect by port
        boolean isPython = baseUrl.contains(":8001");
        logger.info("🔍 Backend type auto-detected from URL {}: {}",
            baseUrl, isPython ? "Python" : "Ollama");
        return isPython;
    }
    
    /**
     * Create embedding service from environment variable.
     *
     * Checks EMBEDDING_SERVICE_URL and EMBEDDING_MODEL environment variables.
     * This allows easy backend switching without code changes.
     */
    public static EmbeddingService fromEnvironment() {
        String url = System.getenv("EMBEDDING_SERVICE_URL");
        String model = System.getenv("EMBEDDING_MODEL");

        if (url != null && !url.isEmpty()) {
            // Use custom model if specified, otherwise default to nomic-embed-text
            String embeddingModel = (model != null && !model.isEmpty()) ? model : "nomic-embed-text";
            logger.info("🔧 Using embedding service from environment: {} (model: {})", url, embeddingModel);
            EmbeddingService service = new EmbeddingService(url, embeddingModel);

            // Test connectivity
            try {
                service.testConnection();
                logger.info("✅ Embedding service connection verified");
            } catch (Exception e) {
                logger.error("❌ Failed to connect to embedding service at {}: {}", url, e.getMessage());
                throw new RuntimeException("Embedding service connection failed", e);
            }

            return service;
        }
        logger.info("Using default Ollama backend");
        return new EmbeddingService();
    }
    
    /**
     * Generate embedding vector for the given text.
     * Uses base64 encoding to safely transport text with special characters,
     * code blocks, and multi-line content without JSON escaping issues.
     *
     * Includes retry logic with exponential backoff to handle Ollama instability.
     *
     * @param text The text to embed
     * @return 768-dimensional embedding vector
     */
    public float[] generateEmbedding(String text) {
        // Validate input
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot generate embedding for null or empty text. Text: " +
                (text == null ? "null" : "'" + text + "'"));
        }

        String trimmedText = text.trim();

        // Retry logic for Ollama instability
        int maxRetries = 5;
        int retryDelayMs = 2000; // Start with 2 seconds

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return generateEmbeddingInternal(trimmedText);
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    // Last attempt failed, throw the error
                    String errorMsg = String.format(
                        "Failed to generate embedding after %d attempts for text (length: %d): %s",
                        maxRetries,
                        trimmedText.length(),
                        e.getMessage()
                    );
                    throw new RuntimeException(errorMsg, e);
                }

                // Log retry attempt
                logger.warn("Embedding generation failed (attempt {}/{}), retrying in {}ms: {}",
                    attempt, maxRetries, retryDelayMs, e.getMessage());

                // Wait before retry with exponential backoff
                try {
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }

        // Should never reach here
        throw new RuntimeException("Unexpected error in retry logic");
    }

    /**
     * Internal method to generate embedding (called by retry logic).
     */
    private float[] generateEmbeddingInternal(String trimmedText) {
        // Rate limiting to avoid overwhelming Ollama
        synchronized (this) {
            long now = System.currentTimeMillis();
            long timeSinceLastRequest = now - lastRequestTime;
            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                try {
                    Thread.sleep(MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestTime = System.currentTimeMillis();
        }

        try {
            Map<String, Object> requestBody;
            String encodedText = null;  // For logging purposes
            String apiEndpoint;

            if (isPythonBackend) {
                // Python backend: use base64 encoding to avoid JSON escaping issues
                encodedText = Base64.getEncoder()
                    .encodeToString(trimmedText.getBytes(StandardCharsets.UTF_8));

                requestBody = Map.of(
                    "model", model,
                    "prompt", encodedText,
                    "encoding", "base64"
                );
                apiEndpoint = baseUrl + "/api/embeddings";
                logger.debug("Using Python backend with base64 encoding");
            } else {
                // Ollama backend: use plain text
                requestBody = Map.of(
                    "model", model,
                    "prompt", trimmedText
                );
                apiEndpoint = baseUrl + "/api/embeddings";
                logger.debug("Using Ollama backend with plain text");
            }

            String jsonBody = gson.toJson(requestBody);
            
            // Debug logging with detailed size information
            if (isPythonBackend && encodedText != null) {
                logger.debug("Generating embedding for text (original: {} chars, encoded: {} chars)", 
                    trimmedText.length(), encodedText.length());
            } else {
                logger.debug("Generating embedding for text ({} chars)", trimmedText.length());
            }
            logger.debug("JSON body size: {} bytes", jsonBody.getBytes(StandardCharsets.UTF_8).length);
            logger.trace("Request body: {}", jsonBody);
            logger.trace("Request body (first 200 chars): {}", 
                jsonBody.substring(0, Math.min(200, jsonBody.length())));
            
            // Build HTTP request with explicit charset and longer timeout
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(120))  // Longer timeout for stability
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            // Check response status
            if (response.statusCode() != 200) {
                String errorMsg = String.format(
                    "Embedding generation failed: %d - %s%nFull request body: %s",
                    response.statusCode(),
                    response.body(),
                    jsonBody
                );
                throw new RuntimeException(errorMsg);
            }
            
            // Parse response
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            // Ollama/Python format: {"embedding": [...]}
            JsonArray embeddingArray = json.getAsJsonArray("embedding");

            if (embeddingArray == null || embeddingArray.size() == 0) {
                throw new RuntimeException(
                    "Empty embedding array in response. Response body: " + response.body());
            }

            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.get(i).getAsFloat();
            }

            logger.debug("Generated {}-dimensional embedding successfully", embedding.length);
            return embedding;

        } catch (Exception e) {
            // Rethrow to be caught by retry logic
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get the embedding dimension for this model.
     * Returns the actual dimension based on the model being used.
     */
    public int getEmbeddingDimension() {
        // Qwen3 embedding models produce 1024-dimensional embeddings
        if (model.contains("qwen3")) {
            return 1024;
        }
        // nomic-embed-text and most others produce 768-dimensional embeddings
        return 768;
    }
    
    /**
     * Test connection to the embedding service.
     * Uses /health for Python backend, /api/tags for Ollama.
     * 
     * @throws RuntimeException if connection fails
     */
    private void testConnection() {
        try {
            // Use different health check endpoints based on backend
            String healthPath = isPythonBackend ? "/health" : "/api/tags";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + healthPath))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
            
            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Health check failed with status: " + response.statusCode());
            }
            
            logger.debug("Health check response from {}: {}", healthPath, response.body());
            
        } catch (Exception e) {
            throw new RuntimeException("Connection test failed: " + e.getMessage(), e);
        }
    }
}
