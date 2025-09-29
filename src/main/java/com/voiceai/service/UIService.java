package com.voiceai.service;

import com.voiceai.UIConstants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

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
    private ComboBox<AudioRecordingService.AudioSource> audioSourceComboBox;
    private Button refreshSourcesButton;

    public Scene createAndShowUI(Stage stage, double windowWidth, double windowHeight,
                                 String existingApiKey, boolean useRealTimeTranscription) {
        VBox root = new VBox(UIConstants.ROOT_SPACING);
        root.setPadding(new Insets(UIConstants.ROOT_PADDING));
        root.setStyle(UIConstants.ROOT_STYLE);

        VBox apiSection = createApiSection(existingApiKey);

        HBox mainContent = createMainContent(useRealTimeTranscription);

        root.getChildren().addAll(apiSection, mainContent);

        Scene scene = new Scene(root, windowWidth, windowHeight);
        return scene;
    }

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

        if (existingApiKey != null && !existingApiKey.trim().isEmpty()) {
            apiKeyField.setText("•".repeat(Math.min(existingApiKey.length(), 32)));
            apiKeyField.setPromptText("API key loaded from settings");
        }

        testConnectionButton = new Button(UIConstants.TEST_CONNECTION_TEXT);
        testConnectionButton.setStyle(UIConstants.LIGHT_BUTTON_STYLE);

        connectionStatus = new Label(UIConstants.DISCONNECTED_STATUS);
        connectionStatus.setStyle(UIConstants.ERROR_STATUS_STYLE);

        tokenCounterLabel = new Label("Tokens: 0");
        tokenCounterLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        apiKeyBox.getChildren().addAll(apiKeyField, testConnectionButton, connectionStatus, tokenCounterLabel);

        // Audio source selection row
        HBox audioSourceBox = createAudioSourceSelector();

        apiSection.getChildren().addAll(titleLabel, apiKeyBox, audioSourceBox);

        return apiSection;
    }

    private HBox createAudioSourceSelector() {
        HBox audioSourceBox = new HBox(UIConstants.CONTROL_SPACING);
        audioSourceBox.setAlignment(Pos.CENTER_LEFT);

        Label audioSourceLabel = new Label(UIConstants.AUDIO_SOURCE_LABEL);
        audioSourceLabel.setStyle(UIConstants.LABEL_STYLE);

        audioSourceComboBox = new ComboBox<>();
        audioSourceComboBox.setPromptText(UIConstants.AUDIO_SOURCE_PROMPT);
        audioSourceComboBox.setPrefWidth(UIConstants.AUDIO_SOURCE_COMBO_WIDTH);
        audioSourceComboBox.setStyle(UIConstants.COMBO_BOX_STYLE);

        refreshSourcesButton = new Button(UIConstants.REFRESH_SOURCES_TEXT);
        refreshSourcesButton.setStyle(UIConstants.REFRESH_BUTTON_STYLE);
        refreshSourcesButton.setTooltip(new Tooltip("Refresh audio sources"));

        audioSourceBox.getChildren().addAll(audioSourceLabel, audioSourceComboBox, refreshSourcesButton);

        return audioSourceBox;
    }

    private HBox createMainContent(boolean useRealTimeTranscription) {
        HBox mainContent = new HBox(UIConstants.MAIN_CONTENT_SPACING);
        mainContent.setAlignment(Pos.TOP_CENTER);

        VBox transcriptionPanel = createTranscriptionPanel(useRealTimeTranscription);
        transcriptionPanel.setPrefWidth(UIConstants.PANEL_WIDTH);

        VBox chatPanel = createChatPanel();
        chatPanel.setPrefWidth(UIConstants.PANEL_WIDTH);

        mainContent.getChildren().addAll(transcriptionPanel, chatPanel);

        return mainContent;
    }

    private VBox createTranscriptionPanel(boolean useRealTimeTranscription) {
        VBox panel = new VBox(UIConstants.PANEL_SPACING);

        HBox header = new HBox(UIConstants.CONTROL_SPACING);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label(UIConstants.TRANSCRIPT_HEADER);
        headerLabel.setStyle(UIConstants.HEADER_STYLE);

        transcriptionStatusLabel = new Label("");
        transcriptionStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        realTimeCheckBox = new CheckBox("Real-time");
        realTimeCheckBox.setSelected(useRealTimeTranscription);
        realTimeCheckBox.setStyle("-fx-font-size: 12px;");

        header.getChildren().addAll(headerLabel, transcriptionStatusLabel, realTimeCheckBox);

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

        transcriptionArea = new TextArea();
        transcriptionArea.setPromptText(UIConstants.TRANSCRIPT_PLACEHOLDER_IDLE);
        transcriptionArea.setPrefHeight(UIConstants.TRANSCRIPT_AREA_HEIGHT);
        transcriptionArea.setWrapText(true);
        transcriptionArea.setStyle(UIConstants.TEXT_AREA_STYLE);

        panel.getChildren().addAll(header, controls, transcriptionArea);
        panel.setStyle(UIConstants.PANEL_STYLE);

        return panel;
    }

    private VBox createChatPanel() {
        VBox panel = new VBox(UIConstants.PANEL_SPACING);

        HBox header = new HBox(UIConstants.CONTROL_SPACING);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label(UIConstants.CHAT_HEADER);
        headerLabel.setStyle(UIConstants.HEADER_STYLE);

        chatStatusLabel = new Label("");
        chatStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        header.getChildren().addAll(headerLabel, chatStatusLabel);

        chatArea = new TextArea();
        chatArea.setPromptText(UIConstants.CHAT_PLACEHOLDER);
        chatArea.setPrefHeight(UIConstants.CHAT_AREA_HEIGHT);
        chatArea.setWrapText(true);
        chatArea.setEditable(false);
        chatArea.setStyle(UIConstants.TEXT_AREA_STYLE);

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
    public ComboBox<AudioRecordingService.AudioSource> getAudioSourceComboBox() { return audioSourceComboBox; }
    public Button getRefreshSourcesButton() { return refreshSourcesButton; }
}