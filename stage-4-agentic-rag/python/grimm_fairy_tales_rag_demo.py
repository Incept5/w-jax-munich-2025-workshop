#!/usr/bin/env python3
"""
Simple Grimm Fairy Tales RAG Search Demo

A minimal RAG system using Qwen3 embeddings for demonstration purposes.
"""

import torch
import torch.nn.functional as F
from torch import Tensor
from transformers import AutoTokenizer, AutoModel
import time
import re
from typing import List, Dict, Any, Optional, Tuple
import ollama
from ollama import Options
from rich.console import Console
import tiktoken


# Configuration
EMBEDDING_MODEL = 'Qwen/Qwen3-Embedding-0.6B'
EMBEDDING_DIM = 512
BATCH_SIZE = 64
MAX_TOKEN_LENGTH = 512
TOP_K = 6


def last_token_pool(last_hidden_states: Tensor, attention_mask: Tensor) -> Tensor:
    """Extract the last token embeddings efficiently."""
    left_padding = (attention_mask[:, -1].sum() == attention_mask.shape[0])
    if left_padding:
        return last_hidden_states[:, -1]
    else:
        sequence_lengths = attention_mask.sum(dim=1) - 1
        batch_size = last_hidden_states.shape[0]
        return last_hidden_states[torch.arange(batch_size, device=last_hidden_states.device), sequence_lengths]


def get_embeddings(texts: List[str], model, tokenizer, device: str) -> torch.Tensor:
    """Get embeddings for a batch of texts."""
    texts_with_eot = [text + "<|endoftext|>" for text in texts]
    
    batch_dict = tokenizer(
        texts_with_eot, 
        padding=True, 
        truncation=True, 
        max_length=MAX_TOKEN_LENGTH,
        return_tensors="pt"
    ).to(device)
    
    with torch.no_grad():
        outputs = model(**batch_dict)
    
    embeddings = last_token_pool(outputs.last_hidden_state, batch_dict['attention_mask'])
    
    # Apply dimension reduction
    if EMBEDDING_DIM and EMBEDDING_DIM < embeddings.shape[1]:
        embeddings = embeddings[:, :EMBEDDING_DIM]
    
    embeddings = F.normalize(embeddings, p=2, dim=1)
    return embeddings


def chunk_text(text: str) -> List[Dict[str, Any]]:
    """Split text into chapters using quadruple newlines."""
    chapters = re.split(r'\n\n\n\n', text)
    
    # Initialize tiktoken encoder
    encoder = tiktoken.get_encoding("cl100k_base")  # GPT-4 encoding
    
    chunks = []
    for i, chapter in enumerate(chapters):
        cleaned = ' '.join(chapter.strip().split())
        if len(cleaned) > 100:  # Only include substantial chapters
            token_count = len(encoder.encode(cleaned))
            chunks.append({
                'id': i,
                'text': cleaned,
                'length': len(cleaned),  # Keep byte length for backwards compatibility
                'tokens': token_count
            })
    
    return chunks


def search_chunks(query: str, chunks: List[Dict[str, Any]], chunk_embeddings: torch.Tensor, 
                 model, tokenizer, device: str) -> List[Dict[str, Any]]:
    """Search chunks using embedding similarity."""
    
    # Get query embedding
    query_embeddings = get_embeddings([query], model, tokenizer, device)
    query_embedding = query_embeddings[0:1]
    
    # Compute similarities
    with torch.no_grad():
        similarities = chunk_embeddings @ query_embedding.T
        similarities = similarities.flatten()
    
    # Find top results
    top_values, top_indices = torch.topk(similarities, k=min(TOP_K, len(similarities)), largest=True)
    
    # Build results
    results = []
    for i in range(len(top_indices)):
        idx = top_indices[i].item()
        score = top_values[i].item()
        chunk = chunks[idx]
        results.append({
            'text': chunk['text'],
            'similarity': score,
            'length': chunk['length'],
            'tokens': chunk['tokens']
        })
    
    return results


def generate_all_embeddings(chunks: List[Dict[str, Any]], model, tokenizer, device: str) -> torch.Tensor:
    """Generate embeddings for all chunks."""
    print(f"Generating embeddings for {len(chunks)} chunks...")
    
    all_embeddings = []
    for i in range(0, len(chunks), BATCH_SIZE):
        batch_chunks = chunks[i:i + BATCH_SIZE]
        batch_texts = [chunk['text'] for chunk in batch_chunks]
        batch_embeddings = get_embeddings(batch_texts, model, tokenizer, device)
        all_embeddings.append(batch_embeddings)
    
    return torch.cat(all_embeddings, dim=0)


