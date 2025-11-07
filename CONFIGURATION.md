
# Configuration Guide

This document describes how to configure the workshop applications to use remote or custom AI backend servers.

## Overview

All workshop stages support configurable backend URLs via:
1. **Environment Variables** (recommended for deployment)
2. **System Properties** (useful for testing)
3. **Fallback Defaults** (localhost for development)

**Configuration Precedence**: System Property > Environment Variable > Default Value

## Backend URL Configuration

### Ollama Backend

**Default**: `http://localhost:11434`

Configure via:
- **Environment Variable**: `OLLAMA_BASE_URL`
- **System Property**: `ollama.base.url`

Examples:
```bash
# Environment variable (recommended)
export OLLAMA_BASE_URL=http://192.168.1.100:11434
./run.sh "What's the weather in Tokyo?"

# System property (one-time override)
java -Dollama.base.url=http://192.168.1.100:11434 -jar stage-2-simple-agent.jar

# Docker environment
docker run -e OLLAMA_BASE_URL=http://host.docker.internal:11434 ...
```

### LM Studio Backend

**Default**: `http://localhost:1234/v1`

Configure via:
- **Environment Variable**: `LMSTUDIO_BASE_URL`
- **System Property**: `lmstudio.base.url`

Examples:
```bash
# Environment variable
export LMSTUDIO_BASE_URL=http://192.168.1.100:1234/v1

# System property
java -Dlmstudio.base.url=http://192.168.1.100:1234/v1 -jar stage-0-demo.jar -b lmstudio
```

### MLX-VLM Backend (Apple Silicon)

**Default**: `http://localhost:8000`

Configure via:
- **Environment Variable**: `MLX_VLM_BASE_URL`
- **System Property**: `mlx.vlm.base.url`

Examples:
```bash
# Environment variable
export MLX_VLM_BASE_URL=http://192.168.1.100:8000

# System property
java -Dmlx.vlm.base.url=http://192.168.1.100:8000 -jar stage-0-demo.jar -b mlx_vlm
```

## Stage-Specific Configuration

### Stage 0: Foundation Demo

Uses the shared backend configuration automatically:

```bash
# Remote Ollama server
cd stage-0-demo
export OLLAMA_BASE_URL=http://ai-server.local:11434
./run.sh "Hello from remote server!"

# Or with system property
./run.sh -Dollama.base.url=http://ai-server.local:11434 "Hello!"
```

### Stage 1: Function Calling

Configure via environment variable:

```bash
cd stage-1-function-calling
export OLLAMA_BASE_URL=http://ai-server.local:11434
./run.sh
```

### Stage 2: Simple Agent

Configure via environment variable or system property:

```bash
cd stage-2-simple-agent

# Environment variable
export OLLAMA_BASE_URL=http://ai-server.local:11434
./run.sh "What's the weather in Tokyo?"

# Or override model AND URL
./run.sh --model qwen3:4b -Dollama.base.url=http://ai-server.local:11434 "Task..."
```

### Stage 3: MCP Server

Configure via environment variable:

```bash
cd stage-3-mcp-server

# Agent mode
export OLLAMA_BASE_URL=http://ai-server.local:11434
./run.sh agent "What's the weather in Paris?"

# Server mode (for MCP Inspector)
export OLLAMA_BASE_URL=http://ai-server.local:11434
./run.sh server
```

### Stage 4: RAG Agent

Configure via environment variable (used by both demo and ingestion):

```bash
cd stage-4-agentic-rag

# Set environment variable
export OLLAMA_BASE_URL=http://ai-server.local:11434

# Ingest documents (uses embedding model)
./ingest.sh

# Run RAG agent
./run.sh "What does the documentation say about..."
```

The `repos.yaml` configuration file can also override the URL in the settings section:

```yaml
settings:
  chunkSize: 800
  chunkOverlap: 200
  similarityThreshold: 0.7
  embeddingModel: nomic-embed-text
  ollamaBaseUrl: http://ai-server.local:11434  # Override here
```

## Testing Configuration

All integration tests respect the configuration:

```bash
# Run tests against remote server
export OLLAMA_BASE_URL=http://ai-server.local:11434
mvn test

# Or specific stage
cd stage-2-simple-agent
export OLLAMA_BASE_URL=http://ai-server.local:11434
mvn test
```

## Docker Compose Configuration

