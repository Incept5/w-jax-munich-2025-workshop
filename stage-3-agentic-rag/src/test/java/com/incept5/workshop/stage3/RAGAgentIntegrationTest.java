
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
 * Complete RAG Agent integration test.
 * 
 * Tests the full agent workflow:
 * 1. Agent receives user query
 * 2. Agent decides to search documentation
 * 3. Vector search retrieves relevant docs
 * 4. Agent synthesizes answer from retrieved context
 * 5. Agent maintains conversation memory
 * 
 * Prerequisites:
 * - PostgreSQL with pgvector running (docker-compose up -d)
 * - Ollama running with models: qwen3:4b, nomic-embed-text
 * - Documents ingested (./ingest.sh)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    @Order(1)
    @DisplayName("1. Test simple question: What is Embabel?")
    void testSimpleQuestion() throws Exception {
        logger.info("\n=== Test 1: Simple Question ===");
        
        String question = "What is Embabel?";
        logger.info("User question: '{}'", question);
        
        long startTime = System.currentTimeMillis();
        String response = agent.chat(question);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("\n--- Agent Response ({} ms) ---", duration);
        logger.info(response);
        logger.info("--- End Response ---\n");
        
        // Verify response quality
        assertNotNull(response, "Response should not be null");
        assertFalse(response.trim().isEmpty(), "Response should not be empty");
        
        // Check for key terms in response
        String responseLower = response.toLowerCase();
        boolean hasRelevantContent = 
            responseLower.contains("embabel") || 
            responseLower.contains("agent") || 
            responseLower.contains("framework") ||
            responseLower.contains("goap") ||
            responseLower.contains("rod johnson");
        
        assertTrue(hasRelevantContent, 
            "Response should contain relevant information about Embabel");
        
        // Verify conversation memory
        ConversationMemory.Message[] history = agent.getConversationHistory();
        assertTrue(history.length >= 2, "Should have at least user message and assistant response");
        
        logger.info("✓ Simple question test passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("2. Test follow-up question using context")
    void testFollowUpQuestion() throws Exception {
        logger.info("\n=== Test 2: Follow-up Question ===");
        
        String followUp = "Can you give me an example?";
        logger.info("Follow-up question: '{}'", followUp);
        
        long startTime = System.currentTimeMillis();
        String response = agent.chat(followUp);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("\n--- Agent Response ({} ms) ---", duration);
        logger.info(response);
        logger.info("--- End Response ---\n");
        
        // Verify response
        assertNotNull(response, "Response should not be null");
        assertFalse(response.trim().isEmpty(), "Response should not be empty");
        
        // The agent should understand "example" refers to Embabel from previous context
        String responseLower = response.toLowerCase();
        boolean hasExample = 
            responseLower.contains("example") || 
            responseLower.contains("@agent") ||
            responseLower.contains("@action") ||
            responseLower.contains("code") ||
            responseLower.contains("class");
        
        assertTrue(hasExample, 
            "Response should contain an example or reference to code");
        
        // Verify conversation memory has grown
        ConversationMemory.Message[] history = agent.getConversationHistory();
        assertTrue(history.length >= 4, 
            "Should have at least 2 user messages and 2 assistant responses");
        
        logger.info("✓ Follow-up question test passed");
    }
    
    @Test
    @Order(3)
    @DisplayName("3. Test specific technical question")
    void testTechnicalQuestion() throws Exception {
        logger.info("\n=== Test 3: Technical Question ===");
        
        // Clear history for fresh start
        agent.clearHistory();
        logger.info("Conversation history cleared");
        
        String question = "How does Embabel use Goal-Oriented Action Planning?";
        logger.info("Technical question: '{}'", question);
        
        long startTime = System.currentTimeMillis();
        String response = agent.chat(question);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("\n--- Agent Response ({} ms) ---", duration);
        logger.info(response);
        logger.info("--- End Response ---\n");
        
        // Verify response
        assertNotNull(response, "Response should not be null");
        assertFalse(response.trim().isEmpty(), "Response should not be empty");
        
        // Check for technical terms
        String responseLower = response.toLowerCase();
        boolean hasTechnicalContent = 
            responseLower.contains("goap") || 
            responseLower.contains("goal") ||
            responseLower.contains("action") ||
            responseLower.contains("planning") ||
            responseLower.contains("condition");
        
        assertTrue(hasTechnicalContent, 
            "Response should contain technical information about GOAP");
        
        logger.info("✓ Technical question test passed");
    }
    
    @Test
    @Order(4)
    @DisplayName("4. Test Spring AI related question")
    void testSpringAIQuestion() throws Exception {
        logger.info("\n=== Test 4: Spring AI Question ===");
        
        // Clear history for fresh start
        agent.clearHistory();
        logger.info("Conversation history cleared");
        
        String question = "What is Spring AI ChatClient?";
        logger.info("Spring AI question: '{}'", question);
        
        long startTime = System.currentTimeMillis();
        String response = agent.chat(question);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("\n--- Agent Response ({} ms) ---", duration);
        logger.info(response);
        logger.info("--- End Response ---\n");
        
        // Verify response
        assertNotNull(response, "Response should not be null");
        assertFalse(response.trim().isEmpty(), "Response should not be empty");
        
        // Check for Spring AI terms
        String responseLower = response.toLowerCase();
        boolean hasSpringAIContent = 
            responseLower.contains("spring ai") || 
            responseLower.contains("chatclient") ||
            responseLower.contains("llm") ||
            responseLower.contains("ai model");
        
        assertTrue(hasSpringAIContent, 
            "Response should contain information about Spring AI");
        
        logger.info("✓ Spring AI question test passed");
    }
    
    @Test
    @Order(5)
    @DisplayName("5. Test multi-turn conversation flow")
    void testMultiTurnConversation() throws Exception {
        logger.info("\n=== Test 5: Multi-turn Conversation ===");
        
        // Clear history for fresh conversation
        agent.clearHistory();
        logger.info("Conversation history cleared");
        
        // Turn 1: Ask about Embabel
        String q1 = "What is Embabel?";
        logger.info("\nTurn 1 - User: '{}'", q1);
        String r1 = agent.chat(q1);
        logger.info("Turn 1 - Assistant: {}", r1.substring(0, Math.min(200, r1.length())) + "...");
        assertNotNull(r1);
        
        // Turn 2: Ask who created it (requires context from turn 1)
        String q2 = "Who created it?";
        logger.info("\nTurn 2 - User: '{}'", q2);
        String r2 = agent.chat(q2);
        logger.info("Turn 2 - Assistant: {}", r2.substring(0, Math.min(200, r2.length())) + "...");
        assertNotNull(r2);
        
        // Should mention Rod Johnson
        assertTrue(r2.toLowerCase().contains("rod johnson"), 
            "Should identify Rod Johnson as creator based on context");
        
        // Turn 3: Ask for more details (requires context from turns 1 and 2)
        String q3 = "What other frameworks has he created?";
        logger.info("\nTurn 3 - User: '{}'", q3);
        String r3 = agent.chat(q3);
        logger.info("Turn 3 - Assistant: {}", r3.substring(0, Math.min(200, r3.length())) + "...");
        assertNotNull(r3);
        
        // Should mention Spring Framework
        assertTrue(r3.toLowerCase().contains("spring"), 
            "Should identify Spring Framework based on Rod Johnson context");
        
        // Verify conversation history
        ConversationMemory.Message[] history = agent.getConversationHistory();
        assertTrue(history.length >= 6, 
            "Should have 3 user messages and 3 assistant responses");
        
        logger.info("\n✓ Multi-turn conversation test passed");
        logger.info("  Final conversation length: {} messages", history.length);
    }
    
    @Test
    @Order(6)
    @DisplayName("6. Test conversation memory limit")
    void testConversationMemoryLimit() throws Exception {
        logger.info("\n=== Test 6: Conversation Memory Limit ===");
        
        // Clear history
        agent.clearHistory();
        
        // Send messages to exceed memory limit (maxConversationHistory = 10)
        for (int i = 1; i <= 12; i++) {
            String question = "Question number " + i;
            logger.info("Sending message {}: '{}'", i, question);
            agent.chat(question);
        }
        
        // Check that history doesn't exceed limit
        ConversationMemory.Message[] history = agent.getConversationHistory();
        logger.info("Final history size: {} messages", history.length);
        
        assertTrue(history.length <= 20, // 10 pairs (user + assistant) = 20 messages max
            "History should not exceed maximum conversation history setting");
        
        logger.info("✓ Memory limit test passed");
    }
    
    @Test
    @Order(7)
    @DisplayName("7. Test tool invocation tracking")
    void testToolInvocationTracking() throws Exception {
        logger.info("\n=== Test 7: Tool Invocation Tracking ===");
        
        // Clear history
        agent.clearHistory();
        
        String question = "Tell me about Embabel's architecture";
        logger.info("Question designed to trigger tool use: '{}'", question);
        
        String response = agent.chat(question);
        
        // Verify response
        assertNotNull(response);
        assertFalse(response.trim().isEmpty());
        
        // Check conversation history for tool results (system messages)
        ConversationMemory.Message[] history = agent.getConversationHistory();
        logger.info("Conversation history has {} messages", history.length);
        
        boolean hasSystemMessage = false;
        for (ConversationMemory.Message msg : history) {
            if ("system".equals(msg.role())) {
                hasSystemMessage = true;
                logger.info("Found system message (tool result): {}", 
                    msg.content().substring(0, Math.min(100, msg.content().length())) + "...");
                break;
            }
        }
        
        assertTrue(hasSystemMessage, 
            "Should have at least one system message (tool result) in history");
        
        logger.info("✓ Tool invocation tracking test passed");
    }
    
    @Test
    @Order(8)
    @DisplayName("8. Test response quality metrics")
    void testResponseQualityMetrics() throws Exception {
        logger.info("\n=== Test 8: Response Quality Metrics ===");
        
        // Clear history
        agent.clearHistory();
        
        String question = "What is Embabel and how does it differ from other agent frameworks?";
        logger.info("Question: '{}'", question);
        
        long startTime = System.currentTimeMillis();
        String response = agent.chat(question);
        long duration = System.currentTimeMillis() - startTime;
        
        // Metrics
        int responseLength = response.length();
        int wordCount = response.split("\\s+").length;
        boolean hasStructure = response.contains("\n") || response.contains(".");
        
        logger.info("\nResponse Metrics:");
        logger.info("  Duration: {} ms", duration);
        logger.info("  Length: {} characters", responseLength);
        logger.info("  Word count: {} words", wordCount);
        logger.info("  Has structure: {}", hasStructure);
        
        // Quality assertions
        assertTrue(responseLength > 100, 
            "Response should be reasonably detailed (>100 chars)");
        assertTrue(wordCount > 20, 
            "Response should have substantial content (>20 words)");
        assertTrue(hasStructure, 
            "Response should be well-structured with sentences/paragraphs");
        assertTrue(duration < 60000, 
            "Response should complete within reasonable time (<60s)");
        
        logger.info("✓ Response quality test passed");
    }
}
