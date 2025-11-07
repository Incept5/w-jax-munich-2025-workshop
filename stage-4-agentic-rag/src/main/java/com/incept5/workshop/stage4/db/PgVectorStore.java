package com.incept5.workshop.stage4.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.incept5.workshop.stage4.ingestion.EmbeddingService;
import com.pgvector.PGvector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL vector store using pgvector extension.
 * 
 * Provides:
 * - Document storage with embeddings
 * - Similarity search with cosine distance
 * - Metadata filtering
 * - Idempotent ingestion (hash-based deduplication)
 */
public class PgVectorStore implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(PgVectorStore.class);
    
    private final DataSource dataSource;
    private final EmbeddingService embeddingService;
    private final Gson gson;
    
    public PgVectorStore(DataSource dataSource, EmbeddingService embeddingService) {
        this.dataSource = dataSource;
        this.embeddingService = embeddingService;
        this.gson = new Gson();
    }
    
    /**
     * Store a document chunk with its embedding.
     * Uses file hash to avoid duplicate ingestion.
     */
    public void store(String content, String source, String fileHash, 
                     int chunkIndex, Map<String, Object> metadata) throws SQLException {
        // 1. Generate embedding for the content
        float[] embedding = embeddingService.generateEmbedding(content);
        
        // 2. Store in database (ON CONFLICT DO NOTHING for idempotency)
        String sql = """
            INSERT INTO documents (content, source, file_hash, chunk_index, metadata, embedding)
            VALUES (?, ?, ?, ?, ?::jsonb, ?)
            ON CONFLICT (source, file_hash, chunk_index) DO NOTHING
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, content);
            stmt.setString(2, source);
            stmt.setString(3, fileHash);
            stmt.setInt(4, chunkIndex);
            stmt.setString(5, gson.toJson(metadata));
            stmt.setObject(6, new PGvector(embedding));
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.debug("Stored chunk {}/{} from {}", chunkIndex, fileHash, source);
            }
        }
    }
    
    /**
     * Search for documents similar to the query.
     * 
     * @param query The search query
     * @param topK Number of results to return
     * @param threshold Minimum similarity score (0-1)
     * @return List of matching documents
     */
    public List<Document> search(String query, int topK, double threshold) throws SQLException {
        // 1. Generate query embedding
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        
        // 2. Execute similarity search
        // Uses cosine distance (<=> operator) from pgvector
        String sql = """
            SELECT id, content, source, file_hash, chunk_index, metadata,
                   1 - (embedding <=> ?::vector) as similarity
            FROM documents
            WHERE 1 - (embedding <=> ?::vector) > ?
            ORDER BY similarity DESC
            LIMIT ?
            """;
        
        List<Document> results = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            PGvector pgVector = new PGvector(queryEmbedding);
            stmt.setObject(1, pgVector);
            stmt.setObject(2, pgVector);
            stmt.setDouble(3, threshold);
            stmt.setInt(4, topK);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String metadataJson = rs.getString("metadata");
                    Map<String, Object> metadata = gson.fromJson(metadataJson, 
                        new TypeToken<Map<String, Object>>(){}.getType());
                    
                    results.add(new Document(
                        rs.getInt("id"),
                        rs.getString("content"),
                        rs.getString("source"),
                        rs.getString("file_hash"),
                        rs.getInt("chunk_index"),
                        metadata,
                        rs.getDouble("similarity")
                    ));
                }
            }
        }
        
        logger.info("Vector search returned {} results for query (threshold: {})", 
            results.size(), threshold);
        return results;
    }
    
    /**
     * Check if a file has already been ingested.
     */
    public boolean isIngested(String source, String fileHash) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM documents WHERE source = ? AND file_hash = ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, source);
            stmt.setString(2, fileHash);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }
    
    /**
     * Get total document count.
     */
    public int getTotalDocuments() throws SQLException {
        String sql = "SELECT COUNT(*) FROM documents";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    /**
     * Get document count by source.
     */
    public Map<String, Integer> getDocumentCountsBySource() throws SQLException {
        String sql = "SELECT source, COUNT(*) as count FROM documents GROUP BY source";
        
        Map<String, Integer> counts = new java.util.HashMap<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                counts.put(rs.getString("source"), rs.getInt("count"));
            }
        }
        
        return counts;
    }
    
    /**
     * Get a specific chunk by its coordinates.
     * Used for retrieving neighboring chunks.
     * 
     * @param source The source repository
     * @param fileHash The file hash
     * @param chunkIndex The chunk index
     * @return The document if found, null otherwise
     */
    public Document getChunkByIndex(String source, String fileHash, int chunkIndex) throws SQLException {
        String sql = """
            SELECT id, content, source, file_hash, chunk_index, metadata
            FROM documents
            WHERE source = ? AND file_hash = ? AND chunk_index = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, source);
            stmt.setString(2, fileHash);
            stmt.setInt(3, chunkIndex);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String metadataJson = rs.getString("metadata");
                    Map<String, Object> metadata = gson.fromJson(metadataJson,
                        new TypeToken<Map<String, Object>>(){}.getType());
                    
                    return new Document(
                        rs.getInt("id"),
                        rs.getString("content"),
                        rs.getString("source"),
                        rs.getString("file_hash"),
                        rs.getInt("chunk_index"),
                        metadata
                    );
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get neighboring chunks around a document.
     * 
     * @param doc The center document
     * @param radius Number of chunks before and after (e.g., 1 = immediate neighbors)
     * @return List of documents in chunk order (may include the original)
     */
    public List<Document> getNeighboringChunks(Document doc, int radius) throws SQLException {
        String sql = """
            SELECT id, content, source, file_hash, chunk_index, metadata
            FROM documents
            WHERE source = ? AND file_hash = ? 
              AND chunk_index BETWEEN ? AND ?
            ORDER BY chunk_index
            """;
        
        List<Document> neighbors = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, doc.source());
            stmt.setString(2, doc.fileHash());
            stmt.setInt(3, doc.chunkIndex() - radius);
            stmt.setInt(4, doc.chunkIndex() + radius);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String metadataJson = rs.getString("metadata");
                    Map<String, Object> metadata = gson.fromJson(metadataJson,
                        new TypeToken<Map<String, Object>>(){}.getType());
                    
                    neighbors.add(new Document(
                        rs.getInt("id"),
                        rs.getString("content"),
                        rs.getString("source"),
                        rs.getString("file_hash"),
                        rs.getInt("chunk_index"),
                        metadata
                    ));
                }
            }
        }
        
        logger.debug("Retrieved {} neighboring chunks for document {}", neighbors.size(), doc.id());
        return neighbors;
    }
    
    @Override
    public void close() {
        // DataSource cleanup is handled by HikariCP
        logger.info("PgVectorStore closed");
    }
}
