
package com.incept5.workshop.stage1;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.model.AIResponse;
import com.incept5.workshop.stage1.tool.FunctionTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for interacting with Ollama API with function calling support.
 * 
 * This client extends the basic Ollama functionality with support for
 * function/tool definitions and execution, allowing LLMs to call
 * registered functions during their response generation.
 */
public class FunctionCallingClient {
    private static final Logger logger = LoggerFactory.getLogger(FunctionCallingClient.class);

    private final HttpClient httpClient;
    private final Gson gson;
    private final Map<String, FunctionTool> availableFunctions;
    private final String chatEndpoint;

    /**
     * Creates a function calling client.
     *
     * @param baseUrl The Ollama base URL (e.g., "http://localhost:11434")
     */
    public FunctionCallingClient(String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(300))
                .build();
        this.gson = new Gson();
        this.availableFunctions = new HashMap<>();
        this.chatEndpoint = baseUrl + "/api/chat";
    }

    /**
     * Register a function tool.
     *
     * @param tool The function tool to register
     */
    public void registerFunction(FunctionTool tool) {
        availableFunctions.put(tool.getName(), tool);
        logger.debug("Registered function: {}", tool.getName());
    }

    /**
     * Result of a function calling test.
     */
    public static class FunctionCallResult {
        private final String model;
        private final String prompt;
        private final boolean success;
        private final String error;
        private final JsonObject rawResponse;
        private final List<FunctionCallDetail> functionDetails;

        public FunctionCallResult(String model, String prompt, boolean success, String error,
                                  JsonObject rawResponse, List<FunctionCallDetail> functionDetails) {
            this.model = model;
            this.prompt = prompt;
            this.success = success;
            this.error = error;
            this.rawResponse = rawResponse;
            this.functionDetails = functionDetails != null ? functionDetails : new ArrayList<>();
        }

        public String getModel() {
            return model;
        }

        public String getPrompt() {
            return prompt;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }

        public JsonObject getRawResponse() {
            return rawResponse;
        }

        public List<FunctionCallDetail> getFunctionDetails() {
            return functionDetails;
        }
    }

    /**
     * Details of a function call.
     */
    public static class FunctionCallDetail {
        private final String name;
        private final String args;
        private final String result;

        public FunctionCallDetail(String name, String args, String result) {
            this.name = name;
            this.args = args;
            this.result = result;
        }

        public String getName() {
            return name;
        }

        public String getArgs() {
            return args;
        }

        public String getResult() {
            return result;
        }

        @Override
        public String toString() {
            return name + "(" + args + ") --> " + result;
        }
    }

    /**
     * Send a prompt with function calling support.
     *
     * @param model          The model name
     * @param prompt         The user prompt
     * @param expectedResult The expected result (for testing)
     * @param temperature    The temperature setting
     * @return FunctionCallResult containing the test results
     */
    public FunctionCallResult sendPromptWithFunctions(String model, String prompt,
                                                       String expectedResult, double temperature) {
        List<FunctionCallDetail> functionDetails = new ArrayList<>();
        String error = null;
        JsonObject rawResponse = null;
        boolean success = false;

        try {
            // Build the request
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("stream", false);

            // Add messages
            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);
            requestBody.add("messages", messages);

            // Add tools
            JsonArray tools = new JsonArray();
            for (FunctionTool tool : availableFunctions.values()) {
                tools.add(tool.toJson());
            }
            requestBody.add("tools", tools);

            // Add options
            JsonObject options = new JsonObject();
            options.addProperty("temperature", temperature);
            requestBody.add("options", options);

            String requestBodyJson = gson.toJson(requestBody);
            logger.debug("Request: {}", requestBodyJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chatEndpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(300))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                error = "HTTP " + response.statusCode() + ": " + response.body();
                logger.debug("Error response: {}", error);
                return new FunctionCallResult(model, prompt, false, error, null, functionDetails);
            }

            rawResponse = gson.fromJson(response.body(), JsonObject.class);

            // Check if the message has tool calls
            if (rawResponse.has("message")) {
                JsonObject messageNode = rawResponse.getAsJsonObject("message");
                if (messageNode.has("tool_calls")) {
                    JsonArray toolCalls = messageNode.getAsJsonArray("tool_calls");

                    String lastResult = null;

                    for (JsonElement toolCallElement : toolCalls) {
                        JsonObject toolCall = toolCallElement.getAsJsonObject();
                        if (toolCall.has("function")) {
                            JsonObject functionNode = toolCall.getAsJsonObject("function");
                            String functionName = functionNode.get("name").getAsString();
                            JsonObject argsNode = functionNode.getAsJsonObject("arguments");

                            FunctionTool tool = availableFunctions.get(functionName);
                            if (tool != null) {
                                // Execute the function
                                String functionResult = tool.execute(argsNode);
                                lastResult = functionResult;

                                // Format arguments for display
                                StringBuilder argsStr = new StringBuilder();
                                for (Map.Entry<String, JsonElement> entry : argsNode.entrySet()) {
                                    if (argsStr.length() > 0) argsStr.append(", ");
                                    argsStr.append(entry.getKey()).append("=").append(entry.getValue().getAsString());
                                }

                                functionDetails.add(new FunctionCallDetail(functionName, argsStr.toString(), functionResult));
                            }
                        }
                    }

                    // Check if the final result matches expected
                    if (lastResult != null && lastResult.equals(expectedResult)) {
                        success = true;
                    }
                }
            }

        } catch (Exception e) {
            error = e.getMessage();
            logger.debug("Error in function call", e);
        }

        return new FunctionCallResult(model, prompt, success, error, rawResponse, functionDetails);
    }
}
