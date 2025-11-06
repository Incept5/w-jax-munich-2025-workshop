package com.incept5.ollama.backend;

import com.incept5.ollama.exception.AIBackendException;
import com.incept5.ollama.model.AIResponse;
import com.incept5.ollama.model.LMStudioRequest;
import com.incept5.ollama.model.LMStudioResponse;
import com.incept5.ollama.model.ModelInfo;
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
 * LM Studio backend implementation using OpenAI-compatible API
 * Default endpoint: http://localhost:1234/v1
 * Extends AbstractHttpBackend for common HTTP functionality
 */
public class LMStudioBackend extends AbstractHttpBackend {
    private static final Logger logger = LoggerFactory.getLogger(LMStudioBackend.class);

    public LMStudioBackend(String baseUrl, String model, Duration requestTimeout) {
        super(baseUrl, model, requestTimeout);
    }

    @Override
    public AIResponse generate(String prompt, String systemPrompt, Map<String, Object> options)
            throws AIBackendException {
        var request = LMStudioRequest.create(model, prompt, systemPrompt, options, false);
        return sendRequest(request);
    }

    @Override
    public AIResponse generateStreaming(
            String prompt,
            String systemPrompt,
            Map<String, Object> options,
            Consumer<String> chunkConsumer
    ) throws AIBackendException {
        var request = LMStudioRequest.create(model, prompt, systemPrompt, options, true);
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

    private AIResponse sendRequest(LMStudioRequest request) throws AIBackendException {
        try {
            String jsonBody = gson.toJson(request);
            String fullUrl = baseUrl + "/chat/completions";
            logger.debug("Sending request to LM Studio: {}", fullUrl);
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
            throw wrapException(e, "Failed to connect to LM Studio");
        }
    }

    private AIResponse sendStreamingRequest(LMStudioRequest request, Consumer<String> chunkConsumer)
            throws AIBackendException {
        try {
            String jsonBody = gson.toJson(request);
            logger.debug("Sending streaming request to LM Studio");

            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
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
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        if (!data.isBlank()) {
                            var chunk = gson.fromJson(data, LMStudioResponse.class);
                            String content = chunk.getContent();
                            if (content != null && !content.isEmpty()) {
                                chunkConsumer.accept(content);
                                fullContent.append(content);
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
            var lmResponse = gson.fromJson(jsonResponse, LMStudioResponse.class);
            return lmResponse.toAIResponse();
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
        // LM Studio doesn't provide model info endpoint in OpenAI-compatible API
        return null;
    }

    @Override
    public BackendType getBackendType() {
        return BackendType.LMSTUDIO;
    }

    @Override
    public boolean supportsModelInfo() {
        return false;
    }
}
