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
        String ollamaBaseUrl
    ) {
        public Settings() {
            this(800, 200, 0.7, "nomic-embed-text", "http://localhost:11434");
        }
    }
}
