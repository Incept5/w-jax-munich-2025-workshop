package com.incept5.workshop.stage1.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolCallParser with JSON format.
 */
class ToolCallParserTest {
    
    private final ToolCallParser parser = new ToolCallParser();
    
    @Test
    void testParseValidJsonInCodeBlock() {
        String llmResponse = """
                I need to check the weather for Paris.
                
                ```json
                {
                  "tool": "weather",
                  "parameters": {
                    "city": "Paris"
                  }
                }
                ```
                """;
        
        ToolCallParser.ToolCall toolCall = parser.parse(llmResponse);
        
        assertNotNull(toolCall);
        assertEquals("weather", toolCall.toolName());
        assertEquals(1, toolCall.parameters().size());
        assertEquals("Paris", toolCall.parameters().get("city"));
    }
    
    @Test
    void testParseValidJsonWithoutCodeBlock() {
        String llmResponse = """
                I need to check the weather for Paris.
                {"tool": "weather", "parameters": {"city": "Paris"}}
                """;
        
        ToolCallParser.ToolCall toolCall = parser.parse(llmResponse);
        
        assertNotNull(toolCall);
        assertEquals("weather", toolCall.toolName());
        assertEquals("Paris", toolCall.parameters().get("city"));
    }
    
    @Test
    void testParseMultipleParameters() {
        String llmResponse = """
                ```json
                {
                  "tool": "country_info",
                  "parameters": {
                    "country": "France",
                    "format": "detailed"
                  }
                }
                ```
                """;
        
        ToolCallParser.ToolCall toolCall = parser.parse(llmResponse);
        
        assertNotNull(toolCall);
        assertEquals("country_info", toolCall.toolName());
        assertEquals(2, toolCall.parameters().size());
        assertEquals("France", toolCall.parameters().get("country"));
        assertEquals("detailed", toolCall.parameters().get("format"));
    }
    
    @Test
    void testParseNoToolCall() {
        String llmResponse = "The weather in Paris is sunny today.";
        
        ToolCallParser.ToolCall toolCall = parser.parse(llmResponse);
        
        assertNull(toolCall);
    }
    
    @Test
    void testParseInvalidJson() {
        String llmResponse = """
                ```json
                {
                  "tool": "weather",
                  "parameters": {
                    "city": "Paris"
                  // missing closing brace
                }
                ```
                """;
        
        ToolCallParser.ToolCall toolCall = parser.parse(llmResponse);
        
        assertNull(toolCall);
    }
    
    @Test
    void testParseMissingToolField() {
        String llmResponse = """
                ```json
                {
                  "parameters": {
                    "city": "Paris"
                  }
                }
                ```
                """;
        
        ToolCallParser.ToolCall toolCall = parser.parse(llmResponse);
        
        assertNull(toolCall);
    }
    
    @Test
    void testParseEmptyParameters() {
        String llmResponse = """
                ```json
                {
                  "tool": "list_countries"
                }
                ```
                """;
        
        ToolCallParser.ToolCall toolCall = parser.parse(llmResponse);
        
        assertNotNull(toolCall);
        assertEquals("list_countries", toolCall.toolName());
        assertEquals(0, toolCall.parameters().size());
    }
}
