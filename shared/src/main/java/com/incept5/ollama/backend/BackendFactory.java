package com.incept5.ollama.backend;

import com.incept5.ollama.config.OllamaConfig;

import java.time.Duration;

/**
 * Factory for creating AI backend instances based on configuration
 */
public class BackendFactory {

    /**
     * Create a backend from configuration
     *
     * @param backendType the type of backend to create
     * @param baseUrl the base URL for the backend (null uses default)
     * @param model the model name/identifier
     * @param requestTimeout the request timeout
     * @return the created backend
     */
    public static AIBackend createBackend(
            BackendType backendType,
            String baseUrl,
            String model,
            Duration requestTimeout
    ) {
        // Use default URL if not provided
        String url = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl
                : backendType.getDefaultBaseUrl();

        return switch (backendType) {
            case OLLAMA -> {
                var config = OllamaConfig.builder()
                        .baseUrl(url)
                        .model(model)
                        .requestTimeout(requestTimeout)
                        .build();
                yield new OllamaBackend(config);
            }
            case LMSTUDIO -> new LMStudioBackend(url, model, requestTimeout);
            case MLX_VLM -> new MLXVLMBackend(url, model, requestTimeout);
        };
    }

    /**
     * Create a backend with default timeout
     */
    public static AIBackend createBackend(BackendType backendType, String baseUrl, String model) {
        return createBackend(backendType, baseUrl, model, Duration.ofMinutes(5));
    }

    /**
     * Create a backend with default URL and timeout
     */
    public static AIBackend createBackend(BackendType backendType, String model) {
        return createBackend(backendType, null, model);
    }
}
