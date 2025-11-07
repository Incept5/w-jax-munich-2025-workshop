package com.incept5.workshop.stage4.ingestion;

import com.incept5.workshop.stage4.db.DatabaseConfig;
import com.incept5.workshop.stage4.db.PgVectorStore;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Main ingestion service that orchestrates the RAG pipeline.
 * 
 * Workflow:
 * 1. Read repos.yaml configuration
 * 2. For each repository:
 *    a. Load repository content (from git or fetch fresh with gitingest)
 *    b. Chunk the extracted text
 *    c. Generate embeddings for each chunk
 *    d. Store in PostgreSQL with pgvector
 * 3. Report statistics
 */
public class IngestionService {
    private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);
    
    public enum Mode {
        USE_LOCAL,  // Use committed files from git (default)
        REFRESH     // Fetch fresh data with gitingest
    }
    
    private final PgVectorStore vectorStore;
    private final DocumentChunker chunker;
    private final String dataDir;
    private final Mode mode;
    
    public IngestionService(PgVectorStore vectorStore, DocumentChunker chunker, String dataDir) {
        this(vectorStore, chunker, dataDir, Mode.USE_LOCAL);
    }
    
    public IngestionService(PgVectorStore vectorStore, DocumentChunker chunker, String dataDir, Mode mode) {
        this.vectorStore = vectorStore;
        this.chunker = chunker;
        this.dataDir = dataDir;
        this.mode = mode;
    }
    
    /**
     * Run the ingestion pipeline for all configured repositories.
     */
    public void ingest(IngestionConfig config) throws IOException, SQLException {
        logger.info("Starting ingestion pipeline for {} repositories", 
            config.repositories().size());
        
        int totalDocuments = 0;
        
        for (RepoConfig repo : config.repositories()) {
            logger.info("Processing repository: {} ({})", repo.name(), repo.url());
            
            try {
                int count = processRepository(repo);
                totalDocuments += count;
                logger.info("✓ Completed {}: {} documents", repo.name(), count);
            } catch (Exception e) {
                logger.error("✗ Failed to process {}: {}", repo.name(), e.getMessage(), e);
            }
        }
        
        logger.info("Ingestion complete! Total documents: {}", totalDocuments);
        
        // Print statistics
        Map<String, Integer> counts = vectorStore.getDocumentCountsBySource();
        logger.info("\nDocument counts by source:");
        counts.forEach((source, count) -> 
            logger.info("  {} : {}", source, count));
    }
    
    private int processRepository(RepoConfig repo) throws IOException, SQLException {
        // 1. Run gitingest to extract repository content
        String outputFile = runGitIngest(repo);
        
        // 2. Read the extracted content
        String content = Files.readString(Path.of(outputFile));
        
        // 3. Calculate file hash for deduplication
        String fileHash = calculateHash(content);
        
        // 4. Check if already ingested
        if (vectorStore.isIngested(repo.name(), fileHash)) {
            logger.info("Repository {} already ingested (hash: {}), skipping", 
                repo.name(), fileHash.substring(0, 8));
            return 0;
        }
        
        // 5. Chunk the content
        List<String> chunks = chunker.chunk(content);
        logger.info("Created {} chunks from {}", chunks.size(), repo.name());
        
        // 6. Store each chunk with embedding
        int stored = 0;
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            
            // Metadata for this chunk
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("repository", repo.url());
            metadata.put("branch", repo.branch());
            metadata.put("description", repo.description());
            metadata.put("total_chunks", chunks.size());
            
            // Store with embedding
            vectorStore.store(chunk, repo.name(), fileHash, i, metadata);
            stored++;
            
            // Progress feedback
            if ((i + 1) % 10 == 0 || i == chunks.size() - 1) {
                logger.info("Progress: {}/{} chunks", i + 1, chunks.size());
            }
        }
        
        return stored;
    }
    
    private String runGitIngest(RepoConfig repo) throws IOException {
        // Ensure data directory exists
        File dataDir = new File(this.dataDir);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Output file path
        String outputFile = this.dataDir + "/" + repo.name() + ".txt";
        File output = new File(outputFile);
        
        // In USE_LOCAL mode, just check if file exists
        if (mode == Mode.USE_LOCAL) {
            if (output.exists()) {
                logger.info("Using committed file: {}", outputFile);
                return outputFile;
            } else {
                throw new IOException(
                    "Repository file not found: " + outputFile + "\n" +
                    "Expected file is missing from git. Either:\n" +
                    "  1. Ensure the file is committed to git, or\n" +
                    "  2. Run with --refresh flag to fetch fresh data"
                );
            }
        }
        
        // In REFRESH mode, fetch fresh data with gitingest
        logger.info("Running gitingest for {} (refresh mode)", repo.url());
        
        ProcessBuilder pb = new ProcessBuilder(
            "gitingest",
            repo.url(),
            "-o", outputFile
        );
        pb.inheritIO();
        
        Process process = pb.start();
        
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("gitingest failed with exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("gitingest interrupted", e);
        }
        
        logger.info("gitingest output saved to: {}", outputFile);
        return outputFile;
    }
    
    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }
    
    /**
     * Get default Ollama base URL from environment or system property
     */
    private static String getDefaultOllamaBaseUrl() {
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
    
    /**
     * Main entry point for the ingestion service.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java -jar stage-4-agentic-rag.jar <repos.yaml> [--refresh]");
            System.err.println("  --refresh: Fetch fresh data with gitingest (requires gitingest installed)");
            System.exit(1);
        }
        
        String configFile = args[0];
        Mode mode = Mode.USE_LOCAL;
        
        // Check for --refresh flag
        if (args.length > 1 && "--refresh".equals(args[1])) {
            mode = Mode.REFRESH;
            logger.info("Refresh mode enabled - will fetch fresh repository data");
        } else {
            logger.info("Using committed repository files from git");
        }
        
        logger.info("Reading configuration from: {}", configFile);
        
        // 1. Load configuration
        Yaml yaml = new Yaml();
        Map<String, Object> yamlData;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            yamlData = yaml.load(fis);
        }
        
        // Parse repositories
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reposList = (List<Map<String, Object>>) yamlData.get("repositories");
        List<RepoConfig> repos = reposList.stream()
            .map(m -> new RepoConfig(
                (String) m.get("name"),
                (String) m.get("url"),
                (String) m.get("branch"),
                (String) m.get("description")
            ))
            .toList();
        
        // Parse settings
        @SuppressWarnings("unchecked")
        Map<String, Object> settingsMap = (Map<String, Object>) yamlData.get("settings");
        IngestionConfig.Settings settings = new IngestionConfig.Settings(
            (Integer) settingsMap.getOrDefault("chunkSize", 800),
            (Integer) settingsMap.getOrDefault("chunkOverlap", 200),
            ((Number) settingsMap.getOrDefault("similarityThreshold", 0.7)).doubleValue(),
            (String) settingsMap.getOrDefault("embeddingModel", "nomic-embed-text"),
            (String) settingsMap.getOrDefault("ollamaBaseUrl", getDefaultOllamaBaseUrl())
        );
        
        IngestionConfig config = new IngestionConfig(repos, settings);
        
        logger.info("Configuration loaded: {} repositories", config.repositories().size());
        
        // 2. Run Flyway migrations
        logger.info("Running database migrations...");
        DataSource dataSource = DatabaseConfig.createDataSource();
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
        flyway.migrate();
        logger.info("✓ Migrations complete");
        
        // 3. Create services
        // Use environment-aware embedding service (supports both Ollama and Python backends)
        EmbeddingService embeddingService = EmbeddingService.fromEnvironment();
        
        // Alternative: Explicitly specify backend
        // EmbeddingService embeddingService = new EmbeddingService(
        //     "http://localhost:8001",  // Python service
        //     "nomic-embed-text"
        // );
        
        PgVectorStore vectorStore = new PgVectorStore(dataSource, embeddingService);
        
        DocumentChunker chunker = new DocumentChunker(
            config.settings().chunkSize(),
            config.settings().chunkOverlap()
        );
        
        // 4. Run ingestion
        IngestionService service = new IngestionService(
            vectorStore,
            chunker,
            "data/gitingest-output",
            mode
        );
        
        service.ingest(config);
        
        // 5. Cleanup
        vectorStore.close();
        
        logger.info("✅ Ingestion pipeline complete!");
    }
}
