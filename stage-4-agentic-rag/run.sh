#!/bin/bash

# Run script for Stage 4: RAG Agent Demo

set -e

echo "🚀 Stage 4: RAG Agent Demo"
echo

# Check if jar exists
if [ ! -f "target/stage-4-agentic-rag-1.0-SNAPSHOT.jar" ]; then
    echo "📦 Building project..."
    mvn clean package -DskipTests
    echo
fi

# Parse command-line arguments
VERBOSE=""
MODEL=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose|-v)
            VERBOSE="--verbose"
            shift
            ;;
        --model|-m)
            MODEL="--model $2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: ./run.sh [OPTIONS]"
            echo
            echo "Options:"
            echo "  --verbose, -v          Show agent reasoning steps"
            echo "  --model, -m MODEL      Override LLM model (default: qwen3:4b)"
            echo "  --help, -h             Show this help message"
            echo
            echo "Examples:"
            echo "  ./run.sh                           # Use default model"
            echo "  ./run.sh --model qwen2.5:7b        # Use specific model"
            echo "  ./run.sh -m mistral:7b --verbose   # Model + verbose mode"
            echo
            echo "Environment Variables:"
            echo "  OLLAMA_MODEL           Override default LLM model"
            echo "  OLLAMA_BASE_URL        Override Ollama URL (default: http://localhost:11434)"
            echo
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Display mode info
if [ -n "$VERBOSE" ]; then
    echo "🔍 Running in verbose mode (showing agent reasoning)"
else
    echo "💡 Tip: Use --verbose to see agent reasoning steps"
fi

if [ -n "$MODEL" ]; then
    echo "🎯 Using model: ${MODEL#--model }"
fi

echo

# Run the agent (explicitly specify the demo class)
java -cp target/stage-4-agentic-rag-1.0-SNAPSHOT.jar \
    com.incept5.workshop.stage4.agent.RAGAgentDemo $VERBOSE $MODEL
