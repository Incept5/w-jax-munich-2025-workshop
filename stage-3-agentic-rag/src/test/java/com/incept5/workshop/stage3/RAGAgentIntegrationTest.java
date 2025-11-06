package com.incept5.workshop.stage3;

import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.backend.BackendFactory;
import com.incept5.ollama.backend.BackendType;
import com.incept5.workshop.stage3.agent.ConversationMemory;
import com.incept5.workshop.stage3.agent.RAGAgent;
import com.incept5.workshop.stage3.db.DatabaseConfig;
import com.incept5.workshop.stage3.db.Document;
import com.incept5.workshop.stage3.db.PgVectorStore;
import com.incept5.workshop.stage3.ingestion.EmbeddingService;
import com.incept5.workshop.stage3.tool.RAGTool;
import com.incept5.workshop.stage3.tool.ToolRegistry;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Stage 3: RAG Agent
 * 
 * Prerequisites:
 * - Ollama running on localhost:11434
 * - Model available: incept5/Jan-v1-2509:fp16
 * - Embedding model available: nomic-embed-text
 * - PostgreSQL + pgvector running (via docker-compose)
 * - Documents ingested (via ingest.sh)
 * 
 * This test verifies:
 * - Vector search functionality
 * - Conversation memory
 * - Multi-turn conversations
 * - Tool calling with context expansion
 * - Agent reasoning loop
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RAGAgentIntegrationTest {
    
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String LLM_MODEL = "incept5/Jan-v1-2509:fp16";
    private static final String EMBEDDING_MODEL = "nomic-embed-text";
    
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/workshop_rag";
    private static final String DB_USER = "workshop";
    private static final String DB_PASSWORD = "workshop123";
    
    private static AIBackend backend;
    private static PgVectorStore vectorStore;
    private static EmbeddingService embeddingService;
    private static ToolRegistry toolRegistry;
    
    @BeforeAll
    static void setUp() throws Exception {
        System.out.println("\n=== Setting up Stage 3 Integration Test ===\n");
        
        // Setup backend
        System.out.println("1. Connecting to Ollama...");
        backend = BackendFactory.createBackend(
            BackendType.OLLAMA,
            OLLAMA_BASE_URL,
            LLM_MODEL,
            Duration.ofSeconds(300)
        );
        System.out.println("   ✓ Backend ready");
        
        // Setup database
        System.out.println("2. Connecting to PostgreSQL...");
        DataSource dataSource = DatabaseConfig.createDataSource(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("   ✓ Database ready");
        
        // Setup embedding service
        System.out.println("3. Initializing embedding service...");
        embeddingService = new EmbeddingService(OLLAMA_BASE_URL, EMBEDDING_MODEL);
        System.out.println("   ✓ Embedding service ready");
        
        // Setup vector store
        System.out.println("4. Initializing vector store...");
        vectorStore = new PgVectorStore(dataSource, embeddingService);
        
        int docCount = vectorStore.getTotalDocuments();
        System.out.println("   ✓ Vector store ready (" + docCount + " documents)");
        
        if (docCount == 0) {
            System.err.println("\n⚠️  WARNING: No documents in database!");
            System.err.println("   Please run './ingest.sh' before running tests.");
        }
        
        // Setup tools
        System.out.println("5. Registering tools...");
        toolRegistry = new ToolRegistry();
        toolRegistry.register(new RAGTool(vectorStore));
        System.out.println("   ✓ Tools ready: " + String.join(", ", toolRegistry.getToolNames()));
        
        System.out.println("\n=== Setup Complete ===\n");
    }
    
    @AfterAll
    static void tearDown() throws Exception {
        if (vectorStore != null) {
            vectorStore.close();
        }
        if (backend != null) {
            backend.close();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("1. Vector Search - Find Embabel Documentation")
    void testVectorSearch() throws Exception {
        System.out.println("\n=== Test 1: Vector Search ===");
        
        // Search for Embabel-related content
        List<Document> results = vectorStore.search("What is Embabel framework?", 5, 0.7);
        
        System.out.println("Found " + results.size() + " results");
        assertFalse(results.isEmpty(), "Should find documents about Embabel");
        
        // Check that at least one result mentions Embabel or related concepts
        boolean foundRelevant = results.stream()
            .anyMatch(doc -> doc.content().toLowerCase().contains("embabel") ||
                           doc.content().toLowerCase().contains("agent") ||
                           doc.content().toLowerCase().contains("goap"));
        
        assertTrue(foundRelevant, "Results should contain Embabel-related content");
        
        // Display first result
        if (!results.isEmpty()) {
            Document first = results.get(0);
            System.out.println("\nTop result:");
            System.out.println("  Source: " + first.source());
            System.out.println("  Similarity: " + String.format("%.2f", first.similarity()));
            System.out.println("  Content preview: " + 
                first.content().substring(0, Math.min(200, first.content().length())) + "...");
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("2. Neighboring Chunks - Context Expansion")
    void testNeighboringChunks() throws Exception {
        System.out.println("\n=== Test 2: Neighboring Chunks ===");
        
        // Get a document
        List<Document> results = vectorStore.search("Embabel agent example", 1, 0.7);
        assertFalse(results.isEmpty(), "Should find at least one document");
        
        Document doc = results.get(0);
        System.out.println("Original chunk: " + doc.chunkIndex());
        
        // Get neighboring chunks
        List<Document> neighbors = vectorStore.getNeighboringChunks(doc, 1);
        
        System.out.println("Found " + neighbors.size() + " neighboring chunks");
        assertFalse(neighbors.isEmpty(), "Should find neighboring chunks");
        
        // Verify chunks are in order
        for (int i = 0; i < neighbors.size() - 1; i++) {
            assertTrue(neighbors.get(i).chunkIndex() <= neighbors.get(i + 1).chunkIndex(),
                "Chunks should be in order");
        }
        
        System.out.println("Chunk indices: " + 
            neighbors.stream().map(d -> String.valueOf(d.chunkIndex())).toList());
    }
    
    @Test
    @Order(3)
    @DisplayName("3. RAG Tool - Search Documentation")
    void testRAGTool() throws Exception {
        System.out.println("\n=== Test 3: RAG Tool ===");
        
        RAGTool tool = new RAGTool(vectorStore);
        
        // Test basic search
        Map<String, Object> params = new HashMap<>();
        params.put("query", "How to create an agent?");
        params.put("topK", 3);
        
        String result = tool.execute(params);
        
        System.out.println("Tool result length: " + result.length() + " characters");
        assertNotNull(result, "Tool should return a result");
        assertTrue(result.contains("Document"), "Result should contain formatted documents");
        
        // Test with context expansion
        params.put("expandContext", true);
        String expandedResult = tool.execute(params);
        
        System.out.println("Expanded result length: " + expandedResult.length() + " characters");
        assertTrue(expandedResult.length() >= result.length(), 
            "Expanded result should be at least as long as basic search");
    }
    
    @Test
    @Order(4)
    @DisplayName("4. Conversation Memory - Multi-turn Context")
    void testConversationMemory() {
        System.out.println("\n=== Test 4: Conversation Memory ===");
        
        ConversationMemory memory = new ConversationMemory(5);
        
        // Add messages
        memory.addUserMessage("What is Embabel?");
        memory.addAssistantMessage("Embabel is an agent framework...");
        memory.addUserMessage("Show me an example");
        
        System.out.println("History size: " + memory.size());
        assertEquals(3, memory.size(), "Should have 3 messages");
        
        // Check formatting
        String formatted = memory.formatHistory();
        System.out.println("Formatted history length: " + formatted.length());
        
        assertTrue(formatted.contains("user:"), "Should contain user messages");
        assertTrue(formatted.contains("assistant:"), "Should contain assistant messages");
        
        // Test trimming
        for (int i = 0; i < 10; i++) {
            memory.addUserMessage("Message " + i);
        }
        
        System.out.println("After adding 10 more messages: " + memory.size());
        assertTrue(memory.size() <= 5, "Should trim to max size");
    }
    
    @Test
    @Order(5)
    @DisplayName("5. RAG Agent - Single Turn Conversation")
    void testSingleTurnConversation() throws Exception {
        System.out.println("\n=== Test 5: Single Turn Conversation ===");
        
        RAGAgent agent = RAGAgent.builder()
            .backend(backend)
            .toolRegistry(toolRegistry)
            .maxConversationHistory(10)
            .maxIterations(10)
            .verbose(false)
            .build();
        
        System.out.println("User: What is Embabel?");
        String response = agent.chat("What is Embabel?");
        
        System.out.println("\nAgent response length: " + response.length() + " characters");
        System.out.println("First 200 chars: " + 
            response.substring(0, Math.min(200, response.length())));
        
        assertNotNull(response, "Agent should provide a response");
        assertFalse(response.isEmpty(), "Response should not be empty");
        assertTrue(response.length() > 50, "Response should be substantial");
        
        // Check conversation history
        assertEquals(2, agent.getHistorySize(), "Should have user + assistant messages");
    }
    
    @Test
    @Order(6)
    @DisplayName("6. RAG Agent - Multi-turn Conversation")
    void testMultiTurnConversation() throws Exception {
        System.out.println("\n=== Test 6: Multi-turn Conversation ===");
        
        RAGAgent agent = RAGAgent.builder()
            .backend(backend)
            .toolRegistry(toolRegistry)
            .maxConversationHistory(10)
            .maxIterations(10)
            .verbose(false)
            .build();
        
        // First turn
        System.out.println("\nTurn 1:");
        System.out.println("User: Tell me about Embabel");
        String response1 = agent.chat("Tell me about Embabel");
        System.out.println("Agent: " + response1.substring(0, Math.min(150, response1.length())) + "...");
        
        assertNotNull(response1);
        assertTrue(agent.getHistorySize() >= 2);
        
        // Second turn (requires context from first)
        System.out.println("\nTurn 2:");
        System.out.println("User: Can you give me an example?");
        String response2 = agent.chat("Can you give me an example?");
        System.out.println("Agent: " + response2.substring(0, Math.min(150, response2.length())) + "...");
        
        assertNotNull(response2);
        assertTrue(agent.getHistorySize() >= 4, "Should have multiple messages in history");
        
        // Third turn
        System.out.println("\nTurn 3:");
        System.out.println("User: What about Spring AI?");
        String response3 = agent.chat("What about Spring AI?");
        System.out.println("Agent: " + response3.substring(0, Math.min(150, response3.length())) + "...");
        
        assertNotNull(response3);
        
        System.out.println("\nFinal history size: " + agent.getHistorySize());
    }
    
    @Test
    @Order(7)
    @DisplayName("7. Full Integration - Complex Query with Context")
    void testComplexQueryWithContext() throws Exception {
        System.out.println("\n=== Test 7: Complex Query ===");
        
        RAGAgent agent = RAGAgent.builder()
            .backend(backend)
            .toolRegistry(toolRegistry)
            .maxConversationHistory(10)
            .maxIterations(10)
            .verbose(true)  // Enable verbose for this test
            .build();
        
        System.out.println("\nComplex query: How do I create an agent with Embabel? Show me code.");
        
        String response = agent.chat(
            "How do I create an agent with Embabel? Show me a code example."
        );
        
        System.out.println("\n=== Agent Response ===");
        System.out.println(response);
        System.out.println("======================");
        
        assertNotNull(response);
        assertTrue(response.length() > 100, "Response should be detailed");
        
        // The agent should have searched documentation and provided an answer
        assertTrue(agent.getHistorySize() >= 2, "Should have conversation history");
    }
}
