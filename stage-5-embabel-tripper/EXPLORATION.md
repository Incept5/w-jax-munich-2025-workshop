
# Tripper Code Exploration Guide

This guide provides structured activities for exploring the Tripper codebase during the workshop. Rather than coding exercises, these activities focus on understanding production patterns through guided code reading and discussion.

**Time**: 65 minutes total  
**Approach**: Guided exploration → Discussion → Key takeaways

---

## Activity 1: Run and Observe (15 minutes)

**Goal**: Experience Tripper as a user and observe its behavior

### Part A: Create Your First Travel Plan (8 min)

1. **Start the application** (if not running):
   ```bash
   cd ~/projects/tripper  # or wherever you cloned it
   ./run.sh
   ```

2. **Open the UI**: http://localhost:8747/

3. **Create a travel plan** with these inputs:
   - **Destination**: "Munich, Germany"
   - **Duration**: "3 days"
   - **Interests**: "Technology, beer gardens, history"
   - **Budget**: "Medium"

4. **Watch carefully**:
   - Notice the SSE (Server-Sent Events) streaming updates
   - See how the UI updates in real-time
   - Observe which MCP tools are being called
   - Note the structure of the final itinerary

**Discussion Questions**:
- What happens first? (Hint: Look for "Planner Agent" messages)
- How does the UI update progressively?
- What tools are called and in what order?

### Part B: Explore Monitoring (7 min)

1. **Check Zipkin traces**:
   - Open http://localhost:9411/
   - Click "Run Query" to see recent traces
   - Find your travel plan request
   - Click to see the detailed trace
   - Observe the service calls and timing

2. **Check Actuator endpoints**:
   - Health: http://localhost:8747/actuator/health
   - Metrics: http://localhost:8747/actuator/metrics
   - Info: http://localhost:8747/actuator/info

3. **Check Platform info**:
   - Open http://localhost:8747/platform
   - See LLM configurations
   - View available MCP tools
   - Check system information

**Discussion Questions**:
- How long did your request take?
- Which service calls took the most time?
- What health checks are configured?
- What metrics are being tracked?

---

## Activity 2: Follow the Request Flow (20 minutes)

**Goal**: Trace a travel plan request from UI to response

### Part A: Entry Point - Controller (5 min)

**File**: `src/main/kotlin/com/embabel/tripper/web/JourneyHtmxController.kt`

1. **Open the file** in your editor

2. **Find the main endpoint**:
   ```kotlin
   @PostMapping("/journey")
   fun journey(...)
   ```

3. **Observe**:
   - How does it receive the form data?
   - What does it return? (Hint: SSE response)
   - How is the agent invoked?

**Key Code to Understand**:
```kotlin
// The controller starts the agent
val result = agent.createItinerary(
    travelBrief = ...,
    eventSink = eventSink
)
```

**Discussion Questions**:
- Why use SSE instead of regular HTTP response?
- How does Spring handle the form submission?
- What's the role of `eventSink`?

### Part B: Agent Logic - TripperAgent (10 min)

**File**: `src/main/kotlin/com/embabel/tripper/agent/TripperAgent.kt`

1. **Find the main method**:
   ```kotlin
   fun createItinerary(...): ResearchedItinerary
   ```

2. **Follow the workflow** - Find these actions:
   ```kotlin
   @Action
   fun confirmExpensiveOperation(...)
   
   @Action
   fun findPointsOfInterest(...)
   
   @Action
   fun researchPointsOfInterest(...)
   ```

3. **Observe the pattern**:
   - Each action is a discrete step
   - Actions read from "blackboard" (context)
   - Actions return typed domain objects
   - Embabel orchestrates the flow

**Key Concepts**:

