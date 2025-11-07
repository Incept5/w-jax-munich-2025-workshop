package com.incept5.workshop.stage4.tool;

import java.util.Map;

/**
 * Interface for tools that can be used by the RAG agent.
 * 
 * Tools represent capabilities the agent can invoke to accomplish tasks,
 * such as searching documentation, retrieving information, or performing actions.
 */
public interface Tool {
    
    /**
     * Get the name of this tool.
     * This name is used by the LLM when calling the tool.
     * 
     * @return The tool name (e.g., "search_documentation")
     */
    String name();
    
    /**
     * Get a description of what this tool does.
     * This helps the LLM understand when to use the tool.
     * 
     * @return A concise description of the tool's purpose
     */
    String description();
    
    /**
     * Get the JSON schema for this tool's parameters.
     * This defines what parameters the tool accepts and their types.
     * 
     * @return JSON schema string
     */
    String getParameterSchema();
    
    /**
     * Execute the tool with the given arguments.
     * 
     * @param arguments The tool arguments as a map of parameter names to values
     * @return The tool execution result as a string
     * @throws Exception if the tool execution fails
     */
    String execute(Map<String, Object> arguments) throws Exception;
}
