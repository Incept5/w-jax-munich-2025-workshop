#!/bin/bash
# Test both embedding backends

echo "🧪 Testing Embedding Backends"
echo

# Test Python service
echo "1️⃣  Testing Python Service (http://localhost:8001)..."
if curl -s -X POST http://localhost:8001/api/embeddings \
    -H "Content-Type: application/json" \
    -d '{"model":"nomic-embed-text","prompt":"test"}' > /dev/null 2>&1; then
    echo "   ✅ Python service is working"
else
    echo "   ❌ Python service failed (is it running?)"
    echo "      Start with: cd embedding-service && ./start.sh"
fi

echo

# Test Ollama
echo "2️⃣  Testing Ollama (http://localhost:11434)..."
if curl -s -X POST http://localhost:11434/api/embeddings \
    -H "Content-Type: application/json" \
    -d '{"model":"nomic-embed-text","prompt":"test"}' > /dev/null 2>&1; then
    echo "   ✅ Ollama is working (bug may be fixed!)"
else
    echo "   ❌ Ollama failed (expected due to known bug)"
fi

echo
echo "📋 Recommendation: Use Python service until Ollama bug is resolved"
echo
echo "Usage:"
echo "  ./ingest.sh                    # Use Python (default)"
echo "  ./ingest.sh --backend=python   # Use Python (explicit)"
echo "  ./ingest.sh --backend=ollama   # Use Ollama (not recommended)"
