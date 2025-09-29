package com.voiceai.service;

import com.voiceai.ui.UIConstants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Service for managing UI creation and layout.
 * Handles all UI component creation, styling, and organization.
 */
public class UIService {

    // UI Component references
    private TextField apiKeyField;
    private Button testConnectionButton;
    private Label connectionStatus;
    private Button recButton;
    private Button saveButton;
    private Button selectAllButton;
    private Button loadButton;
    private TextArea transcriptionArea;
    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Button insertTranscriptButton;
    private Button clearButton;
    private Label chatStatusLabel;
    private Label tokenCounterLabel;
    private Label transcriptionStatusLabel;
    private CheckBox realTimeCheckBox;

    /**
     * Creates and shows the main UI
     *
     * @param stage the primary stage
     * @param windowWidth the window width
     * @param windowHeight the window height
     * @param existingApiKey the existing API key (masked)
     * @param useRealTimeTranscription whether real-time transcription is enabled
     * @return the created scene
     */
    public Scene createAndShowUI(Stage stage, double windowWidth, double windowHeight,
                                 String existingApiKey, boolean useRealTimeTranscription) {
        // Create main layout
        VBox root = new VBox(UIConstants.ROOT_SPACING);
        root.setPadding(new Insets(UIConstants.ROOT_PADDING));
        root.setStyle(UIConstants.ROOT_STYLE);

        // API Configuration Section
        VBox apiSection = createApiSection(existingApiKey);

        // Main Content - Two panels
        HBox mainContent = createMainContent(useRealTimeTranscription);

        root.getChildren().addAll(apiSection, mainContent);

        Scene scene = new Scene(root, windowWidth, windowHeight);
        return scene;
    }

    /**
     * Creates the API configuration section
     */
    private VBox createApiSection(String existingApiKey) {
        VBox apiSection = new VBox(UIConstants.SECTION_SPACING);

        Label titleLabel = new Label(UIConstants.APP_TITLE);
        titleLabel.setStyle(UIConstants.TITLE_STYLE);

        HBox apiKeyBox = new HBox(UIConstants.CONTROL_SPACING);
        apiKeyBox.setAlignment(Pos.CENTER_LEFT);

        apiKeyField = new TextField();
        apiKeyField.setPromptText(UIConstants.API_KEY_PROMPT);
        apiKeyField.setPrefWidth(UIConstants.API_KEY_FIELD_WIDTH);
        apiKeyField.setStyle(UIConstants.INPUT_FIELD_STYLE);

        // Pre-populate API key field if available (masked for security)
        if (existingApiKey != null && !existingApiKey.trim().isEmpty()) {
            apiKeyField.setText("•".repeat(Math.min(existingApiKey.length(), 32)));
            apiKeyField.setPromptText("API key loaded from settings");
        }

        testConnectionButton = new Button(UIConstants.TEST_CONNECTION_TEXT);
        testConnectionButton.setStyle(UIConstants.LIGHT_BUTTON_STYLE);

        connectionStatus = new Label(UIConstants.DISCONNECTED_STATUS);
        connectionStatus.setStyle(UIConstants.ERROR_STATUS_STYLE);

        // Token counter label
        tokenCounterLabel = new Label("Tokens: 0");
        tokenCounterLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        apiKeyBox.getChildren().addAll(apiKeyField, testConnectionButton, connectionStatus, tokenCounterLabel);
        apiSection.getChildren().addAll(titleLabel, apiKeyBox);

        return apiSection;
    }

    /**
     * Creates the main content area with both panels
     */
    private HBox createMainContent(boolean useRealTimeTranscription) {
        HBox mainContent = new HBox(UIConstants.MAIN_CONTENT_SPACING);
        mainContent.setAlignment(Pos.TOP_CENTER);

        // Left Panel - Transcription
        VBox transcriptionPanel = createTranscriptionPanel(useRealTimeTranscription);
        transcriptionPanel.setPrefWidth(UIConstants.PANEL_WIDTH);

        // Right Panel - Chat GPT
        VBox chatPanel = createChatPanel();
        chatPanel.setPrefWidth(UIConstants.PANEL_WIDTH);

        mainContent.getChildren().addAll(transcriptionPanel, chatPanel);

        return mainContent;
    }

