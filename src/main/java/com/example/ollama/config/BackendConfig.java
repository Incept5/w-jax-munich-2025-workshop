package com.example.ollama.config;

import com.example.ollama.backend.BackendType;

import java.time.Duration;

/**
 * Configuration for AI backend
 */
public record BackendConfig(
        BackendType backendType,
        String baseUrl,
        String model,
        Duration requestTimeout
) {
    /**
     * Create a builder for BackendConfig
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for BackendConfig
     */
    public static class Builder {
        private BackendType backendType = BackendType.OLLAMA;
        private String baseUrl = null; // Will use backend's default
        private String model = "gemma3";
        private Duration requestTimeout = Duration.ofMinutes(5);

        public Builder backendType(BackendType backendType) {
            this.backendType = backendType;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public BackendConfig build() {
            return new BackendConfig(backendType, baseUrl, model, requestTimeout);
        }
    }
}
