#!/bin/bash
# Test the embedding service

echo "🧪 Testing embedding service..."
echo

response=$(curl -s -X POST http://localhost:8001/api/embeddings \
    -H "Content-Type: application/json" \
    -d '{
        "model": "nomic-embed-text",
        "prompt": "This is a test"
    }')

if [ $? -eq 0 ]; then
    echo "✅ Service is responding"
    echo ""
    echo "Response (first 100 chars):"
    echo "$response" | head -c 100
    echo "..."
    echo ""
    echo "Full response length: $(echo "$response" | wc -c) characters"
else
    echo "❌ Service is not responding"
    exit 1
fi
