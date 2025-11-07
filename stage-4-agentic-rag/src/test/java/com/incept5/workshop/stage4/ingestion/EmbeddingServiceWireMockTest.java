
package com.incept5.workshop.stage4.ingestion;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WireMock-based integration test to investigate HTTP request issues.
 * 
 * This test captures exactly what the Java HttpClient sends to help debug
 * why the Python embedding service receives empty request bodies.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmbeddingServiceWireMockTest {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingServiceWireMockTest.class);
    
    private static WireMockServer wireMockServer;
    private static final List<Request> capturedRequests = new ArrayList<>();
    private static final Gson gson = new Gson();
    
    private EmbeddingService embeddingService;
    
    @BeforeAll
    static void setupWireMock() {
        logger.info("=== Setting up WireMock Server ===");
        
        // Start WireMock on a random port
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
            .dynamicPort());  // Request journal is enabled by default
        
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        
        logger.info("✓ WireMock server started on port: {}", wireMockServer.port());
        logger.info("=== WireMock Setup Complete ===\n");
    }
    
    @AfterAll
    static void tearDownWireMock() {
        logger.info("\n=== Shutting down WireMock Server ===");
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            logger.info("✓ WireMock server stopped");
        }
    }
    
    @BeforeEach
    void setup() {
        // Reset WireMock for each test
        wireMockServer.resetAll();
        capturedRequests.clear();
        
        // Create EmbeddingService pointing to WireMock
        String baseUrl = "http://localhost:" + wireMockServer.port();
        embeddingService = new EmbeddingService(baseUrl, "nomic-embed-text");
        
        logger.info("Test setup complete. Base URL: {}", baseUrl);
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: Capture Basic HTTP Request")
    void testCaptureBasicRequest() {
        logger.info("\n=== Test 1: Capture Basic HTTP Request ===");
        
        // Setup mock response
        float[] mockEmbedding = new float[768];
        for (int i = 0; i < 768; i++) {
            mockEmbedding[i] = 0.1f;
        }
        
        JsonObject response = new JsonObject();
        response.add("embedding", gson.toJsonTree(mockEmbedding));
        
        stubFor(post(urlEqualTo("/api/embeddings"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(response.toString())));
        
        // Make request
        String testText = "Hello, World!";
        logger.info("Sending test text: '{}'", testText);
        
        float[] result = embeddingService.generateEmbedding(testText);
        
        // Capture the request
        wireMockServer.getAllServeEvents().forEach(event -> {
            Request request = event.getRequest();
            capturedRequests.add(request);
            
            logger.info("\n📥 CAPTURED REQUEST:");
            logger.info("   Method: {}", request.getMethod());
            logger.info("   URL: {}", request.getUrl());
            logger.info("   HTTP Version: {}", request.getProtocol());
            logger.info("   Headers:");
            request.getHeaders().all().forEach(header -> 
                logger.info("      {}: {}", header.key(), header.firstValue()));
            
            byte[] bodyBytes = request.getBody();
            logger.info("   Body Size: {} bytes", bodyBytes.length);
            logger.info("   Body (raw): {}", new String(bodyBytes, StandardCharsets.UTF_8));
            
            // Parse and log the JSON body
            if (bodyBytes.length > 0) {
                try {
                    JsonObject body = gson.fromJson(new String(bodyBytes, StandardCharsets.UTF_8), JsonObject.class);
                    logger.info("   Body (parsed JSON):");
                    logger.info("      model: {}", body.get("model").getAsString());
                    logger.info("      encoding: {}", body.get("encoding").getAsString());
                    logger.info("      prompt length: {} chars", body.get("prompt").getAsString().length());
                    
                    // Decode base64 to show original text
                    String encodedPrompt = body.get("prompt").getAsString();
                    String decodedPrompt = new String(
                        Base64.getDecoder().decode(encodedPrompt), 
                        StandardCharsets.UTF_8);
                    logger.info("      prompt (decoded): '{}'", decodedPrompt);
                } catch (Exception e) {
                    logger.error("   Failed to parse JSON body: {}", e.getMessage());
                }
            } else {
                logger.error("   ❌ EMPTY BODY DETECTED!");
            }
        });
        
        // Assertions
        assertEquals(1, capturedRequests.size(), "Should capture exactly one request");
        Request capturedRequest = capturedRequests.get(0);
        
        assertTrue(capturedRequest.getBody().length > 0, 
            "Request body should not be empty");
        
        // Verify the request was POST
        assertEquals("POST", capturedRequest.getMethod().getName());
        
        // Verify Content-Type header
        assertTrue(capturedRequest.containsHeader("Content-Type"));
        String contentType = capturedRequest.getHeader("Content-Type");
        assertTrue(contentType.contains("application/json"), 
            "Content-Type should be application/json");
        
        logger.info("\n✓ Test 1 PASSED: Request successfully captured with body");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: Compare with Expected curl Request")
    void testCompareWithCurl() {
        logger.info("\n=== Test 2: Compare with Expected curl Request ===");
        
        // Setup mock response
        float[] mockEmbedding = new float[768];
        for (int i = 0; i < 768; i++) {
            mockEmbedding[i] = 0.1f;
        }
        
        JsonObject response = new JsonObject();
        response.add("embedding", gson.toJsonTree(mockEmbedding));
        
        stubFor(post(urlEqualTo("/api/embeddings"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(response.toString())));
        
        // Make request
        String testText = "test";
        embeddingService.generateEmbedding(testText);
        
        // Get the captured request
        Request capturedRequest = wireMockServer.getAllServeEvents().get(0).getRequest();
        byte[] bodyBytes = capturedRequest.getBody();
        
        logger.info("\n📊 COMPARISON:");
        logger.info("Expected curl request:");
        logger.info("  curl -X POST http://localhost:8001/api/embeddings \\");
        logger.info("    -H \"Content-Type: application/json\" \\");
        logger.info("    -d '{{\"model\":\"nomic-embed-text\",\"prompt\":\"dGVzdA==\",\"encoding\":\"base64\"}}'");
        logger.info("\nActual Java request:");
        logger.info("  Method: POST");
        logger.info("  URL: /api/embeddings");
        logger.info("  Content-Type: {}", capturedRequest.getHeader("Content-Type"));
        logger.info("  Body size: {} bytes", bodyBytes.length);
        logger.info("  Body: {}", new String(bodyBytes, StandardCharsets.UTF_8));
        
        // Verify body content
        assertNotNull(bodyBytes);
        assertTrue(bodyBytes.length > 0, "Body should not be empty");
        
        // Parse and verify JSON structure
        JsonObject body = gson.fromJson(new String(bodyBytes, StandardCharsets.UTF_8), JsonObject.class);
        assertTrue(body.has("model"), "Body should have 'model' field");
        assertTrue(body.has("prompt"), "Body should have 'prompt' field");
        assertTrue(body.has("encoding"), "Body should have 'encoding' field");
        assertEquals("base64", body.get("encoding").getAsString(), 
            "Encoding should be 'base64'");
        
        // Decode and verify the prompt
        String encodedPrompt = body.get("prompt").getAsString();
        String decodedPrompt = new String(
            Base64.getDecoder().decode(encodedPrompt), 
            StandardCharsets.UTF_8);
        assertEquals(testText, decodedPrompt, 
            "Decoded prompt should match original text");
        
        logger.info("\n✓ Test 2 PASSED: Request format matches curl expectations");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: Verify Content-Length Header")
    void testContentLengthHeader() {
        logger.info("\n=== Test 3: Verify Content-Length Header ===");
        
        // Setup mock response
        float[] mockEmbedding = new float[768];
        for (int i = 0; i < 768; i++) {
            mockEmbedding[i] = 0.1f;
        }
        
        JsonObject response = new JsonObject();
        response.add("embedding", gson.toJsonTree(mockEmbedding));
        
        stubFor(post(urlEqualTo("/api/embeddings"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(response.toString())));
        
        // Make request
        embeddingService.generateEmbedding("test");
        
        // Get the captured request
        Request capturedRequest = wireMockServer.getAllServeEvents().get(0).getRequest();
        
        logger.info("\n📏 CONTENT-LENGTH ANALYSIS:");
        logger.info("Headers present:");
        capturedRequest.getHeaders().all().forEach(header -> 
            logger.info("  {}: {}", header.key(), header.firstValue()));
        
        byte[] bodyBytes = capturedRequest.getBody();
        int actualBodySize = bodyBytes.length;
        
        logger.info("\nBody size: {} bytes", actualBodySize);
        
        // Check if Content-Length header is present
        if (capturedRequest.containsHeader("Content-Length")) {
            String contentLength = capturedRequest.getHeader("Content-Length");
            int headerValue = Integer.parseInt(contentLength);
            logger.info("Content-Length header: {}", headerValue);
            logger.info("Match: {}", headerValue == actualBodySize ? "✓" : "✗");
            
            assertEquals(actualBodySize, headerValue, 
                "Content-Length header should match actual body size");
        } else {
            logger.warn("⚠️  Content-Length header NOT present");
            logger.warn("This might cause issues with some servers");
        }
        
        logger.info("\n✓ Test 3 PASSED: Content-Length analysis complete");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: Test with Code Block (Complex Content)")
    void testWithCodeBlock() {
        logger.info("\n=== Test 4: Test with Code Block (Complex Content) ===");
        
        // Setup mock response
        float[] mockEmbedding = new float[768];
        for (int i = 0; i < 768; i++) {
            mockEmbedding[i] = 0.1f;
        }
        
        JsonObject response = new JsonObject();
        response.add("embedding", gson.toJsonTree(mockEmbedding));
        
        stubFor(post(urlEqualTo("/api/embeddings"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(response.toString())));
        
        // Test with code block containing special characters
        String testCode = """
            public class Test {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            """;
        
        logger.info("Sending code block ({} chars):\n{}", testCode.length(), testCode);
        
        embeddingService.generateEmbedding(testCode);
        
        // Get the captured request
        Request capturedRequest = wireMockServer.getAllServeEvents().get(0).getRequest();
        byte[] bodyBytes = capturedRequest.getBody();
        
        logger.info("\n📦 COMPLEX CONTENT TEST:");
        logger.info("Body size: {} bytes", bodyBytes.length);
        
        assertTrue(bodyBytes.length > 0, "Body should not be empty for code block");
        
        // Parse and verify
        JsonObject body = gson.fromJson(new String(bodyBytes, StandardCharsets.UTF_8), JsonObject.class);
        String encodedPrompt = body.get("prompt").getAsString();
        String decodedPrompt = new String(
            Base64.getDecoder().decode(encodedPrompt), 
            StandardCharsets.UTF_8);
        
        assertEquals(testCode.trim(), decodedPrompt.trim(), 
            "Decoded code block should match original");
        
        logger.info("✓ Code block successfully encoded and transmitted");
        logger.info("\n✓ Test 4 PASSED: Complex content handled correctly");
    }
}
