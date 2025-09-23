package com.voiceai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages a conversation context with ChatGPT
 */
public class Conversation {

    private static final int MAX_TOKENS_LIMIT = 8000; // Conservative limit for GPT-3.5/4
    private static final int ESTIMATED_TOKENS_PER_CHAR = 4; // Rough estimation

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("messages")
    private List<ChatMessage> messages;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("lastModified")
    private LocalDateTime lastModified;

    // Default constructor for Jackson
    public Conversation() {
        this.id = UUID.randomUUID().toString();
        this.messages = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.title = "New Conversation";

        // Add default system message for Italian context
        addSystemMessage("Sei un assistente AI che aiuta con le trascrizioni. " +
                "Rispondi sempre in italiano in modo chiaro e professionale. " +
                "Quando elabori trascrizioni, mantieni il significato originale " +
                "e fornisci risposte utili e pertinenti.");
    }

    public Conversation(String title) {
        this();
        this.title = title;
    }

    /**
     * Adds a message to the conversation
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        updateLastModified();

        // Auto-generate title from first user message if still default
        if ("New Conversation".equals(title) &&
                message.getRole() == ChatMessage.Role.USER &&
                message.getContent().length() > 0) {
            generateTitle(message.getContent());
        }
    }

    /**
     * Adds a user message
     */
    public void addUserMessage(String content) {
        addMessage(ChatMessage.userMessage(content));
    }

    /**
     * Adds an assistant message
     */
    public void addAssistantMessage(String content) {
        addMessage(ChatMessage.assistantMessage(content));
    }

    /**
     * Adds a system message
     */
    public void addSystemMessage(String content) {
        addMessage(ChatMessage.systemMessage(content));
    }

    /**
     * Gets messages formatted for OpenAI API
     */
    public List<ChatMessage> getMessagesForAPI() {
        // Trim conversation if it's getting too long
        List<ChatMessage> apiMessages = new ArrayList<>();

        // Always include system messages
        for (ChatMessage msg : messages) {
            if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                apiMessages.add(msg);
            }
        }

        // Add recent conversation history within token limit
        List<ChatMessage> conversationHistory = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg.getRole() != ChatMessage.Role.SYSTEM) {
                conversationHistory.add(msg);
            }
        }

        // Estimate tokens and trim if necessary
        int estimatedTokens = 0;
        List<ChatMessage> recentMessages = new ArrayList<>();

        // Add messages from most recent backwards until we hit token limit
        for (int i = conversationHistory.size() - 1; i >= 0; i--) {
            ChatMessage msg = conversationHistory.get(i);
            int msgTokens = estimateTokens(msg.getContent());

            if (estimatedTokens + msgTokens > MAX_TOKENS_LIMIT) {
                break;
            }

            estimatedTokens += msgTokens;
            recentMessages.add(0, msg); // Add to beginning to maintain order
        }

        apiMessages.addAll(recentMessages);
        return apiMessages;
    }

    /**
     * Estimates token count for a string (rough approximation)
     */
    private int estimateTokens(String text) {
        return Math.max(1, text.length() / ESTIMATED_TOKENS_PER_CHAR);
    }

    /**
     * Generates a title from the first user message
     */
    private void generateTitle(String firstMessage) {
        if (firstMessage.length() > 50) {
            this.title = firstMessage.substring(0, 47) + "...";
        } else {
            this.title = firstMessage;
        }

        // Clean up the title
        this.title = this.title.replaceAll("[\\r\\n]+", " ").trim();
    }

    /**
     * Updates last modified timestamp
     */
    private void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Clears all messages except system messages
     */
    public void clearConversation() {
        messages.removeIf(msg -> msg.getRole() != ChatMessage.Role.SYSTEM);
        updateLastModified();
    }

    /**
     * Gets the last user message
     */
    public ChatMessage getLastUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.getRole() == ChatMessage.Role.USER) {
                return msg;
            }
        }
        return null;
    }

    /**
     * Gets the last assistant message
     */
    public ChatMessage getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.getRole() == ChatMessage.Role.ASSISTANT) {
                return msg;
            }
        }
        return null;
    }

    /**
     * Gets conversation summary for display
     */
    public String getDisplaySummary() {
        int userMessages = 0;
        int assistantMessages = 0;

        for (ChatMessage msg : messages) {
            if (msg.getRole() == ChatMessage.Role.USER) {
                userMessages++;
            } else if (msg.getRole() == ChatMessage.Role.ASSISTANT) {
                assistantMessages++;
            }
        }

        return String.format("%d messages (%d user, %d assistant)",
                userMessages + assistantMessages, userMessages, assistantMessages);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        updateLastModified();
    }

    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages); // Return copy to prevent external modification
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = new ArrayList<>(messages);
        updateLastModified();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets total message count (excluding system messages)
     */
    public int getMessageCount() {
        return (int) messages.stream()
                .filter(msg -> msg.getRole() != ChatMessage.Role.SYSTEM)
                .count();
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", messageCount=" + getMessageCount() +
                ", lastModified=" + lastModified +
                '}';
    }
}