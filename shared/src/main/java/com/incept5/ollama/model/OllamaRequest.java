package com.incept5.ollama.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Record representing an Ollama API request using Java 21 records feature.
 * Records provide immutable data carriers with automatic getters, equals, hashCode, and toString.
 */
public record OllamaRequest(
        String model,
        String prompt,
        @SerializedName("system") String systemPrompt,
        boolean stream,
        Map<String, Object> options,
        List<String> images
) {
    /**
     * Compact constructor with validation
     */
    public OllamaRequest {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Model name cannot be null or blank");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be null or blank");
        }
    }

    /**
     * Builder pattern for convenient object creation
     */
    public static Builder builder(String model, String prompt) {
        return new Builder(model, prompt);
    }

    public static class Builder {
        private final String model;
        private final String prompt;
        private String systemPrompt;
        private boolean stream = false;
        private Map<String, Object> options;
        private List<String> images;

        private Builder(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder options(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        public Builder images(List<String> images) {
            this.images = images;
            return this;
        }

        public OllamaRequest build() {
            return new OllamaRequest(model, prompt, systemPrompt, stream, options, images);
        }
    }
}
