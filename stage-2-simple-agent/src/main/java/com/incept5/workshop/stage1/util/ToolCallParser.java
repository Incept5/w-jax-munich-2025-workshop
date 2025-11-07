
package com.incept5.workshop.stage1.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses tool calls from LLM responses.
 * 
 * The LLM is taught to format tool calls using JSON within code blocks:
 * 
 * ```json
 * {
 *   "tool": "weather",
 *   "parameters": {
 *     "city": "Paris"
 *   }
 * }
 * ```
 * 
 * This parser extracts the tool name and parameters from such formatted responses.
 */
public class ToolCallParser {
    private static final Logger logger = LoggerFactory.getLogger(ToolCallParser.class);
    private static final Gson gson = new Gson();
    
    // Pattern to match JSON code blocks (with or without language specifier)
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n(.*?)\\n```",
            Pattern.DOTALL
    );
    
    // Pattern to match raw JSON objects (fallback if no code block)
    // This pattern handles nested braces for the parameters object
    private static final Pattern RAW_JSON_PATTERN = Pattern.compile(
            "\\{[^{}]*\"tool\"[^{}]*\\{[^}]*\\}[^}]*\\}",
            Pattern.DOTALL
    );
    
    /**
     * Parses a tool call from an LLM response.
     * 
     * @param llmResponse the complete response from the LLM
     * @return a ToolCall object if a tool use was found, null otherwise
     */
    public ToolCall parse(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return null;
        }
        
        // First, try to find JSON in a code block
        Matcher codeBlockMatcher = JSON_BLOCK_PATTERN.matcher(llmResponse);
        String jsonContent = null;
        
        if (codeBlockMatcher.find()) {
            jsonContent = codeBlockMatcher.group(1).trim();
            logger.debug("Found JSON in code block: {}", jsonContent);
        } else {
            // Fallback: try to find raw JSON object
            Matcher rawJsonMatcher = RAW_JSON_PATTERN.matcher(llmResponse);
            if (rawJsonMatcher.find()) {
                jsonContent = rawJsonMatcher.group(0).trim();
                logger.debug("Found raw JSON object: {}", jsonContent);
            }
        }
        
        if (jsonContent == null) {
            logger.debug("No tool use JSON found in response");
            return null;
        }
        
        // Parse the JSON
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
            
            // Extract tool name
            if (!jsonObject.has("tool")) {
                logger.warn("JSON found but missing 'tool' field");
                return null;
            }
            
            String toolName = jsonObject.get("tool").getAsString();
            logger.debug("Tool name: {}", toolName);
            
            // Extract parameters
            Map<String, String> parameters = new HashMap<>();
            
            if (jsonObject.has("parameters") && jsonObject.get("parameters").isJsonObject()) {
                JsonObject paramsObj = jsonObject.getAsJsonObject("parameters");
                
                // Convert all parameter values to strings
                paramsObj.entrySet().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue().isJsonPrimitive() 
                            ? entry.getValue().getAsString() 
                            : entry.getValue().toString();
                    parameters.put(key, value);
                    logger.debug("Parameter: {} = {}", key, value);
                });
            }
            
            return new ToolCall(toolName, parameters);
            
        } catch (JsonSyntaxException e) {
            logger.warn("Failed to parse JSON tool call: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error parsing tool call: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Represents a parsed tool call.
     * 
     * @param toolName the name of the tool to call
     * @param parameters the parameters to pass to the tool
     */
    public record ToolCall(String toolName, Map<String, String> parameters) {
        @Override
        public String toString() {
            return String.format("ToolCall{tool='%s', params=%s}", toolName, parameters);
        }
    }
}
