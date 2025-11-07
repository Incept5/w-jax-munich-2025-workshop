package com.incept5.workshop.stage3;

import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.backend.BackendFactory;
import com.incept5.ollama.backend.BackendType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Arrays;

/**
 * Demo application for MCP Server and Agent.
 * 
 * This class demonstrates three modes:
 * 1. Server Mode: Start an MCP server that exposes tools
 * 2. Agent Mode: Run an agent that uses MCP tools
 * 3. Interactive Mode: Chat with the agent interactively
 * 
 * Usage:
 *   # Start MCP server (for MCP Inspector or Claude Desktop)
 *   java -jar stage-2-mcp-server.jar server
 * 
 *   # Run agent with a single task
 *   java -jar stage-2-mcp-server.jar agent "What's the weather in Tokyo?"
 * 
 *   # Interactive chat mode
 *   java -jar stage-2-mcp-server.jar interactive
 * 
 *   # Default (no args) starts the server
 *   java -jar stage-2-mcp-server.jar
 */
public class MCPDemo {
    private static final Logger logger = LoggerFactory.getLogger(MCPDemo.class);
    
    public static void main(String[] args) {
        // Determine mode from arguments
        String mode = args.length > 0 ? args[0] : "server";
        
        switch (mode.toLowerCase()) {
            case "server" -> runServer();
            case "agent" -> runAgent(args);
            case "interactive" -> runInteractive();
            default -> {
                printUsage();
                System.exit(1);
            }
        }
    }
    
    /**
     * Runs the MCP server mode.
     */
    private static void runServer() {
        logger.info("=".repeat(60));
        logger.info("Simple MCP Server Demo");
        logger.info("=".repeat(60));
        logger.info("");
        logger.info("This server exposes tools via the Model Context Protocol (MCP).");
        logger.info("It communicates using JSON-RPC 2.0 over STDIO.");
        logger.info("");
        logger.info("Available tools:");
        logger.info("  - weather: Get weather information for a city");
        logger.info("  - country_info: Get information about a country");
        logger.info("");
        logger.info("Test with MCP Inspector:");
        logger.info("  npx @modelcontextprotocol/inspector java -jar target/stage-2-mcp-server.jar");
        logger.info("");
        logger.info("Or integrate with Claude Desktop by adding to claude_desktop_config.json:");
        logger.info("  \"mcpServers\": {");
        logger.info("    \"workshop\": {");
        logger.info("      \"command\": \"java\",");
        logger.info("      \"args\": [\"-jar\", \"/path/to/stage-2-mcp-server.jar\", \"server\"]");
        logger.info("    }");
        logger.info("  }");
        logger.info("");
        logger.info("=".repeat(60));
        logger.info("Starting server...");
        logger.info("=".repeat(60));
        
        // Create and run the MCP server
        SimpleMCPServer server = new SimpleMCPServer();
        server.run();
    }
    
    /**
     * Runs the agent mode with a single task.
     */
    private static void runAgent(String[] args) {
        if (args.length < 2) {
            System.err.println("Error: Agent mode requires a task argument");
            System.err.println("Usage: java -jar stage-2-mcp-server.jar agent \"Your task here\"");
            System.exit(1);
        }
        
        // Join all remaining args as the task
        String task = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        System.out.println("=".repeat(60));
        System.out.println("MCP Agent Demo");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Task: " + task);
        System.out.println();
        
        // Check for --verbose flag
        boolean verbose = Arrays.asList(args).contains("--verbose") || 
                         Arrays.asList(args).contains("-v");
        
        try {
            // Create AI backend
            AIBackend backend = BackendFactory.createBackend(
                    BackendType.OLLAMA,
                    "http://localhost:11434",
                    "incept5/Jan-v1-2509:fp16",
                    Duration.ofSeconds(300)
            );
            
            // Start MCP server as subprocess
            String jarPath = "target/stage-2-mcp-server.jar";
            MCPClient mcpClient = new MCPClient("java", "-jar", jarPath, "server");
            
            // Create and run agent
            try (MCPAgent agent = new MCPAgent(backend, mcpClient)) {
                MCPAgent.AgentResult result = agent.run(task, verbose);
                
                System.out.println();
                System.out.println("=".repeat(60));
                System.out.println("Final Answer:");
                System.out.println("=".repeat(60));
                System.out.println(result.response());
                System.out.println();
                System.out.println("Completed in " + result.iterations() + " iterations");
            }
            
            backend.close();
            
        } catch (Exception e) {
            System.err.println("Error running agent: " + e.getMessage());
            logger.error("Agent execution failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Runs the interactive chat mode.
     */
    private static void runInteractive() {
        System.out.println("=".repeat(60));
        System.out.println("MCP Agent - Interactive Mode");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("This mode lets you chat with an AI agent that can use MCP tools.");
        System.out.println("Type 'exit' or 'quit' to end the session.");
        System.out.println();
        
        try {
            // Create AI backend
            AIBackend backend = BackendFactory.createBackend(
                    BackendType.OLLAMA,
                    "http://localhost:11434",
                    "incept5/Jan-v1-2509:fp16",
                    Duration.ofSeconds(300)
            );
            
            // Start MCP server as subprocess
            String jarPath = "target/stage-2-mcp-server.jar";
            MCPClient mcpClient = new MCPClient("java", "-jar", jarPath, "server");
            
            System.out.println("Connected to MCP server");
            System.out.println("Available tools:");
            for (MCPClient.MCPTool tool : mcpClient.getAvailableTools()) {
                System.out.println("  - " + tool.name() + ": " + tool.description());
            }
            System.out.println();
            
            // Create agent
            try (MCPAgent agent = new MCPAgent(backend, mcpClient);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                
                while (true) {
                    System.out.print("You: ");
                    String input = reader.readLine();
                    
                    if (input == null || input.trim().equalsIgnoreCase("exit") || 
                        input.trim().equalsIgnoreCase("quit")) {
                        System.out.println("Goodbye!");
                        break;
                    }
                    
                    if (input.trim().isEmpty()) {
                        continue;
                    }
                    
                    System.out.println();
                    
                    try {
                        MCPAgent.AgentResult result = agent.run(input, false);
                        System.out.println("Agent: " + result.response());
                        System.out.println();
                    } catch (Exception e) {
                        System.err.println("Error: " + e.getMessage());
                        logger.error("Error processing request", e);
                    }
                }
            }
            
            backend.close();
            
        } catch (Exception e) {
            System.err.println("Error in interactive mode: " + e.getMessage());
            logger.error("Interactive mode failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Prints usage information.
     */
    private static void printUsage() {
        System.out.println("Usage: java -jar stage-2-mcp-server.jar [mode] [options]");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  server              Start MCP server (default)");
        System.out.println("  agent <task>        Run agent with a single task");
        System.out.println("  interactive         Start interactive chat mode");
        System.out.println();
        System.out.println("Agent mode options:");
        System.out.println("  --verbose, -v       Show detailed agent reasoning");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Start server for MCP Inspector");
        System.out.println("  java -jar stage-2-mcp-server.jar server");
        System.out.println();
        System.out.println("  # Run agent with task");
        System.out.println("  java -jar stage-2-mcp-server.jar agent \"What's the weather in Paris?\"");
        System.out.println();
        System.out.println("  # Interactive mode");
        System.out.println("  java -jar stage-2-mcp-server.jar interactive");
    }
}
