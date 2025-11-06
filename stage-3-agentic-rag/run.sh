#!/bin/bash

# Run script for Stage 3: RAG Agent Demo

set -e

echo "🚀 Stage 3: RAG Agent Demo"
echo

# Check if jar exists
if [ ! -f "target/stage-3-agentic-rag.jar" ]; then
    echo "📦 Building project..."
    mvn clean package -DskipTests
    echo
fi

# Check for verbose flag
VERBOSE=""
if [ "$1" == "--verbose" ] || [ "$1" == "-v" ]; then
    VERBOSE="--verbose"
    echo "🔍 Running in verbose mode (showing agent reasoning)"
else
    echo "💡 Tip: Use --verbose to see agent reasoning steps"
fi

echo

# Run the agent
java -jar target/stage-3-agentic-rag.jar $VERBOSE
