#!/usr/bin/env python3
"""
Local embedding service compatible with Ollama API format.
Uses sentence-transformers for fast, local embedding generation.

This service provides a workaround for Ollama's embedding API bug
while maintaining API compatibility for drop-in replacement.
"""
import base64
import logging
from typing import List, Optional

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import uvicorn
import json

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
logger.info("Loading model: %s", MODEL_NAME)
model = SentenceTransformer(MODEL_NAME, trust_remote_code=True)
logger.info("✓ Model loaded successfully (%d dimensions)",
            model.get_sentence_embedding_dimension())


class EmbeddingRequest(BaseModel):
    """Request model for embedding generation."""
    model: str
    prompt: str
    encoding: Optional[str] = "plain"  # "plain" or "base64"


class EmbeddingResponse(BaseModel):
    """Response model for embedding generation."""
    embedding: List[float]

@app.post("/api/embeddings")
async def generate_embedding(request: Request) -> EmbeddingResponse:
    """
    Generate embedding for the given text.
    
    Supports both plain text and base64-encoded input to handle
    special characters, code blocks, and multi-line content safely.
    
    Compatible with Ollama's /api/embeddings endpoint format.
    """
    # Read and log raw request body
    try:
        body_bytes = await request.body()
        logger.info("📥 Raw request received:")
        logger.info("   Content-Type: %s", request.headers.get("content-type"))
        logger.info("   Body length: %d bytes", len(body_bytes))
        logger.info("   Body (first 200 chars): %s",
                   body_bytes[:200].decode('utf-8', errors='replace'))

        # Parse JSON manually
        body_str = body_bytes.decode('utf-8')
        request_data = json.loads(body_str)

        # Extract fields
        model_name = request_data.get('model')
        prompt = request_data.get('prompt')
        encoding = request_data.get('encoding', 'plain')

        logger.info("   Parsed - Model: %s, Encoding: %s, Prompt length: %d",
                   model_name, encoding, len(prompt) if prompt else 0)

        if not prompt:
            raise HTTPException(status_code=422, detail="Missing 'prompt' field")
    except json.JSONDecodeError as e:
        logger.error("❌ JSON decode error: %s", e)
        logger.error("   Raw body: %s", body_bytes[:500])
        raise HTTPException(status_code=422, detail=f"Invalid JSON: {str(e)}") from e
    except Exception as e:
        logger.error("❌ Request parsing error: %s", e)
        raise HTTPException(status_code=422,
                           detail=f"Request parsing failed: {str(e)}") from e

    try:
        # DECODE if base64 encoded
        if encoding == "base64":
            logger.debug("Decoding base64 input (length: %d)",
                        len(prompt))
            try:
                decoded_bytes = base64.b64decode(prompt)
                text = decoded_bytes.decode('utf-8')
                logger.debug("Decoded to %d characters", len(text))
            except base64.binascii.Error as e:
                logger.error("❌ Base64 decoding failed: %s", e)
                logger.error("   Raw prompt (first 200 chars): %s", prompt[:200])
                raise HTTPException(
                    status_code=422,
                    detail=f"Invalid base64 encoding: {str(e)}"
                ) from e
            except UnicodeDecodeError as e:
                logger.error("❌ UTF-8 decoding failed: %s", e)
                logger.error("   Decoded bytes (first 100): %s", decoded_bytes[:100])
                raise HTTPException(
                    status_code=422,
                    detail=f"Invalid UTF-8 content: {str(e)}"
                ) from e
        else:
            text = prompt

        logger.debug("Generating embedding for text: %s...", text[:50])

        # Generate embedding on the decoded/plain text
        embedding = model.encode(text, convert_to_tensor=False)

        # Convert to list and return
        return EmbeddingResponse(embedding=embedding.tolist())

    except HTTPException as e:
        # Log HTTP exceptions before re-raising
        logger.error("❌ HTTP Exception: %s", e.detail)
        raise
    except Exception as e:
        logger.error("❌ Embedding generation failed: %s", e)
        logger.error("   Request details: model=%s, encoding=%s, prompt_len=%d",
                    model_name, encoding, len(prompt) if prompt else 0)
        raise HTTPException(status_code=500, detail=str(e)) from e

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
