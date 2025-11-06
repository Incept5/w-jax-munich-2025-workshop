package com.incept5.ollama.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for MLX-VLM API
 */
public record MLXVLMResponse(
        String model,
        String text,
        Usage usage
) {
    /**
     * Usage statistics
     */
    public record Usage(
            @SerializedName("input_tokens") Integer inputTokens,
            @SerializedName("output_tokens") Integer outputTokens,
            @SerializedName("total_tokens") Integer totalTokens,
            @SerializedName("prompt_tps") Double promptTps,
            @SerializedName("generation_tps") Double generationTps,
            @SerializedName("peak_memory") Double peakMemory
    ) {}

    /**
     * Get the response content
     */
    public String getContent() {
        return text != null ? text : "";
    }

    /**
     * Convert to AIResponse
     */
    public AIResponse toAIResponse() {
        String content = getContent();

        Integer promptTokens = usage != null ? usage.inputTokens() : null;
        Integer completionTokens = usage != null ? usage.outputTokens() : null;

        // MLX-VLM doesn't provide timing in nanoseconds
        return new AIResponse(
                model,
                content,
                true,  // done
                null,  // totalDuration
                null,  // promptEvalDuration
                promptTokens,  // promptEvalCount
                null,  // evalDuration
                completionTokens  // evalCount
        );
    }
}
