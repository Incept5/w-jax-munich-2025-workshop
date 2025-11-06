package com.example.ollama.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for mapping and extracting model parameters from options map
 */
public class ParameterMapper {

    /**
     * Get max tokens from options map
     * Checks both 'max_tokens' and 'num_ctx' (Ollama-style)
     *
     * @param options Options map
     * @return Max tokens value or null if not set
     */
    public static Integer getMaxTokens(Map<String, Object> options) {
        if (options == null) {
            return null;
        }

        // Try max_tokens first (OpenAI/LM Studio style)
        Object maxTokens = options.get("max_tokens");
        if (maxTokens != null) {
            return toInteger(maxTokens);
        }

        // Fall back to num_ctx (Ollama style)
        Object numCtx = options.get("num_ctx");
        if (numCtx != null) {
            return toInteger(numCtx);
        }

        return null;
    }

    /**
     * Get temperature from options map
     *
     * @param options Options map
     * @return Temperature value or null if not set
     */
    public static Double getTemperature(Map<String, Object> options) {
        if (options == null) {
            return null;
        }

        Object temp = options.get("temperature");
        return temp != null ? toDouble(temp) : null;
    }

    /**
     * Get top_p from options map
     *
     * @param options Options map
     * @return top_p value or null if not set
     */
    public static Double getTopP(Map<String, Object> options) {
        if (options == null) {
            return null;
        }

        Object topP = options.get("top_p");
        return topP != null ? toDouble(topP) : null;
    }

    /**
     * Get seed from options map
     *
     * @param options Options map
     * @return Seed value or null if not set
     */
    public static Integer getSeed(Map<String, Object> options) {
        if (options == null) {
            return null;
        }

        Object seed = options.get("seed");
        return seed != null ? toInteger(seed) : null;
    }

    /**
     * Get images from options map
     *
     * @param options Options map
     * @return List of image paths/URLs or empty list if not set
     */
    @SuppressWarnings("unchecked")
    public static List<String> getImages(Map<String, Object> options) {
        if (options == null) {
            return List.of();
        }

        Object images = options.get("images");
        if (images == null) {
            return List.of();
        }

        if (images instanceof List) {
            try {
                return new ArrayList<>((List<String>) images);
            } catch (ClassCastException e) {
                return List.of();
            }
        }

        return List.of();
    }

    /**
     * Create an options map with common parameters
     *
     * @param temperature Temperature value (0.0-2.0)
     * @param maxTokens Maximum tokens to generate
     * @param topP Top-p sampling parameter
     * @param seed Random seed for reproducibility
     * @return Options map with non-null parameters
     */
    public static Map<String, Object> createOptions(
            Double temperature,
            Integer maxTokens,
            Double topP,
            Integer seed
    ) {
        Map<String, Object> options = new java.util.HashMap<>();

        if (temperature != null) {
            options.put("temperature", temperature);
        }
        if (maxTokens != null) {
            options.put("max_tokens", maxTokens);
        }
        if (topP != null) {
            options.put("top_p", topP);
        }
        if (seed != null) {
            options.put("seed", seed);
        }

        return options.isEmpty() ? null : options;
    }

    /**
     * Validate temperature is in valid range
     *
     * @param temperature Temperature value
     * @return true if valid (0.0-2.0) or null
     */
    public static boolean isValidTemperature(Double temperature) {
        return temperature == null || (temperature >= 0.0 && temperature <= 2.0);
    }

    /**
     * Validate top_p is in valid range
     *
     * @param topP Top-p value
     * @return true if valid (0.0-1.0) or null
     */
    public static boolean isValidTopP(Double topP) {
        return topP == null || (topP >= 0.0 && topP <= 1.0);
    }

    /**
     * Validate max tokens is positive
     *
     * @param maxTokens Max tokens value
     * @return true if valid (> 0) or null
     */
    public static boolean isValidMaxTokens(Integer maxTokens) {
        return maxTokens == null || maxTokens > 0;
    }

    /**
     * Convert object to Integer safely
     */
    private static Integer toInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Convert object to Double safely
     */
    private static Double toDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