**Action-Based Design**:
```kotlin
@Action  // Embabel annotation
fun findPointsOfInterest(
    travelBrief: JourneyTravelBrief,  // Input from previous step
    travelers: Travelers,              // Input from previous step
    context: OperationContext          // Shared state
): ItineraryIdeas {                    // Typed output
    return context.ai()
        .withLlm(config.thinkerLlm)    // Use GPT-4.1
        .withPromptElements(...)       // Add persona, context
        .generate(ItineraryIdeas::class) // Generate typed result
}
```

**Blackboard Pattern**:
```kotlin
// Store intermediate results
context.put(ItineraryIdeas::class, ideas)

// Retrieve later
val ideas = context.get(ItineraryIdeas::class)
```

**Discussion Questions**:
- How is this different from Stage 1's loop-based approach?
- Why return typed domain objects?
- What's the benefit of the blackboard pattern?
- How does Embabel know what order to run actions?

### Part C: Domain Models (5 min)

**File**: `src/main/kotlin/com/embabel/tripper/agent/domain.kt`

1. **Browse the domain models**:
   ```kotlin
   data class JourneyTravelBrief(...)
   data class Travelers(...)
   data class ItineraryIdeas(...)
   data class ResearchedItinerary(...)
   ```

2. **Notice**:
   - Type-safe data structures
   - Clear intent and naming
   - Embabel can serialize/deserialize
   - LLM generates these directly

**Discussion Questions**:
- How do these compare to String-based parsing in Stage 1?
- What's the benefit of type safety here?
- How does the LLM know what structure to generate?

---

## Activity 3: Configuration and Personas (15 minutes)

**Goal**: Understand how LLMs and agents are configured

### Part A: Application Configuration (8 min)

**File**: `src/main/resources/application.yml`

1. **Find the tripper configuration**:
   ```yaml
   embabel:
     tripper:
       word-count: 1200
       image-width: 850
       max-concurrency: 12
       
       thinker-llm:
         model: gpt-4.1
       
       planner:
         persona: ...
         llm:
           model: gpt-4.1
       
       researcher:
         persona: ...
         llm:
           model: gpt-4.1-mini
   ```

2. **Notice the two-LLM strategy**:
   - **Planner**: GPT-4.1 (expensive, high quality)
   - **Researcher**: GPT-4.1-mini (cheaper, sufficient quality)

3. **Examine persona structure**:
   ```yaml
   planner:
     persona:
       name: Hermes
       persona: You are an expert travel planner
       voice: friendly and concise
       objective: Help the user plan an amazing trip
     llm:
       model: gpt-4.1
   ```

**Discussion Questions**:
- Why use two different models?
- What's the cost/quality tradeoff?
- How do personas affect agent behavior?
- Could you swap in different models?

### Part B: Spring Boot Integration (7 min)

**File**: `src/main/kotlin/com/embabel/tripper/TripperApplication.kt`

1. **Observe the minimal setup**:
   ```kotlin
   @SpringBootApplication
   class TripperApplication
   
   fun main(args: Array<String>) {
       runApplication<TripperApplication>(*args)
   }
   ```

2. **Why so simple?**:
   - Embabel auto-configuration via starters
   - Spring Boot convention over configuration
   - Component scanning finds agents automatically

**File**: `src/main/kotlin/com/embabel/tripper/config/ToolsConfig.kt`

3. **See how tools are configured**:
   ```kotlin
   @Configuration
   class ToolsConfig {
       @Bean
       fun mcpTools(): McpServerClient {
           // Connect to MCP Gateway
       }
   }
   ```

**Discussion Questions**:
- How does Spring find the TripperAgent?
- Where does Embabel configuration come from?
- How are MCP tools injected?

---

## Activity 4: Security and Infrastructure (15 minutes)

**Goal**: Understand production patterns for security and deployment

### Part A: Security Configuration (8 min)

**File**: `src/main/kotlin/com/embabel/agent/web/security/SecurityConfig.kt`

