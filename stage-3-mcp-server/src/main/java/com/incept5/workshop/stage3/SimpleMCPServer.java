package com.incept5.workshop.stage3;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.incept5.workshop.stage3.tool.CountryInfoTool;
import com.incept5.workshop.stage3.tool.Tool;
import com.incept5.workshop.stage3.tool.WeatherTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple MCP-compatible server that exposes tools via JSON-RPC 2.0.
 * 
 * This implementation demonstrates the Model Context Protocol concepts:
 * 1. JSON-RPC 2.0 message format over STDIO
 * 2. Tool discovery via tools/list
 * 3. Tool execution via tools/call
 * 4. MCP initialization handshake
 * 
 * While this is a simplified implementation for educational purposes,
 * it demonstrates the core MCP protocol patterns that are used by
 * real MCP servers and clients.
 * 
 * For production use, consider using the official MCP Java SDK:
 * https://github.com/modelcontextprotocol/java-sdk
 */
public class SimpleMCPServer {
    private static final Logger logger = LoggerFactory.getLogger(SimpleMCPServer.class);
    
    private final Map<String, Tool> tools;
    private final Gson gson;
    private final BufferedReader reader;
    private final PrintWriter writer;
    
    /**
     * Creates a new MCP server with default tools.
     * Communication happens over STDIO (standard input/output).
     */
    public SimpleMCPServer() {
        this.tools = new HashMap<>();
        this.gson = new Gson();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new PrintWriter(System.out, true);
        
        // Register our tools
        registerTool(new WeatherTool());
        registerTool(new CountryInfoTool());
        
        logger.info("MCP server created with {} tools", tools.size());
    }
    
