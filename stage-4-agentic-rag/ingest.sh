#!/bin/bash
# Stage 4: RAG Ingestion Pipeline
# Supports both Ollama and Python embedding backends

set -e  # Exit on error

echo "🚀 Stage 4: RAG Ingestion Pipeline"
echo

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Parse command line arguments
REFRESH_MODE=false
EMBEDDING_BACKEND=${EMBEDDING_BACKEND:-"ollama"}  # default to python

for arg in "$@"; do
    if [[ "$arg" == "--refresh" ]]; then
        REFRESH_MODE=true
    elif [[ "$arg" == "--backend=ollama" ]]; then
        EMBEDDING_BACKEND="ollama"
    elif [[ "$arg" == "--backend=python" ]]; then
        EMBEDDING_BACKEND="python"
    fi
done

if [ "$REFRESH_MODE" = true ]; then
    echo -e "${BLUE}🔄 Refresh mode enabled - will fetch fresh repository data${NC}"
fi

# Step 1: Backend selection and verification
echo
if [ "$EMBEDDING_BACKEND" = "python" ]; then
    echo -e "${BLUE}🐍 Using Python embedding service (recommended)${NC}"
    EMBEDDING_URL="http://localhost:8001"
    
    # Check if Python service is running
    if ! curl -s $EMBEDDING_URL/health > /dev/null 2>&1; then
        echo -e "${RED}❌ Python embedding service not running!${NC}"
        echo
        echo "Start it with:"
        echo -e "  ${GREEN}cd embedding-service && ./start.sh${NC}"
        echo
        echo "Or switch to Ollama (not recommended due to bug):"
        echo -e "  ${YELLOW}EMBEDDING_BACKEND=ollama ./ingest.sh${NC}"
        echo
        exit 1
    fi
    echo -e "${GREEN}✓ Python service is ready${NC}"
    
elif [ "$EMBEDDING_BACKEND" = "ollama" ]; then
    echo -e "${YELLOW}🦙 Using Ollama (⚠️  has known bug, may fail)${NC}"
    EMBEDDING_URL="http://localhost:11434"
    
    # Check if Ollama is running
    if ! curl -s $EMBEDDING_URL/api/tags > /dev/null 2>&1; then
        echo -e "${RED}❌ Ollama not running!${NC}"
        echo
        echo "Start Ollama with:"
        echo -e "  ${GREEN}ollama serve${NC}"
        echo
        echo "Or switch to Python service (recommended):"
        echo -e "  ${GREEN}EMBEDDING_BACKEND=python ./ingest.sh${NC}"
        echo
        exit 1
    fi
    
    # Check if embedding model is available
    if ! curl -s $EMBEDDING_URL/api/tags | grep -q "nomic-embed-text"; then
        echo -e "${YELLOW}⚠️  nomic-embed-text model not found${NC}"
        echo "Pulling model... (this may take a few minutes)"
        ollama pull nomic-embed-text
        echo -e "${GREEN}✓ Model pulled${NC}"
    fi
    echo -e "${GREEN}✓ Ollama is ready${NC}"
    
else
    echo -e "${RED}❌ Unknown backend: $EMBEDDING_BACKEND${NC}"
    echo "Valid options: python, ollama"
    echo
    echo "Usage:"
    echo "  ./ingest.sh                    # Use Python (default)"
    echo "  EMBEDDING_BACKEND=ollama ./ingest.sh"
    echo "  ./ingest.sh --backend=python"
    exit 1
fi

# Export for Java to use
export EMBEDDING_SERVICE_URL=$EMBEDDING_URL
export EMBEDDING_BACKEND=$EMBEDDING_BACKEND

# Step 2: Check gitingest only if refresh mode
if [ "$REFRESH_MODE" = true ]; then
    echo
    echo -e "${BLUE}📦 Checking gitingest installation...${NC}"
    if ! command -v gitingest &> /dev/null; then
        echo -e "${RED}Error: gitingest is required for --refresh mode${NC}"
        echo "Please install it first:"
        echo "  brew install pipx && pipx install gitingest"
        echo "  OR"
        echo "  pipx install gitingest"
        echo
        echo "Alternatively, run without --refresh to use committed files."
        exit 1
    fi
    echo -e "${GREEN}✓ gitingest is available${NC}"
else
    echo
    echo -e "${BLUE}📄 Using committed repository files from git${NC}"
    echo -e "${BLUE}   (Use --refresh to fetch fresh data with gitingest)${NC}"
fi

# Step 3: Build the Java project
echo
echo -e "${BLUE}🔨 Building Stage 3 project...${NC}"
cd "$(dirname "$0")"
mvn clean package -DskipTests
echo -e "${GREEN}✓ Build complete${NC}"

# Step 4: Start PostgreSQL + pgvector
echo
echo -e "${BLUE}🐘 Starting PostgreSQL with pgvector...${NC}"
docker-compose up -d

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec stage4-pgvector pg_isready -U workshop -d workshop_rag &> /dev/null; then
        echo -e "${GREEN}✓ PostgreSQL is ready${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}Error: PostgreSQL failed to start after 30 seconds${NC}"
        exit 1
    fi
    sleep 1
done

# Step 5: Run ingestion (includes Flyway migrations)
echo
echo -e "${BLUE}📚 Starting ingestion pipeline...${NC}"
echo "This will:"
echo "  1. Run database migrations (Flyway)"
echo "  2. Process repositories"
echo "  3. Generate embeddings with $EMBEDDING_BACKEND"
echo "  4. Store in PostgreSQL with pgvector"
echo

# Run the ingestion
if [ "$REFRESH_MODE" = true ]; then
    java -jar target/stage-4-agentic-rag-1.0-SNAPSHOT.jar repos.yaml --refresh
else
    java -jar target/stage-4-agentic-rag-1.0-SNAPSHOT.jar repos.yaml
fi

echo
echo -e "${GREEN}✅ Ingestion pipeline complete!${NC}"
echo
echo "Next steps:"
echo "  - Check documents: docker exec -it stage4-pgvector psql -U workshop -d workshop_rag -c 'SELECT COUNT(*) FROM documents;'"
echo "  - Query by source: docker exec -it stage4-pgvector psql -U workshop -d workshop_rag -c 'SELECT source, COUNT(*) FROM documents GROUP BY source;'"
echo "  - Stop database: docker-compose down"
