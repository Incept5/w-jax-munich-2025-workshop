package com.incept5.ollama.backend;

/**
 * Enum representing supported AI backend types
 * 
 * <p>Base URLs can be configured via environment variables or system properties:
 * <ul>
 *   <li>OLLAMA: OLLAMA_BASE_URL or -Dollama.base.url (default: http://localhost:11434)</li>
 *   <li>LMSTUDIO: LMSTUDIO_BASE_URL or -Dlmstudio.base.url (default: http://localhost:1234/v1)</li>
 *   <li>MLX_VLM: MLX_VLM_BASE_URL or -Dmlx.vlm.base.url (default: http://localhost:8000)</li>
 * </ul>
 */
public enum BackendType {
    /**
     * Ollama - custom API format
     * Configure via OLLAMA_BASE_URL environment variable or -Dollama.base.url system property
     */
    OLLAMA("ollama.base.url", "OLLAMA_BASE_URL", "http://localhost:11434"),

    /**
     * LM Studio - OpenAI-compatible format
     * Configure via LMSTUDIO_BASE_URL environment variable or -Dlmstudio.base.url system property
     */
    LMSTUDIO("lmstudio.base.url", "LMSTUDIO_BASE_URL", "http://localhost:1234/v1"),

    /**
     * MLX-VLM - Apple Silicon optimized, supports multimodal
     * Configure via MLX_VLM_BASE_URL environment variable or -Dmlx.vlm.base.url system property
     */
    MLX_VLM("mlx.vlm.base.url", "MLX_VLM_BASE_URL", "http://localhost:8000");

    private final String systemPropertyKey;
    private final String environmentVariableKey;
    private final String fallbackUrl;

    BackendType(String systemPropertyKey, String environmentVariableKey, String fallbackUrl) {
        this.systemPropertyKey = systemPropertyKey;
        this.environmentVariableKey = environmentVariableKey;
        this.fallbackUrl = fallbackUrl;
    }

    /**
     * Get the default base URL for this backend type.
     * Checks in order: System property > Environment variable > Fallback URL
     * 
     * @return the configured or default base URL
     */
    public String getDefaultBaseUrl() {
        // Check system property first
        String url = System.getProperty(systemPropertyKey);
        if (url != null && !url.isBlank()) {
            return url;
        }
        
        // Check environment variable
        url = System.getenv(environmentVariableKey);
        if (url != null && !url.isBlank()) {
            return url;
        }
        
        // Return fallback
        return fallbackUrl;
    }
}
