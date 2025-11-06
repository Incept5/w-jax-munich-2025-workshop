package com.incept5.ollama.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response model for LM Studio (OpenAI-compatible format)
 */
public record LMStudioResponse(
        String id,
        String object,
        Long created,
        String model,
        List<Choice> choices,
        Usage usage
) {
    /**
     * Choice in the response
     */
    public record Choice(
            Integer index,
            Message message,
            Delta delta,
            @SerializedName("finish_reason") String finishReason
    ) {}

    /**
     * Message in the choice
     */
    public record Message(String role, String content) {}

    /**
     * Delta for streaming responses
     */
    public record Delta(String role, String content) {}

    /**
     * Token usage information
     */
    public record Usage(
            @SerializedName("prompt_tokens") Integer promptTokens,
            @SerializedName("completion_tokens") Integer completionTokens,
            @SerializedName("total_tokens") Integer totalTokens
    ) {}

    /**
     * Get the response content
     */
    public String getContent() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        var choice = choices.get(0);
        if (choice.message() != null) {
            return choice.message().content();
        }
        if (choice.delta() != null && choice.delta().content() != null) {
            return choice.delta().content();
        }
        return "";
    }

    /**
     * Check if this is the final response
     */
    public boolean isDone() {
        if (choices == null || choices.isEmpty()) {
            return true;
        }
        return choices.get(0).finishReason() != null;
    }

    /**
     * Convert to AIResponse
     */
    public AIResponse toAIResponse() {
        String content = getContent();
        boolean done = isDone();

        Integer promptTokens = usage != null ? usage.promptTokens() : null;
        Integer completionTokens = usage != null ? usage.completionTokens() : null;

        // LM Studio doesn't provide timing in nanoseconds, so we leave them null
        return new AIResponse(
                model,
                content,
                done,
                null,  // totalDuration
                null,  // promptEvalDuration
                promptTokens,  // promptEvalCount
                null,  // evalDuration
                completionTokens  // evalCount
        );
    }
}
