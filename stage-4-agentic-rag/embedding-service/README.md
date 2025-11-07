# Local Embedding Service

A FastAPI-based embedding service that provides Ollama-compatible API for generating embeddings using sentence-transformers.

## Prerequisites

- **Conda** (Miniconda or Anaconda)
  - Install from: https://docs.conda.io/en/latest/miniconda.html
- **8GB+ RAM** recommended for model loading

## Quick Start

### 1. Start the Service

```bash
cd stage-4-agentic-rag/embedding-service
./start.sh
```

The script will:
- Check for conda installation
- Create a conda environment (first time only)
- Install all dependencies
- Start the server on http://localhost:8001

### 2. Test the Service

```bash
# Health check
curl http://localhost:8001/health

# Generate embedding
curl -X POST http://localhost:8001/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{"model": "nomic-embed-text", "prompt": "Hello, world!"}'
```

## Manual Setup

If you prefer to set up the environment manually:

```bash
# Create conda environment
conda env create -f environment.yml

# Activate environment
conda activate embedding-service

# Start server
python server.py
```

## Environment Management

### Update Dependencies

```bash
# Update environment.yml with new packages
conda env update -f environment.yml --prune
```

### Recreate Environment

```bash
# Remove existing environment
conda env remove -n embedding-service

# Create fresh environment
conda env create -f environment.yml
```

### Remove Environment

```bash
conda env remove -n embedding-service
```

## API Endpoints

### POST /api/embeddings

Generate embedding for text (Ollama-compatible format).

**Request:**
```json
{
  "model": "nomic-embed-text",
  "prompt": "Your text here"
}
```

**Response:**
```json
{
  "embedding": [0.123, -0.456, ...]
}
```

### GET /health

Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "model": "nomic-ai/nomic-embed-text-v1.5",
  "dimensions": 768
}
```

### GET /

Service information.

## Model Information

**Default Model:** `nomic-ai/nomic-embed-text-v1.5`
- Dimensions: 768
- Context Length: 8192 tokens
- License: Apache 2.0

The model is downloaded automatically on first run and cached locally.

## Troubleshooting

### Conda Not Found

If you get "conda: command not found":

```bash
# Initialize conda for your shell
conda init bash  # or zsh, fish, etc.

# Restart your shell or source the config
source ~/.bashrc  # or ~/.zshrc
```

### Module Not Found Errors (einops, etc.)

If you see "No module named 'einops'" or similar import errors despite successful installation:

**Root Cause:** Old venv or PYTHONPATH interfering with conda environment.

**Solution:**

1. Run the verification script:
   ```bash
   ./test-conda-fix.sh
   ```

2. If issues persist, recreate the environment:
   ```bash
   # Remove old environment
   conda env remove -n embedding-service
   
   # Remove any old venv directory
   rm -rf venv/
   
   # Start fresh
   ./start.sh
   ```

3. Check for interfering environment variables:
   ```bash
   # These should be empty or not set
   echo $PYTHONPATH
   echo $VIRTUAL_ENV
   ```

**Why This Happens:**
- The start.sh script now clears PYTHONPATH/VIRTUAL_ENV before activation
- Uses `conda run` instead of `conda activate` for reliability
- Python 3.11 is used (3.14 not yet supported by sentence-transformers)

### Port Already in Use

If port 8001 is already in use, modify `server.py`:

```python
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8002, log_level="info")  # Change port
```

### Model Download Issues

If model download fails:

1. Check internet connection
2. Try manual download:
   ```python
   from sentence_transformers import SentenceTransformer
   model = SentenceTransformer("nomic-ai/nomic-embed-text-v1.5")
   ```

### Memory Issues

If you encounter out-of-memory errors:
- Close other applications
- Ensure at least 8GB RAM available
- Consider using a smaller model

### Python Version Issues

The service requires **Python 3.11** (specified in environment.yml). If you see Python 3.14 or other versions:

1. Remove the environment:
   ```bash
   conda env remove -n embedding-service
   ```

2. Recreate with correct Python version:
   ```bash
   ./start.sh
   ```

3. Verify:
   ```bash
   conda run -n embedding-service python --version
   # Should show: Python 3.11.x
   ```

## Development

### Running Tests

```bash
conda activate embedding-service
pytest tests/  # (if tests are added)
```

### Code Formatting

```bash
conda activate embedding-service
pip install black
black server.py
```

## Architecture

```
┌─────────────────────────────────────┐
│  Java RAG Agent (stage-4-agentic-rag)│
└────────────────┬────────────────────┘
                 │ HTTP POST
                 │ /api/embeddings
                 ▼
┌─────────────────────────────────────┐
│  FastAPI Server (port 8001)          │
│  ├─ /api/embeddings                  │
│  ├─ /health                          │
│  └─ /                                │
└────────────────┬────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│  sentence-transformers               │
│  └─ nomic-embed-text-v1.5           │
└─────────────────────────────────────┘
```

## Why This Service?

This service provides a workaround for Ollama's embedding API bug while maintaining API compatibility for drop-in replacement in the workshop RAG agent.

Benefits:
- **Fast**: Local inference with optimized transformers
- **Compatible**: Drop-in replacement for Ollama embedding API
- **Reliable**: No Ollama bugs or network issues
- **Flexible**: Easy to swap models

## License

This service is part of the W-JAX Munich 2025 Workshop materials.
