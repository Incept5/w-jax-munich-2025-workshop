package com.example.ollama.model;

import com.google.gson.annotations.SerializedName;

/**
 * Record representing an Ollama API response using Java 21 records feature.
 */
public record OllamaResponse(
        String model,
        @SerializedName("created_at") String createdAt,
        String response,
        boolean done,
        int[] context,
        @SerializedName("total_duration") Long totalDuration,
        @SerializedName("load_duration") Long loadDuration,
        @SerializedName("prompt_eval_count") Integer promptEvalCount,
        @SerializedName("prompt_eval_duration") Long promptEvalDuration,
        @SerializedName("eval_count") Integer evalCount,
        @SerializedName("eval_duration") Long evalDuration
) {
    // Constants for duration conversion
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private static final double NANOS_PER_MICROSECOND = 1_000.0;

    /**
     * Check if the response contains an error
     */
    public boolean hasError() {
        return response == null || response.isBlank();
    }

    /**
     * Get formatted timing information in Ollama style
     */
    public String getTimingInfo() {
        if (totalDuration == null) {
            return "No timing information available";
        }

        StringBuilder info = new StringBuilder();

        // Total duration
        info.append(formatDuration("total duration", totalDuration));

        // Load duration
        if (loadDuration != null && loadDuration > 0) {
            info.append(formatDuration("load duration", loadDuration));
        }

        // Prompt evaluation
        if (promptEvalCount != null && promptEvalCount > 0) {
            info.append(String.format("prompt eval count:    %d token(s)%n", promptEvalCount));

            if (promptEvalDuration != null && promptEvalDuration > 0) {
                info.append(formatDuration("prompt eval duration", promptEvalDuration));

                // Calculate prompt eval rate
                double promptRate = (promptEvalCount * NANOS_PER_SECOND) / promptEvalDuration;
                info.append(String.format("prompt eval rate:     %.2f tokens/s%n", promptRate));
            }
        }

        // Response generation
        if (evalCount != null && evalCount > 0) {
            info.append(String.format("eval count:           %d token(s)%n", evalCount));

            if (evalDuration != null && evalDuration > 0) {
                info.append(formatDuration("eval duration", evalDuration));

                // Calculate eval rate
                double evalRate = (evalCount * NANOS_PER_SECOND) / evalDuration;
                info.append(String.format("eval rate:            %.2f tokens/s%n", evalRate));
            }
        }

        return info.toString();
    }

    /**
     * Format duration in nanoseconds to human-readable format
     */
    private String formatDuration(String label, long nanos) {
        if (nanos >= NANOS_PER_SECOND) {
            // Display in seconds (4 decimal places for 5 significant figures)
            return String.format("%-21s %.4fs%n", label + ":", nanos / NANOS_PER_SECOND);
        } else if (nanos >= NANOS_PER_MILLISECOND) {
            // Display in milliseconds (2 decimal places)
            return String.format("%-21s %.2fms%n", label + ":", nanos / NANOS_PER_MILLISECOND);
        } else if (nanos >= NANOS_PER_MICROSECOND) {
            // Display in microseconds (2 decimal places)
            return String.format("%-21s %.2fµs%n", label + ":", nanos / NANOS_PER_MICROSECOND);
        } else {
            // Display in nanoseconds
            return String.format("%-21s %dns%n", label + ":", nanos);
        }
    }
}
