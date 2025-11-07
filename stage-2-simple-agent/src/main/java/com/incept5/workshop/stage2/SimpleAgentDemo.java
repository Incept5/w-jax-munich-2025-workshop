
package com.incept5.workshop.stage2;

import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.backend.BackendFactory;
import com.incept5.ollama.config.BackendConfig;
import com.incept5.workshop.stage2.tool.CountryInfoTool;
import com.incept5.workshop.stage2.tool.ToolRegistry;
import com.incept5.workshop.stage2.tool.WeatherTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo application for the Simple Agent.
 * 
 * This demonstrates how to:
 * 1. Set up a tool registry with real-world API tools
 * 2. Create an AI backend (Ollama, LM Studio, etc.)
 * 3. Initialize the agent
 * 4. Run the agent on tasks
 * 
 * Usage:
 *   java -jar stage-2-simple-agent.jar
 *   java -jar stage-2-simple-agent.jar "What's the weather in Tokyo?"
 *   java -jar stage-2-simple-agent.jar --verbose "Weather in Paris vs Berlin"
 */
public class SimpleAgentDemo {
    private static final Logger logger = LoggerFactory.getLogger(SimpleAgentDemo.class);
    
    // Default task if none provided
    private static final String DEFAULT_TASK = 
            "What's the weather like in the capital of France?";
    
    public static void main(String[] args) {
        // Parse arguments
        boolean verbose = false;
        String task = null;
        
        for (int i = 0; i < args.length; i++) {
            if ("--verbose".equals(args[i]) || "-v".equals(args[i])) {
                verbose = true;
            } else if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                printHelp();
                return;
            } else if (!args[i].startsWith("-")) {
                task = args[i];
            }
        }
        
        // Use default task if none provided
        if (task == null || task.isBlank()) {
            task = DEFAULT_TASK;
            System.out.println("No task provided, using default task:");
            System.out.println("  \"" + task + "\"");
            System.out.println();
            System.out.println("Run with --help to see usage options.");
            System.out.println();
        }
        
        // Print header
        System.out.println("""
                ╔══════════════════════════════════════════════════════════╗
                ║         Simple Agent with Tool Calling - Stage 1         ║
                ╚══════════════════════════════════════════════════════════╝
                """);
        
        try {
            // Initialize tools
            System.out.println("Initializing tools...");
            ToolRegistry toolRegistry = new ToolRegistry()
                    .register(new WeatherTool())
                    .register(new CountryInfoTool());
            
            System.out.println("✓ Registered " + toolRegistry.size() + " tools: " + 
                    String.join(", ", toolRegistry.getToolNames()));
            System.out.println();
            
            // Initialize AI backend
            System.out.println("Connecting to AI backend...");
            BackendConfig config = BackendConfig.builder().build(); // Uses defaults
            AIBackend backend = BackendFactory.createBackend(
                    config.backendType(),
                    config.baseUrl(),
                    config.model(),
                    config.requestTimeout()
            );
            
            System.out.println("✓ Connected to " + backend.getBackendType() + 
                    " (model: " + backend.getModelName() + ")");
            System.out.println();
            
            // Run agent
            System.out.println("Task: " + task);
            System.out.println("=".repeat(60));
            System.out.println();
            
            try (backend) {
                SimpleAgent agent = new SimpleAgent(backend, toolRegistry);
                SimpleAgent.AgentResult result = agent.run(task, verbose);
                
                // Print result
                if (!verbose) {
                    System.out.println();
                }
                System.out.println("=".repeat(60));
                System.out.println("FINAL ANSWER:");
                System.out.println("=".repeat(60));
                System.out.println(result.response());
                System.out.println("=".repeat(60));
                System.out.println();
                System.out.println("Completed in " + result.iterations() + " iteration(s)");
                
                if (!result.completed()) {
                    System.out.println("⚠ Warning: Task did not complete within iteration limit");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error running agent", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void printHelp() {
        System.out.println("""
                Simple Agent Demo - Stage 1
                
                A simple AI agent that can use real-world API tools to answer questions.
                
                Usage:
                  java -jar stage-2-simple-agent.jar [OPTIONS] [TASK]
                
                Options:
                  -v, --verbose    Show detailed step-by-step execution
                  -h, --help       Show this help message
                
                Examples:
                  # Use default task
                  java -jar stage-2-simple-agent.jar
                
                  # Custom task
                  java -jar stage-2-simple-agent.jar "What's the weather in Tokyo?"
                
                  # Multi-step reasoning
                  java -jar stage-2-simple-agent.jar "What's the weather in Japan's capital?"
                
                  # Comparison task
                  java -jar stage-2-simple-agent.jar "Compare weather in Paris and Berlin"
                
                  # With verbose output
                  java -jar stage-2-simple-agent.jar --verbose "Tell me about Brazil"
                
                Available Tools:
                  - weather: Gets real-time weather for any city (wttr.in API)
                  - country_info: Gets information about countries (REST Countries API)
                
                Default Task:
                  "What's the weather like in the capital of France?"
                
                Features:
                  ✓ Real-world API integration (no authentication needed)
                  ✓ Multi-step reasoning (compose multiple tool calls)
                  ✓ Automatic tool selection based on task
                  ✓ Simple XML-based tool calling format
                  ✓ Graceful error handling
                
                Note: Requires an AI backend (Ollama by default) to be running.
                """);
    }
}
