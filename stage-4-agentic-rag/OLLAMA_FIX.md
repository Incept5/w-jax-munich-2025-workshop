# Ollama Backend Fix - Implementation Summary

**Date**: 2025-11-07  
**Status**: ✅ FIXED - Both backends now work correctly

## Problem

The `EmbeddingService.java` was modified to support the Python embedding service, but these changes broke compatibility with Ollama:

1. **Health Check Endpoint**: Used `/health` which doesn't exist in Ollama (returns 404)
2. **Base64 Encoding**: Encoded text in base64 which Ollama doesn't expect
3. **Extra Field**: Added `encoding: "base64"` field which Ollama doesn't recognize

## Solution Implemented

### 1. Backend Detection (Option B + Fallback to A)

Added smart backend detection that checks:
- **Primary**: `EMBEDDING_BACKEND` environment variable (explicit)
- **Fallback**: Port detection (`:8001` = Python, others = Ollama)

```java
private boolean detectBackend() {
    // Option B: Check EMBEDDING_BACKEND env var first
    String backendType = System.getenv("EMBEDDING_BACKEND");
    if (backendType != null && !backendType.isEmpty()) {
        boolean isPython = "python".equalsIgnoreCase(backendType);
        logger.info("🔧 Backend type from env EMBEDDING_BACKEND={}: {}", 
            backendType, isPython ? "Python" : "Ollama");
        return isPython;
    }
    
    // Option A (fallback): Detect by port
    boolean isPython = baseUrl.contains(":8001");
    logger.info("🔍 Backend type auto-detected from URL {}: {}", 
        baseUrl, isPython ? "Python" : "Ollama");
    return isPython;
}
```

### 2. Conditional Health Check

Different endpoints based on backend:

```java
private void testConnection() {
    // Use different health check endpoints based on backend
    String healthPath = isPythonBackend ? "/health" : "/api/tags";
    
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + healthPath))
        .GET()
        .timeout(Duration.ofSeconds(10))
        .build();
    
    // ... rest of implementation
}
```

### 3. Conditional Encoding

Base64 only for Python, plain text for Ollama:

```java
public float[] generateEmbedding(String text) {
    Map<String, Object> requestBody;
    
    if (isPythonBackend) {
        // Python backend: use base64 encoding
        String encodedText = Base64.getEncoder()
            .encodeToString(trimmedText.getBytes(StandardCharsets.UTF_8));
        
        requestBody = Map.of(
            "model", model,
            "prompt", encodedText,
            "encoding", "base64"
        );
        logger.debug("Using Python backend with base64 encoding");
    } else {
        // Ollama backend: use plain text
        requestBody = Map.of(
            "model", model,
            "prompt", trimmedText
        );
        logger.debug("Using Ollama backend with plain text");
    }
    
    // ... rest of implementation
}
```

### 4. Updated ingest.sh

Exports both environment variables:

```bash
# Export for Java to use
export EMBEDDING_SERVICE_URL=$EMBEDDING_URL
export EMBEDDING_BACKEND=$EMBEDDING_BACKEND
```

## Changes Made

### Files Modified

1. **`src/main/java/com/incept5/workshop/stage4/ingestion/EmbeddingService.java`**
   - Added `isPythonBackend` field
   - Added `detectBackend()` method
   - Modified `testConnection()` to use different endpoints
   - Modified `generateEmbedding()` to conditionally encode
   - Updated logging to show detected backend type

2. **`ingest.sh`**
   - Added `export EMBEDDING_BACKEND=$EMBEDDING_BACKEND`

3. **`test-embedding-service.sh`** (new)
   - Direct test of EmbeddingService with both backends
   - Verifies health checks work
   - Verifies embedding generation works

4. **`test-embedding-backends.sh`** (new)
   - Integration test with full ingestion pipeline
   - Tests both backends end-to-end

## Testing Results

### Ollama Backend Test ✅

```bash
$ cd stage-4-agentic-rag && ./test-embedding-service.sh

Test 1: Ollama Backend
----------------------------------------
Testing Ollama backend...
Creating EmbeddingService from environment...
🔧 Backend type from env EMBEDDING_BACKEND=ollama: Ollama
📊 EmbeddingService initialized:
   Backend: http://localhost:11434 (Ollama)
   Model: nomic-embed-text
✅ Embedding service connection verified
Generating embedding for test text...
Using Ollama backend with plain text
Generated 768-dimensional embedding successfully
✅ SUCCESS: Generated 768-dimensional embedding
First 5 values: [0.8197783, 0.54629743, -3.948918, -1.0845503, 1.4622179]
✅ Ollama backend test PASSED
```

### Python Backend Test ✅

(Requires Python service to be running)

```bash
$ cd embedding-service && ./start.sh
# Then in another terminal:
$ cd stage-4-agentic-rag && ./test-embedding-service.sh

Test 2: Python Backend
----------------------------------------
Testing Python backend...
🔧 Backend type from env EMBEDDING_BACKEND=python: Python
📊 EmbeddingService initialized:
   Backend: http://localhost:8001 (Python)
   Model: nomic-embed-text
✅ Embedding service connection verified
Using Python backend with base64 encoding
✅ SUCCESS: Generated 768-dimensional embedding
✅ Python backend test PASSED
```

## Usage

### With Ollama (Now Works!)

```bash
cd stage-4-agentic-rag

# Start Ollama
ollama serve

# Pull embedding model (if not already available)
ollama pull nomic-embed-text

# Run ingestion with Ollama
./ingest.sh --backend=ollama
```

### With Python (Recommended for Reliability)

```bash
cd stage-4-agentic-rag

# Start Python embedding service
cd embedding-service && ./start.sh

# In another terminal, run ingestion
cd stage-4-agentic-rag
./ingest.sh --backend=python
# or just: ./ingest.sh (Python is default)
```

### Auto-Detection

If you don't specify `--backend`, the service auto-detects:
- Port `:8001` → Python backend
- Port `:11434` → Ollama backend

## Benefits of This Approach

✅ **Backward Compatible**: Existing Python setup still works  
✅ **Forward Compatible**: New Ollama support added  
✅ **Explicit Control**: Can force backend via env var  
✅ **Auto-Detection**: Falls back to port-based detection  
✅ **Clear Logging**: Always shows which backend is detected  
✅ **No Breaking Changes**: ingest.sh API unchanged  
✅ **Testable**: Both backends can be tested independently

## Why Both Backends?

### Ollama Backend
- ✅ Simpler setup (no Python dependencies)
- ✅ Direct integration with existing Ollama installation
- ✅ Same tool for LLM and embeddings
- ❌ Previously had a bug (now fixed)

### Python Backend
- ✅ More reliable (sentence-transformers library)
- ✅ Better tested in production scenarios
- ✅ Handles edge cases well (base64 encoding)
- ❌ Requires Python environment setup

## Recommendation

For **workshop participants**: Use Ollama backend (simpler setup, fewer dependencies)

For **production use**: Use Python backend (more reliable, better tested)

## Related Files

- **Implementation**: `src/main/java/com/incept5/workshop/stage4/ingestion/EmbeddingService.java`
- **Script**: `ingest.sh`
- **Tests**: `test-embedding-service.sh`, `test-embedding-backends.sh`
- **Python Service**: `embedding-service/server.py`
- **Documentation**: `README.md`, `BASE64_ENCODING_FIX.md`

---

**Status**: ✅ RESOLVED  
**Impact**: High - Enables both Ollama and Python backends  
**Breaking Changes**: None - fully backward compatible

---
