package com.example.ollama;

import com.example.ollama.backend.AIBackend;
import com.example.ollama.backend.BackendFactory;
import com.example.ollama.backend.BackendType;
import com.example.ollama.config.BackendConfig;
import com.example.ollama.exception.AIBackendException;
import com.example.ollama.model.AIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Modern Java 21+ demonstration of Ollama API integration.
 * Features:
 * - Records for immutable data models
 * - Virtual threads for concurrency
 * - Pattern matching for error handling
 * - Text blocks for multi-line strings
 * - Sealed classes for exception hierarchy
 * - Enhanced switch expressions
 * - CLI argument parsing
 * - Proper logging with SLF4J
 */
public class OllamaDemo {
    private static final Logger logger = LoggerFactory.getLogger(OllamaDemo.class);

    public static void main(String[] args) {
        // Check for raw mode FIRST
        List<String> argList = Arrays.asList(args);
        boolean rawMode = argList.contains("--raw") || argList.contains("-r");

        // Suppress ALL logging in raw mode - do this immediately
        if (rawMode) {
            suppressAllLogging();
        } else {
            logger.info("Starting Ollama Java Demo");
        }

        // Parse CLI arguments and create backend
        var backendConfig = parseArguments(args);
        var backend = BackendFactory.createBackend(
                backendConfig.backendType(),
                backendConfig.baseUrl(),
                backendConfig.model(),
                backendConfig.requestTimeout()
        );

        // Try-with-resources ensures proper cleanup
        try (backend) {
            runDemo(backend, args);
        } catch (Exception e) {
            if (!rawMode) {
                logger.error("Fatal error", e);
                System.err.println("Error: " + e.getMessage());
            } else {
                // In raw mode, just print the error message to stderr
                System.err.println(e.getMessage());
            }
            System.exit(1);
        }
    }

    /**
     * Run the demo based on command-line arguments
     */
    private static void runDemo(AIBackend backend, String[] args) throws AIBackendException {
        // Check mode flags
        List<String> argList = Arrays.asList(args);
        boolean streaming = argList.contains("--stream") || argList.contains("-s");
        boolean rawMode = argList.contains("--raw") || argList.contains("-r");

        // Get the prompt from arguments or use default
        String prompt = extractPrompt(args).orElse("What is the capital of France?");

        // Extract model parameters
        var modelParams = extractModelParameters(args);

        // Raw mode: output only the response, nothing else
        if (rawMode) {
            runRawMode(backend, prompt, streaming, modelParams);
            return;
        }

        // Standard mode: full display with headers and timing
        System.out.println("""
                ╔══════════════════════════════════════════════════════════╗
                ║           Ollama Java Demo Application               ║
                ╚══════════════════════════════════════════════════════════╝
                """);

        System.out.println("Model: " + backend.getModelName());

        // Always display model information in standard mode
        displayModelInfo(backend);

        // Display model parameters if configured
        if (modelParams.hasParameters()) {
            System.out.println("Model Parameters:");
            System.out.println("-".repeat(60));
            System.out.print(modelParams.getSummary());
            System.out.println("-".repeat(60) + "\n");
        }

        System.out.println("Prompt: " + prompt);
        System.out.println("Mode: " + (streaming ? "Streaming" : "Standard"));
        System.out.println("=".repeat(60));
        System.out.println();

        if (streaming) {
            runStreamingDemo(backend, prompt, modelParams);
        } else {
            runStandardDemo(backend, prompt, modelParams);
        }
    }

    /**
     * Run in raw mode - output only the response
     */
    private static void runRawMode(AIBackend backend, String prompt, boolean streaming, ModelParameters modelParams) throws AIBackendException {
        if (streaming) {
            // Streaming: print chunks directly
            backend.generateStreaming(prompt, modelParams.systemPrompt(), modelParams.options(), chunk -> System.out.print(chunk));
        } else {
            // Non-streaming: print complete response
            AIResponse response = backend.generate(prompt, modelParams.systemPrompt(), modelParams.options());
            System.out.print(response.response());
        }
    }

    /**
     * Display detailed model information
     */
    private static void displayModelInfo(AIBackend backend) {
        try {
            var modelInfo = backend.getModelInfo(backend.getModelName());
            if (modelInfo != null && modelInfo.hasDetails()) {
                System.out.println("\nModel Information:");
                System.out.println("-".repeat(60));
                System.out.print(modelInfo.getSummary());
                System.out.println("-".repeat(60));
            } else {
                System.out.println("(Model information not available)\n");
            }
        } catch (Exception e) {
            logger.warn("Could not fetch model information: {}", e.getMessage());
        }
    }

    /**
     * Run standard (non-streaming) demo
     */
    private static void runStandardDemo(AIBackend backend, String prompt, ModelParameters modelParams) throws AIBackendException {
        System.out.println("Generating response...\n");

        AIResponse response = backend.generate(prompt, modelParams.systemPrompt(), modelParams.options());

        System.out.println("Response:");
        System.out.println("-".repeat(60));
        System.out.println(response.response());
        System.out.println("-".repeat(60));
        System.out.println();

        // Display timing information in Ollama style
        System.out.print(response.getTimingInfo());
    }

