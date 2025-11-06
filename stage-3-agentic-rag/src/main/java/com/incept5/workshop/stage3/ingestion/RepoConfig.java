package com.incept5.workshop.stage3.ingestion;

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
