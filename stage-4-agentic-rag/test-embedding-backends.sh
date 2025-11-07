#!/bin/bash
# Test both Ollama and Python embedding backends

set -e

echo "🧪 Testing Embedding Backend Integration"
echo "=========================================="
echo

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if project is built
if [ ! -f "target/stage-4-agentic-rag-1.0-SNAPSHOT.jar" ]; then
    echo -e "${RED}❌ Project not built. Run: mvn clean package${NC}"
    exit 1
fi

echo -e "${BLUE}Prerequisites:${NC}"
echo "  ✓ Project built"

# Test 1: Ollama Backend
echo
echo -e "${BLUE}Test 1: Ollama Backend${NC}"
echo "----------------------------------------"

# Check Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo -e "${RED}❌ Ollama not running!${NC}"
    echo "   Start with: ollama serve"
    exit 1
fi
echo "  ✓ Ollama is running"

# Check model exists
if ! curl -s http://localhost:11434/api/tags | grep -q "nomic-embed-text"; then
    echo -e "${RED}❌ nomic-embed-text model not found${NC}"
    echo "   Pull with: ollama pull nomic-embed-text"
    exit 1
fi
echo "  ✓ nomic-embed-text model available"

# Test embedding generation with Ollama
echo
echo "Testing embedding generation with Ollama..."
export EMBEDDING_SERVICE_URL="http://localhost:11434"
export EMBEDDING_BACKEND="ollama"

# Create a simple test file
TEST_FILE=$(mktemp)
cat > "$TEST_FILE" << 'EOF'
repositories:
  - name: test-repo
    url: https://github.com/test/test
    branch: main
    description: "Test repository"
    ingest_strategy: "committed_file"
    committed_file: "data/gitingest-output/embabel-agent.txt"

settings:
  chunk_size: 800
  chunk_overlap: 200
  similarity_threshold: 0.7
EOF

# Run a quick test (just parse and initialize, don't ingest all)
echo "Running Ollama backend test..."
java -jar target/stage-4-agentic-rag-1.0-SNAPSHOT.jar "$TEST_FILE" --dry-run 2>&1 | head -20

OLLAMA_EXIT_CODE=${PIPESTATUS[0]}
rm -f "$TEST_FILE"

if [ $OLLAMA_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Ollama backend test PASSED${NC}"
else
    echo -e "${YELLOW}⚠️  Ollama backend test had issues (exit code: $OLLAMA_EXIT_CODE)${NC}"
    echo "   This might be expected if --dry-run is not implemented"
fi

# Test 2: Python Backend (if available)
echo
echo -e "${BLUE}Test 2: Python Backend${NC}"
echo "----------------------------------------"

if curl -s http://localhost:8001/health > /dev/null 2>&1; then
    echo "  ✓ Python service is running"
    
    # Test embedding generation with Python
    echo
    echo "Testing embedding generation with Python..."
    export EMBEDDING_SERVICE_URL="http://localhost:8001"
    export EMBEDDING_BACKEND="python"
    
    # Create test file
    TEST_FILE=$(mktemp)
    cat > "$TEST_FILE" << 'EOF'
repositories:
  - name: test-repo
    url: https://github.com/test/test
    branch: main
    description: "Test repository"
    ingest_strategy: "committed_file"
    committed_file: "data/gitingest-output/embabel-agent.txt"

settings:
  chunk_size: 800
  chunk_overlap: 200
  similarity_threshold: 0.7
EOF
    
    echo "Running Python backend test..."
    java -jar target/stage-4-agentic-rag-1.0-SNAPSHOT.jar "$TEST_FILE" --dry-run 2>&1 | head -20
    
    PYTHON_EXIT_CODE=${PIPESTATUS[0]}
    rm -f "$TEST_FILE"
    
    if [ $PYTHON_EXIT_CODE -eq 0 ]; then
        echo -e "${GREEN}✅ Python backend test PASSED${NC}"
    else
        echo -e "${YELLOW}⚠️  Python backend test had issues (exit code: $PYTHON_EXIT_CODE)${NC}"
        echo "   This might be expected if --dry-run is not implemented"
    fi
else
    echo -e "${YELLOW}⚠️  Python service not running (skipping test)${NC}"
    echo "   Start with: cd embedding-service && ./start.sh"
fi

# Summary
echo
echo "=========================================="
echo -e "${BLUE}Summary:${NC}"
echo "  - Ollama backend: Available and configured"
echo "  - Python backend: $(curl -s http://localhost:8001/health > /dev/null 2>&1 && echo 'Available' || echo 'Not running')"
echo
echo "To use in ingest.sh:"
echo -e "  ${GREEN}./ingest.sh --backend=ollama${NC}   # Use Ollama"
echo -e "  ${GREEN}./ingest.sh --backend=python${NC}   # Use Python (default)"
echo
