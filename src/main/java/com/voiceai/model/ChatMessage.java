package com.voiceai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single chat message in the conversation
 */
public class ChatMessage {

    public enum Role {
        @JsonProperty("system")
        SYSTEM("system"),

        @JsonProperty("user")
        USER("user"),

        @JsonProperty("assistant")
        ASSISTANT("assistant");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @JsonProperty("role")
    private Role role;

    @JsonProperty("content")
    private String content;

    private LocalDateTime timestamp;

    // Default constructor for Jackson
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(Role role, String content, LocalDateTime timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets formatted timestamp for display
     */
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Gets display name for the role
     */
    public String getDisplayName() {
        switch (role) {
            case USER:
                return "You";
            case ASSISTANT:
                return "ChatGPT";
            case SYSTEM:
                return "System";
            default:
                return role.getValue();
        }
    }

    /**
     * Creates a user message
     */
    public static ChatMessage userMessage(String content) {
        return new ChatMessage(Role.USER, content);
    }

    /**
     * Creates an assistant message
     */
    public static ChatMessage assistantMessage(String content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }

    /**
     * Creates a system message
     */
    public static ChatMessage systemMessage(String content) {
        return new ChatMessage(Role.SYSTEM, content);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "role=" + role +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChatMessage that = (ChatMessage) o;

        if (role != that.role) return false;
        if (!content.equals(that.content)) return false;
        return timestamp.equals(that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = role.hashCode();
        result = 31 * result + content.hashCode();
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}