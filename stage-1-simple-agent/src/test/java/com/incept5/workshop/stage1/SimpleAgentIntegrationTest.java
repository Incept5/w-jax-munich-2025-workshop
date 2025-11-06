package com.incept5.workshop.stage1;

import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.backend.BackendFactory;
import com.incept5.ollama.backend.BackendType;
import com.incept5.ollama.config.BackendConfig;
import com.incept5.ollama.exception.AIBackendException;
import com.incept5.workshop.stage1.tool.CountryInfoTool;
import com.incept5.workshop.stage1.tool.ToolRegistry;
import com.incept5.workshop.stage1.tool.WeatherTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SimpleAgent with real AI backend and real API calls.
 * 
 * These tests verify the complete agent loop including:
 * - AI backend connectivity (Ollama)
 * - Real tool execution (weather and country info APIs)
 * - Multi-step reasoning with tool chaining
 * - Proper iteration counting and completion status
 * 
 * REQUIREMENTS:
 * - Ollama must be running on localhost:11434
 * - Model qwen2.5:3b must be available
 * - Network access to wttr.in and restcountries.com
 * 
 * These tests can be skipped by setting SKIP_INTEGRATION_TESTS=true
 */
class SimpleAgentIntegrationTest {
    
    private AIBackend backend;
    private ToolRegistry toolRegistry;
    private SimpleAgent agent;
    
    @BeforeEach
    void setUp() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("Setting up integration test environment...");
        System.out.println("=".repeat(70));
        
