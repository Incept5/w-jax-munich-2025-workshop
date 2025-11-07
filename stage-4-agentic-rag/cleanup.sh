#!/bin/bash

# cleanup.sh - Clean up Stage 3 RAG infrastructure for fresh start
# This script stops containers, removes volumes, and cleans up generated files

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================================="
echo "Stage 3 RAG Cleanup Script"
echo "=================================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null 2>&1; then
    echo -e "${RED}Error: docker-compose not found${NC}"
    echo "Please install Docker Compose to use this script"
    exit 1
fi

# Use 'docker compose' if available, otherwise 'docker-compose'
if docker compose version &> /dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

echo -e "${YELLOW}Step 1: Stopping Docker containers...${NC}"
if $DOCKER_COMPOSE ps --quiet | grep -q .; then
    $DOCKER_COMPOSE down
    echo -e "${GREEN}✓ Containers stopped${NC}"
else
    echo -e "${GREEN}✓ No running containers found${NC}"
fi
echo ""

echo -e "${YELLOW}Step 2: Removing Docker volumes...${NC}"
if docker volume ls --quiet --filter name=stage-4-agentic-rag_pgvector_data | grep -q .; then
    $DOCKER_COMPOSE down -v
    echo -e "${GREEN}✓ Volumes removed${NC}"
else
    echo -e "${GREEN}✓ No volumes found${NC}"
fi
echo ""

echo -e "${YELLOW}Step 3: Checking for orphaned containers...${NC}"
if docker ps -a --filter name=stage4-pgvector --quiet | grep -q .; then
    docker rm -f stage4-pgvector 2>/dev/null || true
    echo -e "${GREEN}✓ Orphaned containers removed${NC}"
else
    echo -e "${GREEN}✓ No orphaned containers found${NC}"
fi
echo ""

echo -e "${YELLOW}Step 4: Cleaning up generated files (optional)...${NC}"
read -p "Do you want to remove generated data files in data/gitingest-output/? (y/N): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [ -d "data/gitingest-output" ]; then
        rm -rf data/gitingest-output/*.txt
        echo -e "${GREEN}✓ Generated data files removed${NC}"
    else
        echo -e "${GREEN}✓ No data directory found${NC}"
    fi
else
    echo -e "${YELLOW}⊘ Skipping data file cleanup${NC}"
fi
echo ""

echo -e "${YELLOW}Step 5: Cleaning Maven build artifacts (optional)...${NC}"
read -p "Do you want to clean Maven target directory? (y/N): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [ -d "target" ]; then
        rm -rf target
        echo -e "${GREEN}✓ Maven artifacts cleaned${NC}"
    else
        echo -e "${GREEN}✓ No target directory found${NC}"
    fi
else
    echo -e "${YELLOW}⊘ Skipping Maven cleanup${NC}"
fi
echo ""

echo "=================================================="
echo -e "${GREEN}Cleanup complete!${NC}"
echo "=================================================="
echo ""
echo "You now have a fresh environment. To restart:"
echo "  1. docker-compose up -d"
echo "  2. ./ingest.sh"
echo "  3. ./run.sh"
echo ""
