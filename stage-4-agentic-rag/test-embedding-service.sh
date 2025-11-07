#!/bin/bash
# Direct test of EmbeddingService with both backends

set -e

echo "🧪 Direct EmbeddingService Backend Test"
echo "=========================================="
echo

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

# Test Java class
cat > /tmp/TestEmbedding.java << 'EOF'
import com.incept5.workshop.stage4.ingestion.EmbeddingService;

public class TestEmbedding {
    public static void main(String[] args) {
        try {
            System.out.println("Creating EmbeddingService from environment...");
            EmbeddingService service = EmbeddingService.fromEnvironment();
            
            System.out.println("Generating embedding for test text...");
            float[] embedding = service.generateEmbedding("Hello, this is a test!");
            
            System.out.println("✅ SUCCESS: Generated " + embedding.length + "-dimensional embedding");
            System.out.println("First 5 values: [" + 
                embedding[0] + ", " + 
                embedding[1] + ", " + 
                embedding[2] + ", " + 
                embedding[3] + ", " + 
                embedding[4] + "]");
            
        } catch (Exception e) {
            System.err.println("❌ FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
EOF

# Check if project is built
if [ ! -f "target/stage-4-agentic-rag-1.0-SNAPSHOT.jar" ]; then
    echo -e "${RED}❌ Project not built. Run: mvn clean package${NC}"
    exit 1
fi

# Compile test class
echo "Compiling test class..."
javac -cp target/stage-4-agentic-rag-1.0-SNAPSHOT.jar /tmp/TestEmbedding.java -d /tmp/

echo

# Test 1: Ollama Backend
echo -e "${BLUE}Test 1: Ollama Backend${NC}"
echo "----------------------------------------"

if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo -e "${RED}❌ Ollama not running!${NC}"
    exit 1
fi

if ! curl -s http://localhost:11434/api/tags | grep -q "nomic-embed-text"; then
    echo -e "${RED}❌ nomic-embed-text model not found${NC}"
    exit 1
fi

echo "Testing Ollama backend..."
export EMBEDDING_SERVICE_URL="http://localhost:11434"
export EMBEDDING_BACKEND="ollama"

java -cp /tmp:target/stage-4-agentic-rag-1.0-SNAPSHOT.jar TestEmbedding

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Ollama backend test PASSED${NC}"
else
    echo -e "${RED}❌ Ollama backend test FAILED${NC}"
    exit 1
fi

echo

# Test 2: Python Backend (if available)
echo -e "${BLUE}Test 2: Python Backend${NC}"
echo "----------------------------------------"

if curl -s http://localhost:8001/health > /dev/null 2>&1; then
    echo "Testing Python backend..."
    export EMBEDDING_SERVICE_URL="http://localhost:8001"
    export EMBEDDING_BACKEND="python"
    
    java -cp /tmp:target/stage-4-agentic-rag-1.0-SNAPSHOT.jar TestEmbedding
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Python backend test PASSED${NC}"
    else
        echo -e "${RED}❌ Python backend test FAILED${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠️  Python service not running (skipping)${NC}"
    echo "   Start with: cd embedding-service && ./start.sh"
fi

echo
echo "=========================================="
echo -e "${GREEN}✅ All available backends tested successfully!${NC}"
echo

# Cleanup
rm -f /tmp/TestEmbedding.java /tmp/TestEmbedding.class
