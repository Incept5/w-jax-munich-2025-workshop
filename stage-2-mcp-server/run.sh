#!/bin/bash

# Stage 2: MCP Server Runner Script
# This script makes it easy to run the MCP server

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR_FILE="$SCRIPT_DIR/target/stage-2-mcp-server.jar"

# Print header
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Stage 2: MCP Server${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

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
echo "   npx @modelcontextprotocol/inspector java -jar \"$JAR_FILE\""
echo ""
echo "2. ${BLUE}Claude Desktop${NC} (Integration):"
echo "   Add to claude_desktop_config.json:"
echo "   {"
echo "     \"mcpServers\": {"
echo "       \"workshop\": {"
echo "         \"command\": \"java\","
echo "         \"args\": [\"-jar\", \"$JAR_FILE\"]"
echo "       }"
echo "     }"
echo "   }"
echo ""
echo "3. ${BLUE}Manual Testing${NC} (Send JSON-RPC messages to STDIN):"
echo "   Example initialize:"
echo "   echo '{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"initialize\",\"params\":{}}' | java -jar \"$JAR_FILE\""
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Starting server...${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Run the server
java -jar "$JAR_FILE"
