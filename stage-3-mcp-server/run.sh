#!/bin/bash

# Stage 2: MCP Server & Agent Runner Script
# This script makes it easy to run the MCP server or agent

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR_FILE="$SCRIPT_DIR/target/stage-2-mcp-server.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}JAR file not found. Building...${NC}"
    echo ""
    cd "$SCRIPT_DIR"
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Build failed!${NC}"
        exit 1
    fi
    echo ""
fi

# Determine mode from first argument
MODE="${1:-server}"

case "$MODE" in
    server)
        # Print header
        echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
        echo -e "${BLUE}  Stage 2: MCP Server${NC}"
        echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
        echo ""
        
        # Show usage info
        echo -e "${GREEN}MCP Server - Ready to accept connections${NC}"
        echo ""
        echo "This server exposes tools via the Model Context Protocol (MCP)."
        echo ""
        echo -e "${YELLOW}Available Tools:${NC}"
        echo "  • weather       - Get weather information for a city"
        echo "  • country_info  - Get information about a country"
        echo ""
        echo -e "${YELLOW}Testing Options:${NC}"
        echo ""
        echo "1. ${BLUE}MCP Inspector${NC} (Interactive testing):"
        echo "   npx @modelcontextprotocol/inspector java -jar \"$JAR_FILE\" server"
        echo ""
        echo "2. ${BLUE}Claude Desktop${NC} (Integration):"
        echo "   Add to claude_desktop_config.json:"
        echo "   {"
        echo "     \"mcpServers\": {"
        echo "       \"workshop\": {"
        echo "         \"command\": \"java\","
        echo "         \"args\": [\"-jar\", \"$JAR_FILE\", \"server\"]"
        echo "       }"
        echo "     }"
        echo "   }"
        echo ""
        echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}Starting server...${NC}"
        echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
        echo ""
        
        # Run the server
        java -jar "$JAR_FILE" server
        ;;
    
    agent)
        if [ $# -lt 2 ]; then
            echo -e "${RED}Error: Agent mode requires a task argument${NC}"
            echo ""
            echo "Usage: $0 agent \"Your task here\" [--verbose]"
            echo ""
            echo "Examples:"
            echo "  $0 agent \"What's the weather in Paris?\""
            echo "  $0 agent \"Tell me about Japan\" --verbose"
            exit 1
        fi
        
        # Run the agent with all remaining arguments
        shift  # Remove 'agent' from args
        java -jar "$JAR_FILE" agent "$@"
        ;;
    
    interactive)
        # Print header
        echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
        echo -e "${BLUE}  Stage 2: MCP Agent - Interactive Mode${NC}"
        echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
        echo ""
        
        # Run interactive mode
        java -jar "$JAR_FILE" interactive
        ;;
    
    help|--help|-h)
        echo -e "${BLUE}Stage 2: MCP Server & Agent${NC}"
        echo ""
        echo "Usage: $0 [mode] [options]"
        echo ""
        echo -e "${YELLOW}Modes:${NC}"
        echo "  server              Start MCP server (default)"
        echo "  agent <task>        Run agent with a single task"
        echo "  interactive         Start interactive chat mode"
        echo "  help                Show this help message"
        echo ""
        echo -e "${YELLOW}Agent Mode Options:${NC}"
        echo "  --verbose, -v       Show detailed agent reasoning"
        echo ""
        echo -e "${YELLOW}Examples:${NC}"
        echo "  # Start server for MCP Inspector"
        echo "  $0 server"
        echo ""
        echo "  # Run agent with task"
        echo "  $0 agent \"What's the weather in Paris?\""
        echo ""
        echo "  # Run agent with verbose output"
        echo "  $0 agent \"Tell me about Japan\" --verbose"
        echo ""
        echo "  # Interactive chat mode"
        echo "  $0 interactive"
        ;;
    
    *)
        echo -e "${RED}Error: Unknown mode '$MODE'${NC}"
        echo ""
        echo "Run '$0 help' for usage information"
        exit 1
        ;;
esac
