09:00–09:15 | Welcome & Overview (15 min)
Objectives
Set the tone: practical, open, privacy-first, no vendor lock-in.
Explain what “agents” are and how this differs from “LLMs that chat.”
Highlight what will be achieved by the end of the day.
Talking points
Agents = LLMs + tools + memory + autonomy
Today: build → connect → collaborate
Local vs. cloud models (trade-offs, privacy, performance)
Quick agenda preview
09:15–09:50 | Environment Setup & Verification (35 min)
Objective
Get everyone’s environment working and test local inference.
Checklist
Verify installations:
Ollama or LM Studio
Java + Maven (for Embabel later)
Python + requirements (for basic agent scripts)
Test models: Qwen 2.5 7B, Mistral 7B, Gemma 2 9B
Optional: set OpenAI API key for cloud comparison
Clone workshop repo and run initial test script
Exercise
“Run a local inference” (ollama run qwen2.5 or LM Studio equivalent)
Confirm JSON schema tool-calling readiness
Troubleshooting mini-demo: latency, permissions, GPU/CPU mode
09:50–10:30 | How Agents Work (40 min)
Teaching segment
The agent loop: Observe → Think → Act → Reflect
From prompt chaining → autonomous reasoning
Architectures: ReAct, Reflexion, Planner–Executor
Memory and context management
When to use agents vs. scripts
How tool-calling fits into reasoning loops
Mini Exercise (10 min)
Draw or pseudo-code your own agent loop.
Identify where the “tool” call happens.
10:30–10:50 | Morning Break (20 min)
12:13
10:50–11:40 | Tool-Calling in Practice (50 min)
Lecture + Demo
What “tool-calling” really means — structured JSON calls
Comparison: OpenAI vs open-source tool schemas
Native function calling in:
Qwen 2.5: most complete schema support
Mistral 7B: partial but usable
Gemma 2: via wrapper
Handling responses, retries, and validation
Reliability vs. performance trade-offs (latency, hallucination rate, deterministic behaviour)
Demo:
Run a Python “calculator” or “weather” tool via Qwen through Ollama, printing full tool-call JSON.
Discussion:
When to keep tools simple vs. complex? Why schema clarity matters for MCP later.
11:40–12:30 | Exercise – Your First Working Agent (50 min)
Objective
Build a minimal but complete autonomous loop using local models.
Steps
Start from provided agent template.
Define a simple tool (maths, file reader, or URL fetcher).
Let the model decide when to call it.
Observe reasoning logs (“thoughts,” tool call, result handling).
Optionally, compare with OpenAI gpt-4-turbo behaviour.
Outcome
You now have a functional local agent using open-source tool-calling, ready for MCP integration.
Wrap-up mini-discussion (5 min):
How does the agent decide when to act?
What could be improved (memory, context, parallel tools)?
12:30–13:20 | Lunch Break (50 min)
By Lunch:
Everyone can run and test models locally.
They understand the agent reasoning loop.
They’ve built an agent with one or more callable tools.
They’re conceptually ready to connect these capabilities to the Model Context Protocol layer in the afternoon.
12:13
13:20 – 13:40 | Recap & Transition to MCP + Embabel (20 min)
Objective
Re-anchor the morning work (local agents, tool calling) and preview how the afternoon builds on it.
Talking Points
What we’ve built: local reasoning + tool calling
What’s next: interoperability + orchestration
MCP → open protocol for context and tools
Embabel → enterprise-grade agent framework on the JVM
Why Java still matters: scalability, security, and integration with existing systems
Mini Demo:
Show Embabel project structure: Agent.java, Reasoner.java, Tool.java, and MCPConnector.java.
Quick run of an Embabel agent calling a dummy calculator tool.
13:40 – 14:20 | Model Context Protocol (MCP) Deep Dive (40 min)
Teaching + Demo
What MCP standardises: context, tool metadata, and capability exchange
Client :left_right_arrow: Server model: messages, schemas, and JSON-RPC style
Example transports: HTTP, WebSocket, gRPC
How OpenAI and Anthropic MCP align with open-source versions
How Embabel implements an MCP client internally
Hands-On Exercise 1 — “Your First Embabel MCP Agent” (20 min)
Start with provided Embabel skeleton project
Configure an MCP connection (e.g. local mcp-server-fs or SQLite)
Run agent and inspect tool discovery logs
Observe agent requests and server responses in the console
Outcome:
A working Embabel agent connected to a live MCP server.
14:20 – 14:55 | Agentic RAG and Data Integration (35 min)
Concepts
Why RAG is central to agent reasoning
Open embeddings (bge-small, nomic-embed-text)
Building an MCP RAG service (retriever endpoint)
Integrating Embabel’s KnowledgeConnector with that endpoint
Hands-On Exercise 2 — “Embabel + RAG Connector” (20 min)
Extend your Embabel agent to query an MCP RAG endpoint (documents provided).
Run a prompt like “Summarise key points from policy.pdf”.
Observe embedding lookups and response reasoning.
Outcome:
Embabel agent can query and ground answers from local data — fully private, fully open.
14:55 – 15:15 | Afternoon Break (20 min)
12:13
15:15 – 15:55 | Multi-Agent Teams & Heterogeneous Models (40 min)
Teaching + Demo
Single vs. multi-agent systems
Patterns: Planner-Executor, Research-Critic, Coordinator-Worker
Different model strengths (Qwen, Mistral, Gemma)
How Embabel lets you define multiple agents and message passing between them
Example orchestration graph
Hands-On Exercise 3 — “Embabel Team Play” (25 min)
Add a second agent in your Embabel config (e.g. Researcher + Summariser).
Use different models for each role.
Let one agent call a tool via MCP, another aggregate results.
Observe the message flow in the logs or visual debug view.
Outcome:
A functioning two-agent Embabel workflow — setting up for enterprise orchestration.
15:55 – 16:20 | Enterprise Patterns & Deployment (25 min)
Topics
Embabel modules and lifecycle
Integration with Spring Boot / Quarkus services
Scaling MCP connections and agents
Security, logging, and auditability
Example deployment: private cloud agent mesh
Optional mini-exercise:
Edit the application.yml to switch model endpoints or security modes (local → remote inference).
Outcome:
Participants see how to move from local demos to production architecture.
16:20 – 16:30 | Wrap-Up & Discussion (10 min)
Recap
Agents → MCP → RAG → Multi-Agent → Embabel deployment
Open models: Qwen, Mistral, Gemma
Take-home repo + docs
Suggested follow-ups: build custom MCP servers, explore Embabel Sentinels & Guards, try private deployments