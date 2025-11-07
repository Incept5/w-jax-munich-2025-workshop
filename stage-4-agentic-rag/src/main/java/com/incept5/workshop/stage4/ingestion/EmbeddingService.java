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
 * Embedding generation service using Ollama.
 * 
 * Uses the nomic-embed-text model which produces 768-dimensional embeddings.
 * These embeddings are optimized for semantic search and clustering.
 */
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;
    
    public EmbeddingService(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
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