        try {
            // Initialize tool registry with real API tools
            toolRegistry = new ToolRegistry()
                    .register(new WeatherTool())
                    .register(new CountryInfoTool());
            
            System.out.println("✓ Registered " + toolRegistry.size() + " tools: " + 
                    String.join(", ", toolRegistry.getToolNames()));
            
            // Initialize AI backend with reasonable timeout for tests
            BackendConfig config = BackendConfig.builder()
                    .backendType(BackendType.OLLAMA)
                    .baseUrl("http://localhost:11434")
                    .model("qwen2.5:3b")
                    .requestTimeout(Duration.ofSeconds(60))
                    .build();
            
            backend = BackendFactory.createBackend(
                    config.backendType(),
                    config.baseUrl(),
                    config.model(),
                    config.requestTimeout()
            );
            
            System.out.println("✓ Connected to " + backend.getBackendType() + 
                    " (model: " + backend.getModelName() + ")");
            
            // Create agent with reasonable iteration limit for tests
            agent = new SimpleAgent(backend, toolRegistry, 10);
            
            System.out.println("✓ Agent initialized (max 10 iterations)");
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize test environment: " + e.getMessage());
            System.err.println("\nREQUIREMENTS:");
            System.err.println("  1. Ollama must be running on localhost:11434");
            System.err.println("  2. Model 'qwen2.5:3b' must be available");
            System.err.println("  3. Network access to wttr.in and restcountries.com");
            System.err.println("\nTo skip these tests, set: SKIP_INTEGRATION_TESTS=true");
            
            fail("Test environment setup failed: " + e.getMessage(), e);
        }
    }
    
    @AfterEach
    void tearDown() {
        if (backend != null) {
            try {
                backend.close();
                System.out.println("\n✓ Backend connection closed");
            } catch (Exception e) {
                System.err.println("Warning: Failed to close backend: " + e.getMessage());
            }
        }
    }
    
    /**
     * Test Case 1: Simple Single-Tool Task
     * 
     * Verifies the agent can complete a straightforward task using a single tool.
     * This tests:
     * - Basic agent loop functionality
     * - Single tool invocation
     * - Proper completion detection
     * - Response generation
     */
    @Test
    void testSimpleSingleToolTask() throws AIBackendException {
        System.out.println("TEST CASE 1: Simple Single-Tool Task");
        System.out.println("-".repeat(70));
        System.out.println("Task: Get weather for Tokyo (single weather tool call)");
        System.out.println();
        
        String task = "What's the weather in Tokyo?";
        
        long startTime = System.currentTimeMillis();
        SimpleAgent.AgentResult result = agent.run(task, true); // verbose for test visibility
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST RESULTS:");
        System.out.println("=".repeat(70));
        System.out.println("Completed: " + result.completed());
        System.out.println("Iterations: " + result.iterations());
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Response length: " + result.response().length() + " chars");
        System.out.println();
        System.out.println("Response preview:");
        System.out.println(result.response().substring(0, Math.min(200, result.response().length())) + "...");
        System.out.println("=".repeat(70));
        
        // Assertions
        assertTrue(result.completed(), 
                "Agent should complete the task successfully");
        
        assertTrue(result.iterations() >= 1 && result.iterations() <= 5, 
                "Simple task should complete in 1-5 iterations, got: " + result.iterations());
        
        assertNotNull(result.response(), 
                "Agent should provide a response");
        
        assertFalse(result.response().isBlank(), 
                "Response should not be empty");
        
        assertTrue(result.response().toLowerCase().contains("tokyo") || 
                   result.response().toLowerCase().contains("weather") ||
                   result.response().toLowerCase().contains("temperature"),
                "Response should mention Tokyo or weather information");
        
        System.out.println("✓ All assertions passed for single-tool task\n");
    }
    
    /**
     * Test Case 2: Multi-Step Reasoning Task
     * 
     * Verifies the agent can chain multiple tools to accomplish a complex task.
     * This tests:
     * - Multi-step reasoning capability
     * - Tool chaining (country_info → weather)
     * - Context maintenance across iterations
     * - Proper final answer synthesis
     * 
     * The task requires:
     * 1. First use country_info tool to find France's capital (Paris)
     * 2. Then use weather tool to check Paris weather
     * 3. Synthesize both results into final answer
     */
    @Test
    void testMultiStepReasoningTask() throws AIBackendException {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE 2: Multi-Step Reasoning Task");
        System.out.println("-".repeat(70));
        System.out.println("Task: Get weather in France's capital (requires tool chaining)");
        System.out.println("Expected flow: country_info(France) → weather(Paris)");
        System.out.println();
        
        String task = "What's the weather like in the capital of France?";
        
        long startTime = System.currentTimeMillis();
        SimpleAgent.AgentResult result = agent.run(task, true); // verbose for test visibility
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST RESULTS:");
        System.out.println("=".repeat(70));
        System.out.println("Completed: " + result.completed());
        System.out.println("Iterations: " + result.iterations());
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Response length: " + result.response().length() + " chars");
        System.out.println();
        System.out.println("Full Response:");
        System.out.println("-".repeat(70));
        System.out.println(result.response());
        System.out.println("=".repeat(70));
        
        // Assertions
        assertTrue(result.completed(), 
                "Agent should complete multi-step task successfully");
        
        assertTrue(result.iterations() >= 2, 
                "Multi-step task should require at least 2 iterations (country lookup + weather lookup), got: " + 
                result.iterations());
        
        assertTrue(result.iterations() <= 8, 
                "Task should complete efficiently (≤8 iterations), got: " + result.iterations());
        
        assertNotNull(result.response(), 
                "Agent should provide a response");
        
        assertFalse(result.response().isBlank(), 
                "Response should not be empty");
        
        String responseLower = result.response().toLowerCase();
        
        assertTrue(responseLower.contains("paris") || responseLower.contains("france"),
                "Response should mention Paris or France");
        
        assertTrue(responseLower.contains("weather") || 
                   responseLower.contains("temperature") ||
                   responseLower.contains("°c") ||
                   responseLower.contains("sunny") ||
                   responseLower.contains("cloudy") ||
                   responseLower.contains("rain"),
                "Response should contain weather-related information");
        
        System.out.println("\n✓ All assertions passed for multi-step reasoning task\n");
    }
    
    /**
     * Test Case 3: Agent Iteration Limits
     * 
     * Verifies the agent respects max iteration limits and handles incomplete tasks gracefully.
     * This tests:
     * - Max iteration enforcement
     * - Graceful degradation when limit reached
     * - Proper completion status reporting
     */
    @Test
    void testIterationLimitHandling() throws AIBackendException {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE 3: Iteration Limit Handling");
        System.out.println("-".repeat(70));
        System.out.println("Testing agent behavior with very low iteration limit");
        System.out.println();
        
        // Create agent with very low iteration limit to force incomplete status
        SimpleAgent limitedAgent = new SimpleAgent(backend, toolRegistry, 1);
        
        String task = "What's the weather like in the capital of France?";
        
        SimpleAgent.AgentResult result = limitedAgent.run(task, false);
        
        System.out.println("TEST RESULTS:");
        System.out.println("-".repeat(70));
        System.out.println("Completed: " + result.completed());
        System.out.println("Iterations: " + result.iterations());
        System.out.println("Response: " + result.response());
        System.out.println("=".repeat(70));
        
        // With only 1 iteration, the agent likely won't complete a multi-step task
        assertEquals(1, result.iterations(), 
                "Agent should use exactly 1 iteration with max=1");
        
        assertNotNull(result.response(), 
                "Agent should still provide some response");
        
        assertFalse(result.response().isBlank(), 
                "Response should not be empty even if incomplete");
        
        System.out.println("\n✓ All assertions passed for iteration limit handling\n");
    }
    
    /**
     * Test Case 4: Tool Registry Validation
     * 
     * Verifies the tool registry is properly initialized and accessible.
     */
    @Test
    void testToolRegistrySetup() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE 4: Tool Registry Validation");
        System.out.println("-".repeat(70));
        
        assertEquals(2, toolRegistry.size(), 
                "Should have exactly 2 tools registered");
        
        assertTrue(toolRegistry.hasTool("weather"), 
                "Should have weather tool registered");
        
        assertTrue(toolRegistry.hasTool("country_info"), 
                "Should have country_info tool registered");
        
        assertFalse(toolRegistry.getToolNames().isEmpty(), 
                "Tool names list should not be empty");
        
        System.out.println("✓ Tool registry properly configured with: " + 
                String.join(", ", toolRegistry.getToolNames()));
        System.out.println("=".repeat(70) + "\n");
    }
}
