
# 🎯 Stage 4 RAG - Quick Connection Guide

## For Workshop Participants

### Option A: Shared Database (Fastest!) 🚀

**No Docker or ingestion needed!** Just connect to the shared database:

```bash
# Set environment variables
export DB_URL="jdbc:postgresql://172.20.15.241:5432/workshop_rag"
export DB_USER="workshop"
export DB_PASSWORD="workshop123"

# Build and run
cd stage-4-agentic-rag
mvn clean package
./run.sh
```

**That's it!** You're ready to chat with 487 pre-loaded documents about Embabel.

---

### Option B: Local Setup (Fallback) 💻

If you can't reach the shared database:

```bash
cd stage-4-agentic-rag

# Terminal 1: Start embedding service
cd embedding-service
./start.sh

# Terminal 2: Run ingestion
cd ..
./ingest.sh

# Then run
./run.sh
```

---

## Quick Test

**Check if you can reach the shared database:**

```bash
# Test connection
nc -zv 172.20.15.241 5432

# If that works, test database access
psql "postgresql://workshop:workshop123@172.20.15.241:5432/workshop_rag" \
  -c "SELECT COUNT(*) FROM documents;"

# Should show: 487
```

**If connection fails:** Use Option B (local setup)

---

## Troubleshooting

### Can't reach 172.20.15.241:5432

**Solution:** Use local setup (Option B)

### "No documents found"

**Shared DB:** Tell workshop instructor  
**Local DB:** Run `./ingest.sh`

### Slow first query

**Normal!** Ollama loads model on first request (30-60s)  
Subsequent queries: 1-5 seconds

---

## Workshop Host IP

**Shared Database:** `172.20.15.241:5432`  
**Credentials:** `workshop` / `workshop123`  
**Database:** `workshop_rag`  
**Documents:** 487 pre-loaded chunks

---

## Questions to Try

```
💬 What is Embabel?
💬 Show me a simple @Agent example
💬 How do Actions differ from Goals?
💬 What's the Tripper application?
💬 How does Spring AI integration work?
```

---

**Full Documentation:** See [WORKSHOP_SETUP.md](./WORKSHOP_SETUP.md)

