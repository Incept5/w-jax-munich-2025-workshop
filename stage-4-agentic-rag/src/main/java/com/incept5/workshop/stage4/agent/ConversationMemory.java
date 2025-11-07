package com.incept5.workshop.stage4.agent;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-memory conversation history tracker.
 * 
 * Maintains a sliding window of recent messages to provide context
 * for multi-turn conversations. Automatically trims old messages
 * to stay within token limits.
 * 
 * Example:
 * <pre>
 * ConversationMemory memory = new ConversationMemory(10);
 * memory.addUserMessage("What is Embabel?");
 * memory.addAssistantMessage("Embabel is an agent framework...");
 * memory.addUserMessage("Show me an example");
 * 
 * String history = memory.formatHistory();
 * // Returns formatted conversation for LLM prompt
 * </pre>
 */
public class ConversationMemory {
    private final Deque<Message> messages;
    private final int maxMessages;
    private final int maxTokensEstimate;
    
    /**
     * Creates a conversation memory with default token limit.
     * 
     * @param maxMessages Maximum number of messages to retain
     */
    public ConversationMemory(int maxMessages) {
        this(maxMessages, 4000); // Default ~4000 tokens
    }
    
    /**
     * Creates a conversation memory with custom limits.
     * 
     * @param maxMessages Maximum number of messages to retain
     * @param maxTokensEstimate Approximate token limit for history
     */
    public ConversationMemory(int maxMessages, int maxTokensEstimate) {
        this.messages = new ArrayDeque<>();
        this.maxMessages = maxMessages;
        this.maxTokensEstimate = maxTokensEstimate;
    }
    
    /**
     * Add a user message to the conversation.
     * 
     * @param content The message content
     */
    public void addUserMessage(String content) {
        messages.add(new UserMessage(content, Instant.now()));
        trimIfNeeded();
    }
    
    /**
     * Add an assistant message to the conversation.
     * 
     * @param content The message content
     */
    public void addAssistantMessage(String content) {
        messages.add(new AssistantMessage(content, Instant.now()));
        trimIfNeeded();
    }
    
    /**
     * Add a system message (e.g., tool results) to the conversation.
     * 
     * @param content The message content
     */
    public void addSystemMessage(String content) {
        messages.add(new SystemMessage(content, Instant.now()));
        trimIfNeeded();
    }
    
    /**
     * Get the full conversation history.
     * 
     * @return Immutable list of messages in chronological order
     */
    public List<Message> getHistory() {
        return new ArrayList<>(messages);
    }
    
    /**
     * Format conversation history for use in LLM prompts.
     * Only includes user and assistant messages (not system messages).
     * 
     * @return Formatted string with conversation dialogue
     */
    public String formatHistory() {
        StringBuilder history = new StringBuilder();
        
        for (Message msg : messages) {
            // Only include user and assistant messages in conversation history
            // System messages (tool results) are not part of the dialogue
            if (msg instanceof UserMessage || msg instanceof AssistantMessage) {
                history.append(msg.role()).append(": ")
                       .append(msg.content())
                       .append("\n\n");
            }
        }
        
        return history.toString();
    }
    
    /**
     * Get the number of messages in history.
     * 
     * @return Message count
     */
    public int size() {
        return messages.size();
    }
    
    /**
     * Clear all conversation history.
     */
    public void clear() {
        messages.clear();
    }
    
    /**
     * Check if conversation history is empty.
     * 
     * @return true if no messages, false otherwise
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }
    
    /**
     * Trim conversation to stay within limits.
     * Removes oldest messages first, but never removes the current turn.
     */
    private void trimIfNeeded() {
        // Trim by message count
        while (messages.size() > maxMessages) {
            messages.removeFirst();
        }
        
        // Trim by estimated token count
        // Keep at least the last 2 messages (user + assistant)
        while (estimateTokens() > maxTokensEstimate && messages.size() > 2) {
            messages.removeFirst();
        }
    }
    
    /**
     * Estimate total tokens in conversation history.
     * Uses rough heuristic: ~4 characters per token.
     * 
     * @return Estimated token count
     */
    private int estimateTokens() {
        int totalChars = messages.stream()
            .mapToInt(msg -> msg.content().length())
            .sum();
        
        return totalChars / 4; // Rough estimate: 4 chars per token
    }
    
    // Message interface and implementations
    
    /**
     * Base interface for conversation messages.
     */
    public interface Message {
        /**
         * Get the role of the message sender.
         * 
         * @return "user", "assistant", or "system"
         */
        String role();
        
        /**
         * Get the message content.
         * 
         * @return Message text
         */
        String content();
        
        /**
         * Get the timestamp when message was created.
         * 
         * @return Message timestamp
         */
        Instant timestamp();
    }
    
    /**
     * Message from the user.
     */
    public record UserMessage(String content, Instant timestamp) implements Message {
        @Override
        public String role() {
            return "user";
        }
    }
    
    /**
     * Message from the assistant (AI).
     */
    public record AssistantMessage(String content, Instant timestamp) implements Message {
        @Override
        public String role() {
            return "assistant";
        }
    }
    
    /**
     * System message (e.g., tool results, internal state).
     */
    public record SystemMessage(String content, Instant timestamp) implements Message {
        @Override
        public String role() {
            return "system";
        }
    }
}
