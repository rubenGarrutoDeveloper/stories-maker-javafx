package com.voiceai;

import com.voiceai.service.AudioRecordingService;
import com.voiceai.service.OpenAIService;
import com.voiceai.ui.UIConstants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Main extends Application {

    // Services
    private OpenAIService openAIService;
    private AudioRecordingService audioRecordingService;

    // UI Components
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

    // State
    private boolean isApiKeyValid = false;
    private boolean isCurrentlyRecording = false;

    @Override
    public void start(Stage primaryStage) {
        // Initialize services
        openAIService = new OpenAIService();
        audioRecordingService = new AudioRecordingService();

        primaryStage.setTitle(UIConstants.APP_TITLE);

        // Create main layout
        VBox root = new VBox(UIConstants.ROOT_SPACING);
        root.setPadding(new Insets(UIConstants.ROOT_PADDING));
        root.setStyle(UIConstants.ROOT_STYLE);

        // API Configuration Section
        VBox apiSection = createApiSection();

        // Main Content - Two panels
        HBox mainContent = createMainContent();

        root.getChildren().addAll(apiSection, mainContent);

        Scene scene = new Scene(root, UIConstants.MAIN_WINDOW_WIDTH, UIConstants.MAIN_WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();

        // Initialize event handlers
        setupEventHandlers();

        // Update UI state
        updateUIState();
    }

    private VBox createApiSection() {
        VBox apiSection = new VBox(UIConstants.SECTION_SPACING);

        Label titleLabel = new Label(UIConstants.APP_TITLE);
        titleLabel.setStyle(UIConstants.TITLE_STYLE);

        HBox apiKeyBox = new HBox(UIConstants.CONTROL_SPACING);
        apiKeyBox.setAlignment(Pos.CENTER_LEFT);

        apiKeyField = new TextField();
        apiKeyField.setPromptText(UIConstants.API_KEY_PROMPT);
        apiKeyField.setPrefWidth(UIConstants.API_KEY_FIELD_WIDTH);
        apiKeyField.setStyle(UIConstants.INPUT_FIELD_STYLE);

        testConnectionButton = new Button(UIConstants.TEST_CONNECTION_TEXT);
        testConnectionButton.setStyle(UIConstants.LIGHT_BUTTON_STYLE);

        connectionStatus = new Label(UIConstants.DISCONNECTED_STATUS);
        connectionStatus.setStyle(UIConstants.ERROR_STATUS_STYLE);

        apiKeyBox.getChildren().addAll(apiKeyField, testConnectionButton, connectionStatus);
        apiSection.getChildren().addAll(titleLabel, apiKeyBox);

        return apiSection;
    }

    private HBox createMainContent() {
        HBox mainContent = new HBox(UIConstants.MAIN_CONTENT_SPACING);
        mainContent.setAlignment(Pos.TOP_CENTER);

        // Left Panel - Transcription
        VBox transcriptionPanel = createTranscriptionPanel();
        transcriptionPanel.setPrefWidth(UIConstants.PANEL_WIDTH);

        // Right Panel - Chat GPT
        VBox chatPanel = createChatPanel();
        chatPanel.setPrefWidth(UIConstants.PANEL_WIDTH);

        mainContent.getChildren().addAll(transcriptionPanel, chatPanel);

        return mainContent;
    }

    private VBox createTranscriptionPanel() {
        VBox panel = new VBox(UIConstants.PANEL_SPACING);

        // Header
        HBox header = new HBox(UIConstants.CONTROL_SPACING);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label(UIConstants.TRANSCRIPT_HEADER);
        headerLabel.setStyle(UIConstants.HEADER_STYLE);
        header.getChildren().add(headerLabel);

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

    private VBox createChatPanel() {
        VBox panel = new VBox(UIConstants.PANEL_SPACING);

        // Header
        HBox header = new HBox(UIConstants.CONTROL_SPACING);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label(UIConstants.CHAT_HEADER);
        headerLabel.setStyle(UIConstants.HEADER_STYLE);
        header.getChildren().add(headerLabel);

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

    private void setupEventHandlers() {
        // Test Connection button
        testConnectionButton.setOnAction(e -> testApiConnection());

        // Recording button
        recButton.setOnAction(e -> toggleRecording());

        // Save button
        saveButton.setOnAction(e -> saveTranscription());

        // Select All button
        selectAllButton.setOnAction(e -> selectAllTranscription());

        // Load button
        loadButton.setOnAction(e -> loadTranscription());

        // Send message button
        sendButton.setOnAction(e -> sendMessage());

        // Insert transcript button
        insertTranscriptButton.setOnAction(e -> insertTranscript());

        // Clear chat button
        clearButton.setOnAction(e -> clearChat());

        // Enter key for message field
        messageField.setOnAction(e -> sendMessage());
    }

    // Event handler methods
    private void testApiConnection() {
        String apiKey = apiKeyField.getText().trim();

        if (apiKey.isEmpty()) {
            updateConnectionStatus(false, "Please enter an API key");
            return;
        }

        // Disable button and show loading state
        testConnectionButton.setDisable(true);
        testConnectionButton.setText(UIConstants.TESTING_TEXT);
        updateConnectionStatus(false, UIConstants.TESTING_TEXT, UIConstants.WARNING_COLOR);

        // Validate API key asynchronously
        openAIService.validateApiKey(apiKey)
                .thenAccept(result -> {
                    // Update UI on JavaFX Application Thread
                    Platform.runLater(() -> {
                        isApiKeyValid = result.isValid();

                        if (result.isValid()) {
                            // Store the valid API key
                            openAIService.setApiKey(apiKey);
                            updateConnectionStatus(true, result.getMessage());
                            showNotification("API Key validated successfully!", "SUCCESS");
                        } else {
                            updateConnectionStatus(false, result.getMessage());
                            showNotification("API Key validation failed: " + result.getMessage(), "ERROR");
                        }

                        // Re-enable button and update UI state
                        testConnectionButton.setDisable(false);
                        testConnectionButton.setText(UIConstants.TEST_CONNECTION_TEXT);
                        updateUIState();
                    });
                })
                .exceptionally(throwable -> {
                    // Handle any unexpected errors
                    Platform.runLater(() -> {
                        updateConnectionStatus(false, "Validation failed: " + throwable.getMessage());
                        showNotification("Connection test failed", "ERROR");
                        testConnectionButton.setDisable(false);
                        testConnectionButton.setText(UIConstants.TEST_CONNECTION_TEXT);
                    });
                    return null;
                });
    }

    /**
     * Updates the connection status label
     */
    private void updateConnectionStatus(boolean isConnected, String message) {
        updateConnectionStatus(isConnected, message, null);
    }

    /**
     * Updates the connection status label with custom color
     */
    private void updateConnectionStatus(boolean isConnected, String message, String customColor) {
        String color = customColor;
        if (color == null) {
            color = isConnected ? UIConstants.SUCCESS_COLOR : UIConstants.ERROR_COLOR;
        }

        connectionStatus.setText(message);
        connectionStatus.setStyle(UIConstants.createStatusStyle(color));
    }

    /**
     * Shows a temporary notification (simple console output for now)
     */
    private void showNotification(String message, String type) {
        System.out.println("[" + type + "] " + message);
        // TODO: Implement proper notification system (toast, alert, etc.)
    }

    /**
     * Updates UI components based on current application state
     */
    private void updateUIState() {
        // Enable/disable recording button based on API key validity
        recButton.setDisable(!isApiKeyValid);

        // Update button style based on API key validity
        if (isApiKeyValid) {
            recButton.setStyle(UIConstants.PRIMARY_BUTTON_STYLE);
        } else {
            recButton.setStyle(UIConstants.DISABLED_BUTTON_STYLE);
        }

        // Enable/disable send button based on API key validity
        sendButton.setDisable(!isApiKeyValid);
        if (isApiKeyValid) {
            sendButton.setStyle(UIConstants.SEND_BUTTON_STYLE);
        } else {
            sendButton.setStyle(UIConstants.createButtonStyle(UIConstants.GRAY_COLOR));
        }
    }

    private void toggleRecording() {
        if (!isCurrentlyRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        // Check microphone availability
        if (!AudioRecordingService.isMicrophoneAvailable()) {
            showNotification("Microphone not available", "ERROR");
            return;
        }

        // Update UI immediately
        isCurrentlyRecording = true;
        updateRecordingUI();

        // Start recording asynchronously
        audioRecordingService.startRecording()
                .thenCompose(v -> {
                    // Start capturing audio data
                    return audioRecordingService.captureAudio();
                })
                .thenAccept(audioData -> {
                    // This will be called when recording stops
                    Platform.runLater(() -> {
                        if (audioData.length > 0) {
                            showNotification("Recording completed: " + audioData.length + " bytes captured", "SUCCESS");
                            transcriptionArea.appendText("[Processing " + audioData.length + " bytes...]\n");

                            // Transcribe audio using Whisper API
                            openAIService.transcribeAudio(audioData)
                                    .thenAccept(result -> {
                                        Platform.runLater(() -> {
                                            if (result.isSuccess()) {
                                                transcriptionArea.appendText(result.getText() + "\n\n");
                                                showNotification("Transcription completed!", "SUCCESS");
                                            } else {
                                                transcriptionArea.appendText("[Transcription failed: " + result.getMessage() + "]\n\n");
                                                showNotification("Transcription failed: " + result.getMessage(), "ERROR");
                                            }
                                        });
                                    })
                                    .exceptionally(throwable -> {
                                        Platform.runLater(() -> {
                                            transcriptionArea.appendText("[Transcription error: " + throwable.getMessage() + "]\n\n");
                                            showNotification("Transcription error", "ERROR");
                                        });
                                        return null;
                                    });
                        } else {
                            showNotification("No audio data captured", "WARNING");
                        }

                        isCurrentlyRecording = false;
                        updateRecordingUI();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showNotification("Recording failed: " + throwable.getMessage(), "ERROR");
                        isCurrentlyRecording = false;
                        updateRecordingUI();
                    });
                    return null;
                });
    }

    private void stopRecording() {
        if (audioRecordingService.isRecording()) {
            // Update UI to show processing state
            recButton.setText(UIConstants.STOPPING_BUTTON_TEXT);
            recButton.setDisable(true);

            audioRecordingService.stopRecording()
                    .thenAccept(audioData -> {
                        Platform.runLater(() -> {
                            if (audioData.length > 0) {
                                showNotification("Recording stopped: " + audioData.length + " bytes captured", "SUCCESS");
                                transcriptionArea.appendText("[Processing " + audioData.length + " bytes...]\n");

                                // Transcribe audio using Whisper API
                                openAIService.transcribeAudio(audioData)
                                        .thenAccept(result -> {
                                            Platform.runLater(() -> {
                                                if (result.isSuccess()) {
                                                    transcriptionArea.appendText(result.getText() + "\n\n");
                                                    showNotification("Transcription completed!", "SUCCESS");
                                                } else {
                                                    transcriptionArea.appendText("[Transcription failed: " + result.getMessage() + "]\n\n");
                                                    showNotification("Transcription failed: " + result.getMessage(), "ERROR");
                                                }
                                            });
                                        })
                                        .exceptionally(throwable -> {
                                            Platform.runLater(() -> {
                                                transcriptionArea.appendText("[Transcription error: " + throwable.getMessage() + "]\n\n");
                                                showNotification("Transcription error", "ERROR");
                                            });
                                            return null;
                                        });
                            }

                            isCurrentlyRecording = false;
                            updateRecordingUI();
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            showNotification("Failed to stop recording: " + throwable.getMessage(), "ERROR");
                            isCurrentlyRecording = false;
                            updateRecordingUI();
                        });
                        return null;
                    });
        }
    }

    private void updateRecordingUI() {
        if (isCurrentlyRecording) {
            recButton.setText(UIConstants.STOP_BUTTON_TEXT);
            recButton.setStyle(UIConstants.DANGER_BUTTON_STYLE);
            transcriptionArea.setPromptText(UIConstants.TRANSCRIPT_PLACEHOLDER_RECORDING);
        } else {
            recButton.setText(UIConstants.REC_BUTTON_TEXT);
            recButton.setStyle(UIConstants.PRIMARY_BUTTON_STYLE);
            transcriptionArea.setPromptText(UIConstants.TRANSCRIPT_PLACEHOLDER_IDLE);
            recButton.setDisable(false);
        }
    }

    private void saveTranscription() {
        // TODO: Implement save functionality
        System.out.println("Saving transcription...");
    }

    private void selectAllTranscription() {
        transcriptionArea.selectAll();
    }

    private void loadTranscription() {
        // TODO: Implement load functionality
        System.out.println("Loading transcription...");
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // TODO: Implement ChatGPT integration
            chatArea.appendText("You: " + message + "\n\n");
            messageField.clear();
            System.out.println("Sending message: " + message);
        }
    }

    private void insertTranscript() {
        String transcript = transcriptionArea.getText();
        if (!transcript.isEmpty()) {
            messageField.setText(transcript);
        }
    }

    private void clearChat() {
        chatArea.clear();
    }

    public static void main(String[] args) {
        launch(args);
    }
}