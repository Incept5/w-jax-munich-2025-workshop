#!/bin/bash
# Quick test script to verify tool calling works

set -e

echo "🧪 Testing RAG Agent Tool Calling"
echo "=================================="
echo

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "❌ ERROR: Ollama is not running!"
    echo "   Please start Ollama: ollama serve"
    exit 1
fi

echo "✓ Ollama is running"

# Check if model is available
if ! curl -s http://localhost:11434/api/tags | grep -q "incept5/Jan-v1-2509:fp16"; then
    echo "❌ ERROR: Model 'incept5/Jan-v1-2509:fp16' not found!"
    echo "   Please pull the model: ollama pull incept5/Jan-v1-2509:fp16"
    exit 1
fi

echo "✓ Model is available"

# Check if PostgreSQL is running
if ! docker ps | grep -q stage4-pgvector; then
    echo "⚠️  WARNING: PostgreSQL container not running"
    echo "   Starting PostgreSQL..."
    docker-compose up -d
    sleep 5
fi

echo "✓ PostgreSQL is running"

# Check if database has documents
DOC_COUNT=$(docker exec stage4-pgvector psql -U workshop -d workshop_rag -t -c "SELECT COUNT(*) FROM documents;" 2>/dev/null | xargs || echo "0")

if [ "$DOC_COUNT" -eq "0" ]; then
    echo "⚠️  WARNING: No documents in database"
    echo "   Run ./ingest.sh to ingest documentation first"
    echo
fi

echo "✓ Database has $DOC_COUNT documents"
echo

# Run the test with verbose output
echo "Running RAG Agent with test query..."
echo "Query: 'What is Embabel?'"
echo
echo "─────────────────────────────────────────────────────────────────"
echo

java -jar target/stage-4-agentic-rag.jar --verbose --query "What is Embabel?"

echo
echo "─────────────────────────────────────────────────────────────────"
echo
echo "✅ Test complete!"
echo
echo "If you see:"
echo "  ✓ Parsed as tool call: ToolCall{...}"
echo "  Then tool calling is working!"
echo
echo "If you see:"
echo "  ✗ Not a tool call, treating as final answer"
echo "  Then there's still an issue with parsing"