def display_results(query: str, results: List[Dict[str, Any]]) -> None:
    """Display search results."""
    print(f"\nQuery: '{query}'")
    print("=" * 50)
    
    total_tokens = 0
    for i, result in enumerate(results, 1):
        similarity = result['similarity']
        text = result['text']
        tokens = result['tokens']
        total_tokens += tokens
        display_text = text[:150] + "..." if len(text) > 150 else text
        print(f"{i}. [Score: {similarity:.3f}] [Tokens: {tokens}] {display_text}")
    
    print(f"\nTotal tokens in results: {total_tokens}")
    print()


def generate_answer(query: str, results: List[Dict[str, Any]]) -> None:
    """Generate a complete answer using qwen3 via ollama with retrieved context."""
    LLM = "qwen3:4b"
    THINKING = False
    console = Console()
    
    # Format context from retrieved chunks
    context = "\n\n".join([f"Context {i+1}: {result['text'][:1000]}..." if len(result['text']) > 1000 
                           else f"Context {i+1}: {result['text']}" 
                           for i, result in enumerate(results[:3])])  # Use top 3 results
    
    system_prompt = """You are a helpful assistant that answers questions about Grimm fairy tales. 
    Use the provided context from the fairy tales to answer the user's question accurately and concisely.
    Provide your answer in English, even though the source text may be in German.
    Keep your response short and focused on directly answering the question."""
    
    instruction = f"""Based on the following context from Grimm fairy tales, please answer this question: {query}

Context:
{context}

Please provide a short, direct answer in English."""
    
    try:
        response = ollama.chat(
            model=LLM,
            think=THINKING,
            stream=False,
            messages=[
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': instruction}
            ],
            options=Options(
                temperature=0.7,
                num_ctx=32768,  # 32k context
                top_p=0.95,
                top_k=40,
                num_predict=-1
            )
        )
        
        console.print("\n[bold green]🧠 RAG Answer:[/bold green]")
        
        if hasattr(response.message, 'thinking') and response.message.thinking:
            console.print(f"[dim]Thinking: {response.message.thinking[:200]}...[/dim]")
        
        console.print(f"[blue]{response.message.content}[/blue]")
        
    except Exception as e:
        console.print(f"[red]Error generating answer: {e}[/red]")


def main() -> None:
    """Main demo function."""
    print("🔍 Simple Grimm Tales RAG Search Demo")
    print("=" * 40)
    
    # Setup device
    device = 'mps' if torch.backends.mps.is_available() else 'cuda' if torch.cuda.is_available() else 'cpu'
    print(f"Using device: {device}")
    
    # Load model
    print(f"Loading {EMBEDDING_MODEL}...")
    tokenizer = AutoTokenizer.from_pretrained(EMBEDDING_MODEL, padding_side='left')
    model = AutoModel.from_pretrained(EMBEDDING_MODEL).to(device)
    
    # Load document
    try:
        with open("Kinder-und-Hausmärchen-der-Gebrüder-Grimm.txt", "r", encoding="utf8") as f:
            text = f.read()
        print(f"Loaded document: {len(text):,} characters")
    except FileNotFoundError:
        print("Error: 'demos/Kinder-und-Hausmärchen-der-Gebrüder-Grimm.txt' not found!")
        return
    
    # Create chunks
    chunks = chunk_text(text)
    print(f"Created {len(chunks)} chunks")
    
    # Generate embeddings
    start_time = time.time()
    chunk_embeddings = generate_all_embeddings(chunks, model, tokenizer, device)
    embed_time = time.time() - start_time
    print(f"Embeddings generated in {embed_time:.1f}s")
    
    # Demo queries
    queries = [
        "What did the frog king promise the princess in exchange for her golden ball?",
        "What happened to Hansel and Gretel in the forest?",
        "What did Little Red Riding Hood's mother tell her to do?"
    ]
    
    print("\n" + "=" * 60)
    print("SEARCH DEMO")
    print("=" * 60)
    
    for query in queries:
        start_time = time.time()
        results = search_chunks(query, chunks, chunk_embeddings, model, tokenizer, device)
        search_time = time.time() - start_time
        
        display_results(query, results)
        print(f"Search completed in {search_time*1000:.0f}ms")
        
        # Generate complete RAG answer using qwen3
        generate_answer(query, results)
        print("-" * 50)
    
    print("Demo completed!")


if __name__ == "__main__":
    main()
