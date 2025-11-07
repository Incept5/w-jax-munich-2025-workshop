
package com.incept5.workshop.stage1;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.incept5.workshop.stage1.tool.DayOfWeekTool;
import com.incept5.workshop.stage1.tool.FunctionTool;
import com.incept5.workshop.stage1.tool.WeatherTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Main class for demonstrating function calling with Ollama models.
 * 
 * This demo shows how LLMs can call registered functions during their
 * response generation. It tests multiple models with two simple functions:
 * - get_current_day(): Returns the current day of the week
 * - get_weather(city): Returns mock weather data for a city
 * 
 * Usage:
 *   java -jar stage-1-function-calling.jar [model_filter] [max_size_gb]
 * 
 * Examples:
 *   java -jar stage-1-function-calling.jar              # Test all models
 *   java -jar stage-1-function-calling.jar "jan"        # Test models containing "jan"
 *   java -jar stage-1-function-calling.jar "qwen*" 20   # Test qwen models under 20GB
 */
public class FunctionCallingDemo {
    private static final Logger logger = LoggerFactory.getLogger(FunctionCallingDemo.class);
    private static final Gson gson = new Gson();
    
    // Test configuration
    private static final int NUM_RUNS = 10;
    private static final double TEMPERATURE = 0.6;
    private static final double DEFAULT_MAX_SIZE_GB = 100.0;
    private static final String OLLAMA_BASE_URL = getOllamaBaseUrl();
    
    /**
     * Get Ollama base URL from environment or system property
     * Precedence: System property > Environment variable > localhost:11434
     */
    private static String getOllamaBaseUrl() {
        String url = System.getProperty("ollama.base.url");
        if (url != null && !url.isBlank()) {
            return url;
        }
        
        url = System.getenv("OLLAMA_BASE_URL");
        if (url != null && !url.isBlank()) {
            return url;
        }
        
        return "http://localhost:11434";
    }

    private static String modelFilter = null;
    private static double maxSizeGB = DEFAULT_MAX_SIZE_GB;

    /**
     * Test case definition.
     */
    private static class TestCase {
        final String prompt;
        final String expected;

        TestCase(String prompt, String expected) {
            this.prompt = prompt;
            this.expected = expected;
        }
    }

    /**
     * Model information from Ollama.
     */
    private static class OllamaModel {
        String name;
        long sizeBytes;
        String quantization;
        String modified;

        OllamaModel(String name, long sizeBytes, String quantization, String modified) {
            this.name = name;
            this.sizeBytes = sizeBytes;
            this.quantization = quantization;
            this.modified = modified;
        }
    }

    /**
     * Model test results.
     */
    private static class ModelTestResult {
        String modelName;
        long modelSize;
        double avgExecutionTime;
        double stdDevExecutionTime;
        int successRate;
        int totalPossible;
        String error;

