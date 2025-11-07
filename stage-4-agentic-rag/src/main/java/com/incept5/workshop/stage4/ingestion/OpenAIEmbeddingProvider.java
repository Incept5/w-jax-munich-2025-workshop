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
 * OpenAI-based embedding provider using text-embedding-3 models.
 * 
 * Features:
 * - High-quality embeddings from OpenAI
 * - 768-dimensional embeddings (configurable via API)
 * - No local setup required
 * - Paid service (requires API key)
 * 
 * Pricing (as of 2024-2025):
 * - text-embedding-3-small: $0.00002 per 1k tokens
 * - text-embedding-3-large: $0.00013 per 1k tokens
 * 
 * Setup: Set OPENAI_API_KEY environment variable
 */
public class OpenAIEmbeddingProvider implements EmbeddingProvider {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIEmbeddingProvider.class);
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/embeddings";
    private static final int EMBEDDING_DIMENSIONS = 768;
    
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;
    
    /**
     * Create OpenAI embedding provider.
     * 
     * @param apiKey OpenAI API key (required)
     * @param model Model name (e.g., "text-embedding-3-small")
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public OpenAIEmbeddingProvider(String apiKey, String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "OpenAI API key is required. Set OPENAI_API_KEY environment variable.");
        }
        
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
        
        logger.info("🤖 OpenAIEmbeddingProvider initialized:");
        logger.info("   Model: {}", model);
        logger.info("   Dimensions: {}", EMBEDDING_DIMENSIONS);
        logger.info("   API Key: {}...{}", 
            apiKey.substring(0, Math.min(10, apiKey.length())),
            apiKey.length() > 10 ? apiKey.substring(apiKey.length() - 4) : "");
    }
    
    @Override
    public float[] generateEmbedding(String text) {
        // Validate input
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot generate embedding for null or empty text. Text: " + 
                (text == null ? "null" : "'" + text + "'"));
        }
        
        String trimmedText = text.trim();
        
        try {
            // Build OpenAI API request
            // Key point: Set dimensions=768 to match our database schema
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "input", trimmedText,
                "dimensions", EMBEDDING_DIMENSIONS,
                "encoding_format", "float"
            );
            
            String jsonBody = gson.toJson(requestBody);
            
            logger.debug("Generating OpenAI embedding for text ({} chars)", trimmedText.length());
            logger.trace("Request body (first 200 chars): {}", 
                jsonBody.substring(0, Math.min(200, jsonBody.length())));
            
            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            // Check response status
            if (response.statusCode() != 200) {
                String errorMsg = String.format(
                    "OpenAI API request failed: %d - %s",
                    response.statusCode(),
                    response.body()
                );
                throw new RuntimeException(errorMsg);
            }
            
            // Parse response
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            JsonArray dataArray = json.getAsJsonArray("data");
            
            if (dataArray == null || dataArray.size() == 0) {
                throw new RuntimeException(
                    "Empty data array in OpenAI response. Response body: " + response.body());
            }
            
            JsonObject firstEmbedding = dataArray.get(0).getAsJsonObject();
            JsonArray embeddingArray = firstEmbedding.getAsJsonArray("embedding");
            
            if (embeddingArray == null || embeddingArray.size() == 0) {
                throw new RuntimeException(
                    "Empty embedding array in OpenAI response. Response body: " + response.body());
            }
            
            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.get(i).getAsFloat();
            }
            
            // Verify dimensions match expected
            if (embedding.length != EMBEDDING_DIMENSIONS) {
                logger.warn("⚠️  OpenAI returned {}-dimensional embedding, expected {}. " +
                    "This may cause database compatibility issues!", 
                    embedding.length, EMBEDDING_DIMENSIONS);
            }
            
            logger.debug("Generated {}-dimensional embedding successfully", embedding.length);
            return embedding;
            
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors as-is
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format(
                "Failed to generate OpenAI embedding for text (length: %d): %s",
                trimmedText.length(),
                e.getMessage()
            );
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    @Override
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSIONS;
    }
    
    @Override
    public String getProviderName() {
        return "OpenAI";
    }
    
    @Override
    public String getModelName() {
        return model;
    }
    
    @Override
    public void testConnection() {
        try {
            // Test with a simple embedding request
            String testText = "test connection";
            
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "input", testText,
                "dimensions", EMBEDDING_DIMENSIONS,
                "encoding_format", "float"
            );
            
            String jsonBody = gson.toJson(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    "OpenAI API test failed with status: " + response.statusCode() + 
                    " - " + response.body());
            }
            
            logger.debug("OpenAI API connection test successful");
            
        } catch (Exception e) {
            throw new RuntimeException("OpenAI connection test failed: " + e.getMessage(), e);
        }
    }
}
