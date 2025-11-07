package com.incept5.workshop.stage4.db;

import java.util.Map;

/**
 * Document record representing a chunk of text with its metadata.
 * 
 * This is retrieved from vector similarity searches and contains
 * the document content, source information, and similarity score.
 */
public record Document(
    int id,
    String content,
    String source,
    String fileHash,
    int chunkIndex,
    Map<String, Object> metadata,
    double similarity
) {
    /**
     * Create a document without similarity score (for storage).
     */
    public Document(int id, String content, String source, String fileHash, 
                   int chunkIndex, Map<String, Object> metadata) {
        this(id, content, source, fileHash, chunkIndex, metadata, 0.0);
    }
}
