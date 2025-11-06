package com.incept5.workshop.stage3.agent;

import com.incept5.ollama.backend.AIBackend;
import com.incept5.ollama.backend.BackendFactory;
import com.incept5.ollama.backend.BackendType;
import com.incept5.workshop.stage3.db.DatabaseConfig;
import com.incept5.workshop.stage3.db.PgVectorStore;
import com.incept5.workshop.stage3.ingestion.EmbeddingService;
import com.incept5.workshop.stage3.tool.RAGTool;
import com.incept5.workshop.stage3.tool.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Scanner;

/**
 * Interactive CLI demonstration of the RAG-enabled conversational agent.
 * 
 * Usage:
 * <pre>
 * # Standard mode
 * java -jar stage-3-agentic-rag.jar
 * 
 * # Verbose mode (shows agent reasoning)
 * java -jar stage-3-agentic-rag.jar --verbose
 * </pre>
 */
public class RAGAgentDemo {
    private static final Logger logger = LoggerFactory.getLogger(RAGAgentDemo.class);
    
    // Configuration
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String LLM_MODEL = "incept5/Jan-v1-2509:fp16";
    private static final String EMBEDDING_MODEL = "nomic-embed-text";
    
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/workshop_rag";
    private static final String DB_USER = "workshop";
    private static final String DB_PASSWORD = "workshop123";
    
