package com.voiceai.service;

import com.voiceai.model.Conversation;
import com.voiceai.state.ApplicationState;
import com.voiceai.ui.UIConstants;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Service for managing chat operations and conversation flow.
 * Handles message sending, streaming responses, and conversation management.
 */
public class ChatService {

    private static final Logger logger = Logger.getLogger(ChatService.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final OpenAIService openAIService;
    private final NotificationService notificationService;
    private final ApplicationState appState;

    // UI Component references
    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Label statusLabel;

    // Conversation management
    private Conversation currentConversation;

    // State tracking
    private boolean isApiKeyValid = false;

    /**
     * Creates a new ChatService
     *
     * @param openAIService the OpenAI service for chat completions
     * @param notificationService the notification service
     * @param appState the application state
     */
    public ChatService(OpenAIService openAIService,
                       NotificationService notificationService,
                       ApplicationState appState) {
        this.openAIService = openAIService;
        this.notificationService = notificationService;
        this.appState = appState;
        this.currentConversation = new Conversation("StorieS Maker Chat");
    }

    /**
     * Initializes the chat service with UI components
     *
     * @param chatArea the chat text area
     * @param messageField the message input field
     * @param sendButton the send button
     * @param statusLabel the status label
     */
    public void initializeUI(TextArea chatArea, TextField messageField, Button sendButton, Label statusLabel) {
        this.chatArea = chatArea;
        this.messageField = messageField;
        this.sendButton = sendButton;
        this.statusLabel = statusLabel;

        // Set up event handlers
        if (sendButton != null) {
            sendButton.setOnAction(e -> sendMessage());
        }

        if (messageField != null) {
            messageField.setOnAction(e -> sendMessage());
        }

        // Initial UI state
        updateUIState();

        logger.info("ChatService UI initialized");
    }

    /**
     * Sends a message to the chat
     */
    public void sendMessage() {
        sendMessage(messageField.getText().trim());
    }

    /**
     * Sends a message to the chat with specified text
     *
     * @param message the message to send
     */
    public void sendMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        if (!isApiKeyValid) {
            notificationService.showWarning("Please validate API key before sending messages");
            return;
        }

        if (appState.isChatBusy()) {
            notificationService.showWarning("Please wait for the current response to complete");
            return;
        }

        // Clear the input field immediately
        if (messageField != null) {
            messageField.clear();
        }

        // Add user message to conversation and display
        currentConversation.addUserMessage(message);
        appendChatMessage("You", message);

        // Update state to sending
        appState.setChatState(ApplicationState.ChatState.SENDING, "Sending message...");
        updateChatStatus();

        logger.info("Sending chat message: " + message.substring(0, Math.min(message.length(), 50)) + "...");

        // Send to OpenAI with streaming
        openAIService.sendStreamingChatCompletion(
                currentConversation,
                this::handleStreamingChunk,
                this::handleStreamingComplete
        ).exceptionally(throwable -> {
            Platform.runLater(() -> {
                appState.setChatState(ApplicationState.ChatState.ERROR, throwable.getMessage());
                notificationService.showError("Failed to send message", throwable);
                updateChatStatus();
            });
            return null;
        });
    }

