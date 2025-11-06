package com.incept5.ollama.model;

/**
 * Unified response record for all AI backends.
 * Provides a common interface regardless of the underlying backend.
 */
public record AIResponse(
        String model,
        String response,
        boolean done,
        Long totalDuration,
        Long promptEvalDuration,
        Integer promptEvalCount,
        Long evalDuration,
        Integer evalCount
) {
    // Constants for duration conversion
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;
    private static final double NANOS_PER_MICROSECOND = 1_000.0;

    /**
     * Get formatted timing information
     */
    public String getTimingInfo() {
        if (totalDuration == null) {
            return "No timing information available";
        }

        StringBuilder info = new StringBuilder();

        // Total duration
        info.append(formatDuration("total duration", totalDuration));

        // Prompt evaluation
        if (promptEvalCount != null && promptEvalCount > 0) {
            info.append(String.format("prompt eval count:    %d token(s)%n", promptEvalCount));

            if (promptEvalDuration != null && promptEvalDuration > 0) {
                info.append(formatDuration("prompt eval duration", promptEvalDuration));
                double promptRate = (promptEvalCount * NANOS_PER_SECOND) / promptEvalDuration;
                info.append(String.format("prompt eval rate:     %.2f tokens/s%n", promptRate));
            }
        }

        // Response generation
        if (evalCount != null && evalCount > 0) {
            info.append(String.format("eval count:           %d token(s)%n", evalCount));

            if (evalDuration != null && evalDuration > 0) {
                info.append(formatDuration("eval duration", evalDuration));
                double evalRate = (evalCount * NANOS_PER_SECOND) / evalDuration;
                info.append(String.format("eval rate:            %.2f tokens/s%n", evalRate));
            }
        }

        return info.toString();
    }

    private String formatDuration(String label, long nanos) {
        if (nanos >= NANOS_PER_SECOND) {
            return String.format("%-21s %.4fs%n", label + ":", nanos / NANOS_PER_SECOND);
        } else if (nanos >= NANOS_PER_MILLISECOND) {
            return String.format("%-21s %.2fms%n", label + ":", nanos / NANOS_PER_MILLISECOND);
        } else if (nanos >= NANOS_PER_MICROSECOND) {
            return String.format("%-21s %.2fµs%n", label + ":", nanos / NANOS_PER_MICROSECOND);
        } else {
            return String.format("%-21s %dns%n", label + ":", nanos);
        }
    }

    /**
     * Create an AIResponse from an OllamaResponse
     */
    public static AIResponse fromOllamaResponse(OllamaResponse ollamaResponse) {
        return new AIResponse(
                ollamaResponse.model(),
                ollamaResponse.response(),
                ollamaResponse.done(),
                ollamaResponse.totalDuration(),
                ollamaResponse.promptEvalDuration(),
                ollamaResponse.promptEvalCount(),
                ollamaResponse.evalDuration(),
                ollamaResponse.evalCount()
        );
    }
}
