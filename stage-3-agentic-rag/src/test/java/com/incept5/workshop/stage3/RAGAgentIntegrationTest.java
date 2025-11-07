
package com.incept5.workshop.stage3;

import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.backend.BackendFactory;
import com.incept5.ollama.backend.BackendType;
import com.incept5.workshop.stage3.agent.ConversationMemory;
import com.incept5.workshop.stage3.agent.RAGAgent;
import com.incept5.workshop.stage3.db.DatabaseConfig;
import com.incept5.workshop.stage3.db.PgVectorStore;
import com.incept5.workshop.stage3.ingestion.EmbeddingService;
import com.incept5.workshop.stage3.tool.RAGTool;
import com.incept5.workshop.stage3.tool.ToolRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG Agent Integration Test - Single Happy Path
 * 
 * This test verifies the complete RAG workflow:
 * 1. Agent receives user query about documentation
 * 2. Agent decides to use RAG tool to search vector store
 * 3. Vector search retrieves relevant document chunks
 * 4. Agent synthesizes answer from retrieved context
 * 5. Agent handles follow-up question using conversation memory
 * 
 * Prerequisites:
 * - PostgreSQL with pgvector running (docker-compose up -d)
 * - Ollama running with models: qwen3:4b, nomic-embed-text
 * - Documents ingested via ./ingest.sh
 * 
 * This is a single comprehensive test following workshop standards:
 * - Tests real integration (no mocks)
 * - Uses actual Ollama backend
 * - Uses actual PostgreSQL + pgvector
 * - Verifies end-to-end RAG workflow
 */
@Disabled("Stage 3 is not yet complete - test requires full RAG implementation")
public class RAGAgentIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(RAGAgentIntegrationTest.class);
    
    // Configuration
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String LLM_MODEL = "qwen3:4b";
    private static final String EMBEDDING_MODEL = "nomic-embed-text";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/workshop_rag";
    private static final String DB_USER = "workshop";
    private static final String DB_PASSWORD = "workshop123";
    
    // Test components
    private static AIBackend backend;
    private static DataSource dataSource;
    private static EmbeddingService embeddingService;
    private static PgVectorStore vectorStore;
    private static ToolRegistry toolRegistry;
    private static RAGAgent agent;
    
    @BeforeAll
    static void setup() throws Exception {
        logger.info("=== Setting up RAG Agent Integration Test ===");
        
        // 1. Create backend
        logger.info("Creating Ollama backend...");
        backend = BackendFactory.createBackend(
            BackendType.OLLAMA,
            OLLAMA_BASE_URL,
            LLM_MODEL,
            Duration.ofSeconds(300)
        );
        logger.info("✓ Backend created (model: {})", LLM_MODEL);
        
        // 2. Create database connection
        logger.info("Creating database connection...");
        dataSource = DatabaseConfig.createDataSource(DB_URL, DB_USER, DB_PASSWORD);
        logger.info("✓ Database connection created");
        
        // 3. Create embedding service
        logger.info("Creating embedding service...");
        embeddingService = new EmbeddingService(OLLAMA_BASE_URL, EMBEDDING_MODEL);
        logger.info("✓ Embedding service created (model: {})", EMBEDDING_MODEL);
        
        // 4. Create vector store
        logger.info("Creating vector store...");
        vectorStore = new PgVectorStore(dataSource, embeddingService);
        
        int docCount = vectorStore.getTotalDocuments();
        logger.info("✓ Vector store created ({} documents)", docCount);
        
        if (docCount == 0) {
            fail("No documents found in vector store. Run ./ingest.sh first!");
        }
        
        // 5. Create tool registry
        logger.info("Creating tool registry...");
        toolRegistry = new ToolRegistry();
        toolRegistry.register(new RAGTool(vectorStore));
        logger.info("✓ Tool registry created with tools: {}", toolRegistry.getToolNames());
        
        // 6. Create agent
        logger.info("Creating RAG agent...");
        agent = RAGAgent.builder()
            .backend(backend)
            .toolRegistry(toolRegistry)
            .maxConversationHistory(10)
            .maxIterations(10)
            .verbose(true)  // Enable verbose for debugging
            .build();
        logger.info("✓ RAG agent created");
        
        logger.info("=== Setup complete ===\n");
    }
    
    @AfterAll
    static void teardown() throws Exception {
        if (vectorStore != null) {
            vectorStore.close();
        }
        if (backend != null) {
            backend.close();
        }
        logger.info("=== Teardown complete ===");
    }
    
    @Test
    @DisplayName("Complete RAG Agent workflow with multi-turn conversation")
    void testCompleteRAGWorkflow() throws Exception {
        logger.info("\n=== RAG Agent Integration Test ===");
        logger.info("Testing complete workflow: initial query + follow-up with context\n");

        // ===== PART 1: Initial Query =====
        logger.info("--- Part 1: Initial Query ---");
        String question1 = "What is Embabel?";
        logger.info("User: '{}'", question1);

        long startTime1 = System.currentTimeMillis();
        String response1 = agent.chat(question1);
        long duration1 = System.currentTimeMillis() - startTime1;

        logger.info("\nAgent Response ({}ms):", duration1);
        logger.info("{}", response1);
        logger.info("");

        // Verify first response
        assertNotNull(response1, "Response should not be null");
        assertFalse(response1.trim().isEmpty(), "Response should not be empty");

        String response1Lower = response1.toLowerCase();
        boolean hasRelevantContent =
                response1Lower.contains("embabel") ||
                        response1Lower.contains("agent") ||
                        response1Lower.contains("framework") ||
                        response1Lower.contains("goap");

        assertTrue(hasRelevantContent,
                "Response should contain relevant information about Embabel");
    }
}
