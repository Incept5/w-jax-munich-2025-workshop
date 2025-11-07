package com.incept5.workshop.stage3.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Document chunker optimized for gitingest consolidated output.
 * 
 * Strategy:
 * 1. Prefer splitting at file boundaries (================...)
 * 2. If can't split at file boundary, split at blank lines
 * 3. Add intelligent overlap that respects code structure
 * 4. Preserve file headers in chunks for context
 * 
 * Chunk size: 512 tokens (~2000 chars, ~30-40 lines of code)
 * Overlap: 100 tokens (20%, helps maintain context across chunks)
 * 
 * Based on research:
 * - 256-512 tokens is optimal for code retrieval
 * - Smaller chunks = better precision for specific queries
 * - Smart boundaries = better context preservation
 */
public class DocumentChunker {
    private static final Logger logger = LoggerFactory.getLogger(DocumentChunker.class);
    
    private final int chunkSize;      // Target size in tokens (default: 512)
    private final int chunkOverlap;   // Overlap in tokens (default: 100)
    
    // gitingest file separator pattern
    private static final String FILE_SEPARATOR = "={80,}";
    private static final Pattern FILE_HEADER_PATTERN = 
        Pattern.compile("={80,}\\s*\\nFile: (.+?)\\n={80,}");
    
    public DocumentChunker(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }
    
    /**
     * Chunk a gitingest document into overlapping segments.
     * Tries to respect file boundaries when possible.
     */
    public List<String> chunk(String content) {
        List<String> chunks = new ArrayList<>();
        
        // Try to split at file boundaries first
        String[] fileSections = content.split(FILE_SEPARATOR);
        
        StringBuilder currentChunk = new StringBuilder();
        String currentFileHeader = null;
        int currentTokens = 0;
        
        for (String section : fileSections) {
            section = section.trim();
            if (section.isEmpty()) continue;
            
            // Check if this section starts with a file header
            Matcher matcher = FILE_HEADER_PATTERN.matcher(section);
            if (matcher.find()) {
                currentFileHeader = matcher.group(0); // Full header with separators
                section = section.substring(matcher.end()).trim();
            }
            
            int sectionTokens = estimateTokens(section);
            
            // If section is too large, split it
            if (sectionTokens > chunkSize) {
                // Save current chunk if exists
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
                
                // Split large section
                chunks.addAll(splitLargeSection(section, currentFileHeader));
                currentFileHeader = null;
                continue;
            }
            
            // Would adding this section exceed chunk size?
            if (currentTokens + sectionTokens > chunkSize && currentChunk.length() > 0) {
                // Save current chunk
                chunks.add(currentChunk.toString().trim());
                
                // Start new chunk with overlap
                String overlap = getOverlap(chunks.get(chunks.size() - 1));
                currentChunk = new StringBuilder(overlap);
                currentTokens = estimateTokens(overlap);
                
                // Add newline separator if overlap exists
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
            }
            
            // Add file header if we have one
            if (currentFileHeader != null && currentChunk.length() == 0) {
                currentChunk.append(currentFileHeader).append("\n\n");
                currentTokens += estimateTokens(currentFileHeader);
                currentFileHeader = null; // Use it only once
            }
            
            // Add section to current chunk
            currentChunk.append(section);
            currentTokens += sectionTokens;
        }
        
        // Add final chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        logger.info("Chunked document into {} chunks (target: {} tokens, overlap: {} tokens)",
            chunks.size(), chunkSize, chunkOverlap);
        
        return chunks;
    }
    
    /**
     * Split a large section (bigger than chunk size) into smaller chunks.
     * Preserves file header context in each chunk.
     */
    private List<String> splitLargeSection(String section, String fileHeader) {
        List<String> chunks = new ArrayList<>();
        
        // Split by double newlines (paragraphs/blocks)
        String[] blocks = section.split("\n\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        
        // Add file header to first chunk if available
        boolean headerAdded = false;
        
        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;
            
            int blockTokens = estimateTokens(block);
            
            // If single block exceeds chunk size, force split it
            if (blockTokens > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                    headerAdded = false;
                }
                
                chunks.addAll(forceSplitBlock(block, fileHeader));
                continue;
            }
            
            // Would adding this block exceed chunk size?
            if (currentTokens + blockTokens > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                
                // Start new chunk with overlap
                String overlap = getOverlap(chunks.get(chunks.size() - 1));
                currentChunk = new StringBuilder(overlap);
                currentTokens = estimateTokens(overlap);
                headerAdded = false; // Reset for new chunk
                
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
            }
            
            // Add file header to new chunk if not added yet
            if (!headerAdded && fileHeader != null) {
                currentChunk.insert(0, fileHeader + "\n\n");
                currentTokens += estimateTokens(fileHeader);
                headerAdded = true;
            }
            
            // Add block
            if (currentChunk.length() > 0 && !currentChunk.toString().endsWith("\n\n")) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(block);
            currentTokens += blockTokens;
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    /**
     * Force split a block that's too large, by lines.
     */
    private List<String> forceSplitBlock(String block, String fileHeader) {
        List<String> chunks = new ArrayList<>();
        String[] lines = block.split("\n");
        
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;
        boolean headerAdded = false;
        
        for (String line : lines) {
            int lineTokens = estimateTokens(line);
            
            if (currentTokens + lineTokens > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                
                String overlap = getOverlap(chunks.get(chunks.size() - 1));
                currentChunk = new StringBuilder(overlap);
                currentTokens = estimateTokens(overlap);
                headerAdded = false;
                
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n");
                }
            }
            
            // Add file header to new chunk
            if (!headerAdded && fileHeader != null) {
                currentChunk.insert(0, fileHeader + "\n\n");
                currentTokens += estimateTokens(fileHeader);
                headerAdded = true;
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append("\n");
            }
            currentChunk.append(line);
            currentTokens += lineTokens;
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    /**
     * Get overlap from previous chunk.
     * Tries to respect code boundaries by finding good split points.
     */
    private String getOverlap(String previousChunk) {
        int targetOverlapChars = chunkOverlap * 4; // 4 chars per token estimate
        
        if (previousChunk.length() <= targetOverlapChars) {
            return previousChunk;
        }
        
        // Take last N characters
        String overlap = previousChunk.substring(
            previousChunk.length() - targetOverlapChars
        );
        
        // Try to start at a good boundary:
        // 1. File separator (best)
        int fileSepIdx = overlap.indexOf("====");
        if (fileSepIdx > 0 && fileSepIdx < overlap.length() / 2) {
            return overlap.substring(fileSepIdx);
        }
        
        // 2. Double newline (paragraph/block boundary)
        int doubleNewlineIdx = overlap.indexOf("\n\n");
        if (doubleNewlineIdx > 0 && doubleNewlineIdx < overlap.length() / 2) {
            return overlap.substring(doubleNewlineIdx + 2);
        }
        
        // 3. Single newline (line boundary)
        int newlineIdx = overlap.indexOf("\n");
        if (newlineIdx > 0 && newlineIdx < overlap.length() / 2) {
            return overlap.substring(newlineIdx + 1);
        }
        
        // 4. Space (word boundary)
        int spaceIdx = overlap.indexOf(" ");
        if (spaceIdx > 0 && spaceIdx < overlap.length() / 2) {
            return overlap.substring(spaceIdx + 1);
        }
        
        // If no good boundary found, use as-is
        return overlap;
    }
    
    /**
     * Estimate token count from character count.
     * Uses 4 chars per token heuristic (conservative for code).
     */
    private int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }
}
