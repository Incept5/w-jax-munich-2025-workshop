
package com.incept5.workshop.stage2;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.exception.AIBackendException;
import com.incept5.ollama.model.AIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI agent that uses tools exposed by an MCP server.
 * 
 * This agent demonstrates how to:
 * 1. Connect to an MCP server and discover tools
 * 2. Use the LLM to reason about when to use tools
 * 3. Execute tools via the MCP protocol
 * 4. Iterate until the task is complete
 * 
 * The agent implements the classic agent loop:
 * - THINK: Ask the LLM what to do next
 * - ACT: If the LLM wants to use a tool, call it via MCP
 * - OBSERVE: Add the tool result to the context
 * - Repeat until task is complete
 */
public class MCPAgent implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(MCPAgent.class);
    
    private final AIBackend backend;
    private final MCPClient mcpClient;
    private final Gson gson;
    private final int maxIterations;
    
    // Pattern to match JSON tool calls in code blocks
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "```json\\s*\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"parameters\"\\s*:\\s*\\{([^}]*)\\}\\s*\\}\\s*```",
            Pattern.DOTALL
    );
    
    /**
     * Creates a new MCP agent with default settings (max 10 iterations).
     * 
     * @param backend the AI backend to use for reasoning
     * @param mcpClient the MCP client for tool execution
     */
    public MCPAgent(AIBackend backend, MCPClient mcpClient) {
        this(backend, mcpClient, 10);
    }
    
    /**
     * Creates a new MCP agent with custom iteration limit.
     * 
     * @param backend the AI backend to use for reasoning
     * @param mcpClient the MCP client for tool execution
     * @param maxIterations maximum number of think-act-observe iterations
     */
    public MCPAgent(AIBackend backend, MCPClient mcpClient, int maxIterations) {
        this.backend = backend;
        this.mcpClient = mcpClient;
        this.gson = new Gson();
        this.maxIterations = maxIterations;
    }
    
    /**
     * Runs the agent on a task.
     * 
     * @param task the task to accomplish
     * @return the agent's result
     * @throws AIBackendException if there's an error with the AI backend
     * @throws IOException if there's an error communicating with the MCP server
     */
    public AgentResult run(String task) throws AIBackendException, IOException {
        return run(task, false);
    }
    
    /**
     * Runs the agent on a task with optional verbose output.
     * 
     * @param task the task to accomplish
     * @param verbose if true, prints each step of the agent loop
     * @return the agent's result
     * @throws AIBackendException if there's an error with the AI backend
     * @throws IOException if there's an error communicating with the MCP server
     */
    public AgentResult run(String task, boolean verbose) throws AIBackendException, IOException {
        logger.info("Starting MCP agent with task: {}", task);
        
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
            ToolCall toolCall = parseToolCall(llmResponse);
            
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
            
            // OBSERVE: Execute the tool via MCP and add result to context
            String toolResult;
            try {
                toolResult = mcpClient.callTool(toolCall.toolName(), toolCall.parameters());
            } catch (IOException e) {
                logger.error("Tool execution failed", e);
                toolResult = "Error executing tool: " + e.getMessage();
            }
            
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
     * Builds the system prompt that teaches the LLM how to use MCP tools.
     */
    private String buildSystemPrompt() {
        return """
                You are a helpful AI agent that can use tools to answer questions.
                
                """ + mcpClient.getToolDescriptions() + """
                
                To use a tool, output JSON in a code block like this:
                ```json
                {
                  "tool": "weather",
                  "parameters": {
                    "city": "Paris"
                  }
                }
                ```
                
                You can use multiple tools in sequence if needed. For example, to find weather 
                in a country's capital, first use country_info to find the capital, then use 
                weather for that city.
                
                When you have the information needed to answer the user's question:
                1. DO NOT output any more JSON tool calls
                2. Provide the answer directly using the information you gathered
                3. Be specific - include the actual data (temperatures, facts, etc.)
                4. Be concise but complete
                
                Example: If asked "What's the weather in Tokyo?" and you get the result 
                "Tokyo: 18°C, Clear", respond with: "The weather in Tokyo is 18°C and clear."
                
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
     * Parses a tool call from the LLM's response.
     * 
     * Expected format:
     * ```json
     * {
     *   "tool": "tool_name",
     *   "parameters": {
     *     "param1": "value1",
     *     "param2": "value2"
     *   }
     * }
     * ```
     * 
     * @param response the LLM's response text
     * @return the parsed tool call, or null if no tool call is found
     */
    private ToolCall parseToolCall(String response) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        
        if (!matcher.find()) {
            return null;
        }
        
        String toolName = matcher.group(1);
        String parametersJson = matcher.group(2);
        
        // Parse parameters
        Map<String, String> parameters = new HashMap<>();
        
        // Simple parameter parsing - handles "key": "value" pairs
        Pattern paramPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
        Matcher paramMatcher = paramPattern.matcher(parametersJson);
        
        while (paramMatcher.find()) {
            String key = paramMatcher.group(1);
            String value = paramMatcher.group(2);
            parameters.put(key, value);
        }
        
        return new ToolCall(toolName, parameters);
    }
    
    /**
     * Closes the agent and its MCP client.
     */
    @Override
    public void close() {
        logger.info("Closing MCP agent");
        mcpClient.close();
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
    
    /**
     * Represents a tool call parsed from the LLM's response.
     * 
     * @param toolName the name of the tool to call
     * @param parameters the parameters to pass to the tool
     */
    private record ToolCall(String toolName, Map<String, String> parameters) {
        @Override
        public String toString() {
            return String.format("ToolCall{tool='%s', parameters=%s}", toolName, parameters);
        }
    }
}
