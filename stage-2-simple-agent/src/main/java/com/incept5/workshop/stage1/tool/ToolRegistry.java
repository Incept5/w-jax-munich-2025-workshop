
package com.incept5.workshop.stage1.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for managing available tools.
 * 
 * The registry maintains a collection of tools that the agent can use.
 * It provides methods to:
 * - Register new tools
 * - Look up tools by name
 * - Execute tools with parameters
 * - Generate descriptions of all available tools (for the system prompt)
 */
public class ToolRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    
    private final Map<String, Tool> tools;
    
    public ToolRegistry() {
        this.tools = new HashMap<>();
    }
    
    /**
     * Registers a tool in the registry.
     * 
     * @param tool the tool to register
     * @return this registry for method chaining
     */
    public ToolRegistry register(Tool tool) {
        String name = tool.getName();
        logger.debug("Registering tool: {}", name);
        tools.put(name, tool);
        return this;
    }
    
    /**
     * Checks if a tool with the given name is registered.
     * 
     * @param toolName the name of the tool
     * @return true if the tool exists, false otherwise
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
    
    /**
     * Gets a tool by name.
     * 
     * @param toolName the name of the tool
     * @return the tool, or null if not found
     */
    public Tool getTool(String toolName) {
        return tools.get(toolName);
    }
    
    /**
     * Executes a tool with the given parameters.
     * 
     * @param toolName the name of the tool to execute
     * @param parameters the parameters to pass to the tool
     * @return the tool's result, or an error message if the tool doesn't exist
     */
    public String execute(String toolName, Map<String, String> parameters) {
        Tool tool = tools.get(toolName);
        
        if (tool == null) {
            logger.warn("Attempted to execute unknown tool: {}", toolName);
            return String.format("Error: Unknown tool '%s'. Available tools: %s", 
                    toolName, String.join(", ", tools.keySet()));
        }
        
        logger.info("Executing tool: {} with parameters: {}", toolName, parameters);
        
        try {
            return tool.execute(parameters);
        } catch (Exception e) {
            logger.error("Tool execution failed for {}: {}", toolName, e.getMessage(), e);
            return String.format("Error executing tool '%s': %s", toolName, e.getMessage());
        }
    }
    
    /**
     * Gets a set of all registered tool names.
     * 
     * @return set of tool names
     */
    public Set<String> getToolNames() {
        return tools.keySet();
    }
    
    /**
     * Generates a description of all available tools.
     * This is used in the agent's system prompt to teach the LLM
     * what tools are available and how to use them.
     * 
     * @return formatted description of all tools
     */
    public String getToolDescriptions() {
        if (tools.isEmpty()) {
            return "No tools available.";
        }
        
        StringBuilder descriptions = new StringBuilder();
        descriptions.append("Available tools:\n\n");
        
        for (Tool tool : tools.values()) {
            descriptions.append("- ").append(tool.getName()).append("\n");
            descriptions.append("  ").append(tool.getDescription().replace("\n", "\n  "));
            descriptions.append("\n\n");
        }
        
        return descriptions.toString();
    }
    
    /**
     * Returns the number of registered tools.
     * 
     * @return number of tools
     */
    public int size() {
        return tools.size();
    }
}
