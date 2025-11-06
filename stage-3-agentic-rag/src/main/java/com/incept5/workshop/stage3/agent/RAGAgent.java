package com.incept5.workshop.stage3.agent;

import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.exception.AIBackendException;
import com.incept5.ollama.model.AIResponse;
import com.incept5.workshop.stage3.tool.Tool;
import com.incept5.workshop.stage3.tool.ToolRegistry;
import com.incept5.workshop.stage3.util.JsonToolCallParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * RAG-enabled conversational agent.
 * 
 * Features:
 * - Multi-turn conversations with context retention
 * - JSON-format tool calling (Ollama native)
 * - Vector search for relevant documentation
 * - Configurable conversation history length
 * 
 * Example:
 * <pre>
 * RAGAgent agent = RAGAgent.builder()
 *     .backend(backend)
 *     .toolRegistry(toolRegistry)
 *     .maxConversationHistory(10)
 *     .build();
 * 
 * String response1 = agent.chat("What is Embabel?");
 * String response2 = agent.chat("Show me an example");  // Uses context from first question
 * </pre>
 */
public class RAGAgent {
    private static final Logger logger = LoggerFactory.getLogger(RAGAgent.class);
    
    private final AIBackend backend;
    private final ToolRegistry toolRegistry;
    private final ConversationMemory memory;
    private final int maxIterations;
    private final boolean verbose;
    
    private RAGAgent(Builder builder) {
        this.backend = builder.backend;
        this.toolRegistry = builder.toolRegistry;
        this.memory = new ConversationMemory(
            builder.maxConversationHistory,
            builder.maxTokensEstimate
        );
        this.maxIterations = builder.maxIterations;
        this.verbose = builder.verbose;
    }
    
    /**
     * Process a user message and return the agent's response.
     * Maintains conversation context across multiple turns.
     * 
     * @param userMessage The user's message/question
     * @return The agent's response
     * @throws AIBackendException if there's an error communicating with the AI backend
     */
    public String chat(String userMessage) throws AIBackendException {
        logger.info("Processing user message: {}", userMessage);
        
        // 1. Add user message to conversation memory
        memory.addUserMessage(userMessage);
        
        if (verbose) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("USER: " + userMessage);
            System.out.println("═".repeat(70));
        }
        
        // 2. Agent loop: think → act → observe
        String finalResponse = null;
        
        for (int i = 0; i < maxIterations; i++) {
            if (verbose) {
                System.out.println("\n[Iteration " + (i + 1) + "]");
            }
            
            // Build prompt with conversation history
            String prompt = buildPromptWithHistory();
            
            // Get LLM response
            if (verbose) {
                System.out.println("[THINKING...]");
            }
            
            AIResponse response = backend.generate(prompt, buildSystemPrompt(), null);
            String content = response.response().trim();
            
            if (verbose) {
                System.out.println("\nLLM Response:");
                System.out.println("─".repeat(70));
                System.out.println(content);
                System.out.println("─".repeat(70));
            }
            
            // Check if response is a tool call (JSON format)
            Optional<JsonToolCallParser.ToolCall> toolCall = JsonToolCallParser.parse(content);
            
            if (toolCall.isEmpty()) {
                // Final answer - not a tool call
                finalResponse = content;
                memory.addAssistantMessage(content);
                
                if (verbose) {
                    System.out.println("\n[FINAL ANSWER]");
                }
                
                logger.info("Agent completed in {} iterations", i + 1);
                break;
            }
            
            // Execute tool
            if (verbose) {
                System.out.println("\n[TOOL CALL]");
                System.out.println("Tool: " + toolCall.get().name());
                System.out.println("Parameters: " + toolCall.get().parameters());
            }
            
            logger.info("Executing tool: {} with parameters: {}", 
                toolCall.get().name(), toolCall.get().parameters());
            
            try {
                String toolResult = toolRegistry.execute(
                    toolCall.get().name(),
                    toolCall.get().parameters()
                );
                
                if (verbose) {
                    System.out.println("\n[TOOL RESULT]");
                    System.out.println("─".repeat(70));
                    System.out.println(toolResult.substring(0, Math.min(500, toolResult.length())) + 
                        (toolResult.length() > 500 ? "..." : ""));
                    System.out.println("─".repeat(70));
                }
                
                // Add tool result to memory as system message
                memory.addSystemMessage("Tool '" + toolCall.get().name() + "' result:\n" + toolResult);
                
            } catch (Exception e) {
                logger.error("Tool execution failed", e);
                String errorMsg = "Error executing tool: " + e.getMessage();
                memory.addSystemMessage(errorMsg);
                
                if (verbose) {
                    System.err.println("\n[ERROR] " + errorMsg);
                }
            }
        }
        
