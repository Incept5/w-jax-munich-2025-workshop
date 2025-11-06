package com.example.ollama.backend;

/**
 * Enum representing supported AI backend types
 */
public enum BackendType {
    /**
     * Ollama - localhost:11434, custom API format
     */
    OLLAMA("http://localhost:11434"),

    /**
     * LM Studio - localhost:1234/v1, OpenAI-compatible format
     */
    LMSTUDIO("http://localhost:1234/v1"),

    /**
     * MLX-VLM - Apple Silicon optimized, supports multimodal
     */
    MLX_VLM("http://localhost:8000");

    private final String defaultBaseUrl;

    BackendType(String defaultBaseUrl) {
        this.defaultBaseUrl = defaultBaseUrl;
    }

    /**
     * Get the default base URL for this backend type
     */
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }
}
