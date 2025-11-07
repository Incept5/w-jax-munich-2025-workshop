package com.incept5.workshop.stage1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * SimpleFunctionCalling - A minimal example of AI function calling with Ollama
 *
 * This demonstrates the 3-step process of function calling:
 * 1. Send user message with available tools/functions
 * 2. Model decides to call a function and returns parameters
 * 3. Execute function and send results back to model for final response
 */
public class SimpleFunctionCalling {

    private static final String OLLAMA_URL = getOllamaBaseUrl() + "/api/chat";
    
    /**
     * Get Ollama base URL from environment or system property
     */
    private static String getOllamaBaseUrl() {
        String url = System.getProperty("ollama.base.url");
        if (url != null && !url.isBlank()) {
            return url;
        }
        
        url = System.getenv("OLLAMA_BASE_URL");
        if (url != null && !url.isBlank()) {
            return url;
        }
        
        return "http://localhost:11434";
    }
    private static final String MODEL = "granite4";

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Function Calling Demo with Ollama\n");

        // Test currency conversion (will use function)
        chat("Convert 100 USD to GBP");

        // Test regular question (no function needed)
        chat("What is the capital of France?");
    }

    /**
     * Main chat function that handles the complete function calling flow
     */
    public static void chat(String userMessage) throws Exception {
        System.out.println("💬 USER: " + userMessage);
        System.out.println("=".repeat(60));

        HttpClient client = HttpClient.newHttpClient();

        // STEP 1: Send initial request with tools defined
        System.out.println("📤 Step 1: Sending request with available tools...");
        String initialRequest = buildInitialRequest(userMessage);
        String response1 = sendRequest(client, initialRequest);

        // STEP 2: Check if model wants to call a function
        if (response1.contains("\"tool_calls\"")) {
            System.out.println("🎯 Step 2: Model wants to call a function!");

            // Check which function is being called
            String functionName = extractString(response1, "name");
            System.out.println("   Function: " + functionName);

            String functionResult;
            if ("convert_currency".equals(functionName)) {
                // Parse function call parameters
                double amount = extractNumber(response1, "amount");
                String from = extractString(response1, "from_currency");
                String to = extractString(response1, "to_currency");

                System.out.println("   Arguments: amount=" + amount + ", from=" + from + ", to=" + to);

                // Execute the function
                functionResult = convertCurrency(amount, from, to);
                System.out.println("   Result: " + functionResult);
            } else {
                functionResult = "Error: Unknown function '" + functionName + "'";
                System.out.println("   ⚠️  " + functionResult);
            }

            // STEP 3: Send function result back to model
            System.out.println("📤 Step 3: Sending function result back to model...");
            String assistantMessage = extractMessage(response1);
            String followUpRequest = buildFollowUpRequest(userMessage, assistantMessage, functionResult);
            String response2 = sendRequest(client, followUpRequest);

            // Extract final answer
            String finalAnswer = extractString(response2, "content");
            System.out.println("🤖 ASSISTANT: " + finalAnswer);

        } else {
            // No function call needed - direct response
            String answer = extractString(response1, "content");
            System.out.println("🤖 ASSISTANT: " + answer);
        }

        System.out.println("\n" + "=".repeat(60) + "\n");
    }

    /**
     * The actual currency conversion function
     */
    private static String convertCurrency(double amount, String from, String to) {
        // Simple exchange rates (hardcoded for demo)
        double rate = switch(from + "-" + to) {
            case "USD-GBP" -> 0.79;
            case "USD-EUR" -> 0.91;
            case "EUR-USD" -> 1.10;
            case "GBP-USD" -> 1.27;
            default -> 1.0;
        };

        double result = amount * rate;
        return String.format("%.2f %s = %.2f %s (rate: %.2f)", amount, from, result, to, rate);
    }

    /**
     * Build initial request with tools definition
     */
    private static String buildInitialRequest(String userMessage) {
        String escapedMessage = escape(userMessage);
        String systemPrompt = escape("You are a helpful assistant. Provide simple, concise, and non-verbose responses. Be direct and to the point.");
        return """
            {
              "model": "%s",
              "messages": [
                {"role": "system", "content": "%s"},
                {"role": "user", "content": "%s"}
              ],
              "tools": [
                {
                  "type": "function",
                  "function": {
                    "name": "convert_currency",
                    "description": "Convert an amount from one currency to another",
                    "parameters": {
                      "type": "object",
                      "properties": {
                        "amount": {"type": "number", "description": "Amount to convert"},
                        "from_currency": {"type": "string", "description": "Source currency code"},
                        "to_currency": {"type": "string", "description": "Target currency code"}
                      },
                      "required": ["amount", "from_currency", "to_currency"]
                    }
                  }
                }
              ],
              "stream": false
            }
            """.formatted(MODEL, systemPrompt, escapedMessage);
    }

    /**
     * Build follow-up request with function result
     */
    private static String buildFollowUpRequest(String userMessage, String assistantMessage, String functionResult) {
        String escapedMessage = escape(userMessage);
        String escapedResult = escape(functionResult);
        String systemPrompt = escape("You are a helpful assistant. Provide simple, concise, and non-verbose responses. Be direct and to the point.");
        return """
            {
              "model": "%s",
              "messages": [
                {"role": "system", "content": "%s"},
                {"role": "user", "content": "%s"},
                %s,
                {"role": "tool", "content": "%s"}
              ],
              "stream": false
            }
            """.formatted(MODEL, systemPrompt, escapedMessage, assistantMessage, escapedResult);
    }

    /**
     * Send HTTP POST request to Ollama
     */
    private static String sendRequest(HttpClient client, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Extract the full assistant message object from response
     */
    private static String extractMessage(String json) {
        int start = json.indexOf("\"message\":{");
        if (start == -1) return "{}";

        start = json.indexOf('{', start + 10);
        int braceCount = 1;
        int end = start + 1;

        while (braceCount > 0 && end < json.length()) {
            if (json.charAt(end) == '{') braceCount++;
            if (json.charAt(end) == '}') braceCount--;
            end++;
        }

        return json.substring(start, end);
    }

    /**
     * Extract a string value from JSON
     */
    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;

        start += pattern.length();
        int end = start;

        // Find closing quote, handling escaped quotes
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') {
                break;
            }
            end++;
        }

        return json.substring(start, end);
    }

    /**
     * Extract a number value from JSON
     */
    private static double extractNumber(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0;

        start += pattern.length();
        int end = start;

        // Find end of number (comma, space, or brace)
        while (end < json.length() && "0123456789.".contains("" + json.charAt(end))) {
            end++;
        }

        return Double.parseDouble(json.substring(start, end));
    }

    /**
     * Escape special characters for JSON strings
     */
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
