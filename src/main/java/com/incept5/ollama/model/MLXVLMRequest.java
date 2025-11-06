package com.incept5.ollama.model;

import com.incept5.ollama.util.ParameterMapper;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Request model for MLX-VLM API
 */
public record MLXVLMRequest(
        String model,
        String prompt,
        String system,
        @SerializedName("max_tokens") Integer maxTokens,
        Double temperature,
        @SerializedName("top_p") Double topP,
        Integer seed,
        Boolean stream,
        List<String> image,
        List<String> audio
) {
    /**
     * Create a generation request for MLX-VLM
     */
    public static MLXVLMRequest create(
            String model,
            String prompt,
            String systemPrompt,
            Map<String, Object> options,
            boolean stream
    ) {
        // Extract options using ParameterMapper
        Integer maxTokens = ParameterMapper.getMaxTokens(options);
        Double temperature = ParameterMapper.getTemperature(options);
        Double topP = ParameterMapper.getTopP(options);
        Integer seed = ParameterMapper.getSeed(options);

        // Extract images - MLX-VLM accepts file paths directly (no encoding needed)
        List<String> images = ParameterMapper.getImages(options);
        List<String> imageList = images.isEmpty() ? null : images;

        return new MLXVLMRequest(
                model,
                prompt,
                systemPrompt,
                maxTokens,
                temperature,
                topP,
                seed,
                stream,
                imageList,  // Image file paths or URLs
                null        // No audio support yet
        );
    }
}
