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
import java.time.Duration;
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
            return new EmbeddingService(url, "nomic-embed-text");
        }
        logger.warn("⚠️  Using default Ollama backend (has known bug)");
        logger.warn("   Consider setting EMBEDDING_SERVICE_URL=http://localhost:8001");
        return new EmbeddingService();
    }
    
    /**
     * Generate embedding vector for the given text.
     * 
     * @param text The text to embed
     * @return 768-dimensional embedding vector
     */
    public float[] generateEmbedding(String text) {
        try {
            // Build request
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", text
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/embeddings"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Embedding generation failed: " + 
                    response.statusCode() + " - " + response.body());
            }
            
            // Parse response
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            JsonArray embeddingArray = json.getAsJsonArray("embedding");
            
            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.get(i).getAsFloat();
            }
            
            logger.debug("Generated {}-dimensional embedding", embedding.length);
            return embedding;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the embedding dimension for this model.
     */
    public int getEmbeddingDimension() {
        // nomic-embed-text produces 768-dimensional embeddings
        return 768;
    }
}
