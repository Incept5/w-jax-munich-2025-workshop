
package com.incept5.workshop.stage3;

import com.incept5.workshop.stage3.ingestion.EmbeddingService;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmbeddingService Integration Test - Single Happy Path
 * 
 * This test verifies the complete embedding generation workflow:
 * 1. Service connects to Ollama backend
 * 2. Generates embeddings for various text inputs
 * 3. Validates embedding dimensions and properties
 * 4. Tests semantic similarity between related and unrelated texts
 * 
 * Prerequisites:
 * - Ollama running at http://localhost:11434
 * - Model available: nomic-embed-text (run: ollama pull nomic-embed-text)
 * 
 * This is a single comprehensive test following workshop standards:
 * - Tests real integration (no mocks)
 * - Uses actual Ollama backend
 * - Verifies embedding quality metrics
 * - Tests semantic similarity properties
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmbeddingServiceIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingServiceIntegrationTest.class);
    
    // Configuration
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String EMBEDDING_MODEL = "nomic-embed-text";
    private static final int EXPECTED_DIMENSIONS = 768;
    
    // Test data - semantically similar pairs
    private static final String[] SIMILAR_TEXTS = {
        "The cat sat on the mat",
        "A feline rested on the rug"
    };
    
    // Test data - semantically different texts
    private static final String[] DIFFERENT_TEXTS = {
        "The cat sat on the mat",
        "Machine learning models require large datasets"
    };
    
    private static EmbeddingService embeddingService;
    
    @BeforeAll
    static void setup() {
        logger.info("=== Setting up EmbeddingService Integration Test ===");
        
        embeddingService = new EmbeddingService(OLLAMA_BASE_URL, EMBEDDING_MODEL);
        logger.info("✓ EmbeddingService created");
        logger.info("  Base URL: {}", OLLAMA_BASE_URL);
        logger.info("  Model: {}", EMBEDDING_MODEL);
        logger.info("  Expected dimensions: {}", EXPECTED_DIMENSIONS);
        logger.info("=== Setup complete ===\n");
    }
    
    @Test
    @Order(1)
    @DisplayName("1. Test embedding dimension configuration")
    void testEmbeddingDimension() {
        logger.info("\n=== Test 1: Embedding Dimension Configuration ===");
        
        int dimension = embeddingService.getEmbeddingDimension();
        
        logger.info("  Configured dimension: {}", dimension);
        assertEquals(EXPECTED_DIMENSIONS, dimension, 
            "nomic-embed-text should produce 768-dimensional embeddings");
        
        logger.info("✓ Embedding dimension correct\n");
    }
    
    @Test
    @Order(2)
    @DisplayName("2. Test single text embedding generation")
    void testSingleEmbeddingGeneration() {
        logger.info("\n=== Test 2: Single Embedding Generation ===");
        
        String testText = "This is a test sentence for embedding generation.";
        logger.info("  Test text: '{}'", testText);
        
        long startTime = System.currentTimeMillis();
        float[] embedding = embeddingService.generateEmbedding(testText);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("  Generation time: {}ms", duration);
        
        // Verify embedding is not null
        assertNotNull(embedding, "Embedding should not be null");
        
        // Verify correct dimensions
        assertEquals(EXPECTED_DIMENSIONS, embedding.length, 
            "Embedding should have " + EXPECTED_DIMENSIONS + " dimensions");
        
        // Verify embedding has non-zero values
        float sum = 0;
        float sumOfSquares = 0;
        for (float value : embedding) {
            sum += Math.abs(value);
            sumOfSquares += value * value;
        }
        
        assertTrue(sum > 0, "Embedding should have non-zero values");
        
        // Log sample values
        logger.info("  Embedding dimensions: {}", embedding.length);
        logger.info("  Sample values: [{}, {}, {}, {}, {}]", 
            String.format("%.6f", embedding[0]),
            String.format("%.6f", embedding[1]),
            String.format("%.6f", embedding[2]),
            String.format("%.6f", embedding[3]),
            String.format("%.6f", embedding[4]));
        logger.info("  Sum of absolute values: {}", String.format("%.6f", sum));
        logger.info("  L2 norm: {}", String.format("%.6f", Math.sqrt(sumOfSquares)));
        
        logger.info("✓ Single embedding generation successful\n");
    }
    
    @Test
    @Order(3)
    @DisplayName("3. Test embedding generation for different text lengths")
    void testDifferentTextLengths() {
        logger.info("\n=== Test 3: Different Text Lengths ===");
        
        String[] texts = {
            "Short",
            "This is a medium length sentence with several words.",
            "This is a much longer paragraph that contains multiple sentences and ideas. " +
            "It tests how the embedding model handles longer text inputs. " +
            "The model should still produce consistent dimensional embeddings regardless of input length."
        };
        
        for (int i = 0; i < texts.length; i++) {
            String text = texts[i];
            int length = text.length();
            
            logger.info("\n  Text {}: {} chars", i + 1, length);
            logger.info("    Content: {}...", 
                text.substring(0, Math.min(60, text.length())));
            
            long startTime = System.currentTimeMillis();
            float[] embedding = embeddingService.generateEmbedding(text);
            long duration = System.currentTimeMillis() - startTime;
            
            assertNotNull(embedding, "Embedding should not be null for text " + (i + 1));
            assertEquals(EXPECTED_DIMENSIONS, embedding.length, 
                "All embeddings should have same dimensions");
            
            logger.info("    ✓ Generated in {}ms", duration);
            logger.info("    First value: {}", String.format("%.6f", embedding[0]));
        }
        
        logger.info("\n✓ Different text lengths handled correctly\n");
    }
    
    @Test
    @Order(4)
    @DisplayName("4. Test embedding consistency")
    void testEmbeddingConsistency() {
        logger.info("\n=== Test 4: Embedding Consistency ===");
        
        String testText = "Consistency test: The same input should produce the same output.";
        logger.info("  Test text: '{}'", testText);
        
        // Generate embedding twice
        logger.info("\n  Generation 1...");
        float[] embedding1 = embeddingService.generateEmbedding(testText);
        
        logger.info("  Generation 2...");
        float[] embedding2 = embeddingService.generateEmbedding(testText);
        
        // Compare embeddings
        assertEquals(embedding1.length, embedding2.length, 
            "Both embeddings should have same length");
        
        // Calculate difference
        double maxDiff = 0;
        double sumDiff = 0;
        for (int i = 0; i < embedding1.length; i++) {
            double diff = Math.abs(embedding1[i] - embedding2[i]);
            maxDiff = Math.max(maxDiff, diff);
            sumDiff += diff;
        }
        double avgDiff = sumDiff / embedding1.length;
        
        logger.info("\n  Consistency metrics:");
        logger.info("    Max difference: {}", String.format("%.10f", maxDiff));
        logger.info("    Avg difference: {}", String.format("%.10f", avgDiff));
        
        // Embeddings should be very close (allowing for small floating-point differences)
        assertTrue(maxDiff < 1e-6, 
            "Same input should produce nearly identical embeddings");
        
        logger.info("✓ Embeddings are consistent\n");
    }
    
    @Test
    @Order(5)
    @DisplayName("5. Test semantic similarity between similar texts")
    void testSemanticSimilarity() {
        logger.info("\n=== Test 5: Semantic Similarity ===");
        
        logger.info("  Text 1: '{}'", SIMILAR_TEXTS[0]);
        logger.info("  Text 2: '{}'", SIMILAR_TEXTS[1]);
        
        // Generate embeddings
        float[] embedding1 = embeddingService.generateEmbedding(SIMILAR_TEXTS[0]);
        float[] embedding2 = embeddingService.generateEmbedding(SIMILAR_TEXTS[1]);
        
        // Calculate cosine similarity
        double similarity = calculateCosineSimilarity(embedding1, embedding2);
        
        logger.info("\n  Cosine similarity: {}", String.format("%.6f", similarity));
        
        // Similar texts should have relatively high similarity (> 0.5)
        assertTrue(similarity > 0.5, 
            "Semantically similar texts should have similarity > 0.5, got: " + similarity);
        
        logger.info("✓ Semantic similarity detected\n");
    }
    
    @Test
    @Order(6)
    @DisplayName("6. Test semantic difference between unrelated texts")
    void testSemanticDifference() {
        logger.info("\n=== Test 6: Semantic Difference ===");
        
        logger.info("  Text 1: '{}'", DIFFERENT_TEXTS[0]);
        logger.info("  Text 2: '{}'", DIFFERENT_TEXTS[1]);
        
        // Generate embeddings
        float[] embedding1 = embeddingService.generateEmbedding(DIFFERENT_TEXTS[0]);
        float[] embedding2 = embeddingService.generateEmbedding(DIFFERENT_TEXTS[1]);
        
        // Calculate cosine similarity
        double similarity = calculateCosineSimilarity(embedding1, embedding2);
        
        logger.info("\n  Cosine similarity: {}", String.format("%.6f", similarity));
        
        // Different texts should have lower similarity
        assertTrue(similarity < 0.8, 
            "Semantically different texts should have similarity < 0.8, got: " + similarity);
        
        logger.info("✓ Semantic difference detected\n");
    }
    
    @Test
    @Order(7)
    @DisplayName("7. Test embedding properties comparison")
    void testEmbeddingPropertiesComparison() {
        logger.info("\n=== Test 7: Embedding Properties Comparison ===");
        
        // Generate embeddings for similar and different pairs
        float[] similar1 = embeddingService.generateEmbedding(SIMILAR_TEXTS[0]);
        float[] similar2 = embeddingService.generateEmbedding(SIMILAR_TEXTS[1]);
        float[] different1 = embeddingService.generateEmbedding(DIFFERENT_TEXTS[0]);
        float[] different2 = embeddingService.generateEmbedding(DIFFERENT_TEXTS[1]);
        
        // Calculate similarities
        double similarPairSimilarity = calculateCosineSimilarity(similar1, similar2);
        double differentPairSimilarity = calculateCosineSimilarity(different1, different2);
        
        logger.info("\n  Similar pair similarity: {}", 
            String.format("%.6f", similarPairSimilarity));
        logger.info("  Different pair similarity: {}", 
            String.format("%.6f", differentPairSimilarity));
        logger.info("  Difference: {}", 
            String.format("%.6f", similarPairSimilarity - differentPairSimilarity));
        
        // Similar texts should have higher similarity than different texts
        assertTrue(similarPairSimilarity > differentPairSimilarity,
            "Similar texts should have higher similarity than different texts");
        
        logger.info("✓ Embedding properties behave as expected\n");
    }
    
    @Test
    @Order(8)
    @DisplayName("8. Test technical domain text embeddings")
    void testTechnicalDomainEmbeddings() {
        logger.info("\n=== Test 8: Technical Domain Text Embeddings ===");
        
        String[] technicalTexts = {
            "Java is a high-level, object-oriented programming language.",
            "Python is a versatile programming language used for data science.",
            "Machine learning models can be trained on large datasets.",
            "The Spring Framework provides comprehensive infrastructure support for developing Java applications."
        };
        
        logger.info("  Testing {} technical texts", technicalTexts.length);
        
        float[][] embeddings = new float[technicalTexts.length][];
        
        // Generate all embeddings
        for (int i = 0; i < technicalTexts.length; i++) {
            logger.info("\n  Text {}: {}", i + 1, technicalTexts[i]);
            embeddings[i] = embeddingService.generateEmbedding(technicalTexts[i]);
            assertNotNull(embeddings[i], "Embedding should not be null");
            assertEquals(EXPECTED_DIMENSIONS, embeddings[i].length, 
                "All embeddings should have correct dimensions");
        }
        
        // Calculate similarities between all pairs
        logger.info("\n  Pairwise similarities:");
        for (int i = 0; i < embeddings.length; i++) {
            for (int j = i + 1; j < embeddings.length; j++) {
                double similarity = calculateCosineSimilarity(embeddings[i], embeddings[j]);
                logger.info("    Text {} vs Text {}: {}", 
                    i + 1, j + 1, String.format("%.4f", similarity));
            }
        }
        
        // Programming language texts (0 and 1) should be more similar to each other
        double progLangSimilarity = calculateCosineSimilarity(embeddings[0], embeddings[1]);
        double progVsMl = calculateCosineSimilarity(embeddings[0], embeddings[2]);
        
        logger.info("\n  Programming texts similarity: {}", 
            String.format("%.6f", progLangSimilarity));
        logger.info("  Programming vs ML similarity: {}", 
            String.format("%.6f", progVsMl));
        
        // Both should be positive (technical domain), but programming should be more similar
        assertTrue(progLangSimilarity > 0.5, 
            "Programming language texts should have good similarity");
        
        logger.info("✓ Technical domain embeddings generated successfully\n");
    }
    
    @Test
    @Order(9)
    @DisplayName("9. Complete workflow test")
    void testCompleteWorkflow() {
        logger.info("\n=== Test 9: Complete Workflow ===");
        logger.info("Testing complete embedding workflow with workshop-relevant content\n");
        
        // Workshop-relevant test case
        String query = "How do I create an AI agent in Java?";
        String[] documents = {
            "The SimpleAgent class in Java provides a framework for building AI agents with tool-calling capabilities.",
            "Machine learning models can be deployed using containerization technologies like Docker.",
            "Java 21 introduces virtual threads that make concurrent programming easier.",
            "An AI agent uses a reasoning loop: think, act, and observe to accomplish tasks."
        };
        
        logger.info("  Query: '{}'", query);
        logger.info("\n  Documents:");
        for (int i = 0; i < documents.length; i++) {
            logger.info("    {}: {}", i + 1, documents[i]);
        }
        
        // Generate query embedding
        logger.info("\n  Step 1: Generate query embedding...");
        long queryStart = System.currentTimeMillis();
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        long queryDuration = System.currentTimeMillis() - queryStart;
        
        assertNotNull(queryEmbedding, "Query embedding should not be null");
        assertEquals(EXPECTED_DIMENSIONS, queryEmbedding.length, 
            "Query embedding should have correct dimensions");
        logger.info("    ✓ Query embedding generated in {}ms", queryDuration);
        
        // Generate document embeddings and calculate similarities
        logger.info("\n  Step 2: Generate document embeddings and calculate similarities...");
        double maxSimilarity = -1;
        int mostRelevantDoc = -1;
        
        for (int i = 0; i < documents.length; i++) {
            long docStart = System.currentTimeMillis();
            float[] docEmbedding = embeddingService.generateEmbedding(documents[i]);
            long docDuration = System.currentTimeMillis() - docStart;
            
            double similarity = calculateCosineSimilarity(queryEmbedding, docEmbedding);
            
            logger.info("    Doc {}: similarity = {} ({}ms)", 
                i + 1, String.format("%.4f", similarity), docDuration);
            
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostRelevantDoc = i;
            }
        }
        
        logger.info("\n  Step 3: Identify most relevant document...");
        logger.info("    Most relevant: Document {} (similarity: {})", 
            mostRelevantDoc + 1, String.format("%.4f", maxSimilarity));
        logger.info("    Content: {}", documents[mostRelevantDoc]);
        
        // Document 0 and 3 should be most relevant (about agents)
        assertTrue(mostRelevantDoc == 0 || mostRelevantDoc == 3,
            "Most relevant document should be about agents (doc 0 or 3), got: " + (mostRelevantDoc + 1));
        assertTrue(maxSimilarity > 0.5,
            "Most relevant document should have similarity > 0.5");
        
        logger.info("\n✓ Complete workflow successful!");
        logger.info("=== All EmbeddingService Integration Tests PASSED ===\n");
    }
    
    // Helper method to calculate cosine similarity
    private double calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings must have same length");
        }
        
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
