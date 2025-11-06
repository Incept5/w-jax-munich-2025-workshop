-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Documents table with embeddings
-- Stores chunked documents with their vector embeddings
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    source VARCHAR(255) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    chunk_index INTEGER NOT NULL,
    metadata JSONB,
    embedding vector(768),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure we don't duplicate chunks
    UNIQUE(source, file_hash, chunk_index)
);

-- Index for efficient vector similarity search
-- IVFFlat (Inverted File with Flat compression) is faster than exact search
-- cosine distance is best for normalized embeddings
CREATE INDEX idx_documents_embedding ON documents 
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Indexes for metadata queries
CREATE INDEX idx_documents_source ON documents(source);
CREATE INDEX idx_documents_file_hash ON documents(file_hash);
CREATE INDEX idx_documents_metadata ON documents USING gin(metadata);

-- Function for similarity search (helper for testing)
CREATE OR REPLACE FUNCTION search_documents(
    query_embedding vector(768),
    match_threshold float,
    match_count int
)
RETURNS TABLE (
    id int,
    content text,
    source varchar(255),
    similarity float
)
LANGUAGE sql
AS $$
    SELECT
        id,
        content,
        source,
        1 - (embedding <=> query_embedding) as similarity
    FROM documents
    WHERE 1 - (embedding <=> query_embedding) > match_threshold
    ORDER BY similarity DESC
    LIMIT match_count;
$$;
