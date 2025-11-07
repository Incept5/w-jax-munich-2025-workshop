# Stage 1 Integration Tests

This directory contains integration tests for the Simple Agent implementation.

## Overview

The `SimpleAgentIntegrationTest` class provides comprehensive end-to-end testing of the agent with:
- Real AI backend connectivity (Ollama)
- Real API tool calls (weather and country info)
- Multi-step reasoning verification
- Iteration limit enforcement

## Test Cases

### 1. Simple Single-Tool Task
Tests basic agent functionality with a straightforward weather query for Tokyo.
- Verifies single tool invocation
- Checks completion status
- Validates response quality

### 2. Multi-Step Reasoning Task
Tests the agent's ability to chain multiple tools together.
- Default task: "What's the weather like in the capital of France?"
- Expected flow: `country_info(France)` → `weather(Paris)`
- Verifies tool chaining and context maintenance

### 3. Iteration Limit Handling
Tests agent behavior when iteration limits are reached.
- Uses very low iteration limit (1)
- Verifies graceful degradation
- Checks incomplete task handling

### 4. Tool Registry Validation
Tests the tool registry setup.
- Verifies both tools are registered
- Checks tool availability

## Requirements

To run these integration tests, you need:

1. **Ollama running** on `localhost:11434`
2. **Model available**: `qwen2.5:3b` (or configured model)
3. **Network access** to:
   - `wttr.in` (weather API)
   - `restcountries.com` (country info API)

## Running the Tests

### Run All Tests (Default: Skipped)

By default, integration tests are skipped:

```bash
mvn test
```

Output:
```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 4
```

### Enable Integration Tests

Set the environment variable to enable tests:

```bash
# Unix/Linux/macOS
export SKIP_INTEGRATION_TESTS=false
mvn test

# Or in one command
SKIP_INTEGRATION_TESTS=false mvn test

# Windows (PowerShell)
$env:SKIP_INTEGRATION_TESTS="false"
mvn test

# Windows (CMD)
set SKIP_INTEGRATION_TESTS=false
mvn test
```

### Run Only Integration Tests

```bash
SKIP_INTEGRATION_TESTS=false mvn test -Dtest=SimpleAgentIntegrationTest
```

### Run Specific Test Case

```bash
SKIP_INTEGRATION_TESTS=false mvn test -Dtest=SimpleAgentIntegrationTest#testSimpleSingleToolTask
```

## Test Output

When enabled, tests provide verbose output showing:

```
======================================================================
Setting up integration test environment...
======================================================================
✓ Registered 2 tools: weather, country_info
✓ Connected to OLLAMA (model: qwen2.5:3b)
✓ Agent initialized (max 10 iterations)

TEST CASE 1: Simple Single-Tool Task
----------------------------------------------------------------------
Task: Get weather for Tokyo (single weather tool call)

============================================================
Iteration 1
============================================================

[THINKING]
LLM Response:
------------------------------------------------------------
<tool_use>
<tool_name>weather</tool_name>
<city>Tokyo</city>
</tool_use>
------------------------------------------------------------

[ACTING]
Tool call: ToolCall[toolName=weather, parameters={city=Tokyo}]

[OBSERVING]
Tool result:
------------------------------------------------------------
Weather in Tokyo: ⛅️ Partly cloudy, 15°C
------------------------------------------------------------

... (additional iterations if needed) ...

======================================================================
TEST RESULTS:
======================================================================
Completed: true
Iterations: 2
Duration: 3421ms
Response length: 156 chars

Response preview:
The weather in Tokyo is currently partly cloudy with a temperature of 15°C...
======================================================================

✓ All assertions passed for single-tool task
```

## Customizing Test Behavior

### Change Model or Backend

Edit the `setUp()` method in `SimpleAgentIntegrationTest.java`:

```java
BackendConfig config = BackendConfig.builder()
    .backendType(BackendType.OLLAMA)
    .baseUrl("http://localhost:11434")
    .model("llama3.2:3b")  // Change model here
    .requestTimeout(Duration.ofSeconds(60))
    .build();
```

### Adjust Timeout

Increase timeout for slower models:

```java
.requestTimeout(Duration.ofSeconds(120))
```

### Modify Iteration Limits

Change max iterations for tests:

```java
agent = new SimpleAgent(backend, toolRegistry, 15); // Allow more iterations
```

## Troubleshooting

### Tests Skipped

**Symptom**: `Tests run: 4, Skipped: 4`

**Solution**: Enable tests with `SKIP_INTEGRATION_TESTS=false`

### Connection Failed

**Symptom**: 
```
✗ Failed to initialize test environment: Connection refused
```

**Solutions**:
1. Start Ollama: `ollama serve`
2. Verify it's running: `curl http://localhost:11434/api/tags`
3. Check model is available: `ollama list`

### Model Not Found

**Symptom**:
```
Model 'qwen2.5:3b' not found
```

**Solution**: Pull the model:
```bash
ollama pull qwen2.5:3b
```

### Network Errors

**Symptom**: Tool calls fail with network errors

**Solutions**:
1. Check internet connectivity
2. Verify API access:
   ```bash
   curl "https://wttr.in/Tokyo?format=3"
   curl "https://restcountries.com/v3.1/name/france"
   ```
3. Check firewall/proxy settings

### Slow Tests

**Symptom**: Tests take very long to complete

**Solutions**:
1. Use a faster model (e.g., `qwen2.5:1.5b`)
2. Reduce timeout in test setup
3. Run tests with fewer iterations

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      ollama:
        image: ollama/ollama:latest
        ports:
          - 11434:11434
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Pull Ollama model
        run: |
          docker exec ollama ollama pull qwen2.5:3b
      
      - name: Run integration tests
        run: |
          cd stage-1-simple-agent
          SKIP_INTEGRATION_TESTS=false mvn test
```

### Maven Profile (Alternative)

Add to `pom.xml`:

```xml
<profiles>
    <profile>
        <id>integration-tests</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <systemPropertyVariables>
                            <SKIP_INTEGRATION_TESTS>false</SKIP_INTEGRATION_TESTS>
                        </systemPropertyVariables>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Run with:
```bash
mvn test -Pintegration-tests
```

## Best Practices

1. **Run locally before committing** - Verify your changes work
2. **Check model availability** - Ensure test model is pulled
3. **Monitor test duration** - Optimize if tests become too slow
4. **Review verbose output** - Helps debug agent reasoning
5. **Keep tests independent** - Each test should work standalone

## Expected Test Duration

Typical execution times with `qwen2.5:3b`:

| Test Case | Expected Duration |
|-----------|------------------|
| Simple Single-Tool | 2-5 seconds |
| Multi-Step Reasoning | 5-10 seconds |
| Iteration Limit | 2-4 seconds |
| Tool Registry | < 1 second |

**Total**: ~15-30 seconds for all tests

## Related Documentation

- [Stage 1 README](../../README.md) - Main documentation
- [Stage 1 Implementation Notes](../../IMPLEMENTATION_NOTES.md) - Design decisions
- [Root Architecture](../../../../architecture.md) - Overall system design

---

*Last updated: 2025-01-06*
