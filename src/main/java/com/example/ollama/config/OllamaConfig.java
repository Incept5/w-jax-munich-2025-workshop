package com.example.ollama.config;

import java.time.Duration;

/**
 * Configuration record for Ollama client using Java 21+ records.
 * Provides immutable configuration with sensible defaults.
 */
public record OllamaConfig(
        String baseUrl,
        String model,
        Duration connectTimeout,
        Duration requestTimeout
) {
    // Default values
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "gemma3";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(5);

    /**
     * Compact constructor with validation and defaults
     */
    public OllamaConfig {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        if (model == null || model.isBlank()) {
            model = DEFAULT_MODEL;
        }
        if (connectTimeout == null) {
            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }
        if (requestTimeout == null) {
            requestTimeout = DEFAULT_REQUEST_TIMEOUT;
        }
    }

    /**
     * Create configuration with all defaults
     */
    public static OllamaConfig defaultConfig() {
        return new OllamaConfig(null, null, null, null);
    }

    /**
     * Create configuration with custom model
     */
    public static OllamaConfig withModel(String model) {
        return new OllamaConfig(null, model, null, null);
    }

    /**
     * Builder for fluent configuration
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl = DEFAULT_BASE_URL;
        private String model = DEFAULT_MODEL;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        public OllamaConfig build() {
            return new OllamaConfig(baseUrl, model, connectTimeout, requestTimeout);
        }
    }

    /**
     * Get the full API endpoint URL
     */
    public String getApiUrl() {
        return baseUrl + "/api/generate";
    }

    /**
     * Get the model info endpoint URL
     */
    public String getModelInfoUrl() {
        return baseUrl + "/api/show";
    }
}
