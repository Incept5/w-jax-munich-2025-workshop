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
USE_SHARED=false

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
        --shared|-s)
            USE_SHARED=true
            shift
            ;;
        --help|-h)
            echo "Usage: ./run.sh [OPTIONS]"
            echo
            echo "Options:"
            echo "  --verbose, -v          Show agent reasoning steps"
            echo "  --model, -m MODEL      Override LLM model (default: qwen3:4b)"
            echo "  --shared, -s           Connect to shared workshop database"
            echo "  --help, -h             Show this help message"
            echo
            echo "Examples:"
            echo "  ./run.sh                           # Use local database"
            echo "  ./run.sh --shared                  # Use shared workshop database"
            echo "  ./run.sh -s --model qwen2.5:7b     # Shared DB + specific model"
            echo "  ./run.sh -s -m mistral:7b -v       # Shared DB + model + verbose"
            echo
            echo "Environment Variables:"
            echo "  OLLAMA_MODEL           Override default LLM model"
            echo "  OLLAMA_BASE_URL        Override Ollama URL (default: http://localhost:11434)"
            echo "  DB_URL                 Override database URL"
            echo "  DB_USER                Override database username"
            echo "  DB_PASSWORD            Override database password"
            echo
            echo "Database Modes:"
            echo "  Local (default):       jdbc:postgresql://localhost:5432/workshop_rag"
            echo "  Shared (--shared):     jdbc:postgresql://172.20.15.241:5432/workshop_rag"
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

# Set database connection if --shared flag is used
if [ "$USE_SHARED" = true ]; then
    export DB_URL="jdbc:postgresql://172.20.15.241:5432/workshop_rag"
    export DB_USER="workshop"
    export DB_PASSWORD="workshop123"
    echo "📡 Connecting to shared workshop database at 172.20.15.241:5432"
    echo
fi

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