        if (finalResponse == null) {
            finalResponse = "I apologize, but I couldn't complete the task within the iteration limit. " +
                           "The question might be too complex or require more steps.";
            memory.addAssistantMessage(finalResponse);
            logger.warn("Agent reached max iterations ({})", maxIterations);
        }
        
        if (verbose) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("ASSISTANT: " + finalResponse);
            System.out.println("═".repeat(70) + "\n");
        }
        
        return finalResponse;
    }
    
    /**
     * Build the system prompt that teaches the LLM how to use tools.
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a helpful assistant specializing in Embabel and Spring AI frameworks.\n\n");
        
        prompt.append("Available Tools:\n");
        prompt.append(toolRegistry.generateToolSchemas());
        prompt.append("\n\n");
        
        prompt.append("To use a tool, respond with ONLY a JSON object in this format:\n");
        prompt.append("{\n");
        prompt.append("  \"tool\": \"tool_name\",\n");
        prompt.append("  \"parameters\": {\n");
        prompt.append("    \"param1\": \"value1\",\n");
        prompt.append("    \"param2\": value2\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        
        prompt.append("Guidelines:\n");
        prompt.append("1. Search documentation when you need specific information about Embabel or Spring AI\n");
        prompt.append("2. Use expandContext: true when looking for code examples or detailed explanations\n");
        prompt.append("3. After receiving tool results, provide a natural, helpful answer to the user\n");
        prompt.append("4. Don't mention that you searched documentation unless specifically asked\n");
        prompt.append("5. For follow-up questions, use the conversation history for context\n");
        prompt.append("6. Be concise but thorough in your answers\n");
        prompt.append("7. If you don't find relevant information, say so honestly\n");
        
        return prompt.toString();
    }
    
    /**
     * Build prompt with conversation history.
     */
    private String buildPromptWithHistory() {
        return memory.formatHistory();
    }
    
    /**
     * Get the conversation history.
     * 
     * @return List of messages in chronological order
     */
    public ConversationMemory.Message[] getConversationHistory() {
        return memory.getHistory().toArray(new ConversationMemory.Message[0]);
    }
    
    /**
     * Clear conversation history.
     */
    public void clearHistory() {
        memory.clear();
        logger.info("Conversation history cleared");
    }
    
    /**
     * Get the number of messages in conversation history.
     */
    public int getHistorySize() {
        return memory.size();
    }
    
    /**
     * Create a new builder for RAGAgent.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for RAGAgent with sensible defaults.
     */
    public static class Builder {
        private AIBackend backend;
        private ToolRegistry toolRegistry;
        private int maxConversationHistory = 10;
        private int maxTokensEstimate = 4000;
        private int maxIterations = 10;
        private boolean verbose = false;
        
        public Builder backend(AIBackend backend) {
            this.backend = backend;
            return this;
        }
        
        public Builder toolRegistry(ToolRegistry toolRegistry) {
            this.toolRegistry = toolRegistry;
            return this;
        }
        
        public Builder maxConversationHistory(int maxConversationHistory) {
            this.maxConversationHistory = maxConversationHistory;
            return this;
        }
        
        public Builder maxTokensEstimate(int maxTokensEstimate) {
            this.maxTokensEstimate = maxTokensEstimate;
            return this;
        }
        
        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }
        
        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }
        
        public RAGAgent build() {
            if (backend == null) {
                throw new IllegalStateException("Backend is required");
            }
            if (toolRegistry == null) {
                throw new IllegalStateException("ToolRegistry is required");
            }
            
            return new RAGAgent(this);
        }
    }
}
