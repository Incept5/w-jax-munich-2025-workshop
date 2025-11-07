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
 * Python-based embedding provider using local nomic-embed-text model.
 * 
 * Features:
 * - Free and runs locally (no API costs)
 * - 768-dimensional embeddings
 * - Base64 encoding for safe text transport
 * - Requires Python service running on port 8001
 * 
 * Setup: See embedding-service/README.md
 */
public class PythonEmbeddingProvider implements EmbeddingProvider {
    private static final Logger logger = LoggerFactory.getLogger(PythonEmbeddingProvider.class);
    
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;
    
    /**
     * Create Python embedding provider.
     * 
     * @param baseUrl Python service URL (e.g., "http://localhost:8001")
     * @param model Model name (e.g., "nomic-embed-text")
     */
    public PythonEmbeddingProvider(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        // Force HTTP/1.1 to avoid HTTP/2 upgrade issues with FastAPI
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
        
        logger.info("🐍 PythonEmbeddingProvider initialized:");
        logger.info("   URL: {}", baseUrl);
        logger.info("   Model: {}", model);
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
            // Use base64 encoding to avoid JSON escaping issues
            String encodedText = Base64.getEncoder()
                .encodeToString(trimmedText.getBytes(StandardCharsets.UTF_8));
            
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", encodedText,
                "encoding", "base64"
            );
            
            String jsonBody = gson.toJson(requestBody);
            
            logger.debug("Generating embedding for text (original: {} chars, encoded: {} chars)", 
                trimmedText.length(), encodedText.length());
            logger.trace("Request body (first 200 chars): {}", 
                jsonBody.substring(0, Math.min(200, jsonBody.length())));
            
            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/embeddings"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
            
            // Send request
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            // Check response status
            if (response.statusCode() != 200) {
                String errorMsg = String.format(
                    "Embedding generation failed: %d - %s",
                    response.statusCode(),
                    response.body()
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
    
    @Override
    public int getEmbeddingDimension() {
        return 768;
    }
    
    @Override
    public String getProviderName() {
        return "Python";
    }
    
    @Override
    public String getModelName() {
        return model;
    }
    
    @Override
    public void testConnection() {
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