1. **Find the security configuration**:
   ```kotlin
   @Configuration
   @EnableWebSecurity
   class SecurityConfig {
       @Bean
       fun securityFilterChain(...): SecurityFilterChain {
           http
               .oauth2Login { }
               .authorizeHttpRequests {
                   it.requestMatchers("/actuator/**").permitAll()
                   it.anyRequest().authenticated()
               }
       }
   }
   ```

2. **Notice**:
   - OAuth2 with Google
   - Public endpoints (actuator, static resources)
   - Protected endpoints (everything else)
   - Toggleable via `embabel.security.enabled`

3. **Check the .env pattern**:
   ```bash
   # .env file
   OPENAI_API_KEY=sk-proj-...
   BRAVE_API_KEY=BSA...
   GOOGLE_MAPS_API_KEY=AIza...
   ```

**Discussion Questions**:
- Why use OAuth2 instead of basic auth?
- How does the security toggle work?
- Why keep secrets in .env?
- What's missing for production security?

### Part B: Infrastructure as Code (7 min)

**File**: `compose.yaml`

1. **Examine the services**:
   ```yaml
   services:
     mcp-gateway:
       image: docker/mcp-gateway:latest
       ports:
         - 9011:9011
       environment:
         - BRAVE_API_KEY=${BRAVE_API_KEY}
       command:
         - --servers=brave,wikipedia-mcp,google-maps,...
     
     zipkin:
       image: 'openzipkin/zipkin:latest'
       ports:
         - "9411:9411"
   ```

2. **Notice the pattern**:
   - MCP Gateway manages all MCP servers
   - Zipkin for distributed tracing
   - Environment variables from .env
   - Service orchestration

**Discussion Questions**:
- Why use a gateway for MCP servers?
- What does Zipkin give you?
- How would you add another service?
- What's needed for Kubernetes deployment?

---

## Key Comparisons: Workshop Stages vs. Tripper

### Stage 1: Simple Agent → Tripper

| Aspect | Stage 1 | Tripper |
|--------|---------|---------|
| **Design** | Loop-based | Action-based |
| **Tool Calls** | String parsing | Typed domain models |
| **State** | String concatenation | Blackboard pattern |
| **Models** | Single (Ollama) | Multiple (GPT-4.1, GPT-4.1-mini) |
| **Tools** | 2 (weather, country) | 6+ MCP servers |
| **Testing** | Integration test | Production monitoring |

**Key Evolution**:
```
Stage 1: while(true) { ask LLM → parse → execute → repeat }
Tripper: Action1 → Action2 → Action3 (deterministic workflow)
```

### Stage 2: MCP Server → Tripper

| Aspect | Stage 2 | Tripper |
|--------|---------|---------|
| **MCP** | Single server | Gateway managing 6+ servers |
| **Tools** | 2 tools | Dozens of tools |
| **Transport** | STDIO | SSE (Server-Sent Events) |
| **Integration** | Direct subprocess | Docker containers |
| **Security** | None | Tool filtering |

**Key Evolution**:
```
Stage 2: Agent → MCP Server (direct)
Tripper: Agent → MCP Gateway → Multiple MCP Servers (centralized)
```

### Stage 3: RAG → Tripper (Potential)

| Aspect | Stage 3 | Tripper |
|--------|---------|---------|
| **Storage** | PostgreSQL + pgvector | Could add same |
| **Use Case** | Document retrieval | Travel knowledge base |
| **Integration** | RAG tool | Additional action |
| **Embeddings** | Ollama | Docker Model Runner |

**Potential Integration**:
```kotlin
@Action
fun retrieveDestinationKnowledge(
    destination: String,
    context: OperationContext
): DestinationKnowledge {
    // Query pgvector for stored travel info
    // Use before web search
}
```

---

## Discussion Topics

### Architecture Decisions

**Topic 1: Why Action-Based?**
- ✅ More deterministic (explicit workflow)
- ✅ Easier to test (isolated actions)
- ✅ Better observability (named steps)
- ✅ Type-safe (domain models)
- ❌ Less flexible than loops
- ❌ More upfront design needed