    /**
     * Handles streaming response chunks
     */
    private void handleStreamingChunk(String chunk) {
        Platform.runLater(() -> {
            // Update state to streaming if not already
            if (!appState.isChatStreaming()) {
                appState.setChatState(ApplicationState.ChatState.STREAMING, "Receiving response...");
                updateChatStatus();
                // Start the assistant message
                if (chatArea != null) {
                    chatArea.appendText("ChatGPT: ");
                }
            }

            // Append the chunk to chat area
            if (chatArea != null) {
                chatArea.appendText(chunk);
                // Auto-scroll to bottom
                chatArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    /**
     * Handles completion of streaming response
     */
    private void handleStreamingComplete(OpenAIService.ChatCompletionResult result) {
        Platform.runLater(() -> {
            if (result.isSuccess()) {
                // Add assistant message to conversation
                currentConversation.addAssistantMessage(result.getContent());

                // Add newlines for formatting
                if (chatArea != null) {
                    chatArea.appendText("\n\n");
                    chatArea.setScrollTop(Double.MAX_VALUE);
                }

                // Update token usage
                appState.addTokensUsed(result.getTokensUsed());

                // Reset state to idle
                appState.setChatState(ApplicationState.ChatState.IDLE, "");
                updateChatStatus();

                notificationService.showSuccess("Response received (" + result.getTokensUsed() + " tokens)");

                logger.info("Chat response completed. Tokens used: " + result.getTokensUsed());
            } else {
                // Handle error
                appState.setChatState(ApplicationState.ChatState.ERROR, result.getMessage());

                if (chatArea != null) {
                    chatArea.appendText("\n[Error: " + result.getMessage() + "]\n\n");
                    chatArea.setScrollTop(Double.MAX_VALUE);
                }

                notificationService.showError("Chat error", new RuntimeException(result.getMessage()));
                updateChatStatus();

                logger.warning("Chat response error: " + result.getMessage());
            }
        });
    }

    /**
     * Appends a message to the chat area with proper formatting
     *
     * @param sender the sender name
     * @param message the message content
     */
    private void appendChatMessage(String sender, String message) {
        if (chatArea != null) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            chatArea.appendText(sender + " (" + timestamp + "): " + message + "\n\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        }
    }

    /**
     * Inserts text into the message field
     *
     * @param text the text to insert
     */
    public void insertTextToMessageField(String text) {
        if (messageField == null || text == null || text.isEmpty()) {
            return;
        }

        // If there's already text in the message field, add a space
        String currentText = messageField.getText();
        if (!currentText.isEmpty()) {
            messageField.setText(currentText + " " + text);
        } else {
            messageField.setText(text);
        }

        messageField.requestFocus();
        messageField.positionCaret(messageField.getText().length());
    }

    /**
     * Clears the chat with user confirmation
     */
    public void clearChat() {
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Chat");
        alert.setHeaderText("Clear conversation history?");
        alert.setContentText("This will clear all messages in the current conversation. This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (chatArea != null) {
                    chatArea.clear();
                }

                currentConversation.clearConversation();
                appState.resetTokenCounter();
                appState.setChatState(ApplicationState.ChatState.IDLE, "");
                updateChatStatus();

                notificationService.showSuccess("Chat cleared");
                logger.info("Chat conversation cleared");
            }
        });
    }

    /**
     * Clears the chat without confirmation (for programmatic use)
     */
    public void clearChatWithoutConfirmation() {
        if (chatArea != null) {
            chatArea.clear();
        }

        currentConversation.clearConversation();
        appState.resetTokenCounter();
        appState.setChatState(ApplicationState.ChatState.IDLE, "");
        updateChatStatus();

        logger.info("Chat conversation cleared (no confirmation)");
    }

    /**
     * Updates the chat status label based on application state
     */
    private void updateChatStatus() {
        if (statusLabel == null) return;

        String message = "";
        String color = "#666";

        switch (appState.getChatState()) {
            case SENDING:
                message = "Sending message...";
                color = UIConstants.WARNING_COLOR;
                break;
            case RECEIVING:
                message = "Waiting for response...";
                color = UIConstants.WARNING_COLOR;
                break;
            case STREAMING:
                message = "Receiving response...";
                color = UIConstants.SUCCESS_COLOR;
                break;
            case ERROR:
                message = "Error: " + appState.getChatStatusMessage();
                color = UIConstants.ERROR_COLOR;
                break;
            case IDLE:
            default:
                message = appState.getChatStatusMessage();
                break;
        }

        statusLabel.setText(message);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
    }

    /**
     * Updates the API key validity state
     *
     * @param isValid true if the API key is valid
     */
    public void updateApiKeyState(boolean isValid) {
        this.isApiKeyValid = isValid;
        updateUIState();
    }

    /**
     * Updates the overall UI state based on current conditions
     */
    public void updateUIState() {
        updateChatStatus();

        // Enable/disable send button based on API key validity and chat state
        if (sendButton != null) {
            boolean canSendMessage = isApiKeyValid && !appState.isChatBusy();
            sendButton.setDisable(!canSendMessage);

            if (canSendMessage) {
                sendButton.setStyle(UIConstants.SEND_BUTTON_STYLE);
            } else {
                sendButton.setStyle(UIConstants.createButtonStyle(UIConstants.GRAY_COLOR));
            }
        }

        // Enable/disable message field based on chat state
        if (messageField != null) {
            messageField.setDisable(appState.isChatBusy());
        }
    }

    /**
     * Gets the current conversation
     *
     * @return the current conversation
     */
    public Conversation getCurrentConversation() {
        return currentConversation;
    }

    /**
     * Sets a new conversation
     *
     * @param conversation the conversation to set
     */
    public void setConversation(Conversation conversation) {
        if (conversation != null) {
            this.currentConversation = conversation;
            logger.info("Conversation set: " + conversation.getTitle());
        }
    }

    /**
     * Creates a new conversation with a given name
     *
     * @param name the conversation name
     */
    public void createNewConversation(String name) {
        this.currentConversation = new Conversation(name);
        if (chatArea != null) {
            chatArea.clear();
        }
        appState.resetTokenCounter();
        appState.setChatState(ApplicationState.ChatState.IDLE, "");
        updateChatStatus();

        notificationService.showInfo("New conversation created: " + name);
        logger.info("New conversation created: " + name);
    }

    /**
     * Gets the current chat text
     *
     * @return the chat text or empty string if no area is set
     */
    public String getChatText() {
        if (chatArea != null) {
            return chatArea.getText();
        }
        return "";
    }

    /**
     * Gets the current message field text
     *
     * @return the message field text or empty string if no field is set
     */
    public String getMessageFieldText() {
        if (messageField != null) {
            return messageField.getText();
        }
        return "";
    }

    /**
     * Sets the message field text
     *
     * @param text the text to set
     */
    public void setMessageFieldText(String text) {
        if (messageField != null) {
            messageField.setText(text != null ? text : "");
        }
    }

    /**
     * Checks if a chat is currently in progress
     *
     * @return true if chat is busy
     */
    public boolean isChatBusy() {
        return appState.isChatBusy();
    }

    /**
     * Gets the total tokens used in the current session
     *
     * @return the token count
     */
    public int getTokensUsed() {
        return appState.getTokensUsedInSession();
    }

    /**
     * Gets the number of messages in the current conversation
     *
     * @return the message count
     */
    public int getMessageCount() {
        return currentConversation.getMessages().size();
    }

    /**
     * Validates if the chat service is ready to send messages
     *
     * @return validation result
     */
    public ValidationResult validateChatReady() {
        if (!isApiKeyValid) {
            return ValidationResult.failure("API key is not valid");
        }

        if (appState.isChatBusy()) {
            return ValidationResult.failure("Chat is currently busy processing a message");
        }

        return ValidationResult.success("Chat is ready");
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    /**
     * Shutdown the chat service
     */
    public void shutdown() {
        // Clean up any resources if needed
        logger.info("ChatService shutdown complete");
    }
}