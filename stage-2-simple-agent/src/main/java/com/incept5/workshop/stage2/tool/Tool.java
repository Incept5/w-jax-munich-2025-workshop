
package com.incept5.workshop.stage2.tool;

import java.util.Map;

/**
 * Simple interface for agent tools.
 * 
 * A tool is a capability that the agent can use to interact with external systems
 * or perform specific operations. Each tool has:
 * - A unique name for identification
 * - A description explaining what it does and its parameters
 * - An execute method that performs the actual work
 * 
 * This is the foundation of tool-based agents - keeping it simple and extensible.
 */
public interface Tool {
    
    /**
     * Returns the unique name of this tool.
     * Used by the agent to identify which tool to call.
     * 
     * @return tool name (e.g., "weather", "country_info")
     */
    String getName();
    
    /**
     * Returns a human-readable description of what this tool does.
     * This description is included in the agent's system prompt to teach
     * the LLM how and when to use this tool.
     * 
     * @return tool description including parameters
     */
    String getDescription();
    
    /**
     * Executes the tool with the provided parameters.
     * 
     * @param parameters map of parameter name to value
     * @return result of the tool execution as a string
     */
    String execute(Map<String, String> parameters);
}
