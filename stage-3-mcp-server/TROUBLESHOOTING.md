# Stage 3 MCP Server - Troubleshooting Guide

## Common Issues

### 1. "Unable to access jarfile target/stage-3-mcp-server.jar"

**Problem**: The JAR file hasn't been built yet.

**Solution**:
```bash
cd stage-3-mcp-server
mvn clean package
```

This will:
1. Compile the Java source files
2. Create the `target/` directory
3. Build the executable JAR: `target/stage-3-mcp-server.jar`

After building, you should see:
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

Then you can run the MCP Inspector:
```bash
npx @modelcontextprotocol/inspector java -jar target/stage-3-mcp-server.jar server
```

### 2. Build from Root Directory

If you're in the root workshop directory, you can build just this module:

```bash
# Build only stage-3-mcp-server and its dependencies
mvn -pl stage-3-mcp-server -am clean package

# Or build everything
mvn clean package
```

### 3. Verify the Build

After building, verify the JAR exists:

```bash
ls -lh stage-3-mcp-server/target/stage-3-mcp-server.jar
```

You should see output like:
```
-rw-r--r--  1 user  staff   3.2M Nov  7 10:00 stage-3-mcp-server.jar
```

### 4. Quick Test

Test the server directly before using the inspector:

```bash
cd stage-3-mcp-server
java -jar target/stage-3-mcp-server.jar server
```

You should see log output indicating the server is running. Press Ctrl+C to stop.

### 5. Run Script

The `run.sh` script handles the build check automatically:

```bash
cd stage-3-mcp-server
./run.sh server  # Checks if JAR exists and provides helpful error message
```

## MCP Inspector Issues

### Inspector Won't Connect

**Problem**: Inspector shows connection errors.

**Check**:
1. Is the server JAR path correct?
2. Is Java in your PATH? Test with: `java -version`
3. Are you running Java 21+?

**Solution**:
```bash
# Use absolute path if relative path fails
npx @modelcontextprotocol/inspector java -jar $(pwd)/target/stage-3-mcp-server.jar server
```

### JSON Parse Errors

**Problem**: "Unexpected non-whitespace character after JSON"

**Cause**: Something is writing to stdout instead of stderr.

**Solution**: Check `logback.xml` - all logging must target `System.err`:
```xml
<appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
    <target>System.err</target>
</appender>
```

### Missing ID Field Error

**Problem**: "ZodError: Invalid input" with missing 'id' field in JSON-RPC responses

**Cause**: JSON-RPC 2.0 requires the `id` field in all responses, even when null.

**Solution**: Updated in v1.0.1 - responses now always include the `id` field to match the request.

## Tool Execution Issues

### Weather Tool Fails

**Problem**: Weather API returns errors or times out.

**Check**:
1. Internet connection is working
2. wttr.in is accessible: `curl wttr.in/London?format=j1`

### Country Info Tool Fails

**Problem**: Country API returns errors.

**Check**:
1. Internet connection is working
2. REST Countries API is accessible: `curl https://restcountries.com/v3.1/name/france`

## Build Issues

### Maven Command Not Found

**Problem**: `mvn: command not found`

**Solution**: Install Maven 3.9.0+:
- macOS: `brew install maven`
- Linux: `apt-get install maven` or `yum install maven`
- Windows: Download from [maven.apache.org](https://maven.apache.org/download.cgi)

### Java Version Mismatch

**Problem**: "Unsupported class file major version" or similar errors

**Solution**: Ensure Java 21+ is installed and active:
```bash
java -version  # Should show Java 21 or higher
```

Install Java 21:
- macOS: `brew install openjdk@21`
- Linux: `apt-get install openjdk-21-jdk`
- Windows: Download from [adoptium.net](https://adoptium.net/)

### Dependency Resolution Failures

**Problem**: Maven can't download dependencies

**Check**:
1. Internet connection
2. Maven central is accessible
3. Corporate proxy settings (if applicable)

**Solution**: Try clearing Maven's local repository:
```bash
rm -rf ~/.m2/repository
mvn clean package
```

## Quick Checklist

Before running the MCP Inspector, verify:

- [ ] Java 21+ is installed: `java -version`
- [ ] Maven is installed: `mvn -version`
- [ ] Project is built: `ls stage-3-mcp-server/target/stage-3-mcp-server.jar`
- [ ] Server runs standalone: `java -jar stage-3-mcp-server/target/stage-3-mcp-server.jar server`
- [ ] Node.js is installed (for inspector): `node -version`

If all checks pass, the inspector should work:
```bash
npx @modelcontextprotocol/inspector java -jar stage-3-mcp-server/target/stage-3-mcp-server.jar server
```

## Getting Help

If problems persist:

1. Check the detailed error messages in terminal
2. Look at server logs (stderr output)
3. Verify all prerequisites from README.md
4. Review the MCP Inspector documentation
5. Test with simpler invocations first (e.g., `./run.sh agent "test"`)

## Useful Commands

```bash
# Clean everything and rebuild
mvn clean package

# Build with verbose output
mvn -X clean package

# Skip tests if they're failing
mvn clean package -DskipTests

# Run server directly
java -jar target/stage-3-mcp-server.jar server

# Run agent with verbose output
java -jar target/stage-3-mcp-server.jar agent "test query" --verbose

# Check what's in the JAR
jar tf target/stage-3-mcp-server.jar | head -20
```
