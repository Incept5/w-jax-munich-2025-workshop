
package com.incept5.workshop.stage3;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client for communicating with an MCP server via STDIO.
 * 
 * This client handles:
 * - Starting an MCP server as a subprocess
 * - Sending JSON-RPC 2.0 requests over STDIO
 * - Receiving and parsing JSON-RPC responses
 * - Tool discovery and execution
 * 
 * The client maintains a persistent connection to the server process
 * and handles the MCP protocol initialization sequence.
 */
public class MCPClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(MCPClient.class);
    
    private final Process serverProcess;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final Gson gson;
    private final AtomicLong requestIdCounter;
    private final Map<String, MCPTool> availableTools;
    
    /**
     * Creates a new MCP client that starts a server process.
     * 
     * @param serverCommand the command to start the MCP server (e.g., "java")
     * @param serverArgs the arguments for the server command (e.g., ["-jar", "path/to/server.jar"])
     * @throws IOException if the server process cannot be started
     */
    public MCPClient(String serverCommand, String... serverArgs) throws IOException {
        this.gson = new Gson();
        this.requestIdCounter = new AtomicLong(1);
        this.availableTools = new HashMap<>();
        
        logger.info("Starting MCP server: {} {}", serverCommand, String.join(" ", serverArgs));
        
        // Build the command
        List<String> command = new ArrayList<>();
        command.add(serverCommand);
        for (String arg : serverArgs) {
            command.add(arg);
        }
        
        // Start the server process
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT); // Server logs go to our stderr
        this.serverProcess = pb.start();
        
        // Set up communication channels
        this.reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
        this.writer = new PrintWriter(serverProcess.getOutputStream(), true);
        
        logger.info("Server process started");
        
        // Initialize the connection
        initialize();
        
        // Discover available tools
        discoverTools();
    }
    
    /**
     * Sends the MCP initialize request.
     */
    private void initialize() throws IOException {
        logger.info("Sending initialize request");
        
        JsonObject params = new JsonObject();
        params.addProperty("protocolVersion", "2024-11-05");
        
        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("name", "mcp-java-client");
        clientInfo.addProperty("version", "1.0.0");
        params.add("clientInfo", clientInfo);
        
        JsonObject capabilities = new JsonObject();
        params.add("capabilities", capabilities);
        
        JsonObject response = sendRequest("initialize", params);
        
        if (response.has("result")) {
            JsonObject result = response.getAsJsonObject("result");
            logger.info("Initialized with server: {}", 
                    result.has("serverInfo") ? result.getAsJsonObject("serverInfo").get("name") : "unknown");
        } else {
            throw new IOException("Initialize request failed: " + response);
        }
    }
    
    /**
     * Discovers available tools from the server.
     */
    private void discoverTools() throws IOException {
        logger.info("Discovering tools");
        
        JsonObject response = sendRequest("tools/list", new JsonObject());
        
        if (response.has("result")) {
            JsonObject result = response.getAsJsonObject("result");
            JsonArray tools = result.getAsJsonArray("tools");
            
            for (JsonElement toolElement : tools) {
                JsonObject toolObj = toolElement.getAsJsonObject();
                String name = toolObj.get("name").getAsString();
                String description = toolObj.get("description").getAsString();
                JsonObject schema = toolObj.getAsJsonObject("inputSchema");
                
                MCPTool tool = new MCPTool(name, description, schema);
                availableTools.put(name, tool);
                
                logger.info("Discovered tool: {} - {}", name, description);
            }
            
            logger.info("Total tools discovered: {}", availableTools.size());
        } else {
            throw new IOException("Tools list request failed: " + response);
        }
    }
    
    /**
     * Calls a tool on the MCP server.
     * 
     * @param toolName the name of the tool to call
     * @param arguments the arguments to pass to the tool
     * @return the tool result as a string
     * @throws IOException if the request fails
     */
    public String callTool(String toolName, Map<String, String> arguments) throws IOException {
        logger.debug("Calling tool: {} with args: {}", toolName, arguments);
        
        JsonObject params = new JsonObject();
        params.addProperty("name", toolName);
        
        JsonObject argsObj = new JsonObject();
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            argsObj.addProperty(entry.getKey(), entry.getValue());
        }
        params.add("arguments", argsObj);
        
        JsonObject response = sendRequest("tools/call", params);
        
        if (response.has("result")) {
            JsonObject result = response.getAsJsonObject("result");
            
            // Check for error
            if (result.has("isError") && result.get("isError").getAsBoolean()) {
                JsonArray content = result.getAsJsonArray("content");
                if (content.size() > 0) {
                    String errorMessage = content.get(0).getAsJsonObject().get("text").getAsString();
                    throw new IOException("Tool execution error: " + errorMessage);
                }
            }
            
            // Extract text content
            JsonArray content = result.getAsJsonArray("content");
            if (content.size() > 0) {
                return content.get(0).getAsJsonObject().get("text").getAsString();
            } else {
                return "";
            }
        } else if (response.has("error")) {
            JsonObject error = response.getAsJsonObject("error");
            throw new IOException("Tool call failed: " + error.get("message").getAsString());
        } else {
            throw new IOException("Unexpected response format: " + response);
        }
    }
    
    /**
     * Gets the list of available tools.
     * 
     * @return list of tool descriptions
     */
    public List<MCPTool> getAvailableTools() {
        return new ArrayList<>(availableTools.values());
    }
    
    /**
     * Gets a formatted description of all available tools.
     * 
     * @return formatted tool descriptions
     */
    public String getToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available tools:\n\n");
        
        for (MCPTool tool : availableTools.values()) {
            sb.append("Tool: ").append(tool.name()).append("\n");
            sb.append("Description: ").append(tool.description()).append("\n");
            sb.append("Parameters: ").append(formatSchema(tool.inputSchema())).append("\n\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Formats a JSON schema into a readable parameter description.
     */
    private String formatSchema(JsonObject schema) {
        if (!schema.has("properties")) {
            return "none";
        }
        
        StringBuilder sb = new StringBuilder();
        JsonObject properties = schema.getAsJsonObject("properties");
        JsonArray required = schema.has("required") ? schema.getAsJsonArray("required") : new JsonArray();
        
        for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
            String paramName = entry.getKey();
            JsonObject paramSchema = entry.getValue().getAsJsonObject();
            
            sb.append(paramName);
            
            boolean isRequired = false;
            for (JsonElement req : required) {
                if (req.getAsString().equals(paramName)) {
                    isRequired = true;
                    break;
                }
            }
            
            if (isRequired) {
                sb.append(" (required)");
            }
            
            if (paramSchema.has("description")) {
                sb.append(" - ").append(paramSchema.get("description").getAsString());
            }
            
            sb.append("; ");
        }
        
        return sb.toString();
    }
    
    /**
     * Sends a JSON-RPC request and waits for the response.
     */
    private JsonObject sendRequest(String method, JsonObject params) throws IOException {
        long requestId = requestIdCounter.getAndIncrement();
        
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("id", requestId);
        request.addProperty("method", method);
        request.add("params", params);
        
        String requestJson = gson.toJson(request);
        logger.debug("Sending request: {}", requestJson);
        
        writer.println(requestJson);
        writer.flush();
        
        // Read response
        String responseLine = reader.readLine();
        if (responseLine == null) {
            throw new IOException("Server closed connection");
        }
        
        logger.debug("Received response: {}", responseLine);
        
        return gson.fromJson(responseLine, JsonObject.class);
    }
    
    /**
     * Closes the client and terminates the server process.
     */
    @Override
    public void close() {
        logger.info("Closing MCP client");
        
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            logger.warn("Error closing streams", e);
        }
        
        if (serverProcess != null && serverProcess.isAlive()) {
            logger.info("Terminating server process");
            serverProcess.destroy();
            
            try {
                serverProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for server to terminate");
            }
        }
    }
    
    /**
     * Record representing an MCP tool.
     * 
     * @param name the tool name
     * @param description the tool description
     * @param inputSchema the JSON schema for the tool's parameters
     */
    public record MCPTool(String name, String description, JsonObject inputSchema) {
        @Override
        public String toString() {
            return String.format("MCPTool{name='%s', description='%s'}", name, description);
        }
    }
}
