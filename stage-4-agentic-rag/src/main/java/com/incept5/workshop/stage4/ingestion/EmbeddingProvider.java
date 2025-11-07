package com.incept5.workshop.stage4.ingestion;

/**
 * Interface for embedding generation providers.
 * 
 * Implementations:
 * - PythonEmbeddingProvider: Uses local Python service with nomic-embed-text
 * - OpenAIEmbeddingProvider: Uses OpenAI's text-embedding-3 models
 * 
 * All providers must produce 768-dimensional embeddings to maintain
 * compatibility with the existing pgvector database schema.
 */
public interface EmbeddingProvider {
    
    /**
     * Generate embedding vector for the given text.
     * 
     * @param text The text to embed (non-null, non-empty)
     * @return 768-dimensional embedding vector
     * @throws IllegalArgumentException if text is null or empty
     * @throws RuntimeException if embedding generation fails
     */
    float[] generateEmbedding(String text);
    
    /**
     * Get the embedding dimension for this provider.
     * Must always return 768 for database compatibility.
     * 
     * @return 768
     */
    int getEmbeddingDimension();
    
    /**
     * Get the provider name for logging/debugging.
     * 
     * @return Provider name (e.g., "Python", "OpenAI")
     */
    String getProviderName();
    
    /**
     * Get the model name being used.
     * 
     * @return Model name (e.g., "nomic-embed-text", "text-embedding-3-small")
     */
    String getModelName();
    
    /**
     * Test connection to the embedding service.
     * 
     * @throws RuntimeException if connection fails
     */
    void testConnection();
}
