
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
        
        // Verify conversation history
        ConversationMemory.Message[] history1 = agent.getConversationHistory();
        assertTrue(history1.length >= 2, 
            "Should have at least user message and assistant response");
        logger.info("✓ Initial query successful, conversation has {} messages", history1.length);
        
        // ===== PART 2: Follow-up Question =====
        logger.info("\n--- Part 2: Follow-up Question (Using Context) ---");
        String question2 = "Can you give me an example of how to use it?";
        logger.info("User: '{}'", question2);
        
        long startTime2 = System.currentTimeMillis();
        String response2 = agent.chat(question2);
        long duration2 = System.currentTimeMillis() - startTime2;
        
        logger.info("\nAgent Response ({}ms):", duration2);
        logger.info("{}", response2);
        logger.info("");
        
        // Verify follow-up response
        assertNotNull(response2, "Follow-up response should not be null");
        assertFalse(response2.trim().isEmpty(), "Follow-up response should not be empty");
        
        // Agent should understand "it" refers to Embabel from previous context
        String response2Lower = response2.toLowerCase();
        boolean hasExample = 
            response2Lower.contains("example") || 
            response2Lower.contains("@agent") ||
            response2Lower.contains("@action") ||
            response2Lower.contains("code") ||
            response2Lower.contains("class") ||
            response2Lower.contains("annotation");
        
        assertTrue(hasExample, 
            "Response should contain example or code reference based on context");
        
        // Verify conversation memory has grown
        ConversationMemory.Message[] history2 = agent.getConversationHistory();
        assertTrue(history2.length >= 4, 
            "Should have at least 2 user messages and 2 assistant responses");
        logger.info("✓ Follow-up successful, conversation has {} messages", history2.length);
        
        // ===== PART 3: Verify RAG Tool Usage =====
        logger.info("\n--- Part 3: Verify RAG Tool Usage ---");
        
        // Check for system messages (tool results) in history
        boolean hasToolResult = false;
        int systemMessageCount = 0;
        
        for (ConversationMemory.Message msg : history2) {
            if ("system".equals(msg.role())) {
                hasToolResult = true;
                systemMessageCount++;
                String preview = msg.content().substring(0, Math.min(150, msg.content().length()));
                logger.info("Found tool result: {}...", preview);
            }
        }
        
        assertTrue(hasToolResult, 
            "Should have at least one tool invocation (system message) in conversation");
        logger.info("✓ Found {} tool invocation(s)", systemMessageCount);
        
        // ===== PART 4: Response Quality Checks =====
        logger.info("\n--- Part 4: Response Quality ---");
        
        // Check response lengths are reasonable
        assertTrue(response1.length() > 50, 
            "Initial response should be reasonably detailed (>50 chars)");
        assertTrue(response2.length() > 50, 
            "Follow-up response should be reasonably detailed (>50 chars)");
        
        // Check response times are reasonable
        assertTrue(duration1 < 120000, 
            "Initial query should complete within 2 minutes");
        assertTrue(duration2 < 120000, 
            "Follow-up should complete within 2 minutes");
        
        logger.info("Response 1: {} chars in {}ms", response1.length(), duration1);
        logger.info("Response 2: {} chars in {}ms", response2.length(), duration2);
        logger.info("Final conversation: {} messages", history2.length);
        
        logger.info("\n=== ✓ RAG Agent Integration Test PASSED ===");
        logger.info("Successfully tested:");
        logger.info("  - Initial query with vector search");
        logger.info("  - Follow-up using conversation context");
        logger.info("  - Tool invocation tracking");
        logger.info("  - Response quality metrics\n");
    }
}
