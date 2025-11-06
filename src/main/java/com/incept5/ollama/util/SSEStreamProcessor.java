package com.incept5.ollama.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for processing Server-Sent Events (SSE) streams
 * Used by backends that stream responses in SSE format (LM Studio, MLX-VLM)
 */
public class SSEStreamProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SSEStreamProcessor.class);

    private static final String DATA_PREFIX = "data: ";
    private static final String DONE_MARKER = "[DONE]";

    /**
     * Process SSE stream and extract content chunks
     *
     * @param inputStream The input stream containing SSE data
     * @param gson Gson instance for parsing JSON
     * @param chunkExtractor Function to extract text from parsed JSON object
     * @param chunkConsumer Consumer to handle extracted text chunks
     * @param <T> Type of the JSON chunk object
     * @return Complete accumulated text from all chunks
     * @throws IOException If stream reading fails
     */
    public static <T> String processStream(
            InputStream inputStream,
            Gson gson,
            Class<T> chunkType,
            Function<T, String> chunkExtractor,
            Consumer<String> chunkConsumer
    ) throws IOException {
        StringBuilder fullContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(DATA_PREFIX)) {
                    String data = line.substring(DATA_PREFIX.length()).trim();

                    // Skip empty lines
                    if (data.isEmpty()) {
                        continue;
                    }

                    // Check for [DONE] marker (used by OpenAI-compatible APIs)
                    if (DONE_MARKER.equals(data)) {
                        break;
                    }

                    // Parse and extract chunk
                    try {
                        T chunk = gson.fromJson(data, chunkType);
                        String content = chunkExtractor.apply(chunk);

                        if (content != null && !content.isEmpty()) {
                            chunkConsumer.accept(content);
                            fullContent.append(content);
                        }
                    } catch (JsonSyntaxException e) {
                        logger.debug("Failed to parse SSE chunk: {} - {}", data, e.getMessage());
                    }
                }
            }
        }

        return fullContent.toString();
    }

    /**
     * Process SSE stream for OpenAI-compatible format (LM Studio)
     * Extracts content from choices[0].delta.content
     *
     * @param inputStream The input stream
     * @param gson Gson instance
     * @param chunkConsumer Consumer for text chunks
     * @return Complete accumulated text
     * @throws IOException If stream reading fails
     */
    public static String processOpenAIStream(
            InputStream inputStream,
            Gson gson,
            Consumer<String> chunkConsumer
    ) throws IOException {
        return processStream(
                inputStream,
                gson,
                OpenAIStreamChunk.class,
                chunk -> {
                    if (chunk.choices() != null && !chunk.choices().isEmpty()) {
                        var delta = chunk.choices().get(0).delta();
                        return delta != null ? delta.content() : null;
                    }
                    return null;
                },
                chunkConsumer
        );
    }

    /**
     * Process SSE stream for MLX-VLM format
     * Extracts content from chunk field
     *
     * @param inputStream The input stream
     * @param gson Gson instance
     * @param chunkConsumer Consumer for text chunks
     * @return Complete accumulated text
     * @throws IOException If stream reading fails
     */
    public static String processMLXVLMStream(
            InputStream inputStream,
            Gson gson,
            Consumer<String> chunkConsumer
    ) throws IOException {
        return processStream(
                inputStream,
                gson,
                MLXVLMStreamChunk.class,
                MLXVLMStreamChunk::chunk,
                chunkConsumer
        );
    }

    /**
     * OpenAI-compatible stream chunk format (for LM Studio)
     */
    public record OpenAIStreamChunk(
            String id,
            String object,
            long created,
            String model,
            java.util.List<Choice> choices
    ) {
        public record Choice(
                int index,
                Delta delta,
                String finish_reason
        ) {}

        public record Delta(String content, String role) {}
    }

    /**
     * MLX-VLM stream chunk format
     */
    public record MLXVLMStreamChunk(String chunk, String model) {}

    /**
     * Simple validation that a stream contains SSE data
     *
     * @param line First line from stream
     * @return true if line appears to be SSE format
     */
    public static boolean isSSEFormat(String line) {
        return line != null && line.startsWith(DATA_PREFIX);
    }

    /**
     * Check if data line is the [DONE] marker
     *
     * @param dataContent The content after "data: " prefix
     * @return true if this is the done marker
     */
    public static boolean isDoneMarker(String dataContent) {
        return DONE_MARKER.equals(dataContent);
    }
}