For Stage 4 (RAG), configure Ollama URL in Docker environment:

```bash
# If Ollama is on host machine
export OLLAMA_BASE_URL=http://host.docker.internal:11434

# If Ollama is in another container
export OLLAMA_BASE_URL=http://ollama-container:11434

# Then start services
cd stage-4-agentic-rag
docker-compose up -d
```

## Common Scenarios

### Scenario 1: Remote Ollama Server on Local Network

```bash
# Set once in shell profile
echo 'export OLLAMA_BASE_URL=http://192.168.1.100:11434' >> ~/.bashrc
source ~/.bashrc

# Now all stages use remote server
cd stage-2-simple-agent
./run.sh "What's the weather in Munich?"
```

### Scenario 2: Cloud-Hosted Ollama

```bash
# Set environment variable
export OLLAMA_BASE_URL=https://ollama.mycompany.com

# Run any stage
cd stage-3-mcp-server
./run.sh agent "What's the weather in Paris?"
```

### Scenario 3: Different Backend Per Stage

```bash
# Stage 1 with local Ollama
cd stage-1-function-calling
./run.sh

# Stage 2 with remote Ollama
cd stage-2-simple-agent
OLLAMA_BASE_URL=http://ai-server:11434 ./run.sh "Task..."
```

### Scenario 4: Testing Multiple Models

```bash
# Export once
export OLLAMA_BASE_URL=http://ai-server.local:11434

# Test different models
cd stage-2-simple-agent
./run.sh --model qwen3:4b "Task 1"
./run.sh --model mistral:7b "Task 2"
./run.sh --model gemma2:9b "Task 3"
```

## Troubleshooting

### Connection Refused

```
Error: Connection refused to http://localhost:11434
```

**Solution**: Set `OLLAMA_BASE_URL` to point to your running Ollama server:
```bash
export OLLAMA_BASE_URL=http://your-server:11434
```

### Model Not Found

```
Error: Model 'incept5/Jan-v1-2509:fp16' not found
```

**Solution**: Pull the model on the remote server:
```bash
# On the Ollama server
ollama pull incept5/Jan-v1-2509:fp16
```

### Timeout Issues with Remote Server

**Solution**: Increase timeout via system property:
```bash
java -Dollama.base.url=http://slow-server:11434 \
     -Drequest.timeout=600 \
     -jar stage-2-simple-agent.jar
```

### Docker Networking Issues

**Solution**: Use `host.docker.internal` (Mac/Windows) or `172.17.0.1` (Linux):
```bash
export OLLAMA_BASE_URL=http://host.docker.internal:11434
```

## Environment Variable Summary

| Variable | Purpose | Default | Example |
|----------|---------|---------|---------|
| `OLLAMA_BASE_URL` | Ollama server URL | `http://localhost:11434` | `http://192.168.1.100:11434` |
| `LMSTUDIO_BASE_URL` | LM Studio server URL | `http://localhost:1234/v1` | `http://192.168.1.100:1234/v1` |
| `MLX_VLM_BASE_URL` | MLX-VLM server URL | `http://localhost:8000` | `http://192.168.1.100:8000` |

## System Property Summary

| Property | Purpose | Default | Example |
|----------|---------|---------|---------|
| `ollama.base.url` | Ollama server URL | `http://localhost:11434` | `-Dollama.base.url=http://ai.local:11434` |
| `lmstudio.base.url` | LM Studio server URL | `http://localhost:1234/v1` | `-Dlmstudio.base.url=http://ai.local:1234/v1` |
| `mlx.vlm.base.url` | MLX-VLM server URL | `http://localhost:8000` | `-Dmlx.vlm.base.url=http://ai.local:8000` |

## Best Practices

1. **Use Environment Variables** for deployment and persistent configuration
2. **Use System Properties** for temporary overrides during testing
3. **Document Custom URLs** in your deployment scripts
4. **Test Connectivity** before running full workflows:
   ```bash
   curl http://your-server:11434/api/tags
   ```
5. **Consider Security** when exposing Ollama over network (use firewall rules, VPN, or SSH tunnels)

## Next Steps

- Review [Architecture Documentation](./architecture.md) for system design
- See stage-specific README files for detailed usage instructions
- Check [TROUBLESHOOTING.md](./stage-3-mcp-server/TROUBLESHOOTING.md) for common issues
