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

/**
 * Parser for JSON-format tool calls.
 * 
 * Ollama natively supports JSON tool calling format:
 * <pre>
 * {
 *   "tool": "search_documentation",
 *   "parameters": {
 *     "query": "how to create an agent",
 *     "topK": 3
 *   }
 * }
 * </pre>
 * 
 * This is cleaner than XML format used in Stage 1.
 */
public class JsonToolCallParser {
    private static final Logger logger = LoggerFactory.getLogger(JsonToolCallParser.class);
    private static final Gson gson = new Gson();
    
    /**
     * Parse a potential tool call from LLM response.
     * 
     * @param response The LLM response text
     * @return Optional containing ToolCall if valid JSON tool call, empty otherwise
     */
    public static Optional<ToolCall> parse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String trimmed = response.trim();
        
        // Quick check: does it look like JSON?
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            logger.debug("Response doesn't look like JSON tool call");
            return Optional.empty();
        }
        
        try {
            // Try to parse as JSON
            JsonObject json = gson.fromJson(trimmed, JsonObject.class);
            
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
            
            logger.debug("Parsed tool call: {} with {} parameters", toolName, parameters.size());
            return Optional.of(new ToolCall(toolName, parameters));
            
        } catch (JsonSyntaxException e) {
            logger.debug("Failed to parse as JSON: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Unexpected error parsing tool call", e);
            return Optional.empty();
        }
    }
    
    /**
     * Convert JsonObject to Map with proper type handling.
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
                // For now, just store as string representation
                // Could be enhanced to handle nested structures
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
