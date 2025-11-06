package com.example.ollama.backend;

import com.example.ollama.exception.AIBackendException;
import com.example.ollama.model.AIResponse;
import com.example.ollama.model.MLXVLMRequest;
import com.example.ollama.model.MLXVLMResponse;
import com.example.ollama.model.ModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * MLX-VLM backend implementation
 * Default endpoint: http://localhost:8000
 * Supports Vision Language Models on Apple Silicon
 * Extends AbstractHttpBackend for common HTTP functionality
 */
public class MLXVLMBackend extends AbstractHttpBackend {
    private static final Logger logger = LoggerFactory.getLogger(MLXVLMBackend.class);

    /**
     * Stream chunk format from MLX-VLM
     */
    private record StreamChunk(String chunk, String model) {}

    public MLXVLMBackend(String baseUrl, String model, Duration requestTimeout) {
        super(baseUrl, model, requestTimeout);
    }

    @Override
    public AIResponse generate(String prompt, String systemPrompt, Map<String, Object> options)
            throws AIBackendException {
        var request = MLXVLMRequest.create(model, prompt, systemPrompt, options, false);
        return sendRequest(request);
    }

    @Override
    public AIResponse generateStreaming(
            String prompt,
            String systemPrompt,
            Map<String, Object> options,
            Consumer<String> chunkConsumer
    ) throws AIBackendException {
        var request = MLXVLMRequest.create(model, prompt, systemPrompt, options, true);
        return sendStreamingRequest(request, chunkConsumer);
    }

    @Override
    public CompletableFuture<AIResponse> generateAsync(
            String prompt,
            String systemPrompt,
            Map<String, Object> options
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generate(prompt, systemPrompt, options);
            } catch (AIBackendException e) {
                throw new CompletionException("Failed to generate response", e);
            }
        }, virtualThreadExecutor);
    }

    private AIResponse sendRequest(MLXVLMRequest request) throws AIBackendException {
        try {
            String jsonBody = gson.toJson(request);
            String fullUrl = baseUrl + "/generate";
            logger.debug("Sending request to MLX-VLM: {}", fullUrl);
            logger.debug("Request body: {}", jsonBody);

            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .timeout(requestTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            logger.debug("Received response with status code: {}", response.statusCode());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                handleHttpError(response.statusCode(), response.body(), model);
                return null; // unreachable, but required for compilation
            }

        } catch (Exception e) {
            throw wrapException(e, "Failed to connect to MLX-VLM");
        }
    }

    private AIResponse sendStreamingRequest(MLXVLMRequest request, Consumer<String> chunkConsumer)
            throws AIBackendException {
        try {
            String jsonBody = gson.toJson(request);
            logger.debug("Sending streaming request to MLX-VLM");

            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(requestTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new AIBackendException.InvalidResponseException(
                    "Streaming request failed",
                    response.statusCode()
                );
            }

            // Process Server-Sent Events stream
            StringBuilder fullContent = new StringBuilder();
            try (var reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if (!data.isBlank()) {
                            try {
                                // Parse the JSON chunk
                                var chunk = gson.fromJson(data, StreamChunk.class);
                                String content = chunk.chunk();
                                if (content != null && !content.isEmpty()) {
                                    chunkConsumer.accept(content);
                                    fullContent.append(content);
                                }
                            } catch (Exception e) {
                                logger.debug("Failed to parse stream chunk: {}", e.getMessage());
                            }
                        }
                    }
                }
            }

            // Return final response
            return new AIResponse(
                    model,
                    fullContent.toString(),
                    true,
                    null, null, null, null, null
            );

        } catch (Exception e) {
            throw wrapException(e, "Streaming request failed");
        }
    }

    private AIResponse parseResponse(String jsonResponse) throws AIBackendException {
        try {
            var mlxResponse = gson.fromJson(jsonResponse, MLXVLMResponse.class);
            return mlxResponse.toAIResponse();
        } catch (Exception e) {
            logger.error("Failed to parse response", e);
            throw new AIBackendException.InvalidResponseException(
                "Invalid JSON response: " + e.getMessage(),
                200
            );
        }
    }

    @Override
    public ModelInfo getModelInfo(String modelName) throws AIBackendException {
        // MLX-VLM doesn't provide model info endpoint
        return null;
    }

    @Override
    public BackendType getBackendType() {
        return BackendType.MLX_VLM;
    }

    @Override
    public boolean supportsModelInfo() {
        return false;
    }
}
