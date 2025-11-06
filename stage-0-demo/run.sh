
#!/usr/bin/env bash

# W-JAX Munich 2025 Workshop - Stage 0 Demo Runner
# Convenience script to run the Ollama Demo application

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
JAR_FILE="target/stage-0-demo-1.0.0.jar"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Change to script directory
cd "$SCRIPT_DIR"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Error: JAR file not found: $JAR_FILE${NC}"
    echo -e "${YELLOW}Please build the project first:${NC}"
    echo -e "  cd .."
    echo -e "  mvn clean install"
    echo -e "  cd stage-0-demo"
    echo -e "  ./run.sh"
    exit 1
fi

# Run the application with all arguments passed through
echo -e "${GREEN}Running Ollama Demo...${NC}"
java -jar "$JAR_FILE" "$@"
