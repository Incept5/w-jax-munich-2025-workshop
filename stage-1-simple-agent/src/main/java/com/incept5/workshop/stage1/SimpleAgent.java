
package com.incept5.workshop.stage1;

import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.exception.AIBackendException;
import com.incept5.ollama.model.AIResponse;
import com.incept5.workshop.stage1.tool.ToolRegistry;
import com.incept5.workshop.stage1.util.ToolCallParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple agent that can reason and use tools to accomplish tasks.
 * 
 * This agent implements the classic agent loop:
 * 1. THINK: Ask the LLM what to do next
 * 2. ACT: If the LLM wants to use a tool, execute it
 * 3. OBSERVE: Add the tool result to the context
 * 4. Repeat until task is complete
 * 
 * The agent uses a simple XML-like format for tool calls, which the LLM
 * learns through the system prompt and examples.
 */
public class SimpleAgent {
    private static final Logger logger = LoggerFactory.getLogger(SimpleAgent.class);
    
    private final AIBackend backend;
    private final ToolRegistry toolRegistry;
    private final ToolCallParser parser;
    private final int maxIterations;
    
    /**
     * Creates a new agent with default settings (max 10 iterations).
     * 
     * @param backend the AI backend to use for reasoning
     * @param toolRegistry the registry of available tools
     */
    public SimpleAgent(AIBackend backend, ToolRegistry toolRegistry) {
        this(backend, toolRegistry, 10);
    }
    
    /**
     * Creates a new agent with custom iteration limit.
     * 
     * @param backend the AI backend to use for reasoning
     * @param toolRegistry the registry of available tools
     * @param maxIterations maximum number of think-act-observe iterations
     */
    public SimpleAgent(AIBackend backend, ToolRegistry toolRegistry, int maxIterations) {
        this.backend = backend;
        this.toolRegistry = toolRegistry;
        this.parser = new ToolCallParser();
        this.maxIterations = maxIterations;
    }
    
    /**
     * Runs the agent on a task.
     * 
     * The agent will iterate through the think-act-observe loop until either:
     * - The LLM provides a final answer (no tool call)
     * - The maximum number of iterations is reached
     * 
     * @param task the task to accomplish
     * @return the agent's response
     * @throws AIBackendException if there's an error communicating with the AI backend
     */
    public AgentResult run(String task) throws AIBackendException {
        return run(task, false);
    }
    
    /**
     * Runs the agent on a task with optional verbose output.
     * 
     * @param task the task to accomplish
     * @param verbose if true, prints each step of the agent loop
     * @return the agent's response
     * @throws AIBackendException if there's an error communicating with the AI backend
     */
    public AgentResult run(String task, boolean verbose) throws AIBackendException {
        logger.info("Starting agent with task: {}", task);
        
        StringBuilder context = new StringBuilder();
        context.append("Task: ").append(task).append("\n\n");
        
        int iterations = 0;
        
        for (int i = 0; i < maxIterations; i++) {
            iterations++;
            
            if (verbose) {
                System.out.println("\n" + "=".repeat(60));
                System.out.println("Iteration " + iterations);
                System.out.println("=".repeat(60));
            }
            
            // THINK: Ask the LLM what to do
            String prompt = buildPrompt(context.toString());
            
            if (verbose) {
                System.out.println("\n[THINKING]");
            }
            
            logger.debug("Sending prompt to LLM (iteration {})", i + 1);
            AIResponse response = backend.generate(prompt, buildSystemPrompt(), null);
            String llmResponse = response.response();
            
            if (verbose) {
                System.out.println("LLM Response:");
                System.out.println("-".repeat(60));
                System.out.println(llmResponse);
                System.out.println("-".repeat(60));
            }
            
            logger.debug("LLM response: {}", llmResponse);
            
            // ACT: Check if LLM wants to use a tool
            ToolCallParser.ToolCall toolCall = parser.parse(llmResponse);
            
            if (toolCall == null) {
                // No tool call - the LLM has provided a final answer
                logger.info("Agent completed task in {} iterations", i + 1);
                return new AgentResult(llmResponse, iterations, true);
            }
            
            if (verbose) {
                System.out.println("\n[ACTING]");
                System.out.println("Tool call: " + toolCall);
            }
            
            logger.info("Tool call detected: {}", toolCall);
            
            // OBSERVE: Execute the tool and add result to context
            String toolResult = toolRegistry.execute(
                    toolCall.toolName(), 
                    toolCall.parameters()
            );
            
            if (verbose) {
                System.out.println("\n[OBSERVING]");
                System.out.println("Tool result:");
                System.out.println("-".repeat(60));
                System.out.println(toolResult);
                System.out.println("-".repeat(60));
            }
            
            logger.info("Tool result: {}", toolResult);
            
            // Add the tool interaction to context
            context.append("Action: Used tool '").append(toolCall.toolName())
                   .append("' with parameters ").append(toolCall.parameters())
                   .append("\n");
            context.append("Observation: ").append(toolResult).append("\n\n");
        }
        
        // Max iterations reached
        logger.warn("Agent reached max iterations ({})", maxIterations);
        String finalResponse = "I've reached the maximum number of iterations (" + 
                maxIterations + ") without completing the task. " +
                "The task may be too complex or require more steps.";
        
        return new AgentResult(finalResponse, iterations, false);
    }
    
    /**
     * Builds the system prompt that teaches the LLM how to use tools.
     */
    private String buildSystemPrompt() {
        return """
                You are a helpful AI agent that can use tools to answer questions.
                
                """ + toolRegistry.getToolDescriptions() + """
                
                To use a tool, output XML like this:
                <tool_use>
                <tool_name>weather</tool_name>
                <city>Paris</city>
                </tool_use>
                
                You can use multiple tools in sequence if needed. For example, to find weather 
                in a country's capital, first use country_info to find the capital, then use 
                weather for that city.
                
                When you have enough information to answer the question, respond normally 
                without any tool tags. Be concise and helpful.
                
                IMPORTANT: Only include ONE tool call per response. After each tool use, 
                you'll receive the result and can decide what to do next.
                """;
    }
    
    /**
     * Builds the prompt for the current iteration.
     */
    private String buildPrompt(String context) {
        return context + "What should we do next?";
    }
    
    /**
     * Result of running the agent.
     * 
     * @param response the final response text
     * @param iterations number of iterations used
     * @param completed whether the task was completed successfully
     */
    public record AgentResult(String response, int iterations, boolean completed) {
        @Override
        public String toString() {
            return String.format(
                    "AgentResult{completed=%s, iterations=%d, response='%s'}",
                    completed, iterations, response
            );
        }
    }
}
