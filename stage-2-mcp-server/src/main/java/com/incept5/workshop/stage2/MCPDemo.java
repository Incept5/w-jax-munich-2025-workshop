package com.incept5.workshop.stage2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo application for the Simple MCP Server.
 * 
 * This class demonstrates how to:
 * 1. Start an MCP server
 * 2. Expose tools via the Model Context Protocol
 * 3. Handle requests from MCP clients
 * 
 * The server communicates via STDIO (standard input/output) using JSON-RPC 2.0,
 * making it compatible with any MCP client that supports STDIO transport.
 * 
 * Usage:
 *   java -jar stage-2-mcp-server.jar
 * 
 * Or with Maven:
 *   mvn exec:java -Dexec.mainClass="com.incept5.workshop.stage2.MCPDemo"
 * 
 * The server will start and wait for JSON-RPC messages on STDIN.
 * 
 * Example interaction (using the MCP Inspector or Claude Desktop):
 * 
 * 1. Initialize:
 *    → {"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}
 *    ← {"jsonrpc":"2.0","id":"1","result":{"protocolVersion":"2024-11-05",...}}
 * 
 * 2. List tools:
 *    → {"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}
 *    ← {"jsonrpc":"2.0","id":"2","result":{"tools":[...]}}
 * 
 * 3. Call a tool:
 *    → {"jsonrpc":"2.0","id":"3","method":"tools/call","params":{"name":"weather","arguments":{"city":"Paris"}}}
 *    ← {"jsonrpc":"2.0","id":"3","result":{"content":[{"type":"text","text":"Paris: 15°C..."}]}}
 * 
 * For testing with the MCP Inspector:
 *   npx @modelcontextprotocol/inspector java -jar target/stage-2-mcp-server.jar
 */
public class MCPDemo {
    private static final Logger logger = LoggerFactory.getLogger(MCPDemo.class);
    
    public static void main(String[] args) {
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
        logger.info("      \"args\": [\"-jar\", \"/path/to/stage-2-mcp-server.jar\"]");
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
}
