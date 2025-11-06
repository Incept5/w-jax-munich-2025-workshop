package com.incept5.workshop.stage3.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Document chunker with overlap for better context preservation.
 * 
 * Strategy:
 * 1. Split on paragraph boundaries first (double newlines)
 * 2. If paragraph is too large, split further
 * 3. Add overlap between chunks for context continuity
 * 
 * Example:
 * Input: "Para 1...\n\nPara 2...\n\nPara 3..."
 * Output: [
 *   "Para 1...\n\nPara 2...",
 *   "Para 2...\n\nPara 3...",  // Overlap: Para 2
 *   ...
 * ]
 */
public class DocumentChunker {
    private static final Logger logger = LoggerFactory.getLogger(DocumentChunker.class);
    
    private final int chunkSize;      // Target size in tokens
    private final int chunkOverlap;   // Overlap in tokens
    
    public DocumentChunker(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }
    
    /**
     * Chunk a document into overlapping segments.
     */
    public List<String> chunk(String content) {
        // 1. Split into paragraphs
        String[] paragraphs = content.split("\n\n+");
        
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        
        for (String paragraph : paragraphs) {
            // Skip empty paragraphs
            if (paragraph.trim().isEmpty()) {
                continue;
            }
            
            int paragraphTokens = estimateTokens(paragraph);
            
            // If paragraph alone exceeds chunk size, split it
            if (paragraphTokens > chunkSize) {
                // Add current chunk if not empty
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
                
                // Split large paragraph by sentences
                chunks.addAll(splitLargeParagraph(paragraph));
                continue;
            }
            
            // Would adding this paragraph exceed chunk size?
            if (currentTokens + paragraphTokens > chunkSize && currentChunk.length() > 0) {
                // Save current chunk
                chunks.add(currentChunk.toString().trim());
                
                // Start new chunk with overlap
                currentChunk = new StringBuilder();
                currentTokens = 0;
                
                // Add overlap from previous chunk
                if (!chunks.isEmpty()) {
                    String overlap = getOverlap(chunks.get(chunks.size() - 1));
                    if (!overlap.isEmpty()) {
                        currentChunk.append(overlap).append("\n\n");
                        currentTokens = estimateTokens(overlap);
                    }
                }
            }
            
            // Add paragraph to current chunk
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
            currentTokens += paragraphTokens;
        }
        
        // Add final chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        logger.debug("Chunked document into {} chunks", chunks.size());
        return chunks;
    }
    
    private List<String> splitLargeParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        
        // Split by sentences (simple heuristic)
        String[] sentences = paragraph.split("(?<=[.!?])\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        
        for (String sentence : sentences) {
            int sentenceTokens = estimateTokens(sentence);
            
            // If single sentence is too large, force split it
            if (sentenceTokens > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
                
                // Force split by words
                chunks.addAll(forceSplit(sentence));
                continue;
            }
            
            if (currentTokens + sentenceTokens > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                
                // Start new chunk with overlap
                currentChunk = new StringBuilder();
                currentTokens = 0;
                
                if (!chunks.isEmpty()) {
                    String overlap = getOverlap(chunks.get(chunks.size() - 1));
                    if (!overlap.isEmpty()) {
                        currentChunk.append(overlap).append(" ");
                        currentTokens = estimateTokens(overlap);
                    }
                }
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
            currentTokens += sentenceTokens;
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    private List<String> forceSplit(String text) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        
        for (String word : words) {
            int wordTokens = estimateTokens(word);
            
            if (currentTokens + wordTokens > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                currentTokens = 0;
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(word);
            currentTokens += wordTokens;
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    private String getOverlap(String previousChunk) {
        int targetOverlapChars = chunkOverlap * 4; // Rough estimate: 4 chars per token
        
        if (previousChunk.length() <= targetOverlapChars) {
            return previousChunk;
        }
        
        // Take last N characters, but try to start at word boundary
        String overlap = previousChunk.substring(previousChunk.length() - targetOverlapChars);
        
        // Find first space to start at word boundary
        int spaceIndex = overlap.indexOf(' ');
        if (spaceIndex > 0 && spaceIndex < overlap.length() / 2) {
            overlap = overlap.substring(spaceIndex + 1);
        }
        
        return overlap;
    }
    
    private int estimateTokens(String text) {
        // Rough estimate: ~4 characters per token
        // This is conservative for English text
        return Math.max(1, text.length() / 4);
    }
}
