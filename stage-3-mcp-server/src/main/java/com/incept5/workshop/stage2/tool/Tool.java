package com.incept5.workshop.stage2.tool;

import java.util.Map;

/**
 * Tool interface for MCP-compatible tools.
 * 
 * This interface extends the simple tool concept from Stage 1 to be compatible
 * with the Model Context Protocol (MCP). Each tool can be exposed via MCP and
 * used by any MCP-compatible agent or client.
 * 
 * The interface is deliberately simple to allow easy wrapping with MCP-specific
 * functionality while keeping the core tool logic clean and reusable.
 */
public interface Tool {
    
    /**
     * Returns the unique name of this tool.
     * Used by MCP clients to identify which tool to call.
     * 
     * @return tool name (e.g., "weather", "country_info")
     */
    String getName();
    
    /**
     * Returns a human-readable description of what this tool does.
     * This description is exposed via MCP to help agents understand
     * when and how to use this tool.
     * 
     * @return tool description including parameters
     */
    String getDescription();
    
    /**
     * Returns the JSON schema for this tool's parameters.
     * This schema is used by MCP to validate tool calls and help
     * agents understand what parameters are required.
     * 
     * @return JSON schema as a string
     */
    String getParameterSchema();
    
    /**
     * Executes the tool with the provided parameters.
     * 
     * @param parameters map of parameter name to value
     * @return result of the tool execution as a string
     * @throws ToolExecutionException if the tool execution fails
     */
    String execute(Map<String, String> parameters) throws ToolExecutionException;
    
    /**
     * Exception thrown when tool execution fails.
     */
    class ToolExecutionException extends Exception {
        public ToolExecutionException(String message) {
            super(message);
        }
        
        public ToolExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
