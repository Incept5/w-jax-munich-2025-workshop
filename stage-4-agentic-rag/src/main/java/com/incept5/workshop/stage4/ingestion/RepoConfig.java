package com.incept5.workshop.stage4.ingestion;

/**
 * Configuration for a single repository to ingest.
 */
public record RepoConfig(
    String name,
    String url,
    String branch,
    String description
) {
}
