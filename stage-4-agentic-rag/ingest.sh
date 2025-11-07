#!/bin/bash
# Stage 3: RAG Ingestion Pipeline
# This script sets up the complete RAG infrastructure and ingests repositories

set -e  # Exit on error

echo "🚀 Stage 3: RAG Ingestion Pipeline"
echo

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Parse command line arguments
REFRESH_MODE=false
if [[ "$1" == "--refresh" ]]; then
    REFRESH_MODE=true
    echo -e "${BLUE}🔄 Refresh mode enabled - will fetch fresh repository data${NC}"
    echo
fi

# Step 1: Check gitingest only if refresh mode
if [ "$REFRESH_MODE" = true ]; then
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
    echo -e "${BLUE}📄 Using committed repository files from git${NC}"
    echo -e "${BLUE}   (Use --refresh to fetch fresh data with gitingest)${NC}"
fi

# Step 2: Build the Java project
echo
echo -e "${BLUE}🔨 Building Stage 3 project...${NC}"
cd "$(dirname "$0")"
mvn clean package -DskipTests
echo -e "${GREEN}✓ Build complete${NC}"

# Step 3: Start PostgreSQL + pgvector
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

# Step 4: Run ingestion (includes Flyway migrations)
echo
echo -e "${BLUE}📚 Starting ingestion pipeline...${NC}"
echo "This will:"
echo "  1. Run database migrations (Flyway)"
echo "  2. Process repositories with gitingest"
echo "  3. Generate embeddings with Ollama"
echo "  4. Store in PostgreSQL with pgvector"
echo

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags &> /dev/null; then
    echo -e "${RED}Warning: Ollama does not appear to be running at localhost:11434${NC}"
    echo "Please start Ollama with: ollama serve"
    echo
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check if embedding model is available
if ! curl -s http://localhost:11434/api/tags | grep -q "nomic-embed-text"; then
    echo -e "${RED}Warning: nomic-embed-text model not found${NC}"
    echo "Pulling model... (this may take a few minutes)"
    ollama pull nomic-embed-text
    echo -e "${GREEN}✓ Model pulled${NC}"
fi

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
