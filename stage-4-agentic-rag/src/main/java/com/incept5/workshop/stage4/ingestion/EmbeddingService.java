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
 * Embedding generation service with configurable backend.
 * 
 * Supports two backends:
 * 1. Ollama (default): http://localhost:11434
 * 2. Python service (workaround): http://localhost:8001
 * 
 * Note: Due to a bug in Ollama's embedding generation (as of January 2025),
 * we recommend using the Python service for reliable ingestion.
 * See: embedding-service/README.md for setup instructions.
 * 
 * The nomic-embed-text model produces 768-dimensional embeddings optimized
 * for semantic search and clustering.
 */
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;
    
    /**
     * Create embedding service with Ollama backend (default).
     * 
     * WARNING: Ollama has a known bug affecting embedding generation.
     * Consider using the Python service instead:
     *   new EmbeddingService("http://localhost:8001", "nomic-embed-text")
     */
    public EmbeddingService() {
        this("http://localhost:11434", "nomic-embed-text");
    }
    
    /**
     * Create embedding service with custom backend.
     * 
     * @param baseUrl Backend URL (e.g., "http://localhost:8001" for Python service)
     * @param model Model name (e.g., "nomic-embed-text")
     */
    public EmbeddingService(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
        
        logger.info("📊 EmbeddingService initialized:");
        logger.info("   Backend: {}", baseUrl);
        logger.info("   Model: {}", model);
    }
    
    /**
     * Create embedding service from environment variable.
     * 
     * Checks EMBEDDING_SERVICE_URL environment variable, falls back to Ollama.
     * This allows easy backend switching without code changes.
     */
    public static EmbeddingService fromEnvironment() {
        String url = System.getenv("EMBEDDING_SERVICE_URL");
        if (url != null && !url.isEmpty()) {
            logger.info("🔧 Using embedding service from environment: {}", url);
            EmbeddingService service = new EmbeddingService(url, "nomic-embed-text");
            
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
        logger.warn("⚠️  Using default Ollama backend (has known bug)");
        logger.warn("   Consider setting EMBEDDING_SERVICE_URL=http://localhost:8001");
        return new EmbeddingService();
    }
    
    /**
     * Generate embedding vector for the given text.
     * Uses base64 encoding to safely transport text with special characters,
     * code blocks, and multi-line content without JSON escaping issues.
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
        
        try {
            // BASE64 ENCODE the text to avoid JSON escaping issues with code/special chars
            String encodedText = Base64.getEncoder()
                .encodeToString(trimmedText.getBytes(StandardCharsets.UTF_8));
            
            // Build request body with encoded text
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", encodedText,
                "encoding", "base64"  // Signal to server that content is base64-encoded
            );
            
            String jsonBody = gson.toJson(requestBody);
            
            // Debug logging
            logger.debug("Generating embedding for text (original: {} chars, encoded: {} chars)", 
                trimmedText.length(), encodedText.length());
            logger.trace("Request body: {}", jsonBody);
            
            // Build HTTP request with explicit charset
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/embeddings"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            // Check response status
            if (response.statusCode() != 200) {
                String errorMsg = String.format(
                    "Embedding generation failed: %d - %s%nRequest body (first 200 chars): %s",
                    response.statusCode(),
                    response.body(),
                    jsonBody.substring(0, Math.min(200, jsonBody.length()))
                );
                throw new RuntimeException(errorMsg);
            }
            
            // Parse response
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
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
            
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors as-is
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format(
                "Failed to generate embedding for text (length: %d): %s",
                trimmedText.length(),
                e.getMessage()
            );
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    /**
     * Get the embedding dimension for this model.
     */
    public int getEmbeddingDimension() {
        // nomic-embed-text produces 768-dimensional embeddings
        return 768;
    }
    
    /**
     * Test connection to the embedding service.
     * 
     * @throws RuntimeException if connection fails
     */
    private void testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
            
            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Health check failed with status: " + response.statusCode());
            }
            
            logger.debug("Health check response: {}", response.body());
            
        } catch (Exception e) {
            throw new RuntimeException("Connection test failed: " + e.getMessage(), e);
        }
    }
}
