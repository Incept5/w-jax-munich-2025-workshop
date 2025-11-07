#!/usr/bin/env python3
"""
Local embedding service compatible with Ollama API format.
Uses sentence-transformers for fast, local embedding generation.

This service provides a workaround for Ollama's embedding API bug
while maintaining API compatibility for drop-in replacement.
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import uvicorn
import logging
from typing import List

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Local Embedding Service",
    description="Ollama-compatible embedding API using sentence-transformers",
    version="1.0.0"
)

# Load model on startup (cached for subsequent requests)
MODEL_NAME = "nomic-ai/nomic-embed-text-v1.5"
logger.info(f"Loading model: {MODEL_NAME}")
model = SentenceTransformer(MODEL_NAME, trust_remote_code=True)
logger.info(f"✓ Model loaded successfully ({model.get_sentence_embedding_dimension()} dimensions)")

class EmbeddingRequest(BaseModel):
    model: str
    prompt: str

class EmbeddingResponse(BaseModel):
    embedding: List[float]

@app.post("/api/embeddings")
async def generate_embedding(request: EmbeddingRequest) -> EmbeddingResponse:
    """
    Generate embedding for the given text.
    
    Compatible with Ollama's /api/embeddings endpoint format.
    """
    try:
        logger.debug(f"Generating embedding for text: {request.prompt[:50]}...")
        
        # Generate embedding
        embedding = model.encode(request.prompt, convert_to_tensor=False)
        
        # Convert to list and return
        return EmbeddingResponse(embedding=embedding.tolist())
    
    except Exception as e:
        logger.error(f"Embedding generation failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "model": MODEL_NAME,
        "dimensions": model.get_sentence_embedding_dimension()
    }

@app.get("/")
async def root():
    """Root endpoint with service information."""
    return {
        "service": "Local Embedding Service",
        "model": MODEL_NAME,
        "dimensions": model.get_sentence_embedding_dimension(),
        "endpoints": {
            "embeddings": "/api/embeddings (POST)",
            "health": "/health (GET)"
        },
        "note": "Ollama-compatible API for embedding generation"
    }

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001, log_level="info")
