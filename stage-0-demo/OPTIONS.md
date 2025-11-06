# Command-Line Options Reference

Complete list of all available options for the Ollama Java Demo application.

## All Options

| Option | Short | Type | Description | Default | Required |
|--------|-------|------|-------------|---------|----------|
| `--model` | `-m` | String | Model name to use | gemma3 | No |
| `--url` | `-u` | String | Ollama server URL | http://localhost:11434 | No |
| `--prompt` | `-p` | String | Prompt text to send to model | "What is the capital of France?" | No |
| `--timeout` | `-t` | Integer | Request timeout in seconds | 300 | No |
| `--stream` | `-s` | Flag | Enable streaming mode | false | No |
| `--raw` | `-r` | Flag | Raw output mode (response only) | false | No |
| `--system` | `--sys` | String | System prompt for the model | - | No |
| `--temperature` | `--temp` | Double | Temperature (0.0-2.0) | model default | No |
| `--context` | `--ctx` | Integer | Context size in tokens | model default | No |
| `--help` | `-h` | Flag | Show help message | - | No |

## Option Details

### Connection Options

**`--model` / `-m <name>`**
- Specifies which Ollama model to use
- Example: `-m llama2`, `-m gemma3`, `-m mistral`
- Model must be pulled first: `ollama pull <model>`

**`--url` / `-u <url>`**
- Ollama server URL if not running on localhost
- Default: `http://localhost:11434`
- Example: `-u http://remote-server:11434`

**`--timeout` / `-t <seconds>`**
- Maximum time to wait for a response
- Default: 300 seconds (5 minutes)
- Example: `-t 600` for 10 minutes

### Prompt Options

**`--prompt` / `-p <text>`**
- The text prompt to send to the model
- Default: "What is the capital of France?"
- Example: `-p "Explain quantum computing"`

**`--system` / `--sys <text>`**
- System prompt to set model behavior/persona
- No default (model uses its default behavior)
- Example: `--system "You are a helpful coding assistant"`
- Example: `--sys "Be concise and technical"`

### Model Behavior Options

**`--temperature` / `--temp <number>`**
- Controls randomness in output (0.0-2.0)
- Lower = more focused/deterministic
- Higher = more creative/varied
- No default (uses model's default)
- Example: `--temp 0.1` for precise answers
- Example: `--temp 0.8` for creative writing

**`--context` / `--ctx <tokens>`**
- Context window size in tokens
- Larger = can handle longer prompts and generate longer responses
- No default (uses model's default)
- Example: `--ctx 4096`
- Example: `--ctx 8192`

### Output Mode Options

**`--stream` / `-s`**
- Enable streaming mode for real-time token output
- Tokens appear as they're generated
- Lower perceived latency
- Better for long responses
- Example: `-s` or `--stream`

**`--raw` / `-r`**
- Output ONLY the model's response
- No headers, timing, or logging
- Perfect for scripting and piping
- Example: `-r` or `--raw`
- Example: `-r -p "What is 2+2?" | grep 4`

### Help Option

**`--help` / `-h`**
- Display help message with all options
- Shows examples and usage
- Example: `--help` or `-h`

## Common Combinations

### Development/Testing
```bash
# Verbose with streaming
java --enable-preview -jar ollama-java-demo.jar -s -p "Your prompt"

# Quick test with raw output
java --enable-preview -jar ollama-java-demo.jar -r -p "Test"
```

### Production/Scripting
```bash
# Raw mode for piping
java --enable-preview -jar ollama-java-demo.jar -r -p "Generate data" > output.txt

# Controlled generation with parameters
java --enable-preview -jar ollama-java-demo.jar \
  --temp 0.3 --ctx 4096 --system "Be precise" -p "Your prompt"
```

### Creative Tasks
```bash
# High temperature for creativity
java --enable-preview -jar ollama-java-demo.jar \
  --temp 0.8 -p "Write a story"

# With streaming for real-time feedback
java --enable-preview -jar ollama-java-demo.jar \
  -s --temp 0.9 -p "Write a poem"
```

### Technical/Precise Tasks
```bash
# Low temperature for accuracy
java --enable-preview -jar ollama-java-demo.jar \
  --temp 0.1 --system "You are a code assistant" \
  -p "How do I implement binary search?"

# Large context for complex explanations
java --enable-preview -jar ollama-java-demo.jar \
  --ctx 8192 --temp 0.2 -p "Explain microservices architecture"
```

## Notes

- All options are optional (have defaults or are flags)
- Options can be combined in any order
- Short and long forms are equivalent
- Model information is displayed by default (except in raw mode)
- Logging level is WARN by default for clean output
