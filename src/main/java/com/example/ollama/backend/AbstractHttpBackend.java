package com.example.ollama.backend;

import com.example.ollama.exception.AIBackendException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for HTTP-based AI backends
 * Provides common functionality for LM Studio and MLX-VLM backends
 */
public abstract class AbstractHttpBackend implements AIBackend {
    private static final Logger logger = LoggerFactory.getLogger(AbstractHttpBackend.class);

    protected final String baseUrl;
    protected final String model;
    protected final Duration requestTimeout;
    protected final HttpClient httpClient;
    protected final Gson gson;
    protected final ExecutorService virtualThreadExecutor;

    protected AbstractHttpBackend(String baseUrl, String model, Duration requestTimeout) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.requestTimeout = requestTimeout != null ? requestTimeout : Duration.ofMinutes(5);

        // Use virtual threads for efficient I/O
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)  // Use HTTP/1.1 for compatibility
                .connectTimeout(Duration.ofSeconds(30))
                .executor(virtualThreadExecutor)
                .build();

        this.gson = new GsonBuilder().create();

        logger.info("Initialized {} backend with base URL: {}", getBackendType(), baseUrl);
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getModelName() {
        return model;
    }

    /**
     * Handle HTTP response status codes with pattern matching
     *
     * @param statusCode HTTP status code
     * @param responseBody Response body
     * @param modelName Model name for error messages
     * @throws AIBackendException if status code indicates an error
     */
    protected void handleHttpError(int statusCode, String responseBody, String modelName)
            throws AIBackendException {
        if (statusCode == 404) {
            throw new AIBackendException.ModelNotFoundException(modelName);
        } else if (statusCode >= 500) {
            throw new AIBackendException.ConnectionException(
                "Server error: " + responseBody,
                null
            );
        } else {
            throw new AIBackendException.InvalidResponseException(
                "Unexpected response: " + responseBody,
                statusCode
            );
        }
    }

    /**
     * Wrap exceptions in AIBackendException if needed
     *
     * @param e Exception to wrap
     * @param message Error message
     * @return AIBackendException
     */
    protected AIBackendException wrapException(Exception e, String message) {
        if (e instanceof AIBackendException aiBackendEx) {
            return aiBackendEx;
        }
        logger.error(message, e);
        return new AIBackendException.ConnectionException(
            message + ": " + e.getMessage(),
            e
        );
    }

    @Override
    public void close() {
        logger.info("Closing {} backend", getBackendType());
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in time, forcing shutdown");
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor termination");
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
