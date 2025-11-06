
#!/bin/bash

# Quick start script for running function calling demo
#
# Usage:
#   ./run.sh                    # Test all models under 100GB
#   ./run.sh "jan"              # Test models with "jan" in name
#   ./run.sh "jan" 50           # Test models with "jan" under 50GB
#   ./run.sh "" 10              # Test all models under 10GB

echo "Function Calling Demo - Stage 1"
echo "================================"
echo ""

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "ERROR: Ollama is not running or not accessible at http://localhost:11434"
    echo "Please start Ollama first: ollama serve"
    exit 1
fi

echo "Ollama is running ✓"
echo ""

# Check if JAR exists
if [ ! -f "target/stage-1-function-calling.jar" ]; then
    echo "Building project..."
    mvn clean package -q
    if [ $? -ne 0 ]; then
        echo "Build failed. Please check the error messages above."
        exit 1
    fi
    echo ""
fi

# Build arguments
if [ -n "$1" ]; then
    echo "Name filter: $1"
fi

if [ -n "$2" ]; then
    echo "Max size: $2 GB"
fi

echo ""
echo "Starting tests..."
echo ""

# Run the demo
java -jar target/stage-1-function-calling.jar "$@"