    /**
     * Registers a tool with the server.
     */
    private void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
        logger.debug("Registered tool: {}", tool.getName());
    }
    
    /**
     * Runs the MCP server, handling JSON-RPC requests over STDIO.
     */
    public void run() {
        logger.info("Starting MCP server...");
        logger.info("Server is ready to accept connections");
        logger.info("Available tools: {}", tools.keySet());
        
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                handleRequest(line);
            }
        } catch (IOException e) {
            logger.error("Error reading input", e);
        }
        
        logger.info("Server shutting down");
    }
    
    /**
     * Handles a single JSON-RPC request.
     */
    private void handleRequest(String requestJson) {
        Object requestId = null;
        try {
            JsonObject request = gson.fromJson(requestJson, JsonObject.class);
            
            String jsonrpc = request.get("jsonrpc").getAsString();
            String method = request.get("method").getAsString();
            
            // Extract ID - can be string, number, or null
            if (request.has("id")) {
                if (request.get("id").isJsonPrimitive()) {
                    var idElement = request.get("id").getAsJsonPrimitive();
                    if (idElement.isString()) {
                        requestId = idElement.getAsString();
                    } else if (idElement.isNumber()) {
                        requestId = idElement.getAsNumber();
                    }
                } else if (request.get("id").isJsonNull()) {
                    requestId = null;
                }
            }
            
            logger.debug("Received request: method={}, id={}", method, requestId);
            
            JsonObject params = request.has("params") ? 
                    request.getAsJsonObject("params") : new JsonObject();
            
            // Handle the request based on method
            JsonObject response = switch (method) {
                case "initialize" -> handleInitialize(requestId, params);
                case "tools/list" -> handleToolsList(requestId);
                case "tools/call" -> handleToolCall(requestId, params);
                default -> createErrorResponse(requestId, -32601, "Method not found: " + method);
            };
            
            // Send response
            String responseJson = gson.toJson(response);
            writer.println(responseJson);
            writer.flush();
            
        } catch (Exception e) {
            logger.error("Error handling request", e);
            JsonObject error = createErrorResponse(requestId, -32700, 
                    "Parse error: " + e.getMessage());
            writer.println(gson.toJson(error));
            writer.flush();
        }
    }
    
    /**
     * Handles the MCP initialize request.
     */
    private JsonObject handleInitialize(Object id, JsonObject params) {
        logger.info("Handling initialize request");
        
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", "2024-11-05");
        
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "simple-mcp-server");
        serverInfo.addProperty("version", "1.0.0");
        result.add("serverInfo", serverInfo);
        
        JsonObject capabilities = new JsonObject();
        JsonObject toolsCapability = new JsonObject();
        toolsCapability.addProperty("listChanged", false);
        capabilities.add("tools", toolsCapability);
        result.add("capabilities", capabilities);
        
        return createSuccessResponse(id, result);
    }
    
    /**
     * Handles the tools/list request.
     */
    private JsonObject handleToolsList(Object id) {
        logger.info("Handling tools/list request");
        
        List<JsonObject> toolsList = new ArrayList<>();
        
        for (Tool tool : tools.values()) {
            JsonObject toolObj = new JsonObject();
            toolObj.addProperty("name", tool.getName());
            toolObj.addProperty("description", tool.getDescription());
            
            // Parse and add input schema
            JsonObject schema = gson.fromJson(tool.getParameterSchema(), JsonObject.class);
            toolObj.add("inputSchema", schema);
            
            toolsList.add(toolObj);
        }
        
        JsonObject result = new JsonObject();
        result.add("tools", gson.toJsonTree(toolsList));
        
        return createSuccessResponse(id, result);
    }
    
    /**
     * Handles the tools/call request.
     */
    private JsonObject handleToolCall(Object id, JsonObject params) {
        String toolName = params.get("name").getAsString();
        JsonObject arguments = params.has("arguments") ? 
                params.getAsJsonObject("arguments") : new JsonObject();
        
        logger.info("Handling tools/call request: tool={}, args={}", toolName, arguments);
        
        Tool tool = tools.get(toolName);
        if (tool == null) {
            return createErrorResponse(id, -32602, "Unknown tool: " + toolName);
        }
        
        try {
            // Convert JsonObject to Map<String, String>
            Map<String, String> argsMap = new HashMap<>();
            arguments.entrySet().forEach(entry -> 
                    argsMap.put(entry.getKey(), entry.getValue().getAsString()));
            
            // Execute the tool
            String toolResult = tool.execute(argsMap);
            
            // Build MCP response with content array
            JsonObject result = new JsonObject();
            List<JsonObject> contentArray = new ArrayList<>();
            
            JsonObject textContent = new JsonObject();
            textContent.addProperty("type", "text");
            textContent.addProperty("text", toolResult);
            contentArray.add(textContent);
            
            result.add("content", gson.toJsonTree(contentArray));
            result.addProperty("isError", false);
            
            return createSuccessResponse(id, result);
            
        } catch (Tool.ToolExecutionException e) {
            logger.error("Tool execution failed", e);
            
            // Build error response with content array
            JsonObject result = new JsonObject();
            List<JsonObject> contentArray = new ArrayList<>();
            
            JsonObject errorContent = new JsonObject();
            errorContent.addProperty("type", "text");
            errorContent.addProperty("text", "Error: " + e.getMessage());
            contentArray.add(errorContent);
            
            result.add("content", gson.toJsonTree(contentArray));
            result.addProperty("isError", true);
            
            return createSuccessResponse(id, result);
        }
    }
    
    /**
     * Creates a JSON-RPC success response.
     */
    private JsonObject createSuccessResponse(Object id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        
        // ID must always be present in response, matching request ID
        if (id == null) {
            response.add("id", gson.toJsonTree(null));
        } else if (id instanceof Number) {
            response.add("id", gson.toJsonTree(id));
        } else {
            response.addProperty("id", id.toString());
        }
        
        response.add("result", result);
        return response;
    }
    
    /**
     * Creates a JSON-RPC error response.
     */
    private JsonObject createErrorResponse(Object id, int code, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        
        // ID must always be present in response, matching request ID
        if (id == null) {
            response.add("id", gson.toJsonTree(null));
        } else if (id instanceof Number) {
            response.add("id", gson.toJsonTree(id));
        } else {
            response.addProperty("id", id.toString());
        }
        
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        
        return response;
    }
    
    /**
     * Gets the list of registered tools.
     */
    public List<Tool> getTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * Main entry point for the MCP server.
     */
    public static void main(String[] args) {
        SimpleMCPServer server = new SimpleMCPServer();
        server.run();
    }
}
