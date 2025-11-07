
# Stage 4 RAG - Workshop Database Setup

This guide explains how to use the shared workshop database OR run locally.

---

## 🎯 Hybrid Approach: Choose Your Path

### Option A: Use Shared Database (Recommended) 👥

**Advantages:**
- ✅ No Docker setup needed
- ✅ No ingestion required (data already loaded)
- ✅ Faster workshop start
- ✅ Everyone uses same data

**Prerequisites:**
- Java 21+
- Maven 3.9+
- Ollama running with models (see main README)

**Connection Steps:**

1. **Set the database URL:**
```bash
export DB_URL="jdbc:postgresql://172.20.15.241:5432/workshop_rag"
export DB_USER="workshop"
export DB_PASSWORD="workshop123"
```

2. **Build and run:**
```bash
cd stage-4-agentic-rag
mvn clean package
./run.sh
```

3. **Start chatting:**
```
💬 You: What is Embabel?
🤖 Assistant: [Uses shared database with 487 pre-loaded documents]
```

**Test Connection:**
```bash
# Verify you can reach the database
psql "postgresql://workshop:workshop123@172.20.15.241:5432/workshop_rag" -c "SELECT COUNT(*) FROM documents;"
# Should show: 487
```

---

### Option B: Run Locally (Fallback) 💻

**Use this if:**
- Can't reach shared database (network issues)
- Want to experiment with ingestion
- Prefer complete local control

**Prerequisites:**
- All from Option A, PLUS:
- Docker and Docker Compose
- Python 3.9+ with conda

**Setup Steps:**

1. **Start embedding service** (Terminal 1):
```bash
cd stage-4-agentic-rag/embedding-service
./start.sh
# Wait for: "Uvicorn running on http://0.0.0.0:8001"
```

2. **Run ingestion** (Terminal 2):
```bash
cd stage-4-agentic-rag
./ingest.sh
# Takes 2-3 minutes, creates 487 documents
```

3. **Start chatting:**
```bash
./run.sh
```

**Note:** Local setup uses default `localhost:5432`. No environment variables needed.

---

## 🔍 Which Option Am I Using?

The application will tell you at startup:

**Shared Database:**
```
🐘 PostgreSQL Configuration
   URL: jdbc:postgresql://172.20.15.241:5432/workshop_rag
   User: workshop
   Mode: SHARED DATABASE (connected to workshop host)
```

**Local Database:**
```
🐘 PostgreSQL Configuration
   URL: jdbc:postgresql://localhost:5432/workshop_rag
   User: workshop
   Mode: LOCAL DATABASE
```

---

## 🚨 Troubleshooting

### "Connection refused" (Shared Database)

**Problem:** Can't reach workshop host database

**Solutions:**

1. **Check network connectivity:**
```bash
ping 172.20.15.241
# Should get responses
```

2. **Test database port:**
```bash
nc -zv 172.20.15.241 5432
# Should show: Connection succeeded
```

3. **Verify environment variables:**
```bash
echo $DB_URL
# Should show: jdbc:postgresql://172.20.15.241:5432/workshop_rag
```

4. **Fall back to local setup** (Option B above)

### "No documents found"

**Problem:** Database is empty

**Shared Database:** Contact workshop instructor (host needs to run ingestion)

**Local Database:**
```bash
./ingest.sh  # Run ingestion
```

### Slow first query (30-60s)

**Normal!** Ollama loads model into memory on first request.

**Subsequent queries:** 1-5 seconds

---

## 📊 Database Statistics

**After successful connection, verify data:**

```bash
# Count documents
psql "$DB_URL" -U workshop -c "SELECT COUNT(*) FROM documents;"
# Expected: 487

# Check repositories
psql "$DB_URL" -U workshop -c "SELECT DISTINCT repository FROM documents;"
# Expected: embabel-agent, embabel-examples, embabel-java-template, 
#           embabel-kotlin-template, tripper

# Sample document
psql "$DB_URL" -U workshop -c "SELECT repository, chunk_index, LEFT(content, 100) FROM documents LIMIT 1;"
```

---

## 🔐 Security Notes

**For Workshop Instructors:**

1. **Network:** Shared database uses simple credentials (`workshop/workshop123`)
   - ✅ Safe for workshop LANs
   - ⚠️ Do NOT expose to public internet

2. **Firewall:** Ensure port 5432 is accessible on host:
```bash
# macOS
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/local/bin/docker
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblock /usr/local/bin/docker

# Linux
sudo ufw allow 5432/tcp
```

3. **After Workshop:** Stop the shared database:
```bash
cd stage-4-agentic-rag
docker-compose down
```

---

## 🎓 Workshop Flow

**Typical Timeline:**

1. **00:00-00:05** - Instructor starts shared database
2. **00:05-00:10** - Participants test connection (Option A)
3. **00:10-00:15** - Fallback to local if needed (Option B)
4. **00:15-00:35** - Interactive RAG agent exploration

**Most participants should use Option A** (shared database) for fastest start!

---

## Environment Variables Reference

| Variable | Purpose | Default | Example |
|----------|---------|---------|---------|
| `DB_URL` | Database JDBC URL | `jdbc:postgresql://localhost:5432/workshop_rag` | `jdbc:postgresql://172.20.15.241:5432/workshop_rag` |
| `DB_USER` | Database username | `workshop` | `workshop` |
| `DB_PASSWORD` | Database password | `workshop123` | `workshop123` |
| `OLLAMA_MODEL` | LLM model override | `qwen3:4b` | `qwen2.5:7b` |

**Set all at once:**
```bash
export DB_URL="jdbc:postgresql://172.20.15.241:5432/workshop_rag"
export DB_USER="workshop"
export DB_PASSWORD="workshop123"
```

**Or use a one-liner:**
```bash
DB_URL="jdbc:postgresql://172.20.15.241:5432/workshop_rag" ./run.sh
```

---

## 📝 Testing Both Modes

**Quick Test Script:**

```bash
#!/bin/bash

echo "Testing Shared Database..."
export DB_URL="jdbc:postgresql://172.20.15.241:5432/workshop_rag"
./run.sh <<EOF
What is Embabel?
exit
EOF

echo -e "\nTesting Local Database..."
unset DB_URL
docker-compose up -d
./ingest.sh
./run.sh <<EOF
What is Embabel?
exit
EOF
```

---

**Last Updated:** 2025-11-07  
**Workshop Host IP:** 172.20.15.241  
**Database:** PostgreSQL 17 + pgvector

