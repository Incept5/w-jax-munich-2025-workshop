package com.incept5.workshop.stage3.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for managing available tools.
 * 
 * The registry maintains a collection of tools that can be used by the agent.
 * It handles tool lookup, execution, and schema generation for the LLM.
 * 
 * Example:
 * <pre>
 * ToolRegistry registry = new ToolRegistry();
 * registry.register(new RAGTool(vectorStore));
 * 
 * String result = registry.execute("search_documentation", Map.of(
 *     "query", "how to create an agent"
 * ));
 * </pre>
 */
public class ToolRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    
    private final Map<String, Tool> tools;
    
    /**
     * Creates an empty tool registry.
     */
    public ToolRegistry() {
        this.tools = new HashMap<>();
    }
    
    /**
     * Register a tool in the registry.
     * 
     * @param tool The tool to register
     * @throws IllegalArgumentException if a tool with the same name already exists
     */
    public void register(Tool tool) {
        String name = tool.name();
        
        if (tools.containsKey(name)) {
            throw new IllegalArgumentException("Tool already registered: " + name);
        }
        
        tools.put(name, tool);
        logger.info("Registered tool: {}", name);
    }
    
    /**
     * Get a tool by name.
     * 
     * @param name The tool name
     * @return Optional containing the tool if found, empty otherwise
     */
    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }
    
    /**
     * Execute a tool with the given arguments.
     * 
     * @param name The tool name
     * @param arguments The tool arguments
     * @return The tool execution result
     * @throws IllegalArgumentException if the tool is not found
     * @throws Exception if the tool execution fails
     */
    public String execute(String name, Map<String, Object> arguments) throws Exception {
        Tool tool = tools.get(name);
        
        if (tool == null) {
            String error = "Unknown tool: " + name;
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        
        logger.info("Executing tool: {} with arguments: {}", name, arguments);
        
        try {
            String result = tool.execute(arguments);
            logger.debug("Tool execution successful, result length: {}", result.length());
            return result;
        } catch (Exception e) {
            logger.error("Tool execution failed for {}: {}", name, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get all registered tool names.
     * 
     * @return Array of tool names
     */
    public String[] getToolNames() {
        return tools.keySet().toArray(new String[0]);
    }
    
    /**
     * Get the number of registered tools.
     * 
     * @return Tool count
     */
    public int size() {
        return tools.size();
    }
    
    /**
     * Generate JSON schema for all registered tools.
     * This is used to teach the LLM what tools are available.
     * 
     * @return JSON array of tool schemas
     */
    public String generateToolSchemas() {
        StringBuilder schemas = new StringBuilder();
        schemas.append("[\n");
        
        int i = 0;
        for (Tool tool : tools.values()) {
            if (i > 0) {
                schemas.append(",\n");
            }
            
            schemas.append("  {\n");
            schemas.append("    \"name\": \"").append(tool.name()).append("\",\n");
            schemas.append("    \"description\": \"").append(escapeJson(tool.description())).append("\",\n");
            schemas.append("    \"parameters\": ").append(tool.getParameterSchema()).append("\n");
            schemas.append("  }");
            
            i++;
        }
        
        schemas.append("\n]");
        return schemas.toString();
    }
    
    /**
     * Generate a human-readable description of all tools.
     * 
     * @return Formatted tool descriptions
     */
    public String getToolDescriptions() {
        StringBuilder descriptions = new StringBuilder();
        descriptions.append("Available tools:\n\n");
        
        for (Tool tool : tools.values()) {
            descriptions.append("- ").append(tool.name()).append(": ")
                       .append(tool.description()).append("\n");
        }
        
        return descriptions.toString();
    }
    
    /**
     * Escape special characters for JSON strings.
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}
