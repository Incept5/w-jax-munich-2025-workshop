
# Stage 1: Function Calling Demo

## Overview

This stage demonstrates LLM function calling capabilities using two simple functions that don't require external API calls. It's designed to help participants understand how function calling works before moving on to more complex agent patterns in later stages.

## Learning Objectives

- Understand the function calling protocol with Ollama
- Learn how to define function schemas
- See how LLMs decide when to call functions
- Test multiple models for function calling support
- Interpret function calling results

## What You'll Build

A testing framework that:
- Registers two sample functions (day of week, weather)
- Tests multiple Ollama models for function calling capability
- Runs 10 iterations per model with 3 different prompts
- Generates CSV results and statistics

## Sample Functions

### 1. DayOfWeekTool
- **Name**: `get_current_day`
- **Parameters**: None
- **Returns**: Current day of the week (e.g., "Monday")
- **Purpose**: Demonstrates parameter-free function calling

### 2. WeatherTool
- **Name**: `get_weather`
- **Parameters**: `city` (string, required)
- **Returns**: Mock weather data ("Pas Mal" for Paris, "Merd!" for others)
- **Purpose**: Demonstrates function calling with parameters

## Prerequisites

- Ollama running on `http://localhost:11434`
- At least one function-capable model installed (e.g., `incept5/Jan-v1-2509:fp16`)
- Java 21+
- Maven 3.9.0+

## Building

```bash
# From the project root
mvn clean package

# Or build just this stage
mvn -pl stage-1-function-calling clean package
```

## Running

### Test All Models (Under 100GB)
```bash
cd stage-1-function-calling
java -jar target/stage-1-function-calling.jar
```

### Filter by Model Name
```bash
# Models containing "jan" (case-insensitive)
java -jar target/stage-1-function-calling.jar jan

# Models starting with "qwen"
java -jar target/stage-1-function-calling.jar "qwen*"

# Models with "jan" OR "llama"
java -jar target/stage-1-function-calling.jar "jan|llama"

# Models starting with "qwen" OR "phi"
java -jar target/stage-1-function-calling.jar "qwen*|phi*"
```

### Filter by Size
```bash
# All models under 10GB
java -jar target/stage-1-function-calling.jar "" 10

# Models with "jan" under 50GB
java -jar target/stage-1-function-calling.jar jan 50
```

## Example Output

```
================================================================================
MODELS TO BE TESTED
================================================================================
Name filter: "jan"
--------------------------------------------------------------------------------
Model Name                               Size (GB)    Quantization    Modified
--------------------------------------------------------------------------------
incept5/Jan-v1-2509:fp16                 8.05         fp16            2024-11-01
Jan-v1-2509-gguf:Q8_0_32k                4.28         Q8_0            2024-10-28
--------------------------------------------------------------------------------
Total: 2 models
================================================================================


============================================================
Testing: incept5/Jan-v1-2509:fp16
============================================================
  Performing warmup run... PASS: get_current_day() --> Wednesday
Prompt: What is the day of the week today?
  Run 1/10... PASS: get_current_day() --> Wednesday
  Run 2/10... PASS: get_current_day() --> Wednesday
  ...

Prompt: What is the weather in the capital of France?
  Run 1/10... PASS: get_weather(city=Paris) --> Pas Mal
  Run 2/10... PASS: get_weather(city=Paris) --> Pas Mal
  ...

============================================================
FUNCTION CALLING TEST RESULTS SUMMARY
============================================================
Model                          Size (GB)    Time (ms)    σ Time       Success Rate
------------------------------------------------------------------------------------------
incept5/Jan-v1-2509:fp16       8.05         2653         ±711         30/30
Jan-v1-2509-gguf:Q8_0_32k      4.28         1697         ±370         30/30
------------------------------------------------------------------------------------------

DETAILED STATISTICS
============================================================

Total models tested: 2
Perfect score models: 2
Other models: 0

Models with perfect scores:
  ✓ incept5/Jan-v1-2509:fp16
  ✓ Jan-v1-2509-gguf:Q8_0_32k

Results have been saved to function_calling_results.csv
```

## Test Cases

The demo tests three scenarios per model:

1. **"What is the day of the week today?"**
   - Tests: Parameter-free function calling
   - Expected: Calls `get_current_day()` and returns current day

2. **"What is the weather in the capital of France?"**
   - Tests: Parameter extraction and knowledge integration
   - Expected: Calls `get_weather(city=Paris)` and returns "Pas Mal"

3. **"Is it going to be sunny in Paris tomorrow?"**
   - Tests: Understanding different question formats
   - Expected: Calls `get_weather(city=Paris)` and returns "Pas Mal"

## Configuration

Edit `FunctionCallingDemo.java` to customize:

```java
private static final int NUM_RUNS = 10;           // Iterations per test case
private static final double TEMPERATURE = 0.6;     // Generation temperature
private static final double DEFAULT_MAX_SIZE_GB = 100.0;  // Default size filter
```

## Output Files

- **function_calling_results.csv**: Detailed results for each model
  - Columns: Model, Size_GB, Time_ms, Time_StdDev, Success_Rate, Total_Possible

## Key Files

```
stage-1-function-calling/
├── pom.xml                                 # Maven configuration
├── README.md                               # This file
└── src/main/java/com/incept5/workshop/stage1/
    ├── FunctionCallingDemo.java            # Main entry point
    ├── FunctionCallingClient.java          # Ollama function calling client
    └── tool/
        ├── FunctionTool.java               # Tool interface
        ├── DayOfWeekTool.java              # Day of week function
        └── WeatherTool.java                # Weather function
```

## Understanding Function Calling

### Function Schema Format

Functions are defined using JSON Schema:

```json
{
  "type": "function",
  "function": {
    "name": "get_weather",
    "description": "Find the weather for a specific location",
    "parameters": {
      "type": "object",
      "properties": {
        "city": {
          "type": "string",
          "description": "The city or town name"
        }
      },
      "required": ["city"]
    }
  }
}
```

### LLM Decision Process

1. **User Prompt**: "What is the weather in Paris?"
2. **LLM Analysis**: Determines that weather information is needed
3. **Function Selection**: Chooses `get_weather` function
4. **Parameter Extraction**: Extracts `city="Paris"` from context
5. **Function Call**: Returns tool call in response
6. **Execution**: Client executes the function
7. **Result**: Returns "Pas Mal" to the LLM

## Differences from Real Agent (Stage 2)

This demo focuses on **testing function calling capabilities**:
- No agent loop (single round)
- No decision-making process
- No tool chaining
- Mock data only

Stage 2 will introduce:
- Multi-step reasoning loops
- Real external API calls
- Tool selection logic
- Error handling and retries

## Troubleshooting

### "No models found"
- Check that Ollama is running: `ollama serve`
- Verify models are installed: `ollama list`

### "Model does not support tools"
- Not all models support function calling
- Try models known to work: `incept5/Jan-v1-2509:fp16`, `qwen2.5:7b`

### All tests fail
- Check Ollama endpoint: `curl http://localhost:11434/api/tags`
- Verify model supports function calling
- Try increasing temperature (0.6 → 0.8)

## Next Steps

After completing this stage:
1. Understand function schemas and tool definitions
2. See which models support function calling
3. Learn how LLMs extract parameters from prompts
4. Move to **Stage 2** for real agent loops with external APIs

## Workshop Context

- **Duration**: ~40 minutes
- **Position**: Between Stage 0 (Foundation) and Stage 2 (Simple Agent)
- **Focus**: Function calling mechanics before complex agent patterns
- **Type**: Demonstration and experimentation