    /**
     * Run streaming demo with real-time output
     */
    private static void runStreamingDemo(AIBackend backend, String prompt, ModelParameters modelParams) throws AIBackendException {
        System.out.println("Response (streaming):");
        System.out.println("-".repeat(60));

        // Lambda expression for chunk consumer - returns final response with timing
        AIResponse finalResponse = backend.generateStreaming(prompt, modelParams.systemPrompt(), modelParams.options(), chunk -> {
            System.out.print(chunk);
            System.out.flush();
        });

        System.out.println();
        System.out.println("-".repeat(60));
        System.out.println();

        // Display timing information in Ollama style
        System.out.print(finalResponse.getTimingInfo());
    }

    /**
     * Parse command-line arguments and create configuration
     */
    private static BackendConfig parseArguments(String[] args) {
        var builder = BackendConfig.builder();

        for (int i = 0; i < args.length; i++) {
            // Pattern matching with switch (modern approach)
            switch (args[i]) {
                case "--backend", "-b" -> {
                    if (i + 1 < args.length) {
                        String backendName = args[++i].toLowerCase();
                        try {
                            builder.backendType(BackendType.valueOf(backendName.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            System.err.println("Invalid backend: " + backendName);
                            System.err.println("Valid backends: ollama, lmstudio, mlx_vlm");
                            System.exit(1);
                        }
                    }
                }
                case "--model", "-m" -> {
                    if (i + 1 < args.length) {
                        builder.model(args[++i]);
                    }
                }
                case "--url", "-u" -> {
                    if (i + 1 < args.length) {
                        builder.baseUrl(args[++i]);
                    }
                }
                case "--timeout", "-t" -> {
                    if (i + 1 < args.length) {
                        builder.requestTimeout(Duration.ofSeconds(Long.parseLong(args[++i])));
                    }
                }
                case "--help", "-h" -> {
                    printHelp();
                    System.exit(0);
                }
                default -> {} // Ignore unknown arguments
            }
        }

        return builder.build();
    }

    /**
     * Helper method to extract an argument value from command-line arguments
     */
    private static java.util.Optional<String> getArgumentValue(List<String> args, String longForm, String shortForm) {
        int index = args.indexOf(longForm);
        if (index == -1) {
            index = args.indexOf(shortForm);
        }
        if (index != -1 && index + 1 < args.size()) {
            return java.util.Optional.of(args.get(index + 1));
        }
        return java.util.Optional.empty();
    }

    /**
     * Extract prompt from command-line arguments
     */
    private static java.util.Optional<String> extractPrompt(String[] args) {
        return getArgumentValue(Arrays.asList(args), "--prompt", "-p");
    }

    /**
     * Extract model parameters from command-line arguments
     */
    private static ModelParameters extractModelParameters(String[] args) {
        List<String> argList = Arrays.asList(args);

        // Extract system prompt
        String systemPrompt = getArgumentValue(argList, "--system", "--sys").orElse(null);

        // Extract temperature with validation
        Double temperature = null;
        var tempStr = getArgumentValue(argList, "--temperature", "--temp");
        if (tempStr.isPresent()) {
            try {
                double temp = Double.parseDouble(tempStr.get());
                if (temp < 0.0 || temp > 2.0) {
                    logger.warn("Temperature must be between 0.0 and 2.0, got: {}. Using default.", temp);
                } else {
                    temperature = temp;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid temperature value '{}', using default", tempStr.get());
            }
        }

        // Extract context size with validation
        Integer numCtx = null;
        var ctxStr = getArgumentValue(argList, "--context", "--ctx");
        if (ctxStr.isPresent()) {
            try {
                int ctx = Integer.parseInt(ctxStr.get());
                if (ctx <= 0) {
                    logger.warn("Context size must be positive, got: {}. Using default.", ctx);
                } else {
                    numCtx = ctx;
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid context size '{}', using default", ctxStr.get());
            }
        }

        // Extract images
        List<String> images = extractImages(args);

        return new ModelParameters(systemPrompt, temperature, numCtx, images);
    }

    /**
     * Extract image paths from command-line arguments
     */
    private static List<String> extractImages(String[] args) {
        List<String> images = new java.util.ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if ("--images".equals(args[i]) || "-i".equals(args[i])) {
                // Collect all following arguments until next flag
                i++;
                while (i < args.length && !args[i].startsWith("-")) {
                    images.add(args[i]);
                    i++;
                }
                break;
            }
        }
        return images;
    }

    /**
     * Record to hold model parameters
     */
    private record ModelParameters(String systemPrompt, Double temperature, Integer numCtx, List<String> images) {
        public java.util.Map<String, Object> options() {
            if (temperature == null && numCtx == null && (images == null || images.isEmpty())) {
                return null;
            }

            java.util.Map<String, Object> opts = new java.util.HashMap<>();
            if (temperature != null) {
                opts.put("temperature", temperature);
            }
            if (numCtx != null) {
                opts.put("num_ctx", numCtx);
            }
            if (images != null && !images.isEmpty()) {
                opts.put("images", images);
            }
            return java.util.Collections.unmodifiableMap(opts);
        }

        public boolean hasParameters() {
            return systemPrompt != null || temperature != null || numCtx != null ||
                   (images != null && !images.isEmpty());
        }

        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            if (systemPrompt != null) {
                summary.append("System Prompt:        ").append(systemPrompt).append("\n");
            }
            if (temperature != null) {
                summary.append("Temperature:          ").append(temperature).append("\n");
            }
            if (numCtx != null) {
                summary.append("Context Size:         ").append(numCtx).append(" tokens\n");
            }
            if (images != null && !images.isEmpty()) {
                summary.append("Images:               ").append(images.size()).append(" image(s)\n");
                for (int i = 0; i < images.size(); i++) {
                    summary.append("  [").append(i + 1).append("] ").append(images.get(i)).append("\n");
                }
            }
            return summary.toString();
        }
    }

    /**
     * Suppress all logging output for raw mode.
     * Note: This implementation requires Logback as the SLF4J implementation.
     */
    private static void suppressAllLogging() {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            root.setLevel(ch.qos.logback.classic.Level.OFF);
            root.detachAndStopAllAppenders();
        } catch (ClassCastException e) {
            // If not using Logback, fail silently
            System.err.println("Warning: Could not suppress logging. Logback implementation required for raw mode.");
        }
    }

    /**
     * Print help information using text blocks
     */
    private static void printHelp() {
        System.out.println("""
                Ollama Java Demo - Modern Java integration with AI backends

                Usage: java -jar ollama-java-demo.jar [OPTIONS]

                Options:
                  -b, --backend <name>     Backend type: ollama, lmstudio, mlx_vlm (default: ollama)
                  -m, --model <name>       Model name (default: gemma3)
                  -u, --url <url>          Backend server URL (default: backend-specific)
                                           - Ollama: http://localhost:11434
                                           - LM Studio: http://localhost:1234/v1
                                           - MLX-VLM: http://localhost:8000
                  -p, --prompt <text>      Prompt text (default: "What is the capital of France?")
                  -t, --timeout <sec>      Request timeout in seconds (default: 300)
                  -s, --stream             Enable streaming mode
                  -r, --raw                Raw output mode (response only, no formatting)
                  --system, --sys <text>   System prompt for the model
                  --temperature, --temp <n> Temperature (0.0-2.0, default: model default)
                  --context, --ctx <n>     Context size in tokens (default: model default)
                  --images, -i <paths...>  Image file paths or URLs (space-separated)
                  -h, --help               Show this help message

                Examples:
                  # Basic usage with default Ollama backend
                  java --enable-preview -jar ollama-java-demo.jar

                  # Use LM Studio backend
                  java --enable-preview -jar ollama-java-demo.jar -b lmstudio -m "local-model"

                  # LM Studio with custom URL
                  java --enable-preview -jar ollama-java-demo.jar -b lmstudio -u http://localhost:1234/v1

                  # Set system prompt to change model behavior
                  java --enable-preview -jar ollama-java-demo.jar --system "You are a helpful coding assistant"

                  # Adjust temperature (higher = more creative, lower = more focused)
                  java --enable-preview -jar ollama-java-demo.jar --temp 0.8 -p "Write a poem"

                  # Increase context size for longer conversations
                  java --enable-preview -jar ollama-java-demo.jar --ctx 8192

                  # Combine multiple parameters
                  java --enable-preview -jar ollama-java-demo.jar --system "Be concise" --temp 0.3 --ctx 4096

                  # Analyze an image with vision model (Ollama with llava)
                  java --enable-preview -jar ollama-java-demo.jar -m llava -p "What's in this image?" -i photo.jpg

                  # Multiple images
                  java --enable-preview -jar ollama-java-demo.jar -m llava -p "Compare these images" -i img1.jpg img2.png

                  # Raw output mode - only the response text
                  java --enable-preview -jar ollama-java-demo.jar -r -p "Hello"

                  # Raw mode with streaming (for piping to other commands)
                  java --enable-preview -jar ollama-java-demo.jar -r -s -p "Generate code"

                  # Different model with streaming on LM Studio
                  java --enable-preview -jar ollama-java-demo.jar -b lmstudio -m llama2 -s

                Features:
                  • Multiple AI backend support (Ollama, LM Studio, MLX-VLM)
                  • Java 21+ with modern language features
                  • Virtual threads for efficient concurrency
                  • Pattern matching for elegant error handling
                  • Records for immutable data models
                  • Streaming and non-streaming support
                  • Comprehensive logging with SLF4J
                """);
    }
}
