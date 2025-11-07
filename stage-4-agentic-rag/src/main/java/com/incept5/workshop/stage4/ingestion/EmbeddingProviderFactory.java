package com.incept5.workshop.stage4.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating embedding providers based on configuration.
 * 
 * Supports:
 * - Python provider (local, free) - Default
 * - OpenAI provider (cloud, paid)
 * 
 * Configuration sources (in priority order):
 * 1. Environment variable: EMBEDDING_PROVIDER
 * 2. Configuration file: repos.yaml
 * 3. Default: Python provider
 */
public class EmbeddingProviderFactory {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingProviderFactory.class);
    
    /**
     * Create embedding provider from environment and configuration.
     * 
     * Priority:
     * 1. EMBEDDING_PROVIDER env var
     * 2. Configuration file
     * 3. Default to Python
     * 
     * @param config Ingestion configuration from repos.yaml
     * @return Configured embedding provider
     * @throws RuntimeException if provider configuration is invalid
     */
    public static EmbeddingProvider createFromConfig(IngestionConfig config) {
        // Check environment variable first
        String envProvider = System.getenv("EMBEDDING_PROVIDER");
        if (envProvider != null && !envProvider.trim().isEmpty()) {
            logger.info("🔧 EMBEDDING_PROVIDER env var set: {}", envProvider);
            return createProvider(envProvider.trim().toLowerCase(), config);
        }
        
        // Check configuration file
        if (config.settings().embedding() != null) {
            String configProvider = config.settings().embedding().provider();
            if (configProvider != null && !configProvider.trim().isEmpty()) {
                logger.info("📋 Using provider from repos.yaml: {}", configProvider);
                return createProvider(configProvider.trim().toLowerCase(), config);
            }
        }
        
        // Default to Python
        logger.info("📋 No provider specified, defaulting to Python (local, free)");
        return createPythonProvider(config);
    }
    
    /**
     * Create provider by name.
     */
    private static EmbeddingProvider createProvider(String providerName, IngestionConfig config) {
        return switch (providerName) {
            case "python" -> createPythonProvider(config);
            case "openai" -> createOpenAIProvider(config);
            default -> {
                logger.warn("⚠️  Unknown provider '{}', falling back to Python", providerName);
                yield createPythonProvider(config);
            }
        };
    }
    
    /**
     * Create Python embedding provider.
     */
    private static EmbeddingProvider createPythonProvider(IngestionConfig config) {
        // Get URL from environment or config
        String url = System.getenv("PYTHON_EMBEDDING_SERVICE_URL");
        if (url == null || url.trim().isEmpty()) {
            if (config.settings().embedding() != null && 
                config.settings().embedding().python() != null) {
                url = config.settings().embedding().python().url();
            }
        }
        
        // Fallback to default
        if (url == null || url.trim().isEmpty()) {
            url = "http://localhost:8001";
            logger.info("📍 Using default Python service URL: {}", url);
        }
        
        // Get model from config
        String model = "nomic-embed-text";
        if (config.settings().embedding() != null && 
            config.settings().embedding().python() != null &&
            config.settings().embedding().python().model() != null) {
            model = config.settings().embedding().python().model();
        }
        
        PythonEmbeddingProvider provider = new PythonEmbeddingProvider(url, model);
        
        // Test connection
        try {
            logger.info("🔍 Testing Python service connection...");
            provider.testConnection();
            logger.info("✅ Python service is ready at {}", url);
        } catch (Exception e) {
            logger.error("❌ Failed to connect to Python service at {}", url);
            logger.error("   Make sure the service is running: cd embedding-service && ./start.sh");
            throw new RuntimeException("Python embedding service connection failed", e);
        }
        
        return provider;
    }
    
    /**
     * Create OpenAI embedding provider.
     */
    private static EmbeddingProvider createOpenAIProvider(IngestionConfig config) {
        // Get API key from environment
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException(
                "OPENAI_API_KEY environment variable is required for OpenAI provider.\n" +
                "Get your key from: https://platform.openai.com/api-keys\n" +
                "Then set it in your .env file or export it: export OPENAI_API_KEY=sk-..."
            );
        }
        
        // Get model from config
        String model = "text-embedding-3-small";  // Default to cheaper option
        if (config.settings().embedding() != null && 
            config.settings().embedding().openai() != null &&
            config.settings().embedding().openai().model() != null) {
            model = config.settings().embedding().openai().model();
        }
        
        OpenAIEmbeddingProvider provider = new OpenAIEmbeddingProvider(apiKey, model);
        
        // Test connection
        try {
            logger.info("🔍 Testing OpenAI API connection...");
            provider.testConnection();
            logger.info("✅ OpenAI API is ready (model: {})", model);
            
            // Show cost estimate
            showCostEstimate(model);
        } catch (Exception e) {
            logger.error("❌ Failed to connect to OpenAI API");
            logger.error("   Check your API key and network connection");
            throw new RuntimeException("OpenAI API connection failed", e);
        }
        
        return provider;
    }
    
    /**
     * Show cost estimate for OpenAI embeddings.
     */
    private static void showCostEstimate(String model) {
        double costPer1kTokens = model.contains("large") ? 0.00013 : 0.00002;
        int estimatedTokens = 390_000; // 487 documents * ~800 tokens
        double estimatedCost = (estimatedTokens / 1000.0) * costPer1kTokens;
        
        logger.info("💰 Estimated cost for ingestion:");
        logger.info("   Model: {}", model);
        logger.info("   Price: ${} per 1k tokens", String.format("%.5f", costPer1kTokens));
        logger.info("   Est. tokens: ~{}", String.format("%,d", estimatedTokens));
        logger.info("   Est. cost: ~${}", String.format("%.3f", estimatedCost));
        
        if (estimatedCost < 0.01) {
            logger.info("   (less than 1 cent!)");
        }
    }
}
