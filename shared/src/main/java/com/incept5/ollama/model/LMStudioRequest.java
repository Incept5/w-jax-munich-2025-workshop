package com.incept5.ollama.model;

import com.incept5.ollama.util.ImageEncoder;
import com.incept5.ollama.util.ParameterMapper;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Request model for LM Studio (OpenAI-compatible format)
 * Supports both text-only and multi-modal (text + images) content
 */
public record LMStudioRequest(
        String model,
        List<Message> messages,
        @SerializedName("max_tokens") Integer maxTokens,
        Double temperature,
        @SerializedName("stream") boolean stream
) {
    /**
     * Message in chat format - content can be String or List<ContentPart>
     */
    public record Message(String role, Object content) {}

    /**
     * Content part for multi-modal messages
     */
    public record ContentPart(String type, String text, ImageUrl image_url) {}

    /**
     * Image URL wrapper for OpenAI format
     */
    public record ImageUrl(String url) {}

    /**
     * Create a completion request
     */
    public static LMStudioRequest create(
            String model,
            String prompt,
            String systemPrompt,
            Map<String, Object> options,
            boolean stream
    ) {
        var messages = new java.util.ArrayList<Message>();

        // Add system message if provided
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new Message("system", systemPrompt));
        }

        // Extract images from options
        List<String> imagePaths = ParameterMapper.getImages(options);

        // Build user message content
        Object userContent;
        if (imagePaths.isEmpty()) {
            // Simple text-only message
            userContent = prompt;
        } else {
            // Multi-part message with text and images
            var contentParts = new java.util.ArrayList<ContentPart>();

            // Add text part
            contentParts.add(new ContentPart("text", prompt, null));

            // Add image parts (encode to data URLs)
            try {
                List<String> dataUrls = ImageEncoder.processImagePathsToDataUrls(imagePaths);
                for (String dataUrl : dataUrls) {
                    contentParts.add(new ContentPart(
                        "image_url",
                        null,
                        new ImageUrl(dataUrl)
                    ));
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to encode images: " + e.getMessage(), e);
            }

            userContent = contentParts;
        }

        // Add user message
        messages.add(new Message("user", userContent));

        // Extract options
        Double temperature = ParameterMapper.getTemperature(options);
        Integer maxTokens = ParameterMapper.getMaxTokens(options);

        return new LMStudioRequest(model, messages, maxTokens, temperature, stream);
    }
}
