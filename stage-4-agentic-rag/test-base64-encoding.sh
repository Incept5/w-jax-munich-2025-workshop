#!/bin/bash
# Test script to verify base64 encoding fixes the 422 errors

set -e

echo "🧪 Testing Base64 Encoding Solution"
echo "===================================="
echo

# Check if Python service is running
if ! curl -s http://localhost:8001/health > /dev/null 2>&1; then
    echo "❌ Python embedding service is not running!"
    echo "   Start it with: cd embedding-service && ./start.sh"
    exit 1
fi

echo "✅ Python service is running"
echo

# Test 1: Simple text (baseline)
echo "Test 1: Simple text"
echo "-------------------"
SIMPLE_TEXT="Hello, world!"
SIMPLE_BASE64=$(echo -n "$SIMPLE_TEXT" | base64)

RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" http://localhost:8001/api/embeddings \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"nomic-embed-text\",
    \"prompt\": \"$SIMPLE_BASE64\",
    \"encoding\": \"base64\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Simple text: SUCCESS (200)"
else
    echo "❌ Simple text: FAILED ($HTTP_CODE)"
    echo "$RESPONSE"
fi
echo

# Test 2: Text with special characters
echo "Test 2: Special characters (quotes, newlines, backslashes)"
echo "---------------------------------------------------------"
SPECIAL_TEXT='const x = "hello";
const y = "world\\n";
const z = `template ${x}`;'
SPECIAL_BASE64=$(echo -n "$SPECIAL_TEXT" | base64)

RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" http://localhost:8001/api/embeddings \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"nomic-embed-text\",
    \"prompt\": \"$SPECIAL_BASE64\",
    \"encoding\": \"base64\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Special characters: SUCCESS (200)"
else
    echo "❌ Special characters: FAILED ($HTTP_CODE)"
    echo "$RESPONSE"
fi
echo

# Test 3: Multi-line code block
echo "Test 3: Multi-line code block"
echo "-----------------------------"
CODE_BLOCK='public class Example {
    private String name;
    
    public Example(String name) {
        this.name = name;
    }
    
    public String getName() {
        return "Name: \"" + name + "\"";
    }
}'
CODE_BASE64=$(echo -n "$CODE_BLOCK" | base64)

RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" http://localhost:8001/api/embeddings \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"nomic-embed-text\",
    \"prompt\": \"$CODE_BASE64\",
    \"encoding\": \"base64\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Multi-line code: SUCCESS (200)"
else
    echo "❌ Multi-line code: FAILED ($HTTP_CODE)"
    echo "$RESPONSE"
fi
echo

# Test 4: Unicode characters
echo "Test 4: Unicode characters"
echo "-------------------------"
UNICODE_TEXT="Hello 世界! Привет мир! مرحبا بالعالم!"
UNICODE_BASE64=$(echo -n "$UNICODE_TEXT" | base64)

RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" http://localhost:8001/api/embeddings \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"nomic-embed-text\",
    \"prompt\": \"$UNICODE_BASE64\",
    \"encoding\": \"base64\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Unicode text: SUCCESS (200)"
else
    echo "❌ Unicode text: FAILED ($HTTP_CODE)"
    echo "$RESPONSE"
fi
echo

# Test 5: Plain text mode (backwards compatibility)
echo "Test 5: Plain text mode (backwards compatibility)"
echo "------------------------------------------------"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" http://localhost:8001/api/embeddings \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"nomic-embed-text\",
    \"prompt\": \"Simple plain text\",
    \"encoding\": \"plain\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Plain text mode: SUCCESS (200)"
else
    echo "❌ Plain text mode: FAILED ($HTTP_CODE)"
    echo "$RESPONSE"
fi
echo

echo "===================================="
echo "✅ All tests completed!"
echo
echo "Next steps:"
echo "  1. Restart the Python service to load the new code:"
echo "     cd embedding-service && ./start.sh"
echo
echo "  2. Run the ingestion again:"
echo "     cd .. && ./ingest.sh"
echo
echo "The base64 encoding should eliminate all 422 errors!"
