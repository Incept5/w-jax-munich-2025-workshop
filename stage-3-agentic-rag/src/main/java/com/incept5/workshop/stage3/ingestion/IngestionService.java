package com.incept5.workshop.stage3.ingestion;

import com.incept5.workshop.stage3.db.DatabaseConfig;
import com.incept5.workshop.stage3.db.PgVectorStore;
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
 *    a. Use gitingest to extract repository content
 *    b. Chunk the extracted text
 *    c. Generate embeddings for each chunk
 *    d. Store in PostgreSQL with pgvector
 * 3. Report statistics
 */
public class IngestionService {
    private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);
    
    private final PgVectorStore vectorStore;
    private final DocumentChunker chunker;
    private final String dataDir;
    
    public IngestionService(PgVectorStore vectorStore, DocumentChunker chunker, String dataDir) {
        this.vectorStore = vectorStore;
        this.chunker = chunker;
        this.dataDir = dataDir;
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
        
        // Check if gitingest output already exists
        File output = new File(outputFile);
        if (output.exists()) {
            logger.info("Using existing gitingest output: {}", outputFile);
            return outputFile;
        }
        
        // Run gitingest command
        logger.info("Running gitingest for {}", repo.url());
        
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
     * Main entry point for the ingestion service.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java -jar stage-3-agentic-rag.jar <repos.yaml>");
            System.exit(1);
        }
        
        String configFile = args[0];
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
            (String) settingsMap.getOrDefault("ollamaBaseUrl", "http://localhost:11434")
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
        EmbeddingService embeddingService = new EmbeddingService(
            config.settings().ollamaBaseUrl(),
            config.settings().embeddingModel()
        );
        
        PgVectorStore vectorStore = new PgVectorStore(dataSource, embeddingService);
        
        DocumentChunker chunker = new DocumentChunker(
            config.settings().chunkSize(),
            config.settings().chunkOverlap()
        );
        
        // 4. Run ingestion
        IngestionService service = new IngestionService(
            vectorStore,
            chunker,
            "data/gitingest-output"
        );
        
        service.ingest(config);
        
        // 5. Cleanup
        vectorStore.close();
        
        logger.info("✅ Ingestion pipeline complete!");
    }
}
