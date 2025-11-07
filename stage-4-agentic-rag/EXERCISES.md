
# Stage 4 RAG - Hands-On Learning Exercises

These exercises help you understand how RAG works by making small, targeted changes to the system. They progress from simple observations to building new features.

**Time Required:** 5-60 minutes per exercise  
**Difficulty:** ⭐ Easy → ⭐⭐⭐ Advanced → ⭐⭐⭐⭐⭐ Expert

---

## Table of Contents

**Observation Exercises (No Code Changes)**
- [Exercise 1: See What Gets Chunked](#exercise-1-see-what-gets-chunked-) (5 min) ⭐
- [Exercise 2: Visualize Embeddings](#exercise-2-visualize-embeddings-) (10 min) ⭐
- [Exercise 3: Adjust Search Sensitivity](#exercise-3-adjust-search-sensitivity-) (5 min) ⭐
- [Exercise 4: Experiment with topK](#exercise-4-experiment-with-topk-) (5 min) ⭐

**Modification Exercises**
- [Exercise 5: Add Conversation Context Visualization](#exercise-5-add-conversation-context-visualization-) (10 min) ⭐⭐
- [Exercise 6: Test Context Expansion](#exercise-6-test-context-expansion-) (10 min) ⭐⭐
- [Exercise 7: Implement Custom Chunking Strategy](#exercise-7-implement-custom-chunking-strategy-) (30 min) ⭐⭐⭐
- [Exercise 8: Add Metadata Filtering](#exercise-8-add-metadata-filtering-) (30 min) ⭐⭐⭐
- [Exercise 9: Analyze Search Quality](#exercise-9-analyze-search-quality-) (20 min) ⭐⭐⭐

**Building New Features**
- [Exercise 10: Build a Web UI](#exercise-10-build-a-web-ui-) (60+ min) ⭐⭐⭐⭐

**Quick Experiments (No Code Changes)**
- [Experiment A: Different Question Types](#experiment-a-different-question-types)
- [Experiment B: Multi-Turn Conversations](#experiment-b-multi-turn-conversations)
- [Experiment C: Chunk Size Impact](#experiment-c-chunk-size-impact)

**Advanced Challenges**
- [Challenge 1: Implement Hybrid Search](#challenge-1-implement-hybrid-search) ⭐⭐⭐⭐
- [Challenge 2: Add Re-ranking](#challenge-2-add-re-ranking) ⭐⭐⭐⭐
- [Challenge 3: Implement Query Expansion](#challenge-3-implement-query-expansion) ⭐⭐⭐⭐
- [Challenge 4: Add Persistent Memory](#challenge-4-add-persistent-memory) ⭐⭐⭐⭐⭐
- [Challenge 5: Build Multi-Repository Agent](#challenge-5-build-multi-repository-agent) ⭐⭐⭐⭐⭐

---

## Exercise 1: See What Gets Chunked ⭐

**Goal:** Understand how documents are split for vector search

**Time:** 5 minutes  
**Difficulty:** ⭐ Easy

### What to do

Edit `src/main/java/com/incept5/workshop/stage4/ingestion/DocumentChunker.java`:

```java
// Add this after the line that creates chunks (around line 55):
System.out.println("\n=== CHUNK PREVIEW ===");
System.out.println("Total chunks created: " + chunks.size());
for (int i = 0; i < Math.min(3, chunks.size()); i++) {
    System.out.println("\n--- Chunk " + (i + 1) + " ---");
    System.out.println("─".repeat(50));
    String preview = chunks.get(i).substring(0, Math.min(200, chunks.get(i).length()));
    System.out.println(preview + "...");
    System.out.println("─".repeat(50));
    System.out.println("Estimated tokens: " + estimateTokens(chunks.get(i)));
    System.out.println("Length: " + chunks.get(i).length() + " characters");
}
System.out.println("\n=== END PREVIEW ===\n");
```

**Then run:**
```bash
mvn clean package
./ingest.sh
```

### What you'll learn

- How text gets broken into searchable chunks
- Why overlap matters for context preservation
- How token estimation works (4 chars ≈ 1 token)
- Why chunk size affects retrieval quality

### Questions to explore

**Experiment with different chunk sizes** in `repos.yaml`:

```yaml
settings:
  chunk_size: 200    # Try small chunks
  chunk_size: 800    # Default
  chunk_size: 2000   # Try large chunks
```

**Questions:**
- What happens if chunks are too small? (Try 200)
- What happens if chunks are too large? (Try 2000)
- What happens with no overlap? (Try `chunk_overlap: 0`)
- How does overlap help maintain context?

**Test your changes:**
```bash
./cleanup.sh      # Reset database
./ingest.sh       # Re-ingest with new settings
./run.sh
You: Show me the RAGAgent implementation
```

---

## Exercise 2: Visualize Embeddings ⭐

**Goal:** See the 768-dimensional vectors that power semantic search

**Time:** 10 minutes  
**Difficulty:** ⭐ Easy

### What to do

Edit `src/main/java/com/incept5/workshop/stage4/db/PgVectorStore.java`:

```java
// Add this method to the class:
private float calculateMagnitude(float[] vector) {
    float sum = 0;
    for (float v : vector) {
        sum += v * v;
    }
    return (float) Math.sqrt(sum);
}

// Add this in the store() method, after generating embedding (around line 54):
if (chunkIndex == 0) {  // Only show first chunk per file
    System.out.println("\n=== EMBEDDING VISUALIZATION ===");
    System.out.println("Source: " + source);
    System.out.println("Content preview: " + 
        content.substring(0, Math.min(100, content.length())) + "...");
    
    System.out.println("\nFirst 20 dimensions of 768-dimensional vector:");
    for (int i = 0; i < Math.min(20, embedding.length); i++) {
        System.out.printf("  [%d]: %+.4f%n", i, embedding[i]);
    }
    System.out.println("  ... (748 more dimensions)");
    
    float magnitude = calculateMagnitude(embedding);
    System.out.printf("\nVector magnitude: %.4f%n", magnitude);
    System.out.println("Vector dimensions: " + embedding.length);
    System.out.println("=== END EMBEDDING ===\n");
}
```

**Then run:**
```bash
mvn clean package
./cleanup.sh && ./ingest.sh
```

### What you'll learn

- Embeddings are just lists of numbers (not magic!)
- Similar meanings → similar number patterns
- 768 dimensions capture semantic relationships
- Vectors are normalized (magnitude ≈ 1.0)

### Experiment

Try comparing embeddings for similar vs. different concepts:

1. **Note the embedding for "Embabel framework"**
   - Look at the first 10 numbers

2. **Compare to "Spring AI framework"**
   - Should have similar patterns (both are frameworks)

3. **Compare to "weather API"**
   - Should be quite different (different domain)

**Mathematical insight:**
```
Cosine similarity = dot(A, B) / (||A|| × ||B||)

Similar concepts: similarity ≈ 0.7-0.9
Different concepts: similarity ≈ 0.1-0.3
```

---

## Exercise 3: Adjust Search Sensitivity ⭐

**Goal:** Control how strict vector search is

**Time:** 5 minutes  
**Difficulty:** ⭐ Easy

### What to do

Edit `src/main/java/com/incept5/workshop/stage4/tool/RAGTool.java`:

```java
// In the constructor (around line 36), change:
this(vectorStore, 0.5);  // Default: 0.5 (permissive)

// Try these alternatives one at a time:
this(vectorStore, 0.7);  // Stricter (fewer, more relevant results)
this(vectorStore, 0.3);  // More permissive (more results, less relevant)
this(vectorStore, 0.9);  // Very strict (only near-perfect matches)
this(vectorStore, 0.1);  // Very loose (almost everything matches)
```

**Then run:**
```bash
mvn clean package
./run.sh --verbose
```

**Ask:** "What is Embabel?"

### What you'll learn

- Higher threshold = more precise but might miss relevant docs
- Lower threshold = more results but some may be off-topic
- Sweet spot depends on your data quality and query type
- Trade-off between recall (finding all relevant) and precision (only relevant)

### Questions to explore

Test each threshold with these queries:

**Specific query:** "Who created Embabel?"
- High threshold (0.8): Should find exact answer quickly
- Low threshold (0.3): Might return too much irrelevant context

**Broad query:** "Tell me about agent frameworks"
- High threshold (0.8): Might miss relevant docs
- Low threshold (0.3): Returns more context (good for broad questions)

**Vague query:** "How do I use this?"
- High threshold (0.8): Might return nothing (too vague)
- Low threshold (0.3): Returns diverse results

**Recommendation:** Start at 0.5, adjust based on your use case.

---

## Exercise 4: Experiment with topK ⭐

**Goal:** Understand the quality vs. context trade-off

**Time:** 5 minutes  
**Difficulty:** ⭐ Easy

### What to do

Edit `src/main/java/com/incept5/workshop/stage4/tool/RAGTool.java`:

```java
// Around line 72, change the default topK:
Integer topK = (Integer) arguments.getOrDefault("topK", 5);

// Try these alternatives:
Integer topK = (Integer) arguments.getOrDefault("topK", 1);   // Minimal
Integer topK = (Integer) arguments.getOrDefault("topK", 3);   // Conservative
Integer topK = (Integer) arguments.getOrDefault("topK", 10);  // Generous
Integer topK = (Integer) arguments.getOrDefault("topK", 20);  // Maximum
```

**Then run:**
```bash
mvn clean package
./run.sh --verbose
```

### What you'll learn

- More results = more context but also more noise
- Fewer results = focused but might miss important info
- The LLM has to process all retrieved documents
- More isn't always better (diminishing returns)

### Try asking

**Simple factual query:**
```
You: What is Embabel?
```
- **topK=1**: Probably sufficient (one good doc)
- **topK=10**: Might confuse the LLM with too much context

**Complex example request:**
```
You: Show me all different types of @Action examples
```
- **topK=1**: Definitely insufficient
- **topK=10**: Better (shows variety)

**Broad understanding:**
```
You: Explain how Embabel agents work
```
- **topK=3**: Good balance
- **topK=20**: Overkill (too much to process)

### Observe in verbose mode

Watch the `[TOOL RESULT]` section - more docs = longer context for LLM:

```
[TOOL RESULT]
─────────────────────────────
Document 1 (similarity: 0.85)
...
Document 2 (similarity: 0.82)
...
Document 5 (similarity: 0.71)  ← Notice diminishing relevance
```

**Key insight:** After top 3-5 results, similarity often drops significantly.

---

## Exercise 5: Add Conversation Context Visualization ⭐⭐

**Goal:** See how conversation memory works

**Time:** 10 minutes  
**Difficulty:** ⭐⭐ Moderate

### What to do

Edit `src/main/java/com/incept5/workshop/stage4/agent/RAGAgent.java`:

```java
// Add this method to the class:
private void printConversationContext() {
    System.out.println("\n╔═══════════════════════════════════════════════════╗");
    System.out.println("║         CONVERSATION CONTEXT DEBUG                ║");
    System.out.println("╚═══════════════════════════════════════════════════╝");
    
    System.out.println("Messages in memory: " + memory.size());
    
    String history = memory.formatHistory();
    int estimatedTokens = history.length() / 4;
    System.out.println("Estimated tokens: " + estimatedTokens);
    System.out.println("Max tokens allowed: " + memory.getMaxTokensEstimate());
    
    double percentUsed = (estimatedTokens * 100.0) / memory.getMaxTokensEstimate();
    System.out.printf("Memory usage: %.1f%%%n", percentUsed);
    
    System.out.println("\nFormatted history sent to LLM:");
    System.out.println("─".repeat(70));
    System.out.println(history.substring(0, Math.min(500, history.length())) + 
        (history.length() > 500 ? "...\n[" + (history.length() - 500) + " more chars]" : ""));
    System.out.println("─".repeat(70));
}

// Add this in the chat() method, after buildPromptWithHistory() (around line 71):
if (verbose) {
    printConversationContext();
}
```

**Note:** You'll need to add a getter to `ConversationMemory.java`:

```java
public int getMaxTokensEstimate() {
    return maxTokensEstimate;
}
```

**Then run:**
```bash
mvn clean package
./run.sh --verbose
```

### What you'll learn

- How the agent maintains conversation state
- Why follow-up questions work
- How memory is trimmed to stay within token limits
- When context becomes too large

### Have this conversation

```
Turn 1: What is Embabel?
Turn 2: Who created it?          ← "it" refers to previous context
Turn 3: Show me an example        ← "example" implies Embabel example
Turn 4: What does @Action do?     ← New question using Embabel context
Turn 5: Give me another example   ← "another" refers to previous example
```

**Observe:**
- How conversation history grows
- When old messages get trimmed
- How pronouns ("it", "another") work via context

### Experiment

Try hitting memory limits:

```bash
# Ask many questions in a row
You: What is Embabel?
You: Who created it?
You: Show me an example
You: What's an Action?
You: What's a Goal?
You: Show another example
You: How does it integrate with Spring?
You: What's the Tripper app?
You: How do I get started?
You: Where's the documentation?
```

**Watch:** Memory usage increase, then old messages get trimmed.

---

## Exercise 6: Test Context Expansion ⭐⭐

**Goal:** Understand neighboring chunk retrieval

**Time:** 10 minutes  
**Difficulty:** ⭐⭐ Moderate

### What to do

First, modify `src/main/java/com/incept5/workshop/stage4/tool/RAGTool.java`:

```java
// In the expandWithNeighbors() method (around line 119), add debugging:
private List<Document> expandWithNeighbors(List<Document> documents) throws Exception {
    System.out.println("\n╔═══════════════════════════════════════════════════╗");
    System.out.println("║           CONTEXT EXPANSION DEBUG                 ║");
    System.out.println("╚═══════════════════════════════════════════════════╝");
    
    System.out.println("Initial documents: " + documents.size());
    
    Set<String> seen = new LinkedHashSet<>();
    List<Document> expanded = new ArrayList<>();
    
    for (Document doc : documents) {
        System.out.printf("  Expanding: %s chunk %d (similarity: %.2f)%n",
            doc.source(), doc.chunkIndex(), doc.similarity());
        
        // Get neighboring chunks (radius = 1 means immediate neighbors)
        List<Document> neighbors = vectorStore.getNeighboringChunks(doc, 1);
        
        System.out.printf("    → Found %d neighbors (chunks %d to %d)%n",
            neighbors.size(),
            neighbors.isEmpty() ? 0 : neighbors.get(0).chunkIndex(),
            neighbors.isEmpty() ? 0 : neighbors.get(neighbors.size() - 1).chunkIndex());
        
        for (Document neighbor : neighbors) {
            String key = neighbor.source() + ":" + neighbor.fileHash() + ":" + neighbor.chunkIndex();
            
            if (!seen.contains(key)) {
                seen.add(key);
                expanded.add(neighbor);
            }
        }
    }
    
    System.out.println("\nTotal after expansion: " + expanded.size() + " documents");
    System.out.println("═".repeat(55) + "\n");
    
    logger.info("Expanded {} documents to {} with neighbors", documents.size(), expanded.size());
    return expanded;
}
```

**Then run:**
```bash
mvn clean package
./run.sh --verbose
```

### Test it

**Without expansion (default):**
```
You: Show me a Spring AI ChatClient example
```

**With expansion:**
```
You: Search with expandContext=true for "Spring AI ChatClient example"
```

### What you'll learn

- How code examples get split across chunks
- Why retrieving neighboring chunks helps
- The trade-off: more context vs. more tokens to process
- How chunk indices work (sequential numbers per file)

### Observe

**Without expansion:**
```
Found 5 relevant documents
Document 1: ... ChatClient client = ...
Document 2: ... other unrelated code ...
```

**With expansion:**
```
Found 12 documents (expanded with neighbors)
Document 1: ... imports ...
Document 2: ... class declaration ...
Document 3: ... ChatClient client = ...  ← Original match
Document 4: ... method body continues ...
Document 5: ... example usage ...
```

**Result:** More complete code examples!

### When to use expansion

✅ **Use when:**
- Looking for code examples
- Need full method/class definitions
- Context is split across boundaries

❌ **Don't use when:**
- Simple factual questions
- Already getting good answers
- Token limit is a concern

---

## Exercise 7: Implement Custom Chunking Strategy ⭐⭐⭐

**Goal:** Create a smarter chunker for code files

**Time:** 30 minutes  
**Difficulty:** ⭐⭐⭐ Advanced

### The Challenge

The current chunker splits at fixed boundaries (paragraphs, character counts). This can break code in awkward places.

**Your mission:** Modify `DocumentChunker.java` to split at function/class boundaries.

### Hints

Look for these patterns in Java code:
```java
// Class boundaries
Pattern.compile("^(public|private|protected)?\\s*(class|interface|enum)\\s+\\w+");

// Method boundaries
Pattern.compile("^\\s*(public|private|protected)?\\s*\\w+\\s+\\w+\\s*\\(");

// Annotation boundaries
Pattern.compile("^\\s*@\\w+");
```

Keep together:
- Complete method definitions (don't split mid-method)
- Import statements and package declarations
- Class/interface definitions with their methods
- Annotation with the element it annotates

### Starter Code

```java
// Add this method to DocumentChunker.java:
private boolean isMethodOrClassBoundary(String line) {
    String trimmed = line.trim();
    
    // Method declaration
    if (trimmed.matches("(public|private|protected)?\\s*\\w+.*\\(.*\\).*\\{?")) {
        return true;
    }
    
    // Class/interface declaration
    if (trimmed.matches("(public|private|protected)?\\s*(class|interface|enum)\\s+\\w+.*")) {
        return true;
    }
    
    // Annotation
    if (trimmed.startsWith("@")) {
        return true;
    }
    
    return false;
}

// Add this method to find the next good split point:
private int findNextCodeBoundary(String text, int startPos) {
    String[] lines = text.substring(startPos).split("\n");
    int currentPos = 0;
    
    for (String line : lines) {
        if (isMethodOrClassBoundary(line)) {
            return startPos + currentPos;
        }
        currentPos += line.length() + 1; // +1 for newline
    }
    
    return -1; // No boundary found
}
```

### Modify splitLargeSection()

```java
// Around line 95, replace the simple split logic:
private List<String> splitLargeSection(String section, String fileHeader) {
    List<String> chunks = new ArrayList<>();
    
    // Try to split at code boundaries
    int pos = 0;
    while (pos < section.length()) {
        int nextBoundary = findNextCodeBoundary(section, pos);
        
        if (nextBoundary == -1 || (nextBoundary - pos) > chunkSize * 5) {
            // No boundary found or too far away - use paragraph split
            // ... fall back to original logic
        } else {
            // Split at code boundary
            String chunk = section.substring(pos, nextBoundary);
            if (estimateTokens(chunk) >= chunkSize / 2) {  // Only if reasonable size
                chunks.add(chunk);
                pos = nextBoundary;
            } else {
                pos = nextBoundary;  // Too small, keep accumulating
            }
        }
    }
    
    return chunks;
}
```

### Test Your Changes

```bash
mvn clean package
./cleanup.sh
./ingest.sh
./run.sh
```

**Ask:**
```
You: Show me the RAGAgent class implementation
You: What does the PgVectorStore.search method do?
You: Show me how ConversationMemory works
```

### Success Criteria

✅ Agent returns complete method definitions  
✅ No cut-off code blocks mid-method  
✅ Better answers about specific classes/methods  
✅ Methods aren't split from their documentation  

### Bonus

Add support for other languages:
- Python: `def function_name(`, `class ClassName:`
- JavaScript: `function name(`, `class Name {`
- Kotlin: `fun functionName(`, `class ClassName`

---

## Exercise 8: Add Metadata Filtering ⭐⭐⭐

**Goal:** Search within specific repositories

**Time:** 30 minutes  
**Difficulty:** ⭐⭐⭐ Advanced

### The Challenge

Currently, vector search looks across ALL repositories. Sometimes you want to filter by source.

**Example use case:** "Search only embabel-agent repository for Action examples"

### Step 1: Modify PgVectorStore

Edit `src/main/java/com/incept5/workshop/stage4/db/PgVectorStore.java`:

```java
/**
 * Search with optional metadata filters.
 * 
 * @param query The search query
 * @param topK Number of results
 * @param threshold Minimum similarity
 * @param filters Metadata filters (e.g., "source" -> "embabel-agent")
 * @return List of matching documents
 */
public List<Document> search(String query, int topK, double threshold, 
                            Map<String, String> filters) throws SQLException {
    // Generate query embedding
    float[] queryEmbedding = embeddingService.generateEmbedding(query);
    
    // Build dynamic SQL with filters
    StringBuilder sql = new StringBuilder("""
        SELECT id, content, source, file_hash, chunk_index, metadata,
               1 - (embedding <=> ?::vector) as similarity
        FROM documents
        WHERE 1 - (embedding <=> ?::vector) > ?
        """);
    
    // Add source filter
    if (filters != null && filters.containsKey("source")) {
        sql.append(" AND source = ?");
    }
    
    // Add metadata JSONB filters
    if (filters != null && filters.containsKey("repository")) {
        sql.append(" AND metadata->>'repository' = ?");
    }
    
    sql.append(" ORDER BY similarity DESC LIMIT ?");
    
    // Execute with parameters
    List<Document> results = new ArrayList<>();
    
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
        
        int paramIndex = 1;
        PGvector pgVector = new PGvector(queryEmbedding);
        stmt.setObject(paramIndex++, pgVector);
        stmt.setObject(paramIndex++, pgVector);
        stmt.setDouble(paramIndex++, threshold);
        
        // Bind filter parameters
        if (filters != null && filters.containsKey("source")) {
            stmt.setString(paramIndex++, filters.get("source"));
        }
        if (filters != null && filters.containsKey("repository")) {
            stmt.setString(paramIndex++, filters.get("repository"));
        }
        
        stmt.setInt(paramIndex++, topK);
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                // ... (existing result mapping code)
            }
        }
    }
    
    logger.info("Vector search with filters returned {} results", results.size());
    return results;
}

// Keep the old method for backward compatibility
public List<Document> search(String query, int topK, double threshold) throws SQLException {
    return search(query, topK, threshold, null);
}
```

### Step 2: Update RAGTool

Edit `src/main/java/com/incept5/workshop/stage4/tool/RAGTool.java`:

```java
@Override
public String getParameterSchema() {
    return """
        {
          "type": "object",
          "properties": {
            "query": {
              "type": "string",
              "description": "The search query to find relevant documentation"
            },
            "topK": {
              "type": "integer",
              "description": "Number of results to return (default: 5, max: 10)"
            },
            "expandContext": {
              "type": "boolean",
              "description": "Include neighboring chunks for more context"
            },
            "source": {
              "type": "string",
              "description": "Filter by source repository (e.g., 'embabel-agent', 'spring-ai')",
              "enum": ["embabel-agent", "embabel-examples", "embabel-java-template", 
                       "embabel-kotlin-template", "tripper"]
            }
          },
          "required": ["query"]
        }
        """;
}

@Override
public String execute(Map<String, Object> arguments) throws Exception {
    // Extract parameters
    String query = (String) arguments.get("query");
    Integer topK = (Integer) arguments.getOrDefault("topK", 5);
    Boolean expandContext = (Boolean) arguments.getOrDefault("expandContext", false);
    String source = (String) arguments.get("source");
    
    // Build filters
    Map<String, String> filters = null;
    if (source != null && !source.isBlank()) {
        filters = new HashMap<>();
        filters.put("source", source);
        logger.info("Filtering search by source: {}", source);
    }
    
    // Perform search with filters
    List<Document> documents = vectorStore.search(query, topK, defaultThreshold, filters);
    
    if (documents.isEmpty()) {
        if (source != null) {
            return String.format("No relevant documentation found in '%s' for the query. " +
                               "Try searching without source filter.", source);
        }
        return "No relevant documentation found for the query.";
    }
    
    // ... rest of the method
}
```

### Step 3: Test It

```bash
mvn clean package
./run.sh --verbose
```

**Try these:**
```
You: Search embabel-agent for Action examples
You: Search tripper for travel planning code
You: Search spring-ai for ChatClient usage
You: What's in the embabel-examples repository?
```

### What you'll learn

- Combining semantic and structured search
- How metadata enriches RAG
- SQL + vector search hybrid queries
- Performance implications of filters

### Bonus Features

Add more filter types:

```java
// Filter by date range
filters.put("created_after", "2024-01-01");

// Filter by file type
filters.put("file_type", "java");

// Multiple sources (OR condition)
filters.put("sources", "embabel-agent,embabel-examples");
```

---

## Exercise 9: Analyze Search Quality ⭐⭐⭐

**Goal:** Measure retrieval accuracy

**Time:** 20 minutes  
**Difficulty:** ⭐⭐⭐ Advanced

### What to do

Create a test suite to evaluate search quality.

**Create:** `src/test/java/com/incept5/workshop/stage4/SearchQualityTest.java`

```java
package com.incept5.workshop.stage4;

import com.incept5.workshop.stage4.db.DatabaseConfig;
import com.incept5.workshop.stage4.db.Document;
import com.incept5.workshop.stage4.db.PgVectorStore;
import com.incept5.workshop.stage4.ingestion.EmbeddingService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to evaluate RAG search quality.
 * 
 * Measures:
 * - Precision: Are retrieved documents relevant?
 * - Recall: Do we find all relevant documents?
 * - Ranking: Are most relevant docs ranked highest?
 */
public class SearchQualityTest {
    
    private static PgVectorStore vectorStore;
    
    @BeforeAll
    static void setup() throws Exception {
        DataSource dataSource = DatabaseConfig.createDataSource();
        EmbeddingService embeddingService = EmbeddingService.fromEnvironment();
        vectorStore = new PgVectorStore(dataSource, embeddingService);
    }
    
    @Test
    void testFactualQueries() throws Exception {
        // Test cases: query -> expected source
        Map<String, String> testCases = Map.of(
            "What is Embabel?", "embabel-agent",
            "Who created Spring AI?", "spring-ai",
            "Show @Agent annotation example", "embabel-examples",
            "How to use ChatClient", "spring-ai",
            "What is GOAP?", "embabel-agent"
        );
        
        int correct = 0;
        int total = testCases.size();
        
        System.out.println("\n=== FACTUAL QUERY EVALUATION ===\n");
        
        for (var entry : testCases.entrySet()) {
            String query = entry.getKey();
            String expectedSource = entry.getValue();
            
            List<Document> results = vectorStore.search(query, 5, 0.5);
            
            boolean found = !results.isEmpty() && 
                          results.get(0).source().contains(expectedSource);
            
            if (found) correct++;
            
            System.out.printf("Query: %s%n", query);
            System.out.printf("  Expected: %s%n", expectedSource);
            System.out.printf("  Actual: %s%n", 
                results.isEmpty() ? "none" : results.get(0).source());
            System.out.printf("  Result: %s%n%n", found ? "✓ PASS" : "✗ FAIL");
        }
        
        double accuracy = (correct * 100.0) / total;
        System.out.printf("Accuracy: %.1f%% (%d/%d)%n%n", accuracy, correct, total);
        
        assertTrue(accuracy >= 60.0, 
            "Search accuracy should be at least 60% (got " + accuracy + "%)");
    }
    
    @Test
    void testSimilarityScores() throws Exception {
        String query = "What is Embabel?";
        List<Document> results = vectorStore.search(query, 10, 0.3);
        
        System.out.println("\n=== SIMILARITY SCORE ANALYSIS ===\n");
        System.out.println("Query: " + query);
        System.out.println("\nTop 10 results:");
        
        double previousScore = 1.0;
        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            System.out.printf("%2d. %.3f | %s | %s%n",
                i + 1, doc.similarity(), doc.source(),
                doc.content().substring(0, Math.min(60, doc.content().length())) + "...");
            
            // Verify scores are descending
            assertTrue(doc.similarity() <= previousScore,
                "Similarity scores should be in descending order");
            previousScore = doc.similarity();
        }
        
        // Check score distribution
        if (results.size() >= 5) {
            double topScore = results.get(0).similarity();
            double fifthScore = results.get(4).similarity();
            double scoreDrop = topScore - fifthScore;
            
            System.out.printf("%nScore drop (1st to 5th): %.3f%n", scoreDrop);
            
            // Expect at least 0.05 drop (5%) from top to 5th
            assertTrue(scoreDrop >= 0.05,
                "Top results should have significantly higher scores than lower results");
        }
    }
    
    @Test
    void testThresholdSensitivity() throws Exception {
        String query = "Embabel agent framework";
        
        System.out.println("\n=== THRESHOLD SENSITIVITY ===\n");
        System.out.println("Query: " + query);
        System.out.println();
        
        double[] thresholds = {0.3, 0.5, 0.7, 0.9};
        
        for (double threshold : thresholds) {
            List<Document> results = vectorStore.search(query, 10, threshold);
            
            double avgScore = results.stream()
                .mapToDouble(Document::similarity)
                .average()
                .orElse(0.0);
            
            System.out.printf("Threshold %.1f: %d results (avg score: %.3f)%n",
                threshold, results.size(), avgScore);
        }
        
        // At low threshold, should get many results
        List<Document> lowThreshold = vectorStore.search(query, 10, 0.3);
        assertTrue(lowThreshold.size() >= 5,
            "Low threshold should return multiple results");
        
        // At high threshold, might get fewer but higher quality
        List<Document> highThreshold = vectorStore.search(query, 10, 0.8);
        if (!highThreshold.isEmpty()) {
            double avgScore = highThreshold.stream()
                .mapToDouble(Document::similarity)
                .average()
                .orElse(0.0);
            
            assertTrue(avgScore >= 0.8,
                "High threshold results should have high average similarity");
        }
    }
}
```

### Run the tests

```bash
mvn test -Dtest=SearchQualityTest
```

### What you'll learn

- How to evaluate RAG systems objectively
- Common failure modes in vector search
- When to adjust thresholds/topK
- How to measure retrieval quality

### Extend it

Add more test scenarios:

```java
@Test
void testCodeExampleRetrieval() {
    // Test that code examples are retrieved intact
}

@Test
void testMultiTurnQueries() {
    // Test contextual follow-up questions
}

@Test
void testNegativeQueries() {
    // Test queries that should return nothing
}
```

---

## Exercise 10: Build a Web UI ⭐⭐⭐⭐

**Goal:** Create a simple web interface with streaming responses

**Time:** 60+ minutes  
**Difficulty:** ⭐⭐⭐⭐ Expert

### What to build

A web UI where users can chat with the RAG agent, with:
- Real-time streaming responses (word-by-word)
- Conversation history display
- Source attribution for retrieved docs
- Clear button to reset conversation

### Step 1: Add Javalin dependency

Edit `pom.xml`:

```xml
<dependency>
    <groupId>io.javalin</groupId>
    <artifactId>javalin</artifactId>
    <version>6.1.3</version>
</dependency>
```

### Step 2: Create Web Server

**Create:** `src/main/java/com/incept5/workshop/stage4/web/RAGWebServer.java`

```java
package com.incept5.workshop.stage4.web;

import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.backend.BackendFactory;
import com.incept5.workshop.stage4.agent.RAGAgent;
import com.incept5.workshop.stage4.db.DatabaseConfig;
import com.incept5.workshop.stage4.db.PgVectorStore;
import com.incept5.workshop.stage4.ingestion.EmbeddingService;
import com.incept5.workshop.stage4.tool.RAGTool;
import com.incept5.workshop.stage4.tool.ToolRegistry;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RAGWebServer {
    
    private final RAGAgent agent;
    private final Map<String, RAGAgent> sessions = new ConcurrentHashMap<>();
    
    public RAGWebServer() throws Exception {
        // Initialize RAG components
        DataSource dataSource = DatabaseConfig.createDataSource();
        EmbeddingService embeddingService = EmbeddingService.fromEnvironment();
        PgVectorStore vectorStore = new PgVectorStore(dataSource, embeddingService);
        
        AIBackend backend = BackendFactory.createOllamaBackend(
            "http://localhost:11434",
            "incept5/Jan-v1-2509:fp16"
        );
        
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new RAGTool(vectorStore));
        
        this.agent = RAGAgent.builder()
            .backend(backend)
            .toolRegistry(toolRegistry)
            .verbose(false)
            .build();
    }
    
    public void start() {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).start(7070);
        
        // Serve HTML
        app.get("/", ctx -> ctx.html(getIndexHtml()));
        
        // Chat endpoint with Server-Sent Events
        app.sse("/chat", client -> {
            client.onMessage(msg -> {
                String sessionId = client.ctx().queryParam("session");
                if (sessionId == null) sessionId = "default";
                
                RAGAgent sessionAgent = sessions.computeIfAbsent(sessionId, k -> agent);
                
                try {
                    String response = sessionAgent.chat(msg);
                    
                    // Stream response word by word
                    String[] words = response.split(" ");
                    for (String word : words) {
                        client.sendEvent("message", word + " ");
                        Thread.sleep(50); // Simulate streaming
                    }
                    
                    client.sendEvent("done", "");
                } catch (Exception e) {
                    client.sendEvent("error", e.getMessage());
                }
            });
        });
        
        // Clear conversation endpoint
        app.post("/clear", ctx -> {
            String sessionId = ctx.queryParam("session");
            if (sessionId != null) {
                sessions.remove(sessionId);
            }
            ctx.result("OK");
        });
        
        System.out.println("🚀 RAG Web UI running at http://localhost:7070");
    }
    
    private String getIndexHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>RAG Chat</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        max-width: 800px;
                        margin: 50px auto;
                        padding: 20px;
                        background: #f5f5f5;
                    }
                    #chat-container {
                        background: white;
                        border-radius: 10px;
                        padding: 20px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        height: 500px;
                        overflow-y: auto;
                        margin-bottom: 20px;
                    }
                    .message {
                        margin: 10px 0;
                        padding: 10px;
                        border-radius: 5px;
                    }
                    .user-message {
                        background: #007bff;
                        color: white;
                        text-align: right;
                    }
                    .agent-message {
                        background: #e9ecef;
                        color: #333;
                    }
                    #input-container {
                        display: flex;
                        gap: 10px;
                    }
                    #message-input {
                        flex: 1;
                        padding: 10px;
                        border: 1px solid #ddd;
                        border-radius: 5px;
                        font-size: 16px;
                    }
                    button {
                        padding: 10px 20px;
                        border: none;
                        border-radius: 5px;
                        cursor: pointer;
                        font-size: 16px;
                    }
                    #send-btn {
                        background: #007bff;
                        color: white;
                    }
                    #clear-btn {
                        background: #dc3545;
                        color: white;
                    }
                </style>
            </head>
            <body>
                <h1>💬 RAG Agent Chat</h1>
                <div id="chat-container"></div>
                <div id="input-container">
                    <input type="text" id="message-input" placeholder="Ask a question...">
                    <button id="send-btn">Send</button>
                    <button id="clear-btn">Clear</button>
                </div>
                
                <script>
                    const chatContainer = document.getElementById('chat-container');
                    const messageInput = document.getElementById('message-input');
                    const sendBtn = document.getElementById('send-btn');
                    const clearBtn = document.getElementById('clear-btn');
                    
                    let currentMessage = null;
                    
                    function addMessage(content, isUser) {
                        const div = document.createElement('div');
                        div.className = 'message ' + (isUser ? 'user-message' : 'agent-message');
                        div.textContent = content;
                        chatContainer.appendChild(div);
                        chatContainer.scrollTop = chatContainer.scrollHeight;
                        return div;
                    }
                    
                    function sendMessage() {
                        const message = messageInput.value.trim();
                        if (!message) return;
                        
                        addMessage(message, true);
                        messageInput.value = '';
                        sendBtn.disabled = true;
                        
                        currentMessage = addMessage('', false);
                        
                        const eventSource = new EventSource('/chat?session=default');
                        
                        eventSource.addEventListener('message', (e) => {
                            currentMessage.textContent += e.data;
                            chatContainer.scrollTop = chatContainer.scrollHeight;
                        });
                        
                        eventSource.addEventListener('done', () => {
                            eventSource.close();
                            sendBtn.disabled = false;
                            messageInput.focus();
                        });
                        
                        eventSource.addEventListener('error', (e) => {
                            currentMessage.textContent += '\\n[Error: ' + e.data + ']';
                            eventSource.close();
                            sendBtn.disabled = false;
                        });
                        
                        // Send the message
                        fetch('/chat?session=default', {
                            method: 'POST',
                            body: message
                        });
                    }
                    
                    function clearChat() {
                        if (confirm('Clear conversation history?')) {
                            fetch('/clear?session=default', { method: 'POST' });
                            chatContainer.innerHTML = '';
                        }
                    }
                    
                    sendBtn.addEventListener('click', sendMessage);
                    clearBtn.addEventListener('click', clearChat);
                    messageInput.addEventListener('keypress', (e) => {
                        if (e.key === 'Enter') sendMessage();
                    });
                    
                    messageInput.focus();
                </script>
            </body>
            </html>
            """;
    }
    
    public static void main(String[] args) throws Exception {
        new RAGWebServer().start();
    }
}
```

### Step 3: Run it

```bash
mvn clean package
java -cp target/stage-4-agentic-rag.jar \
    com.incept5.workshop.stage4.web.RAGWebServer
```

**Open:** http://localhost:7070

### What you'll learn

- Server-Sent Events for streaming
- Building production RAG UIs
- Real-time agent interactions
- Session management

### Enhancements

Add these features:

1. **Source attribution**: Show which docs were used
2. **Typing indicator**: Show when agent is thinking
3. **Export chat**: Download conversation as JSON
4. **Dark mode**: Add theme toggle
5. **Voice input**: Use Web Speech API

---

## Quick Experiments (No Code Changes)

### Experiment A: Different Question Types

Try these and observe retrieval behavior:

```
1. Factual: "What is Embabel?"
   → Should return definition quickly

2. Procedural: "How do I create an agent?"
   → Should return step-by-step guide

3. Comparative: "Compare Embabel and LangChain"
   → Needs docs from multiple sources

4. Example-seeking: "Show me a complex agent"
   → Should return code examples

5. Troubleshooting: "Why isn't my @Goal working?"
   → Needs diagnostic information
```

**Observe:** 
- Which types work best?
- Why do some fail?
- How can you improve retrieval for each type?

### Experiment B: Multi-Turn Conversations

Test conversation memory:

```
Turn 1: "What is Embabel?"
        → Agent learns about Embabel

Turn 2: "Who created it?"
        → "it" refers to Embabel from context

Turn 3: "Show me an example"
        → "example" implies Embabel example

Turn 4: "What's the difference between Actions and Goals?"
        → New context, but still Embabel-related

Turn 5: "Give me another example"
        → "another" refers to previous example
```

**Observe:**
- When does context help vs. confuse?
- How long does the agent remember?
- What happens when context changes?

### Experiment C: Chunk Size Impact

In `repos.yaml`, try different settings:

```yaml
# Small chunks - very focused retrieval
settings:
  chunk_size: 200
  chunk_overlap: 50

# Medium chunks - balanced (default)
settings:
  chunk_size: 800
  chunk_overlap: 200

# Large chunks - more context
settings:
  chunk_size: 2000
  chunk_overlap: 400
```

**Test with:** "Show me the RAGAgent implementation"

**Re-ingest after each change:**
```bash
./cleanup.sh
./ingest.sh
```

**Observe:**
- Small chunks: More precise but might miss context
- Large chunks: More context but might be too broad
- How does overlap help?

---

## Advanced Challenges

### Challenge 1: Implement Hybrid Search ⭐⭐⭐⭐

Combine vector search (semantic) with full-text search (keyword) using PostgreSQL's `tsvector`.

**Why?** Some queries work better with keywords ("find all uses of ChatClient") while others need semantics ("how do agents work?").

**Hint:** Use PostgreSQL's GIN index on `to_tsvector(content)` and combine with vector similarity.

### Challenge 2: Add Re-ranking ⭐⭐⭐⭐

After retrieval, use a smaller model to re-rank documents by relevance.

**Why?** Initial retrieval might return 20 docs, but only top 5 are relevant. Re-ranking improves precision.

**Hint:** Use a cross-encoder model to score query-document pairs.

### Challenge 3: Implement Query Expansion ⭐⭐⭐⭐

Have the LLM generate multiple diverse queries, retrieve for each, then combine results.

**Why?** User query "how to make agents" could be expanded to:
- "creating agent instances"
- "agent construction patterns"
- "@Agent annotation usage"

**Hint:** Use the LLM to generate 3-5 diverse phrasings, search for each, combine with reciprocal rank fusion.

### Challenge 4: Add Persistent Memory ⭐⭐⭐⭐⭐

Store conversation history in PostgreSQL for multi-session support.

**Why?** Users want to resume conversations later, across different sessions or devices.

**Schema:**
```sql
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    conversation_id UUID REFERENCES conversations(id),
    role VARCHAR(50),
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Challenge 5: Build Multi-Repository Agent ⭐⭐⭐⭐⭐

Create specialized agents for different codebases, orchestrate them with a meta-agent.

**Why?** Different repos need different expertise. A meta-agent routes questions to the right specialist.

**Architecture:**
```
User Question
    ↓
Meta-Agent (Router)
    ↓
    ├→ Embabel Expert Agent
    ├→ Spring AI Expert Agent
    └→ General Code Agent
```

**Hint:** Use a classifier to determine which specialist should handle each question.

---

## Tips for Success

### Development Workflow

1. **Always rebuild after code changes:**
   ```bash
   mvn clean package
   ```

2. **Reset database when changing ingestion:**
   ```bash
   ./cleanup.sh
   ./ingest.sh
   ```

3. **Use verbose mode for debugging:**
   ```bash
   ./run.sh --verbose
   ```

4. **Test with simple queries first:**
   ```
   You: What is Embabel?  # Start simple
   You: Show me a complex multi-agent system  # Then get complex
   ```

### Common Pitfalls

❌ **Don't:** Make multiple changes at once  
✅ **Do:** Change one thing, test, then iterate

❌ **Don't:** Skip rebuilding after changes  
✅ **Do:** `mvn clean package` every time

❌ **Don't:** Test with vague questions  
✅ **Do:** Start with specific, well-defined questions

❌ **Don't:** Ignore SQL errors  
✅ **Do:** Check PostgreSQL logs: `docker-compose logs db`

### Performance Tips

⚡ **Faster ingestion:** Use smaller embedding model  
⚡ **Faster queries:** Lower topK (try 3 instead of 5)  
⚡ **Less memory:** Reduce conversation history (try 5 instead of 10)  
⚡ **Better quality:** Increase similarity threshold (try 0.7 instead of 0.5)

---

## Getting Help

**Code not working?**
1. Check the error message carefully
2. Verify database is running: `docker-compose ps`
3. Verify Python service is running: `curl http://localhost:8001/health`
4. Check logs: `docker-compose logs db`
5. Try verbose mode: `./run.sh --verbose`

**Retrieval not working well?**
1. Check document count: `SELECT COUNT(*) FROM documents;`
2. Test direct vector search (see Exercise 9)
3. Adjust similarity threshold (see Exercise 3)
4. Try different topK values (see Exercise 4)

**Want to contribute?**
Found an interesting exercise or improvement? Open a PR!

---

**Happy Learning! 🚀**

Try the exercises in order, or jump to what interests you most. The key is experimentation - break things, fix them, and learn how RAG really works.
