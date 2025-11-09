-- Update vector dimension from 768 to 1024 for Qwen3 embedding model
-- This migration drops and recreates the table because pgvector doesn't support
-- ALTER COLUMN for vector dimensions

-- Drop dependent objects first
DROP FUNCTION IF EXISTS search_documents(vector(768), float, int);
DROP INDEX IF EXISTS idx_documents_embedding;

-- Drop and recreate table with new dimension
DROP TABLE IF EXISTS documents;

CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    source VARCHAR(255) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    chunk_index INTEGER NOT NULL,
    metadata JSONB,
    embedding vector(1024),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure we don't duplicate chunks
    UNIQUE(source, file_hash, chunk_index)
);

-- Recreate index for new dimension
CREATE INDEX idx_documents_embedding ON documents
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Recreate metadata indexes
CREATE INDEX idx_documents_source ON documents(source);
CREATE INDEX idx_documents_file_hash ON documents(file_hash);
CREATE INDEX idx_documents_metadata ON documents USING gin(metadata);

-- Recreate search function with new dimension
CREATE OR REPLACE FUNCTION search_documents(
    query_embedding vector(1024),
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
