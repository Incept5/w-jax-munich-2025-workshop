
package com.incept5.workshop.stage1.tool;

import com.google.gson.JsonObject;

/**
 * Interface for function tools that can be called by LLMs.
 * 
 * This interface defines the contract for tools that can be registered
 * with the Ollama function calling system. Each tool must provide:
 * - A unique name
 * - A description of what it does
 * - A JSON schema describing its parameters
 * - An execution method that takes parameters and returns a result
 */
public interface FunctionTool {
    /**
     * Get the name of the function.
     * This name will be used by the LLM when calling the function.
     *
     * @return The function name (e.g., "get_current_day")
     */
    String getName();

    /**
     * Get the description of the function.
     * This helps the LLM understand when to use this function.
     *
     * @return The function description
     */
    String getDescription();

    /**
     * Get the JSON schema for the function parameters.
     * This defines what parameters the function accepts, their types,
     * and which ones are required.
     *
     * @return JsonObject representing the parameters schema
     */
    JsonObject getParametersSchema();

    /**
     * Execute the function with the given arguments.
     * This is where the actual function logic is implemented.
     *
     * @param arguments The arguments to pass to the function
     * @return The result of the function execution as a string
     * @throws Exception If there's an error executing the function
     */
    String execute(JsonObject arguments) throws Exception;

    /**
     * Get the JSON representation of the tool for Ollama API.
     * This method converts the tool definition into the format
     * expected by Ollama's function calling API.
     *
     * @return JsonObject representing the tool definition
     */
    JsonObject toJson();
}
