package com.incept5.workshop.stage4.ingestion;

import java.util.List;

/**
 * Configuration for document ingestion.
 */
public record IngestionConfig(
    List<RepoConfig> repositories,
    Settings settings
) {
    public record Settings(
        int chunkSize,
        int chunkOverlap,
        double similarityThreshold,
        String embeddingModel,
        String ollamaBaseUrl,
        EmbeddingConfig embedding
    ) {
        public Settings() {
            this(800, 200, 0.7, "nomic-embed-text", getDefaultOllamaBaseUrl(), null);
        }
        
        /**
         * Get default Ollama base URL from environment or system property
         */
        private static String getDefaultOllamaBaseUrl() {
            String url = System.getProperty("ollama.base.url");
            if (url != null && !url.isBlank()) {
                return url;
            }
            
            url = System.getenv("OLLAMA_BASE_URL");
            if (url != null && !url.isBlank()) {
                return url;
            }
            
            return "http://localhost:11434";
        }
    }
    
    /**
     * Embedding provider configuration.
     */
    public record EmbeddingConfig(
        String provider,
        int dimensions,
        PythonConfig python,
        OpenAIConfig openai
    ) {
        public record PythonConfig(
            String url,
            String model
        ) {}
        
        public record OpenAIConfig(
            String model
        ) {}
    }
}
