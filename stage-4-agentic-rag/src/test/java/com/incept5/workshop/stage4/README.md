
# Stage 3: Agentic RAG - Integration Tests

This directory contains integration tests for the Stage 3 RAG (Retrieval-Augmented Generation) system.

## Test Files

### EmbeddingServiceIntegrationTest.java ✅

**Purpose**: Comprehensive integration test for the `EmbeddingService` class.

**What It Tests**:
1. **Embedding Generation**: Verifies the service can generate embeddings via Ollama
2. **Dimension Validation**: Confirms 768-dimensional vectors (nomic-embed-text)
3. **Consistency**: Same input produces consistent embeddings
4. **Semantic Similarity**: Related texts have higher similarity scores
5. **Semantic Difference**: Unrelated texts have lower similarity scores
6. **Text Length Handling**: Works with short, medium, and long texts
7. **Technical Domain**: Handles domain-specific content correctly
8. **Complete Workflow**: End-to-end embedding generation and similarity search

**Prerequisites**:
```bash
# 1. Ollama must be running
ollama serve

# 2. Model must be available
ollama pull nomic-embed-text
```

**Running the Test**:
```bash
# Run just this test
mvn test -Dtest=EmbeddingServiceIntegrationTest

# Run with verbose output
mvn test -Dtest=EmbeddingServiceIntegrationTest -X
```

**Test Structure**:
- 9 ordered tests that build upon each other
- Uses real Ollama backend (no mocks)
- Tests actual embedding generation and similarity calculations
- Validates workshop-relevant content (Java, AI agents, etc.)

**Key Assertions**:
- Embeddings are 768-dimensional
- Embeddings are consistent for same input
- Similar texts have cosine similarity > 0.5
- Different texts have lower similarity
- Technical domain content is processed correctly

**Expected Output**:
```
=== Setting up EmbeddingService Integration Test ===
✓ EmbeddingService created
=== Setup complete ===

=== Test 1: Embedding Dimension Configuration ===
✓ Embedding dimension correct

=== Test 2: Single Embedding Generation ===
✓ Single embedding generation successful

[... 7 more tests ...]

=== Test 9: Complete Workflow ===
✓ Complete workflow successful!
=== All EmbeddingService Integration Tests PASSED ===
```

---

### RAGAgentIntegrationTest.java 🚧

**Status**: Disabled - Requires full RAG implementation

**Purpose**: End-to-end test of RAG agent with multi-turn conversation.

**Prerequisites**:
```bash
# 1. PostgreSQL with pgvector
docker-compose up -d

# 2. Ollama with models
ollama pull qwen3:4b
ollama pull nomic-embed-text

# 3. Ingest documents
./ingest.sh
```

**Running the Test**:
```bash
# Remove @Disabled annotation first, then:
mvn test -Dtest=RAGAgentIntegrationTest
```

---

### VectorSearchIntegrationTest.java 🚧

**Status**: Disabled - Requires database setup

**Purpose**: Comprehensive testing of vector search functionality.

**Prerequisites**: Same as RAGAgentIntegrationTest

**Running the Test**:
```bash
# Remove @Disabled annotation first, then:
mvn test -Dtest=VectorSearchIntegrationTest
```

---

## Running All Tests

```bash
# Run all tests (skips disabled ones)
mvn test

# Run only integration tests
mvn test -Dgroups=integration

# Run with full debug output
mvn test -X
```

## Test Guidelines

Following the workshop test requirements:

1. ✅ **Single Happy Path**: Each test focuses on the main workflow
2. ✅ **Real Integration**: Uses actual Ollama backend, no mocks
3. ✅ **Clear Documentation**: Each test has comprehensive logging
4. ✅ **Workshop Relevant**: Tests use domain-specific content

## Troubleshooting

### Test Fails: Connection Refused
```
Error: Connection refused to http://localhost:11434
```
**Solution**: Start Ollama
```bash
ollama serve
```

### Test Fails: Model Not Found
```
Error: Model 'nomic-embed-text' not found
```
**Solution**: Pull the embedding model
```bash
ollama pull nomic-embed-text
```

### Test Fails: Embeddings Have Wrong Dimensions
```
Expected: 768, Actual: 384
```
**Solution**: You may have a different model. Verify with:
```bash
ollama show nomic-embed-text
```

### Slow Test Execution
The embedding generation can take 1-3 seconds per call. This is normal for:
- First-time model loading
- Cold start after Ollama restart
- Large text inputs

Subsequent calls are usually faster due to model caching.

## Test Execution Times

Typical execution times on M1 Mac with Ollama:

| Test | Duration | Notes |
|------|----------|-------|
| Test 1: Dimension Config | <10ms | Configuration only |
| Test 2: Single Embedding | 1-3s | First embedding (model load) |
| Test 3: Different Lengths | 3-6s | 3 embeddings |
| Test 4: Consistency | 2-4s | 2 embeddings |
| Test 5: Similarity | 2-4s | 2 embeddings |
| Test 6: Difference | 2-4s | 2 embeddings |
| Test 7: Comparison | 4-8s | 4 embeddings |
| Test 8: Technical Domain | 4-8s | 4 embeddings |
| Test 9: Complete Workflow | 5-10s | 5 embeddings + similarity |
| **Total** | **25-50s** | Full test suite |

---

## Additional Resources

- [EmbeddingService.java](../../../main/java/com/incept5/workshop/stage4/ingestion/EmbeddingService.java) - Source code
- [Stage 3 README](../../../../../../../README.md) - Stage overview
- [Ollama API Docs](https://github.com/ollama/ollama/blob/main/docs/api.md) - Embedding API reference
- [nomic-embed-text](https://ollama.com/library/nomic-embed-text) - Model information
