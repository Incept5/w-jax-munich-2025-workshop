package com.incept5.workshop.stage4.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Embedding generation service with pluggable providers.
 * 
 * Supports multiple embedding providers:
 * 1. Python service (default, free, local) - nomic-embed-text
 * 2. OpenAI API (paid, simple setup) - text-embedding-3-small/large
 * 
 * Configuration:
 * - Set EMBEDDING_PROVIDER environment variable: "python" or "openai"
 * - Or configure in repos.yaml under settings.embedding.provider
 * 
 * All providers produce 768-dimensional embeddings for database compatibility.
 */
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final EmbeddingProvider provider;
    
    /**
     * Create embedding service with default Python provider.
     * 
     * @deprecated Use fromEnvironment() or fromConfig() instead
     */
    @Deprecated
    public EmbeddingService() {
        this(new PythonEmbeddingProvider("http://localhost:8001", "nomic-embed-text"));
    }
    
    /**
     * Create embedding service with specific provider.
     * 
     * @param provider Embedding provider (Python, OpenAI, etc.)
     */
    public EmbeddingService(EmbeddingProvider provider) {
        this.provider = provider;
        
        logger.info("📊 EmbeddingService initialized:");
        logger.info("   Provider: {}", provider.getProviderName());
        logger.info("   Model: {}", provider.getModelName());
        logger.info("   Dimensions: {}", provider.getEmbeddingDimension());
    }
    
    /**
     * Create embedding service from configuration.
     * 
     * Uses EmbeddingProviderFactory to create appropriate provider.
     * 
     * @param config Ingestion configuration from repos.yaml
     * @return Configured embedding service
     */
    public static EmbeddingService fromConfig(IngestionConfig config) {
        EmbeddingProvider provider = EmbeddingProviderFactory.createFromConfig(config);
        return new EmbeddingService(provider);
    }
    
    /**
     * Create embedding service from environment variable.
     * 
     * @deprecated Use fromConfig() with full configuration instead
     */
    @Deprecated
    public static EmbeddingService fromEnvironment() {
        // Create minimal config for backward compatibility
        IngestionConfig.Settings settings = new IngestionConfig.Settings();
        IngestionConfig config = new IngestionConfig(List.of(), settings);
        return fromConfig(config);
    }
    
    /**
     * Generate embedding vector for the given text.
     * 
     * @param text The text to embed
     * @return 768-dimensional embedding vector
     */
    public float[] generateEmbedding(String text) {
        return provider.generateEmbedding(text);
    }
    
    /**
     * Get the embedding dimension for this service.
     */
    public int getEmbeddingDimension() {
        return provider.getEmbeddingDimension();
    }
    
    /**
     * Get the provider name for debugging.
     */
    public String getProviderName() {
        return provider.getProviderName();
    }
    
    /**
     * Get the model name being used.
     */
    public String getModelName() {
        return provider.getModelName();
    }
}
