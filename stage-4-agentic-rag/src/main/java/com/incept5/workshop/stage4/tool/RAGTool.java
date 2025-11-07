package com.incept5.workshop.stage4.tool;

import com.incept5.workshop.stage4.db.Document;
import com.incept5.workshop.stage4.db.PgVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tool for searching Embabel and Spring AI documentation via vector similarity.
 * 
 * This tool is exposed to the LLM via JSON schema.
 * The agent can call this tool to retrieve relevant documentation
 * before answering user questions.
 * 
 * Features:
 * - Vector similarity search
 * - Optional context expansion (neighboring chunks)
 * - Smart result formatting for LLM consumption
 */
public class RAGTool implements Tool {
    private static final Logger logger = LoggerFactory.getLogger(RAGTool.class);
    
    private final PgVectorStore vectorStore;
    private final double defaultThreshold;
    
    /**
     * Creates a RAG tool with default similarity threshold.
     * 
     * @param vectorStore The vector store to search
     */
    public RAGTool(PgVectorStore vectorStore) {
        // Lower threshold (0.5) based on integration testing
        // This provides better recall while maintaining good precision
        this(vectorStore, 0.5);
    }
    
    /**
     * Creates a RAG tool with custom similarity threshold.
     * 
     * @param vectorStore The vector store to search
     * @param defaultThreshold Minimum similarity score (0-1)
     */
    public RAGTool(PgVectorStore vectorStore, double defaultThreshold) {
        this.vectorStore = vectorStore;
        this.defaultThreshold = defaultThreshold;
    }
    
    @Override
    public String name() {
        return "search_documentation";
    }
    
    @Override
    public String description() {
        return "Search Embabel and Spring AI documentation for relevant information. " +
               "Use this when you need to look up specific details, examples, or explanations " +
               "from the documentation to answer the user's question.";
    }
    
    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "query": {
                  "type": "string",
                  "description": "The search query to find relevant documentation"
                },
                "topK": {
                  "type": "integer",
                  "description": "Number of results to return (default: 5, max: 10)"
                },
                "expandContext": {
                  "type": "boolean",
                  "description": "Include neighboring chunks for more context (useful for code examples)"
                }
              },
              "required": ["query"]
            }
            """;
    }
    
    @Override
    public String execute(Map<String, Object> arguments) throws Exception {
        // Extract and validate parameters
        String query = (String) arguments.get("query");
        if (query == null || query.trim().isEmpty()) {
            return "Error: Query parameter is required";
        }
        
        Integer topK = (Integer) arguments.getOrDefault("topK", 5);
        topK = Math.min(Math.max(topK, 1), 10); // Clamp between 1-10
        
        Boolean expandContext = (Boolean) arguments.getOrDefault("expandContext", false);
        
        logger.info("Searching documentation: query='{}', topK={}, expandContext={}", 
            query, topK, expandContext);
        
        try {
            // 1. Perform vector search
            List<Document> documents = vectorStore.search(query, topK, defaultThreshold);
            
            if (documents.isEmpty()) {
                return "No relevant documentation found for the query. " +
                       "Try rephrasing or using different keywords.";
            }
            
            // 2. Optionally expand with neighboring chunks
            if (expandContext) {
                documents = expandWithNeighbors(documents);
            }
            
            // 3. Format results for LLM
            return formatDocuments(documents, expandContext);
            
        } catch (Exception e) {
            logger.error("Error searching documentation", e);
            return "Error searching documentation: " + e.getMessage();
        }
    }
    
    /**
     * Expand document context by retrieving neighboring chunks.
     * 
     * Strategy:
     * - For each document, get immediate neighbors (chunk_index ± 1)
     * - Use LinkedHashSet to maintain order and avoid duplicates
     * - This provides more complete context for code examples and explanations
     */
    private List<Document> expandWithNeighbors(List<Document> documents) throws Exception {
        Set<String> seen = new LinkedHashSet<>();
        List<Document> expanded = new ArrayList<>();
        
        for (Document doc : documents) {
            // Get neighboring chunks (radius = 1 means immediate neighbors)
            List<Document> neighbors = vectorStore.getNeighboringChunks(doc, 1);
            
            for (Document neighbor : neighbors) {
                // Use unique key to avoid duplicates
                String key = neighbor.source() + ":" + neighbor.fileHash() + ":" + neighbor.chunkIndex();
                
                if (!seen.contains(key)) {
                    seen.add(key);
                    expanded.add(neighbor);
                }
            }
        }
        
        logger.info("Expanded {} documents to {} with neighbors", documents.size(), expanded.size());
        return expanded;
    }
    
    /**
     * Format documents for LLM consumption.
     * 
     * Provides:
     * - Clear document boundaries
     * - Source attribution
     * - Similarity scores (for non-expanded results)
     * - Structured format that's easy for LLM to parse
     */
    private String formatDocuments(List<Document> documents, boolean expanded) {
        StringBuilder result = new StringBuilder();
        
        if (expanded) {
            result.append("Found ").append(documents.size())
                  .append(" document chunks (expanded with neighbors):\n\n");
        } else {
            result.append("Found ").append(documents.size())
                  .append(" relevant documents:\n\n");
        }
        
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            
            result.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            result.append("Document ").append(i + 1);
            
            // Add source attribution
            result.append(" [Source: ").append(doc.source()).append("]");
            
            // Add similarity score for non-expanded results
            if (!expanded && doc.similarity() > 0) {
                result.append(" [Similarity: ").append(String.format("%.2f", doc.similarity())).append("]");
            }
            
            result.append("\n");
            result.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            
            // Add content
            result.append(doc.content().trim());
            result.append("\n\n");
        }
        
        return result.toString();
    }
}