        ModelTestResult(String modelName, long modelSize) {
            this.modelName = modelName;
            this.modelSize = modelSize;
        }
    }

    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length > 0) {
            modelFilter = args[0];
            logger.info("Using model filter: {}", modelFilter);
        }

        if (args.length > 1) {
            try {
                maxSizeGB = Double.parseDouble(args[1]);
                logger.info("Using max model size: {} GB", maxSizeGB);
            } catch (NumberFormatException e) {
                logger.warn("Invalid max size '{}', using default {} GB", args[1], DEFAULT_MAX_SIZE_GB);
                maxSizeGB = DEFAULT_MAX_SIZE_GB;
            }
        }

        logger.info("Starting function calling demo...");

        try {
            // Get available models
            List<OllamaModel> models = getAvailableModels();
            if (models.isEmpty()) {
                logger.error("No models found. Please ensure Ollama is running and models are installed.");
                return;
            }

            // Filter suitable models
            List<OllamaModel> suitableModels = filterSuitableModels(models);
            logger.info("Found {} suitable models for testing", suitableModels.size());

            if (suitableModels.isEmpty()) {
                logger.warn("No suitable models found for testing. Adjust the filter criteria.");
                return;
            }

            // Display models that will be tested
            printModelList(suitableModels);

            // Run tests
            List<ModelTestResult> results = new ArrayList<>();
            for (OllamaModel model : suitableModels) {
                ModelTestResult result = testModel(model.name, model.sizeBytes);
                results.add(result);
            }

            // Generate reports
            printReport(results);
            writeResultsCsv(results);

        } catch (Exception e) {
            logger.error("Error in test execution", e);
            e.printStackTrace();
        }
    }

    /**
     * Get available models from Ollama.
     */
    private static List<OllamaModel> getAvailableModels() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_BASE_URL + "/api/tags"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error from Ollama API: " + response.statusCode());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        List<OllamaModel> models = new ArrayList<>();

        if (responseJson.has("models")) {
            JsonArray modelsArray = responseJson.getAsJsonArray("models");
            for (int i = 0; i < modelsArray.size(); i++) {
                JsonObject modelNode = modelsArray.get(i).getAsJsonObject();
                String name = modelNode.get("name").getAsString();
                long size = modelNode.has("size") ? modelNode.get("size").getAsLong() : 0;
                String modified = modelNode.has("modified_at") ? 
                    modelNode.get("modified_at").getAsString() : "unknown";
                
                // Extract quantization
                String quantization = "Unknown";
                if (modelNode.has("details")) {
                    JsonObject details = modelNode.getAsJsonObject("details");
                    if (details.has("quantization_level")) {
                        quantization = details.get("quantization_level").getAsString();
                    }
                }

                models.add(new OllamaModel(name, size, quantization, modified));
            }
        }

        return models;
    }

    /**
     * Filter models based on size, type, and optional name filter.
     * Returns only the first 5 suitable models to keep test duration reasonable.
     */
    private static List<OllamaModel> filterSuitableModels(List<OllamaModel> models) {
        List<OllamaModel> suitable = new ArrayList<>();
        long maxSizeBytes = (long) (maxSizeGB * 1024 * 1024 * 1024);

        Set<String> excludedTypes = new HashSet<>(Arrays.asList(
                "bert", "clip", "embed", "embedding", "sentence-transformer", "bge", "minilm", "arctic-embed"
        ));

        // Compile model filter pattern if provided
        Pattern modelFilterPattern = null;
        if (modelFilter != null && !modelFilter.isEmpty()) {
            try {
                String filterPattern = convertToRegex(modelFilter);
                modelFilterPattern = Pattern.compile(filterPattern, Pattern.CASE_INSENSITIVE);
                logger.info("Compiled model filter pattern: {} (from input: {})", filterPattern, modelFilter);
            } catch (Exception e) {
                logger.warn("Invalid model filter pattern '{}', ignoring: {}", modelFilter, e.getMessage());
            }
        }

        for (OllamaModel model : models) {
            // Limit to first 5 models
            if (suitable.size() >= 5) {
                break;
            }

            if (model.sizeBytes > maxSizeBytes) {
                continue;
            }

            String modelName = model.name.toLowerCase();

            // Apply model name filter if specified
            if (modelFilterPattern != null) {
                if (!modelFilterPattern.matcher(model.name).find()) {
                    continue;
                }
            }

            boolean excluded = false;
            for (String type : excludedTypes) {
                if (modelName.contains(type)) {
                    excluded = true;
                    break;
                }
            }

            if (!excluded) {
                suitable.add(model);
            }
        }

        return suitable;
    }

    /**
     * Convert user-friendly pattern to regex.
     */
    private static String convertToRegex(String pattern) {
        if (pattern.contains("|")) {
            String[] parts = pattern.split("\\|");
            StringBuilder regex = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                if (part.contains("*")) {
                    part = part.replace("*", ".*");
                }
                regex.append(part);
                if (i < parts.length - 1) {
                    regex.append("|");
                }
            }

            return regex.toString();
        }

        if (pattern.contains("*")) {
            return pattern.replace("*", ".*");
        }

        return pattern;
    }

    /**
     * Display the list of models that will be tested.
     */
    private static void printModelList(List<OllamaModel> models) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("MODELS TO BE TESTED");
        System.out.println("=".repeat(80));

        boolean hasFilters = false;
        if (modelFilter != null) {
            System.out.println("Name filter: \"" + modelFilter + "\"");
            hasFilters = true;
        }
        if (maxSizeGB != DEFAULT_MAX_SIZE_GB) {
            System.out.printf("Max size: %.1f GB%n", maxSizeGB);
            hasFilters = true;
        }
        if (hasFilters) {
            System.out.println("-".repeat(80));
        }

        System.out.printf("%-40s %-12s %-15s %-10s%n",
                "Model Name", "Size (GB)", "Quantization", "Modified");
        System.out.println("-".repeat(80));

        for (OllamaModel model : models) {
            double sizeGB = model.sizeBytes / 1_000_000_000.0;
            String modified = model.modified;

            if (modified != null && modified.length() > 10) {
                modified = modified.substring(0, 10);
            }

            System.out.printf("%-40s %-12.2f %-15s %-10s%n",
                    model.name, sizeGB, model.quantization, modified);
        }

        System.out.println("-".repeat(80));
        System.out.printf("Total: %d model%s\n", models.size(), models.size() == 1 ? "" : "s");
        System.out.println("=".repeat(80));
        System.out.println();
    }

    /**
     * Test a single model.
     */
    private static ModelTestResult testModel(String modelName, long modelSize) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Testing: " + modelName);
        System.out.println("=".repeat(60));

        ModelTestResult result = new ModelTestResult(modelName, modelSize);
        FunctionCallingClient client = new FunctionCallingClient(OLLAMA_BASE_URL);

        // Register functions
        client.registerFunction(new DayOfWeekTool());
        client.registerFunction(new WeatherTool());

        // Define test cases
        String currentDay = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        List<TestCase> testCases = Arrays.asList(
                new TestCase("What is the day of the week today?", currentDay),
                new TestCase("What is the weather in the capital of France?", "Pas Mal"),
                new TestCase("Is it going to be sunny in Paris tomorrow?", "Pas Mal")
        );

        // Warmup run
        System.out.print("  Performing warmup run... ");
        FunctionCallingClient.FunctionCallResult warmupResult =
                client.sendPromptWithFunctions(modelName, testCases.get(0).prompt, testCases.get(0).expected, TEMPERATURE);
        printTestResult(warmupResult);

        // If warmup failed with "does not support tools", skip this model
        if (!warmupResult.isSuccess() && warmupResult.getError() != null &&
                warmupResult.getError().contains("does not support tools")) {
            result.error = "Model does not support function calling";
            System.out.println("  ⊗ Skipping remaining tests - model does not support function calling\n");
            return result;
        }

        // Run actual tests
        List<Long> executionTimes = new ArrayList<>();
        int successCount = 0;
        int totalTests = 0;

        for (TestCase testCase : testCases) {
            System.out.println("Prompt: " + testCase.prompt);

            for (int i = 0; i < NUM_RUNS; i++) {
                System.out.print(String.format("  Run %d/%d... ", i + 1, NUM_RUNS));

                long startTime = System.nanoTime();
                FunctionCallingClient.FunctionCallResult testResult =
                        client.sendPromptWithFunctions(modelName, testCase.prompt, testCase.expected, TEMPERATURE);
                long endTime = System.nanoTime();

                executionTimes.add(endTime - startTime);
                if (testResult.isSuccess()) {
                    successCount++;
                }
                totalTests++;

                printTestResult(testResult);
            }
        }

        // Calculate statistics
        result.successRate = successCount;
        result.totalPossible = totalTests;

        if (!executionTimes.isEmpty()) {
            long sum = 0;
            for (long time : executionTimes) {
                sum += time;
            }
            result.avgExecutionTime = (sum / (double) executionTimes.size()) / 1_000_000.0;

            double mean = sum / (double) executionTimes.size();
            double variance = 0;
            for (long time : executionTimes) {
                variance += Math.pow(time - mean, 2);
            }
            variance /= executionTimes.size();
            result.stdDevExecutionTime = Math.sqrt(variance) / 1_000_000.0;
        }

        return result;
    }

    /**
     * Print test result status.
     */
    private static void printTestResult(FunctionCallingClient.FunctionCallResult result) {
        if (result.isSuccess()) {
            List<String> functionCalls = new ArrayList<>();
            for (FunctionCallingClient.FunctionCallDetail detail : result.getFunctionDetails()) {
                functionCalls.add(detail.toString());
            }
            System.out.println("PASS: " + String.join(" -> ", functionCalls));
        } else {
            if (result.getError() != null && !result.getError().isEmpty()) {
                System.out.println("FAIL (" + extractErrorMessage(result.getError()) + ")");
            } else {
                System.out.println("FAIL");
            }
        }
    }

    /**
     * Extract a clean error message.
     */
    private static String extractErrorMessage(String fullError) {
        if (fullError.length() > 100) {
            return fullError.substring(0, 100) + "...";
        }
        return fullError;
    }

    /**
     * Print final report.
     */
    private static void printReport(List<ModelTestResult> results) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("FUNCTION CALLING TEST RESULTS SUMMARY");
        System.out.println("=".repeat(60));

        System.out.printf("%-30s %-12s %-12s %-12s %-15s%n",
                "Model", "Size (GB)", "Time (ms)", "σ Time", "Success Rate");
        System.out.println("-".repeat(90));

        for (ModelTestResult result : results) {
            double sizeGB = result.modelSize / 1_000_000_000.0;
            String successRate = String.format("%d/%d", result.successRate, result.totalPossible);
            System.out.printf("%-30s %-12.2f %-12.0f ±%-11.0f %-15s%n",
                    result.modelName, sizeGB, result.avgExecutionTime, result.stdDevExecutionTime, successRate);
        }

        System.out.println("\nDETAILED STATISTICS");
        System.out.println("=".repeat(60));

        int totalModels = results.size();
        List<String> perfectModels = new ArrayList<>();
        List<ModelTestResult> otherModels = new ArrayList<>();

        for (ModelTestResult result : results) {
            if (result.successRate == result.totalPossible) {
                perfectModels.add(result.modelName);
            } else {
                otherModels.add(result);
            }
        }

        System.out.printf("\nTotal models tested: %d%n", totalModels);
        System.out.printf("Perfect score models: %d%n", perfectModels.size());
        System.out.printf("Other models: %d%n", otherModels.size());

        if (!perfectModels.isEmpty()) {
            System.out.println("\nModels with perfect scores:");
            for (String model : perfectModels) {
                System.out.println("  ✓ " + model);
            }
        }

        if (!otherModels.isEmpty()) {
            System.out.println("\nOther models:");
            for (ModelTestResult result : otherModels) {
                double successPercent = (result.successRate / (double) result.totalPossible) * 100;
                System.out.printf("  ⚠ %s: %.1f%% success rate%n", result.modelName, successPercent);
            }
        }
    }

    /**
     * Write results to CSV file.
     */
    private static void writeResultsCsv(List<ModelTestResult> results) {
        String filename = "function_calling_results.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Model,Size_GB,Time_ms,Time_StdDev,Success_Rate,Total_Possible");

            for (ModelTestResult result : results) {
                double sizeGB = result.modelSize / 1_000_000_000.0;
                writer.printf("%s,%.2f,%.0f,%.0f,%d,%d%n",
                        result.modelName, sizeGB, result.avgExecutionTime,
                        result.stdDevExecutionTime, result.successRate, result.totalPossible);
            }

            logger.info("Results written to {}", filename);
            System.out.printf("\nResults have been saved to %s%n", filename);
        } catch (Exception e) {
            logger.error("Error writing CSV file", e);
        }
    }
}
