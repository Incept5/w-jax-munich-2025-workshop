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

# Check for .env file
if [ -f ".env" ]; then
    echo -e "${BLUE}📋 Loading configuration from .env file${NC}"
    export $(grep -v '^#' .env | xargs)
fi

# Determine embedding provider (priority: CLI arg > env var > repos.yaml > default)
EMBEDDING_PROVIDER=${EMBEDDING_PROVIDER:-"python"}  # default to python

for arg in "$@"; do
    if [[ "$arg" == "--refresh" ]]; then
        REFRESH_MODE=true
    elif [[ "$arg" == "--provider=python" ]] || [[ "$arg" == "--backend=python" ]]; then
        EMBEDDING_PROVIDER="python"
    elif [[ "$arg" == "--provider=openai" ]] || [[ "$arg" == "--backend=openai" ]]; then
        EMBEDDING_PROVIDER="openai"
    fi
done

# Export for Java to use
export EMBEDDING_PROVIDER

if [ "$REFRESH_MODE" = true ]; then
    echo -e "${BLUE}🔄 Refresh mode enabled - will fetch fresh repository data${NC}"
fi

# Step 1: Provider selection and verification
echo
if [ "$EMBEDDING_PROVIDER" = "python" ]; then
    echo -e "${BLUE}🐍 Using Python embedding service${NC}"
    echo -e "${BLUE}   Free, local, 768 dimensions${NC}"
    
    # Get Python service URL from env or default
    PYTHON_URL=${PYTHON_EMBEDDING_SERVICE_URL:-"http://localhost:8001"}
    
    # Check if Python service is running
    if ! curl -s $PYTHON_URL/health > /dev/null 2>&1; then
        echo -e "${RED}❌ Python embedding service not running at $PYTHON_URL${NC}"
        echo
        echo "Start it with:"
        echo -e "  ${GREEN}cd embedding-service && ./start.sh${NC}"
        echo
        echo "Or switch to OpenAI (requires API key):"
        echo -e "  ${YELLOW}./ingest.sh --provider=openai${NC}"
        echo
        exit 1
    fi
    echo -e "${GREEN}✓ Python service is ready at $PYTHON_URL${NC}"
    
elif [ "$EMBEDDING_PROVIDER" = "openai" ]; then
    echo -e "${BLUE}🤖 Using OpenAI embeddings${NC}"
    echo -e "${BLUE}   text-embedding-3-small, 768 dimensions${NC}"
    
    # Check for API key
    if [ -z "$OPENAI_API_KEY" ]; then
        echo -e "${RED}❌ OPENAI_API_KEY not set!${NC}"
        echo
        echo "Get your API key from: https://platform.openai.com/api-keys"
        echo
        echo "Then either:"
        echo -e "  1. Add to .env file: ${GREEN}OPENAI_API_KEY=sk-...${NC}"
        echo -e "  2. Export it: ${GREEN}export OPENAI_API_KEY=sk-...${NC}"
        echo
        echo "Or switch to Python service (free, local):"
        echo -e "  ${GREEN}./ingest.sh --provider=python${NC}"
        echo
        exit 1
    fi
    
    # Show masked API key for confirmation
    MASKED_KEY="${OPENAI_API_KEY:0:7}...${OPENAI_API_KEY: -4}"
    echo -e "${GREEN}✓ OpenAI API key configured: $MASKED_KEY${NC}"
    
    # Show cost estimate
    echo -e "${YELLOW}💰 Estimated cost: ~\$0.008 (less than 1 cent)${NC}"
    echo -e "${YELLOW}   Based on 487 documents × ~800 tokens${NC}"
    
else
    echo -e "${RED}❌ Unknown provider: $EMBEDDING_PROVIDER${NC}"
    echo "Valid options: python, openai"
    echo
    echo "Usage:"
    echo "  ./ingest.sh                      # Use Python (default, free)"
    echo "  ./ingest.sh --provider=openai    # Use OpenAI (paid, ~\$0.008)"
    echo "  EMBEDDING_PROVIDER=openai ./ingest.sh"
    exit 1
fi

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
echo "  3. Generate embeddings with $EMBEDDING_PROVIDER"
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
