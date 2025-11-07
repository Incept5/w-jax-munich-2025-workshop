
package com.incept5.workshop.stage4;

import com.incept5.workshop.stage4.db.DatabaseConfig;
import com.incept5.workshop.stage4.db.Document;
import com.incept5.workshop.stage4.db.PgVectorStore;
import com.incept5.workshop.stage4.ingestion.EmbeddingService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RAG vector search functionality.
 * 
 * This test:
 * 1. Verifies database connectivity and schema
 * 2. Checks existing documents and embeddings
 * 3. Tests embedding generation
 * 4. Tests vector similarity search
 * 5. Debugs issues with zero search results
 * 
 * Prerequisites:
 * - PostgreSQL with pgvector running (docker-compose up -d)
 * - Ollama running with nomic-embed-text model
 * - Documents ingested (./ingest.sh)
 */
@Disabled("Stage 3 is not yet complete - test requires full RAG implementation and database setup")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VectorSearchIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(VectorSearchIntegrationTest.class);
    
    private static DataSource dataSource;
    private static EmbeddingService embeddingService;
    private static PgVectorStore vectorStore;
    
    // Test queries related to ingested content
    private static final String[] TEST_QUERIES = {
        "what is embabel",
        "embabel framework",
        "how to create an agent",
        "agent examples"
    };
    
    /**
     * Get Ollama base URL from environment or system property
     */
    private static String getOllamaBaseUrl() {
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
    
    @BeforeAll
    static void setup() {
        logger.info("=== Setting up Vector Search Integration Test ===");
        
        // Create data source
        dataSource = DatabaseConfig.createDataSource();
        logger.info("✓ DataSource created");
        
        // Create embedding service
        embeddingService = new EmbeddingService(getOllamaBaseUrl(), "nomic-embed-text");
        logger.info("✓ EmbeddingService created");
        
        // Create vector store
        vectorStore = new PgVectorStore(dataSource, embeddingService);
        logger.info("✓ PgVectorStore created");
    }
    
    @AfterAll
    static void teardown() {
        if (vectorStore != null) {
            vectorStore.close();
        }
        logger.info("=== Test teardown complete ===");
    }
    
    @Test
    @Order(1)
    @DisplayName("1. Verify database connectivity")
    void testDatabaseConnectivity() throws SQLException {
        logger.info("\n=== Test 1: Database Connectivity ===");
        
        try (Connection conn = dataSource.getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
            
            logger.info("✓ Database connection successful");
            logger.info("  Database: {}", conn.getMetaData().getDatabaseProductName());
            logger.info("  Version: {}", conn.getMetaData().getDatabaseProductVersion());
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("2. Verify pgvector extension")
    void testPgVectorExtension() throws SQLException {
        logger.info("\n=== Test 2: pgvector Extension ===");
        
        String sql = "SELECT extname, extversion FROM pg_extension WHERE extname = 'vector'";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            assertTrue(rs.next(), "pgvector extension should be installed");
            
            String extName = rs.getString("extname");
            String extVersion = rs.getString("extversion");
            
            assertEquals("vector", extName);
            logger.info("✓ pgvector extension found");
            logger.info("  Version: {}", extVersion);
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("3. Verify documents table schema")
    void testDocumentsTableSchema() throws SQLException {
        logger.info("\n=== Test 3: Documents Table Schema ===");
        
        String sql = """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'documents'
            ORDER BY ordinal_position
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            logger.info("  Columns:");
            int columnCount = 0;
            while (rs.next()) {
                columnCount++;
                String columnName = rs.getString("column_name");
                String dataType = rs.getString("data_type");
                String nullable = rs.getString("is_nullable");
                
                logger.info("    - {} ({}, nullable: {})", columnName, dataType, nullable);
                
                // Verify key columns exist
                if (columnName.equals("embedding")) {
                    assertEquals("USER-DEFINED", dataType, "embedding should be a vector type");
                }
            }
            
            assertTrue(columnCount > 0, "Table should have columns");
            logger.info("✓ Table schema verified ({} columns)", columnCount);
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("4. Check document counts")
    void testDocumentCounts() throws SQLException {
        logger.info("\n=== Test 4: Document Counts ===");
        
        int totalDocs = vectorStore.getTotalDocuments();
        logger.info("  Total documents: {}", totalDocs);
        
        if (totalDocs == 0) {
            logger.warn("⚠ No documents found! Run ./ingest.sh first");
        } else {
            logger.info("✓ Documents exist in database");
            
            // Get counts by source
            Map<String, Integer> countsBySource = vectorStore.getDocumentCountsBySource();
            logger.info("  Documents by source:");
            countsBySource.forEach((source, count) -> 
                logger.info("    - {}: {}", source, count));
        }
        
        assertTrue(totalDocs > 0, "Documents should exist. Run ./ingest.sh if this fails.");
    }
    
    @Test
    @Order(5)
    @DisplayName("5. Verify embeddings are populated")
    void testEmbeddingsPopulated() throws SQLException {
        logger.info("\n=== Test 5: Embeddings Population ===");
        
        String sql = """
            SELECT 
                COUNT(*) as total,
                COUNT(embedding) as with_embedding,
                COUNT(*) - COUNT(embedding) as missing_embedding
            FROM documents
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            assertTrue(rs.next());
            
            int total = rs.getInt("total");
            int withEmbedding = rs.getInt("with_embedding");
            int missingEmbedding = rs.getInt("missing_embedding");
            
            logger.info("  Total documents: {}", total);
            logger.info("  With embeddings: {}", withEmbedding);
            logger.info("  Missing embeddings: {}", missingEmbedding);
            
            if (missingEmbedding > 0) {
                logger.error("✗ {} documents are missing embeddings!", missingEmbedding);
            } else {
                logger.info("✓ All documents have embeddings");
            }
            
            assertEquals(0, missingEmbedding, "All documents should have embeddings");
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("6. Verify embedding dimensions")
    void testEmbeddingDimensions() throws SQLException {
        logger.info("\n=== Test 6: Embedding Dimensions ===");
        
        String sql = "SELECT embedding FROM documents WHERE embedding IS NOT NULL LIMIT 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            assertTrue(rs.next(), "Should have at least one document with embedding");
            
            Object embeddingObj = rs.getObject("embedding");
            assertNotNull(embeddingObj, "Embedding should not be null");
            
            // The embedding is stored as PGvector, convert to string to check
            String embeddingStr = embeddingObj.toString();
            logger.info("  Embedding type: {}", embeddingObj.getClass().getName());
            logger.info("  Embedding preview: {}...", embeddingStr.substring(0, Math.min(100, embeddingStr.length())));
            
            // Count dimensions by counting commas + 1
            int dimensions = embeddingStr.split(",").length;
            logger.info("  Dimensions: {}", dimensions);
            
            assertEquals(768, dimensions, "nomic-embed-text should produce 768-dimensional embeddings");
            logger.info("✓ Embedding dimensions correct");
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("7. Test embedding generation")
    void testEmbeddingGeneration() {
        logger.info("\n=== Test 7: Embedding Generation ===");
        
        String testText = "This is a test sentence for embedding generation";
        logger.info("  Test text: '{}'", testText);
        
        float[] embedding = embeddingService.generateEmbedding(testText);
        
        assertNotNull(embedding, "Embedding should not be null");
        assertEquals(768, embedding.length, "Should generate 768-dimensional embedding");
        
        // Check that embedding has non-zero values
        float sum = 0;
        for (float value : embedding) {
            sum += Math.abs(value);
        }
        
        assertTrue(sum > 0, "Embedding should have non-zero values");
        
        logger.info("✓ Embedding generation successful");
        logger.info("  Dimensions: {}", embedding.length);
        logger.info("  Sample values: [{}, {}, {}, ...]", 
            embedding[0], embedding[1], embedding[2]);
    }
    
    @Test
    @Order(8)
    @DisplayName("8. Test similarity calculations")
    void testSimilarityCalculations() throws SQLException {
        logger.info("\n=== Test 8: Similarity Calculations ===");
        
        // Get two random documents and calculate their similarity
        String sql = """
            SELECT d1.content as content1, d2.content as content2,
                   1 - (d1.embedding <=> d2.embedding) as similarity
            FROM documents d1
            CROSS JOIN documents d2
            WHERE d1.id != d2.id
            LIMIT 5
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            logger.info("  Sample document similarities:");
            int count = 0;
            while (rs.next()) {
                count++;
                String content1 = rs.getString("content1");
                String content2 = rs.getString("content2");
                double similarity = rs.getDouble("similarity");
                
                logger.info("    Similarity: {}", String.format("%.4f", similarity));
                logger.info("      Doc 1: {}...", content1.substring(0, Math.min(60, content1.length())));
                logger.info("      Doc 2: {}...", content2.substring(0, Math.min(60, content2.length())));
            }
            
            assertTrue(count > 0, "Should calculate at least one similarity");
            logger.info("✓ Similarity calculations working");
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("9. Test vector search with various thresholds")
    void testVectorSearchThresholds() throws SQLException {
        logger.info("\n=== Test 9: Vector Search with Various Thresholds ===");
        
        String query = "what is embabel";
        double[] thresholds = {0.0, 0.3, 0.5, 0.7, 0.9};
        
        logger.info("  Query: '{}'", query);
        logger.info("  Testing thresholds: {}", thresholds);
        
        for (double threshold : thresholds) {
            List<Document> results = vectorStore.search(query, 5, threshold);
            
            logger.info("\n  Threshold: {} → {} results", 
                String.format("%.1f", threshold), results.size());
            
            if (!results.isEmpty()) {
                Document topResult = results.get(0);
                logger.info("    Top result (similarity: {}):", 
                    String.format("%.4f", topResult.similarity()));
                logger.info("      Source: {}", topResult.source());
                logger.info("      Content: {}...", 
                    topResult.content().substring(0, Math.min(100, topResult.content().length())));
            }
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("10. Test search with all test queries")
    void testSearchWithTestQueries() throws SQLException {
        logger.info("\n=== Test 10: Search with Test Queries ===");
        
        double threshold = 0.5; // Lower threshold for better results
        int topK = 3;
        
        for (String query : TEST_QUERIES) {
            logger.info("\n  Query: '{}'", query);
            
            List<Document> results = vectorStore.search(query, topK, threshold);
            
            if (results.isEmpty()) {
                logger.warn("    ⚠ No results found");
            } else {
                logger.info("    ✓ Found {} results", results.size());
                
                for (int i = 0; i < results.size(); i++) {
                    Document doc = results.get(i);
                    logger.info("      {}. Similarity: {} | Source: {}", 
                        i + 1, 
                        String.format("%.4f", doc.similarity()),
                        doc.source());
                    logger.info("         Content: {}...", 
                        doc.content().substring(0, Math.min(80, doc.content().length())));
                }
            }
            
            assertTrue(results.size() > 0, 
                "Should find at least one result for query: " + query);
        }
    }
    
    @Test
    @Order(11)
    @DisplayName("11. Debug: Compare query embedding to document embeddings")
    void testDebugEmbeddingComparison() throws SQLException {
        logger.info("\n=== Test 11: Debug Embedding Comparison ===");
        
        String query = "embabel framework";
        logger.info("  Query: '{}'", query);
        
        // Generate query embedding
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        logger.info("  Query embedding dimensions: {}", queryEmbedding.length);
        
        // Get a sample document and its embedding
        String sql = """
            SELECT id, content, source, embedding,
                   1 - (embedding <=> ?::vector) as similarity
            FROM documents
            WHERE content ILIKE '%embabel%'
            LIMIT 3
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            com.pgvector.PGvector pgVector = new com.pgvector.PGvector(queryEmbedding);
            stmt.setObject(1, pgVector);
            
            try (ResultSet rs = stmt.executeQuery()) {
                logger.info("\n  Documents containing 'embabel':");
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String content = rs.getString("content");
                    String source = rs.getString("source");
                    double similarity = rs.getDouble("similarity");
                    
                    logger.info("\n    Document ID: {}", id);
                    logger.info("      Source: {}", source);
                    logger.info("      Similarity: {}", String.format("%.4f", similarity));
                    logger.info("      Content: {}...", 
                        content.substring(0, Math.min(100, content.length())));
                }
            }
        }
    }
    
    @Test
    @Order(12)
    @DisplayName("12. Test IVFFlat index is being used")
    void testIndexUsage() throws SQLException {
        logger.info("\n=== Test 12: Index Usage ===");
        
        // Check if the index exists
        String checkIndexSql = """
            SELECT indexname, indexdef
            FROM pg_indexes
            WHERE tablename = 'documents' AND indexname LIKE '%embedding%'
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkIndexSql);
             ResultSet rs = stmt.executeQuery()) {
            
            boolean indexFound = false;
            while (rs.next()) {
                indexFound = true;
                String indexName = rs.getString("indexname");
                String indexDef = rs.getString("indexdef");
                
                logger.info("  Index found: {}", indexName);
                logger.info("    Definition: {}", indexDef);
            }
            
            assertTrue(indexFound, "Embedding index should exist");
            logger.info("✓ Embedding index exists");
        }
        
        // Test query plan to see if index is used
        String explainSql = """
            EXPLAIN ANALYZE
            SELECT id, content, 1 - (embedding <=> ?::vector) as similarity
            FROM documents
            ORDER BY embedding <=> ?::vector
            LIMIT 5
            """;
        
        String testQuery = "test query";
        float[] queryEmbedding = embeddingService.generateEmbedding(testQuery);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(explainSql)) {
            
            com.pgvector.PGvector pgVector = new com.pgvector.PGvector(queryEmbedding);
            stmt.setObject(1, pgVector);
            stmt.setObject(2, pgVector);
            
            try (ResultSet rs = stmt.executeQuery()) {
                logger.info("\n  Query plan:");
                while (rs.next()) {
                    String plan = rs.getString(1);
                    logger.info("    {}", plan);
                }
            }
        }
    }
    
    @Test
    @Order(13)
    @DisplayName("13. Final integration test")
    void testCompleteRAGWorkflow() throws SQLException {
        logger.info("\n=== Test 13: Complete RAG Workflow ===");
        
        // Test the complete workflow
        String query = "How do I create an agent with Embabel?";
        logger.info("  User query: '{}'", query);
        
        // Step 1: Generate query embedding
        logger.info("\n  Step 1: Generating query embedding...");
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        assertEquals(768, queryEmbedding.length, "Query embedding should be 768-dimensional");
        logger.info("    ✓ Query embedding generated");
        
        // Step 2: Search for similar documents
        logger.info("\n  Step 2: Searching for similar documents...");
        List<Document> results = vectorStore.search(query, 3, 0.5);
        logger.info("    ✓ Found {} results", results.size());
        
        assertFalse(results.isEmpty(), "Should find at least one relevant document");
        
        // Step 3: Display results
        logger.info("\n  Step 3: Retrieved documents:");
        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            logger.info("\n    Document {}", i + 1);
            logger.info("      Similarity: {}", String.format("%.4f", doc.similarity()));
            logger.info("      Source: {}", doc.source());
            logger.info("      Content: {}", 
                doc.content().substring(0, Math.min(200, doc.content().length())));
        }
        
        // Step 4: Verify quality
        Document topResult = results.get(0);
        assertTrue(topResult.similarity() > 0.5, 
            "Top result should have similarity > 0.5");
        
        logger.info("\n✓ Complete RAG workflow successful!");
    }
}
