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
    private final boolean isPythonBackend;
    
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
        // Force HTTP/1.1 to avoid HTTP/2 upgrade issues with FastAPI
        // Java's HttpClient defaults to HTTP/2 which can cause empty body issues
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
     * Option B: Check EMBEDDING_BACKEND env var first
     * Option A (fallback): Detect by port (:8001 = Python)
     * 
     * @return true if Python backend, false if Ollama
     */
    private boolean detectBackend() {
        // Option B: Check EMBEDDING_BACKEND env var first
        String backendType = System.getenv("EMBEDDING_BACKEND");
        if (backendType != null && !backendType.isEmpty()) {
            boolean isPython = "python".equalsIgnoreCase(backendType);
            logger.info("🔧 Backend type from env EMBEDDING_BACKEND={}: {}", 
                backendType, isPython ? "Python" : "Ollama");
            return isPython;
        }
        
        // Option A (fallback): Detect by port
        boolean isPython = baseUrl.contains(":8001");
        logger.info("🔍 Backend type auto-detected from URL {}: {}", 
            baseUrl, isPython ? "Python" : "Ollama");
        return isPython;
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
            Map<String, Object> requestBody;
            String encodedText = null;  // For logging purposes
            
            if (isPythonBackend) {
                // Python backend: use base64 encoding to avoid JSON escaping issues
                encodedText = Base64.getEncoder()
                    .encodeToString(trimmedText.getBytes(StandardCharsets.UTF_8));
                
                requestBody = Map.of(
                    "model", model,
                    "prompt", encodedText,
                    "encoding", "base64"
                );
                logger.debug("Using Python backend with base64 encoding");
            } else {
                // Ollama backend: use plain text
                requestBody = Map.of(
                    "model", model,
                    "prompt", trimmedText
                );
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
                    "Embedding generation failed: %d - %s%nFull request body: %s",
                    response.statusCode(),
                    response.body(),
                    jsonBody
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