    /**
     * Creates the transcription panel
     */
    private VBox createTranscriptionPanel(boolean useRealTimeTranscription) {
        VBox panel = new VBox(UIConstants.PANEL_SPACING);

        // Header with status
        HBox header = new HBox(UIConstants.CONTROL_SPACING);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label(UIConstants.TRANSCRIPT_HEADER);
        headerLabel.setStyle(UIConstants.HEADER_STYLE);

        transcriptionStatusLabel = new Label("");
        transcriptionStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        // Real-time transcription checkbox
        realTimeCheckBox = new CheckBox("Real-time");
        realTimeCheckBox.setSelected(useRealTimeTranscription);
        realTimeCheckBox.setStyle("-fx-font-size: 12px;");

        header.getChildren().addAll(headerLabel, transcriptionStatusLabel, realTimeCheckBox);

        // Control buttons
        HBox controls = new HBox(UIConstants.CONTROL_SPACING);
        controls.setAlignment(Pos.CENTER_LEFT);

        recButton = new Button(UIConstants.REC_BUTTON_TEXT);
        recButton.setStyle(UIConstants.PRIMARY_BUTTON_STYLE);

        saveButton = new Button(UIConstants.SAVE_BUTTON_TEXT);
        saveButton.setStyle(UIConstants.createButtonStyle("#6366f1"));

        selectAllButton = new Button(UIConstants.SELECT_ALL_BUTTON_TEXT);
        selectAllButton.setStyle(UIConstants.SECONDARY_BUTTON_STYLE);

        loadButton = new Button(UIConstants.LOAD_BUTTON_TEXT);
        loadButton.setStyle(UIConstants.INFO_BUTTON_STYLE);

        controls.getChildren().addAll(recButton, saveButton, selectAllButton, loadButton);

        // Transcription area
        transcriptionArea = new TextArea();
        transcriptionArea.setPromptText(UIConstants.TRANSCRIPT_PLACEHOLDER_IDLE);
        transcriptionArea.setPrefHeight(UIConstants.TRANSCRIPT_AREA_HEIGHT);
        transcriptionArea.setWrapText(true);
        transcriptionArea.setStyle(UIConstants.TEXT_AREA_STYLE);

        panel.getChildren().addAll(header, controls, transcriptionArea);
        panel.setStyle(UIConstants.PANEL_STYLE);

        return panel;
    }

    /**
     * Creates the chat panel
     */
    private VBox createChatPanel() {
        VBox panel = new VBox(UIConstants.PANEL_SPACING);

        // Header with status
        HBox header = new HBox(UIConstants.CONTROL_SPACING);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label(UIConstants.CHAT_HEADER);
        headerLabel.setStyle(UIConstants.HEADER_STYLE);

        chatStatusLabel = new Label("");
        chatStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        header.getChildren().addAll(headerLabel, chatStatusLabel);

        // Chat area
        chatArea = new TextArea();
        chatArea.setPromptText(UIConstants.CHAT_PLACEHOLDER);
        chatArea.setPrefHeight(UIConstants.CHAT_AREA_HEIGHT);
        chatArea.setWrapText(true);
        chatArea.setEditable(false);
        chatArea.setStyle(UIConstants.TEXT_AREA_STYLE);

        // Message input section
        VBox inputSection = new VBox(UIConstants.CONTROL_SPACING);

        HBox messageBox = new HBox(UIConstants.CONTROL_SPACING);
        messageBox.setAlignment(Pos.CENTER);

        messageField = new TextField();
        messageField.setPromptText(UIConstants.MESSAGE_PLACEHOLDER);
        messageField.setPrefWidth(UIConstants.MESSAGE_FIELD_WIDTH);
        messageField.setStyle(UIConstants.INPUT_FIELD_STYLE);

        sendButton = new Button(UIConstants.SEND_BUTTON_TEXT);
        sendButton.setStyle(UIConstants.SEND_BUTTON_STYLE);

        messageBox.getChildren().addAll(messageField, sendButton);

        // Action buttons
        HBox actionButtons = new HBox(UIConstants.CONTROL_SPACING);
        actionButtons.setAlignment(Pos.CENTER);

        insertTranscriptButton = new Button(UIConstants.INSERT_TRANSCRIPT_BUTTON_TEXT);
        insertTranscriptButton.setStyle(UIConstants.SECONDARY_BUTTON_STYLE);

        clearButton = new Button(UIConstants.CLEAR_BUTTON_TEXT);
        clearButton.setStyle(UIConstants.PRIMARY_BUTTON_STYLE);

        actionButtons.getChildren().addAll(insertTranscriptButton, clearButton);

        inputSection.getChildren().addAll(messageBox, actionButtons);

        panel.getChildren().addAll(header, chatArea, inputSection);
        panel.setStyle(UIConstants.PANEL_STYLE);

        return panel;
    }

    // Accessor methods for UI components
    public TextField getApiKeyField() { return apiKeyField; }
    public Button getTestConnectionButton() { return testConnectionButton; }
    public Label getConnectionStatus() { return connectionStatus; }
    public Button getRecButton() { return recButton; }
    public Button getSaveButton() { return saveButton; }
    public Button getSelectAllButton() { return selectAllButton; }
    public Button getLoadButton() { return loadButton; }
    public TextArea getTranscriptionArea() { return transcriptionArea; }
    public TextArea getChatArea() { return chatArea; }
    public TextField getMessageField() { return messageField; }
    public Button getSendButton() { return sendButton; }
    public Button getInsertTranscriptButton() { return insertTranscriptButton; }
    public Button getClearButton() { return clearButton; }
    public Label getChatStatusLabel() { return chatStatusLabel; }
    public Label getTokenCounterLabel() { return tokenCounterLabel; }
    public Label getTranscriptionStatusLabel() { return transcriptionStatusLabel; }
    public CheckBox getRealTimeCheckBox() { return realTimeCheckBox; }
}