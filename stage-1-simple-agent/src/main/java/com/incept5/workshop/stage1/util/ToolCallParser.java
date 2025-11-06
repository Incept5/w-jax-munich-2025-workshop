
package com.incept5.workshop.stage1.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses tool calls from LLM responses.
 * 
 * The LLM is taught to format tool calls using simple XML-like tags:
 * 
 * <tool_use>
 * <tool_name>weather</tool_name>
 * <city>Paris</city>
 * </tool_use>
 * 
 * This parser extracts the tool name and parameters from such formatted responses.
 */
public class ToolCallParser {
    private static final Logger logger = LoggerFactory.getLogger(ToolCallParser.class);
    
    // Pattern to match entire tool_use block
    private static final Pattern TOOL_USE_PATTERN = Pattern.compile(
            "<tool_use>(.*?)</tool_use>",
            Pattern.DOTALL
    );
    
    // Pattern to match tool_name
    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile(
            "<tool_name>(.*?)</tool_name>"
    );
    
    // Pattern to match any parameter tag
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
            "<(\\w+)>(.*?)</\\1>"
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
        
        // Look for <tool_use> block
        Matcher toolUseMatcher = TOOL_USE_PATTERN.matcher(llmResponse);
        
        if (!toolUseMatcher.find()) {
            logger.debug("No tool use found in response");
            return null;
        }
        
        String toolUseContent = toolUseMatcher.group(1);
        logger.debug("Found tool_use block: {}", toolUseContent);
        
        // Extract tool name
        Matcher toolNameMatcher = TOOL_NAME_PATTERN.matcher(toolUseContent);
        if (!toolNameMatcher.find()) {
            logger.warn("Tool use block found but no tool_name");
            return null;
        }
        
        String toolName = toolNameMatcher.group(1).trim();
        logger.debug("Tool name: {}", toolName);
        
        // Extract all parameters (excluding tool_name)
        Map<String, String> parameters = new HashMap<>();
        Matcher paramMatcher = PARAMETER_PATTERN.matcher(toolUseContent);
        
        while (paramMatcher.find()) {
            String paramName = paramMatcher.group(1);
            String paramValue = paramMatcher.group(2).trim();
            
            // Skip tool_name as we already extracted it
            if (!"tool_name".equals(paramName)) {
                parameters.put(paramName, paramValue);
                logger.debug("Parameter: {} = {}", paramName, paramValue);
            }
        }
        
        return new ToolCall(toolName, parameters);
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