    public static void main(String[] args) {
        boolean verbose = args.length > 0 && "--verbose".equals(args[0]);
        
        logger.info("Starting RAG Agent Demo (verbose: {})", verbose);
        
        try {
            // Initialize components
            System.out.println("\n🚀 Initializing RAG Agent...");
            
            // 1. Setup backend
            System.out.println("   └─ Connecting to Ollama at " + OLLAMA_BASE_URL + "...");
            AIBackend backend = BackendFactory.createBackend(
                BackendType.OLLAMA,
                OLLAMA_BASE_URL,
                LLM_MODEL,
                Duration.ofSeconds(300)
            );
            System.out.println("   └─ ✓ Backend ready (model: " + LLM_MODEL + ")");
            
            // 2. Setup database connection
            System.out.println("   └─ Connecting to PostgreSQL...");
            DataSource dataSource = DatabaseConfig.createDataSource(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("   └─ ✓ Database connection ready");
            
            // 3. Setup embedding service
            System.out.println("   └─ Initializing embedding service...");
            EmbeddingService embeddingService = new EmbeddingService(OLLAMA_BASE_URL, EMBEDDING_MODEL);
            System.out.println("   └─ ✓ Embedding service ready (model: " + EMBEDDING_MODEL + ")");
            
            // 4. Setup vector store
            System.out.println("   └─ Initializing vector store...");
            PgVectorStore vectorStore = new PgVectorStore(dataSource, embeddingService);
            
            // Check document count
            int docCount = vectorStore.getTotalDocuments();
            System.out.println("   └─ ✓ Vector store ready (" + docCount + " documents indexed)");
            
            if (docCount == 0) {
                System.err.println("\n⚠️  WARNING: No documents found in vector store!");
                System.err.println("   Please run './ingest.sh' first to load documentation.");
                System.exit(1);
            }
            
            // 5. Setup tools
            System.out.println("   └─ Registering tools...");
            ToolRegistry toolRegistry = new ToolRegistry();
            toolRegistry.register(new RAGTool(vectorStore));
            System.out.println("   └─ ✓ Tools registered: " + String.join(", ", toolRegistry.getToolNames()));
            
            // 6. Create agent
            System.out.println("   └─ Building agent...");
            RAGAgent agent = RAGAgent.builder()
                .backend(backend)
                .toolRegistry(toolRegistry)
                .maxConversationHistory(10)
                .maxIterations(10)
                .verbose(verbose)
                .build();
            System.out.println("   └─ ✓ Agent ready!");
            
            // Start interactive loop
            runInteractiveLoop(agent);
            
            // Cleanup
            vectorStore.close();
            backend.close();
            
        } catch (Exception e) {
            logger.error("Fatal error in RAG Agent Demo", e);
            System.err.println("\n❌ Error: " + e.getMessage());
            System.err.println("\nPlease ensure:");
            System.err.println("  1. Ollama is running: ollama serve");
            System.err.println("  2. Models are available: ollama pull " + LLM_MODEL);
            System.err.println("                          ollama pull " + EMBEDDING_MODEL);
            System.err.println("  3. PostgreSQL is running: docker-compose up -d");
            System.err.println("  4. Documents are ingested: ./ingest.sh");
            System.exit(1);
        }
    }
    
    private static void runInteractiveLoop(RAGAgent agent) {
        Scanner scanner = new Scanner(System.in);
        
        printWelcomeBanner();
        
        while (true) {
            System.out.print("\n💬 You: ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            // Check for exit commands
            if (input.equalsIgnoreCase("exit") || 
                input.equalsIgnoreCase("quit") || 
                input.equalsIgnoreCase("q")) {
                System.out.println("\n👋 Goodbye!");
                break;
            }
            
            // Check for clear command
            if (input.equalsIgnoreCase("clear") || input.equalsIgnoreCase("reset")) {
                agent.clearHistory();
                System.out.println("\n🔄 Conversation history cleared.");
                continue;
            }
            
            // Check for history command
            if (input.equalsIgnoreCase("history")) {
                printHistory(agent);
                continue;
            }
            
            // Check for help command
            if (input.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }
            
            // Process message
            try {
                System.out.println("\n🤖 Assistant: ");
                String response = agent.chat(input);
                System.out.println(response);
                
            } catch (Exception e) {
                logger.error("Error processing message", e);
                System.err.println("\n❌ Error: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
    
    private static void printWelcomeBanner() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("  RAG Agent - Embabel & Spring AI Documentation Assistant");
        System.out.println("═".repeat(70));
        System.out.println("\nCommands:");
        System.out.println("  • Type your question to chat with the agent");
        System.out.println("  • 'help' - Show available commands");
        System.out.println("  • 'history' - Show conversation history");
        System.out.println("  • 'clear' - Clear conversation history");
        System.out.println("  • 'exit' or 'quit' - End the conversation");
        System.out.println("\nExample questions:");
        System.out.println("  • What is Embabel?");
        System.out.println("  • How do I create an agent with Embabel?");
        System.out.println("  • Show me a Spring AI ChatClient example");
        System.out.println("  • What's the difference between Embabel and LangChain?");
        System.out.println("\n" + "─".repeat(70));
    }
    
    private static void printHelp() {
        System.out.println("\n" + "─".repeat(70));
        System.out.println("Available Commands:");
        System.out.println("─".repeat(70));
        System.out.println("  help      - Show this help message");
        System.out.println("  history   - Show conversation history");
        System.out.println("  clear     - Clear conversation history and start fresh");
        System.out.println("  exit/quit - End the conversation");
        System.out.println("\nTips:");
        System.out.println("  • Ask follow-up questions - the agent remembers context");
        System.out.println("  • Be specific for better results");
        System.out.println("  • Use 'clear' if you want to start a new topic");
        System.out.println("─".repeat(70));
    }
    
    private static void printHistory(RAGAgent agent) {
        ConversationMemory.Message[] history = agent.getConversationHistory();
        
        if (history.length == 0) {
            System.out.println("\n📜 No conversation history yet.");
            return;
        }
        
        System.out.println("\n" + "─".repeat(70));
        System.out.println("Conversation History (" + history.length + " messages):");
        System.out.println("─".repeat(70));
        
        for (int i = 0; i < history.length; i++) {
            ConversationMemory.Message msg = history[i];
            
            String prefix = switch (msg.role()) {
                case "user" -> "💬 You: ";
                case "assistant" -> "🤖 Assistant: ";
                case "system" -> "⚙️  System: ";
                default -> msg.role() + ": ";
            };
            
            String content = msg.content();
            // Truncate long system messages
            if ("system".equals(msg.role()) && content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            
            System.out.println("\n" + (i + 1) + ". " + prefix);
            System.out.println("   " + content);
        }
        
        System.out.println("\n" + "─".repeat(70));
    }
}
