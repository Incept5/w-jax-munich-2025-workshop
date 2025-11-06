package com.example.ollama.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Record representing detailed model information from Ollama API.
 * Retrieved via the /api/show endpoint.
 */
public record ModelInfo(
        String modelfile,
        String parameters,
        String template,
        ModelDetails details,
        @SerializedName("model_info") Map<String, Object> modelInfo
) {
    // Constants for byte conversion (using binary units: 1 KiB = 1024 bytes)
    private static final long BYTES_PER_KIB = 1024;
    private static final long BYTES_PER_MIB = 1024 * 1024;
    private static final long BYTES_PER_GIB = 1024 * 1024 * 1024;

    /**
     * Nested record for model details
     */
    public record ModelDetails(
            @SerializedName("parent_model") String parentModel,
            String format,
            String family,
            List<String> families,
            @SerializedName("parameter_size") String parameterSize,
            @SerializedName("quantization_level") String quantizationLevel
    ) {}

    /**
     * Get human-readable model information summary
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        if (details != null) {
            // Model family
            if (details.family != null) {
                summary.append("Family:               ").append(details.family).append("\n");
            }

            // Parameter size
            if (details.parameterSize != null) {
                summary.append("Parameters:           ").append(details.parameterSize).append("\n");
            }

            // Quantization
            if (details.quantizationLevel != null) {
                summary.append("Quantization:         ").append(details.quantizationLevel).append("\n");
            }

            // Format
            if (details.format != null) {
                summary.append("Format:               ").append(details.format).append("\n");
            }
        }

        // Model size from modelInfo - try multiple possible field names
        if (modelInfo != null) {
            Long sizeBytes = null;

            // Try different possible field names
            for (String key : new String[]{"general.file_size", "file_size", "size"}) {
                if (modelInfo.containsKey(key)) {
                    Object sizeObj = modelInfo.get(key);
                    if (sizeObj instanceof Number) {
                        sizeBytes = ((Number) sizeObj).longValue();
                        break;
                    }
                }
            }

            if (sizeBytes != null) {
                summary.append("Model Size:           ").append(formatBytes(sizeBytes)).append("\n");
            }

            // Architecture information
            if (modelInfo.containsKey("general.architecture")) {
                Object arch = modelInfo.get("general.architecture");
                if (arch != null) {
                    summary.append("Architecture:         ").append(arch).append("\n");
                }
            }
        }

        return summary.toString();
    }

    /**
     * Format bytes to human-readable format using binary units (KiB, MiB, GiB)
     */
    private String formatBytes(long bytes) {
        if (bytes < BYTES_PER_KIB) {
            return bytes + " B";
        } else if (bytes < BYTES_PER_MIB) {
            return String.format("%.2f KiB", bytes / (double) BYTES_PER_KIB);
        } else if (bytes < BYTES_PER_GIB) {
            return String.format("%.2f MiB", bytes / (double) BYTES_PER_MIB);
        } else {
            return String.format("%.2f GiB", bytes / (double) BYTES_PER_GIB);
        }
    }

    /**
     * Check if model info is available
     */
    public boolean hasDetails() {
        return details != null || (modelInfo != null && !modelInfo.isEmpty());
    }
}