**Topic 2: Why Multiple LLMs?**
- ✅ Cost optimization (mini for research)
- ✅ Quality where needed (4.1 for planning)
- ✅ Different models for different tasks
- ❌ More complexity
- ❌ Consistency challenges

**Topic 3: Why MCP Gateway?**
- ✅ Centralized tool management
- ✅ Tool filtering for security
- ✅ Service isolation (Docker)
- ✅ Dynamic discovery
- ❌ Additional infrastructure
- ❌ Single point of failure

### Production Considerations

**Topic 1: Cost Management**
- Monitor OpenAI usage daily
- Cache results when possible
- Use cheaper models appropriately
- Set budget limits

**Topic 2: Monitoring**
- Zipkin for distributed tracing
- Spring Actuator for health/metrics
- Structured logging
- Cost tracking per request

**Topic 3: Deployment**
- Containerize application
- Use Kubernetes for orchestration
- Environment-based configuration
- Secret management (not .env)

**Topic 4: Security**
- OAuth2 for authentication
- API key rotation
- Rate limiting per user
- Input validation

### Extension Ideas

**Idea 1: Add RAG**
```kotlin
@Action
fun queryTravelKnowledge(
    destination: String,
    context: OperationContext
): TravelKnowledge {
    // Use pgvector from Stage 3
    // Store successful itineraries
    // Query before planning
}
```

**Idea 2: Add More Agents**
```kotlin
@Agent("budget-optimizer")
class BudgetAgent {
    @Action
    fun optimizeBudget(...): OptimizedItinerary
}

@Agent("activity-recommender")  
class ActivityAgent {
    @Action
    fun recommendActivities(...): Activities
}
```

**Idea 3: Add Caching**
```kotlin
@Cacheable("destinations")
fun getDestinationInfo(city: String): DestinationInfo {
    // Cache expensive API calls
}
```

---

## Key Takeaways

### 1. Framework Benefits
- **Embabel** provides structure and conventions
- Action-based design scales better than loops
- Type-safe domain models prevent runtime errors
- Built-in observability and monitoring

### 2. Production Patterns
- **Multiple LLMs**: Optimize cost vs. quality
- **MCP Gateway**: Centralize tool management
- **Spring Boot**: Mature ecosystem
- **Infrastructure as Code**: Docker Compose

### 3. Security First
- OAuth2 from the start
- Environment-based secrets
- Toggleable security for dev
- API key management

### 4. Monitoring Essential
- Distributed tracing (Zipkin)
- Health checks (Actuator)
- Cost tracking per request
- Usage metrics

### 5. Real-World Complexity
- Environment management matters
- Multiple services coordination
- Security considerations
- Cost optimization

---

## Next Steps After Workshop

### 1. Experiment with Tripper
- Modify personas in `application.yml`
- Try different LLM combinations
- Add custom MCP tools
- Extend the domain model

### 2. Build Your Own
- Apply patterns to your domain
- Design your domain models
- Create specialized agents
- Integrate with your systems

### 3. Learn More
- Study Embabel documentation
- Explore MCP servers
- Read Spring Boot guides
- Practice with different models

### 4. Contribute
- Report issues on GitHub
- Suggest improvements
- Share your use cases
- Help others learn

---

## Conclusion

You've now explored a production-ready multi-agent system with enterprise patterns. You've seen how the concepts from Stages 1-3 evolve into a real application:

✅ **Stage 1** → Action-based deterministic agents  
✅ **Stage 2** → MCP Gateway pattern at scale  
✅ **Stage 3** → Potential for RAG integration  
✅ **Production** → Security, monitoring, infrastructure

**Keep building!** 🚀

---

**Workshop**: W-JAX Munich 2025  
**Time**: 15:15-16:20 (65 minutes)  
**Focus**: Understanding production patterns through exploration
