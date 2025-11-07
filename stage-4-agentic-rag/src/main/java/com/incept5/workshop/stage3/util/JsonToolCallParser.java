package com.incept5.workshop.stage3.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for JSON-format tool calls.
 * 
 * Handles LLM responses that may contain JSON in code blocks or raw JSON.
 * This parser is based on the proven approach from Stage 1.
 * 
 * Supported formats:
 * <pre>
 * ```json
 * {
 *   "tool": "search_documentation",
 *   "parameters": {
 *     "query": "how to create an agent",
 *     "topK": 3
 *   }
 * }
 * ```
 * </pre>
 * 
 * Or raw JSON:
 * <pre>
 * {
 *   "tool": "search_documentation",
 *   "parameters": {
 *     "query": "how to create an agent"
 *   }
 * }
 * </pre>
 */
public class JsonToolCallParser {
    private static final Logger logger = LoggerFactory.getLogger(JsonToolCallParser.class);
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
     * Parse a potential tool call from LLM response.
     * 
     * This method first tries to extract JSON from a code block,
     * then falls back to finding raw JSON in the response.
     * 
     * @param response The LLM response text
     * @return Optional containing ToolCall if valid JSON tool call, empty otherwise
     */
    public static Optional<ToolCall> parse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String jsonContent = null;
        
        // First, try to find JSON in a code block
        Matcher codeBlockMatcher = JSON_BLOCK_PATTERN.matcher(response);
        if (codeBlockMatcher.find()) {
            jsonContent = codeBlockMatcher.group(1).trim();
            logger.debug("Found JSON in code block: {}", jsonContent);
        } else {
            // Fallback: try to find raw JSON object
            Matcher rawJsonMatcher = RAW_JSON_PATTERN.matcher(response);
            if (rawJsonMatcher.find()) {
                jsonContent = rawJsonMatcher.group(0).trim();
                logger.debug("Found raw JSON object: {}", jsonContent);
            }
        }
        
        if (jsonContent == null) {
            logger.debug("No JSON tool call found in response");
            return Optional.empty();
        }
        
        // Parse the JSON
        try {
            JsonObject json = gson.fromJson(jsonContent, JsonObject.class);
            
            // Check for required fields
            if (!json.has("tool")) {
                logger.debug("JSON missing 'tool' field");
                return Optional.empty();
            }
            
            if (!json.has("parameters")) {
                logger.debug("JSON missing 'parameters' field");
                return Optional.empty();
            }
            
            String toolName = json.get("tool").getAsString();
            JsonObject paramsJson = json.getAsJsonObject("parameters");
            
            // Convert parameters to Map
            Map<String, Object> parameters = convertJsonToMap(paramsJson);
            
            logger.info("Parsed tool call: {} with {} parameters", toolName, parameters.size());
            return Optional.of(new ToolCall(toolName, parameters));
            
        } catch (JsonSyntaxException e) {
            logger.warn("Failed to parse JSON tool call: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error parsing tool call", e);
            return Optional.empty();
        }
    }
    
    /**
     * Convert JsonObject to Map with proper type handling.
     * 
     * Preserves the actual types of parameters:
     * - Strings remain strings
     * - Integers remain integers
     * - Floats remain floats
     * - Booleans remain booleans
     */
    private static Map<String, Object> convertJsonToMap(JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            
            if (value.isJsonNull()) {
                map.put(key, null);
            } else if (value.isJsonPrimitive()) {
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                
                if (primitive.isString()) {
                    map.put(key, primitive.getAsString());
                } else if (primitive.isNumber()) {
                    // Try to preserve integer vs float
                    Number number = primitive.getAsNumber();
                    if (number.doubleValue() == number.intValue()) {
                        map.put(key, number.intValue());
                    } else {
                        map.put(key, number.doubleValue());
                    }
                } else if (primitive.isBoolean()) {
                    map.put(key, primitive.getAsBoolean());
                }
            } else if (value.isJsonArray() || value.isJsonObject()) {
                // For nested structures, store as string representation
                // Could be enhanced to handle nested structures if needed
                map.put(key, value.toString());
            }
        }
        
        return map;
    }
    
    /**
     * Represents a parsed tool call.
     * 
     * @param name The tool name
     * @param parameters The tool parameters as a map
     */
    public record ToolCall(String name, Map<String, Object> parameters) {
        @Override
        public String toString() {
            return String.format("ToolCall{name='%s', parameters=%s}", name, parameters);
        }
    }
}
