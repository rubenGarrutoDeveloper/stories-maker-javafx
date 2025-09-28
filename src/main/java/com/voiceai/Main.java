package com.voiceai;

import com.voiceai.model.Conversation;
import com.voiceai.service.AudioRecordingService;
import com.voiceai.service.FileOperationsService;
import com.voiceai.service.NotificationService;
import com.voiceai.service.OpenAIService;
import com.voiceai.service.RealTimeTranscriptionService;
import com.voiceai.service.SettingsService;
import com.voiceai.state.ApplicationState;
import com.voiceai.ui.UIConstants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main extends Application {

    // Services
    private OpenAIService openAIService;
    private AudioRecordingService audioRecordingService;
    private RealTimeTranscriptionService realTimeTranscriptionService;
    private NotificationService notificationService;
    private FileOperationsService fileOperationsService;
    private SettingsService settingsService;

    // Application State
    private ApplicationState appState;

    // Conversation Management
    private Conversation currentConversation;

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
    private Label chatStatusLabel;
    private Label tokenCounterLabel;
    private Label transcriptionStatusLabel;

    // Real-time transcription state
    private CheckBox realTimeCheckBox;
    private boolean useRealTimeTranscription = true;

    // Reference to primary stage for file dialogs
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initialize services and state
        notificationService = new NotificationService();
        fileOperationsService = new FileOperationsService(notificationService);
        settingsService = new SettingsService(notificationService);
        openAIService = new OpenAIService();
        audioRecordingService = new AudioRecordingService();
        realTimeTranscriptionService = new RealTimeTranscriptionService(audioRecordingService, openAIService);
        appState = new ApplicationState();
        currentConversation = new Conversation("StorieS Maker Chat");

        // Load settings and apply to UI
        loadApplicationSettings();

        // Set up state change listener
        appState.addStateChangeListener(this::onStateChanged);

        primaryStage.setTitle(UIConstants.APP_TITLE);

        // Apply window geometry from settings
        applyWindowGeometry(primaryStage);

        // Create main layout
        VBox root = new VBox(UIConstants.ROOT_SPACING);
        root.setPadding(new Insets(UIConstants.ROOT_PADDING));
        root.setStyle(UIConstants.ROOT_STYLE);

        // API Configuration Section
        VBox apiSection = createApiSection();

        // Main Content - Two panels
        HBox mainContent = createMainContent();

        root.getChildren().addAll(apiSection, mainContent);

        Scene scene = new Scene(root, settingsService.getWindowWidth(), settingsService.getWindowHeight());
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();

        // Initialize event handlers
        setupEventHandlers();

        // Initial UI state update
        updateUIState();

        // Show startup notification
        notificationService.showInfo("StorieS Maker initialized successfully");

        // Save window geometry on close
        primaryStage.setOnCloseRequest(event -> {
            saveWindowGeometry();
            shutdown();
        });
    }

    /**
     * Loads application settings and applies them to the UI
     */
    private void loadApplicationSettings() {
        // Load API key if available
        String apiKey = settingsService.getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            openAIService.setApiKey(apiKey);
            appState.setApiKeyValid(true);
            appState.setConnectionState(ApplicationState.ConnectionState.CONNECTED, "API key loaded from settings");
        }

        // Load real-time transcription preference
        useRealTimeTranscription = settingsService.isRealTimeTranscriptionEnabled();

        notificationService.showInfo("Settings loaded from: " + settingsService.getConfigurationFileLocation());
    }

    /**
     * Applies window geometry from settings
     */
    private void applyWindowGeometry(Stage stage) {
        double x = settingsService.getWindowX();
        double y = settingsService.getWindowY();

        if (x >= 0 && y >= 0) {
            stage.setX(x);
            stage.setY(y);
        }
    }

    /**
     * Saves current window geometry to settings
     */
    private void saveWindowGeometry() {
        if (primaryStage != null) {
            settingsService.saveWindowGeometry(
                    primaryStage.getX(),
                    primaryStage.getY(),
                    primaryStage.getWidth(),
                    primaryStage.getHeight()
            );
        }
    }

    /**
     * Handles application state changes and updates UI accordingly
     */
    private void onStateChanged(ApplicationState state) {
        Platform.runLater(this::updateUIState);
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

        // Pre-populate API key field if available (masked for security)
        String existingApiKey = settingsService.getApiKey();
        if (existingApiKey != null && !existingApiKey.trim().isEmpty()) {
            // Show masked version for security
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

        // Header with status
        HBox header = new HBox(UIConstants.CONTROL_SPACING);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label(UIConstants.TRANSCRIPT_HEADER);
        headerLabel.setStyle(UIConstants.HEADER_STYLE);

        transcriptionStatusLabel = new Label("");
        transcriptionStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        // Real-time transcription checkbox - load from settings
        realTimeCheckBox = new CheckBox("Real-time");
        realTimeCheckBox.setSelected(useRealTimeTranscription);
        realTimeCheckBox.setStyle("-fx-font-size: 12px;");
        realTimeCheckBox.setOnAction(e -> {
            useRealTimeTranscription = realTimeCheckBox.isSelected();
            // Save preference to settings
            settingsService.setRealTimeTranscriptionEnabled(useRealTimeTranscription);
        });

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

    private void setupEventHandlers() {
        // Test Connection button - now integrates with SettingsService
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

    // Event handler methods - now using SettingsService
    private void testApiConnection() {
        String inputApiKey = apiKeyField.getText().trim();

        // Handle masked API key field
        final String apiKey;
        if (inputApiKey.contains("•") && settingsService.hasApiKey()) {
            // User hasn't changed the field, use existing API key
            apiKey = settingsService.getApiKey();
        } else {
            apiKey = inputApiKey;
        }

        if (apiKey.isEmpty()) {
            appState.setConnectionState(ApplicationState.ConnectionState.ERROR, "Please enter an API key");
            return;
        }

        // Disable button and show loading state
        testConnectionButton.setDisable(true);
        testConnectionButton.setText(UIConstants.TESTING_TEXT);
        appState.setConnectionState(ApplicationState.ConnectionState.TESTING, UIConstants.TESTING_TEXT);

        // Validate API key asynchronously
        openAIService.validateApiKey(apiKey)
                .thenAccept(result -> {
                    // Update UI on JavaFX Application Thread
                    Platform.runLater(() -> {
                        if (result.isValid()) {
                            // Store the valid API key in both OpenAI service and settings
                            openAIService.setApiKey(apiKey);
                            settingsService.setApiKey(apiKey);
                            appState.setApiKeyValid(true);
                            appState.setConnectionState(ApplicationState.ConnectionState.CONNECTED, "Connected");

                            // Update UI to show masked key
                            apiKeyField.setText("•".repeat(Math.min(apiKey.length(), 32)));
                            apiKeyField.setPromptText("API key saved");

                            notificationService.showSuccess("API Key validated and saved!");
                        } else {
                            appState.setConnectionState(ApplicationState.ConnectionState.ERROR, result.getMessage());
                            notificationService.showError("API Key validation failed", new RuntimeException(result.getMessage()));
                        }

                        // Re-enable button
                        testConnectionButton.setDisable(false);
                        testConnectionButton.setText(UIConstants.TEST_CONNECTION_TEXT);
                    });
                })
                .exceptionally(throwable -> {
                    // Handle any unexpected errors
                    Platform.runLater(() -> {
                        appState.setConnectionState(ApplicationState.ConnectionState.ERROR,
                                "Validation failed: " + throwable.getMessage());
                        notificationService.showError("Connection test failed", throwable);
                        testConnectionButton.setDisable(false);
                        testConnectionButton.setText(UIConstants.TEST_CONNECTION_TEXT);
                    });
                    return null;
                });
    }

    /**
     * Updates the connection status label based on application state
     */
    private void updateConnectionStatus() {
        String message = appState.getConnectionMessage();
        String color;

        switch (appState.getConnectionState()) {
            case CONNECTED:
                color = UIConstants.SUCCESS_COLOR;
                break;
            case TESTING:
                color = UIConstants.WARNING_COLOR;
                break;
            case ERROR:
                color = UIConstants.ERROR_COLOR;
                break;
            case DISCONNECTED:
            default:
                color = UIConstants.ERROR_COLOR;
                break;
        }

        connectionStatus.setText(message);
        connectionStatus.setStyle(UIConstants.createStatusStyle(color));
    }

    /**
     * Updates the chat status label based on application state
     */
    private void updateChatStatus() {
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

        chatStatusLabel.setText(message);
        chatStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
    }

    /**
     * Updates the token counter
     */
    private void updateTokenCounter() {
        int tokens = appState.getTokensUsedInSession();
        tokenCounterLabel.setText("Tokens: " + tokens);

        // Change color based on usage
        String color = "#666";
        if (tokens > 5000) {
            color = UIConstants.ERROR_COLOR;
        } else if (tokens > 2000) {
            color = UIConstants.WARNING_COLOR;
        } else if (tokens > 0) {
            color = UIConstants.SUCCESS_COLOR;
        }

        tokenCounterLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
    }

    /**
     * Updates UI components based on current application state
     */
    private void updateUIState() {
        // Update connection status
        updateConnectionStatus();

        // Update chat status
        updateChatStatus();

        // Update token counter
        updateTokenCounter();

        // Enable/disable recording button based on API key validity
        recButton.setDisable(!appState.isApiKeyValid());

        // Update button style based on API key validity
        if (appState.isApiKeyValid()) {
            recButton.setStyle(UIConstants.PRIMARY_BUTTON_STYLE);
        } else {
            recButton.setStyle(UIConstants.DISABLED_BUTTON_STYLE);
        }

        // Enable/disable send button based on API key validity and chat state
        boolean canSendMessage = appState.isApiKeyValid() && !appState.isChatBusy();
        sendButton.setDisable(!canSendMessage);
        messageField.setDisable(appState.isChatBusy());

        if (canSendMessage) {
            sendButton.setStyle(UIConstants.SEND_BUTTON_STYLE);
        } else {
            sendButton.setStyle(UIConstants.createButtonStyle(UIConstants.GRAY_COLOR));
        }

        // Update recording UI based on recording state
        updateRecordingUI();
    }

    private void toggleRecording() {
        if (appState.isRecordingIdle()) {
            startRecording();
        } else if (appState.isRecording()) {
            stopRecording();
        }
    }

    private void startRecording() {
        // Check microphone availability
        if (!AudioRecordingService.isMicrophoneAvailable()) {
            notificationService.showError("Microphone not available");
            return;
        }

        // Update UI immediately
        appState.setRecordingState(ApplicationState.RecordingState.RECORDING);
        updateTranscriptionStatus("Starting recording...");

        // Start recording and capture audio
        audioRecordingService.startRecording()
                .thenCompose(v -> {
                    // If real-time transcription is enabled, start it
                    if (useRealTimeTranscription) {
                        Platform.runLater(() -> {
                            updateTranscriptionStatus("Real-time transcription active");
                            realTimeTranscriptionService.startRealTimeTranscription(
                                    this::onRealTimeTranscriptionReceived,
                                    this::onRealTimeTranscriptionError
                            );
                        });
                    } else {
                        Platform.runLater(() -> updateTranscriptionStatus("Recording..."));
                    }

                    // Start capturing audio data
                    return audioRecordingService.captureAudio();
                })
                .thenAccept(audioData -> {
                    // This is called when recording completes
                    Platform.runLater(() -> {
                        if (!useRealTimeTranscription) {
                            // Process the entire audio at once if not using real-time
                            updateTranscriptionStatus("Processing final transcription...");
                            processAudioData(audioData);
                        } else {
                            // Real-time transcription already handled the audio
                            updateTranscriptionStatus("Recording completed");
                        }
                        appState.setRecordingState(ApplicationState.RecordingState.IDLE);
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        notificationService.showError("Recording failed", throwable);
                        updateTranscriptionStatus("Recording failed");
                        appState.setRecordingState(ApplicationState.RecordingState.IDLE);
                    });
                    return null;
                });
    }

    private void stopRecording() {
        if (audioRecordingService.isRecording()) {
            // Update UI to show processing state
            appState.setRecordingState(ApplicationState.RecordingState.STOPPING);
            updateTranscriptionStatus("Stopping recording...");

            // Stop real-time transcription if active
            if (realTimeTranscriptionService.isActive()) {
                realTimeTranscriptionService.stopRealTimeTranscription();
            }

            // Stop recording
            audioRecordingService.stopRecording()
                    .thenAccept(audioData -> {
                        Platform.runLater(() -> {
                            if (useRealTimeTranscription) {
                                // Real-time already processed everything
                                int chunks = realTimeTranscriptionService.getChunksProcessed();
                                notificationService.showSuccess("Recording stopped. Processed " + chunks + " chunks");
                                updateTranscriptionStatus("Completed (" + chunks + " chunks)");
                            } else {
                                // Process the entire recording now
                                updateTranscriptionStatus("Processing transcription...");
                                processAudioData(audioData);
                            }
                            appState.setRecordingState(ApplicationState.RecordingState.IDLE);
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            notificationService.showError("Failed to stop recording", throwable);
                            updateTranscriptionStatus("Stop failed");
                            appState.setRecordingState(ApplicationState.RecordingState.IDLE);
                        });
                        return null;
                    });
        }
    }

    /**
     * Handles real-time transcription text chunks
     */
    private void onRealTimeTranscriptionReceived(String text) {
        Platform.runLater(() -> {
            transcriptionArea.appendText(text);
            // Auto-scroll to bottom
            transcriptionArea.setScrollTop(Double.MAX_VALUE);

            // Update status with recording duration
            double duration = audioRecordingService.getCurrentRecordingDuration();
            updateTranscriptionStatus(String.format("Recording... (%.1fs)", duration));
        });
    }

    /**
     * Handles real-time transcription errors
     */
    private void onRealTimeTranscriptionError(String error) {
        Platform.runLater(() -> {
            System.err.println("Real-time transcription error: " + error);
            updateTranscriptionStatus("Error: " + error);
        });
    }

    /**
     * Updates the transcription status label
     */
    private void updateTranscriptionStatus(String status) {
        if (status == null || status.isEmpty()) {
            transcriptionStatusLabel.setText("");
        } else {
            transcriptionStatusLabel.setText("• " + status);
        }
    }

    /**
     * Processes audio data and handles transcription (for non-real-time mode)
     */
    private void processAudioData(byte[] audioData) {
        if (audioData.length > 0) {
            notificationService.showInfo("Processing " + audioData.length + " bytes of audio");
            updateTranscriptionStatus("Transcribing...");

            // Transcribe audio using Whisper API
            openAIService.transcribeAudio(audioData)
                    .thenAccept(result -> {
                        Platform.runLater(() -> {
                            if (result.isSuccess()) {
                                transcriptionArea.appendText(result.getText() + "\n\n");
                                notificationService.showSuccess("Transcription completed!");
                                updateTranscriptionStatus("Transcription complete");
                            } else {
                                transcriptionArea.appendText("[Transcription failed: " + result.getMessage() + "]\n\n");
                                notificationService.showError("Transcription failed", new RuntimeException(result.getMessage()));
                                updateTranscriptionStatus("Transcription failed");
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            transcriptionArea.appendText("[Transcription error: " + throwable.getMessage() + "]\n\n");
                            notificationService.showError("Transcription error", throwable);
                            updateTranscriptionStatus("Error");
                        });
                        return null;
                    });
        } else {
            notificationService.showWarning("No audio data captured");
            updateTranscriptionStatus("");
        }
    }

    private void updateRecordingUI() {
        ApplicationState.RecordingState state = appState.getRecordingState();

        switch (state) {
            case RECORDING:
                recButton.setText(UIConstants.STOP_BUTTON_TEXT);
                recButton.setStyle(UIConstants.DANGER_BUTTON_STYLE);
                recButton.setDisable(false);
                transcriptionArea.setPromptText(UIConstants.TRANSCRIPT_PLACEHOLDER_RECORDING);
                break;

            case STOPPING:
                recButton.setText(UIConstants.STOPPING_BUTTON_TEXT);
                recButton.setStyle(UIConstants.DANGER_BUTTON_STYLE);
                recButton.setDisable(true);
                transcriptionArea.setPromptText(UIConstants.TRANSCRIPT_PLACEHOLDER_RECORDING);
                break;

            case IDLE:
            default:
                recButton.setText(UIConstants.REC_BUTTON_TEXT);
                recButton.setStyle(appState.isApiKeyValid() ? UIConstants.PRIMARY_BUTTON_STYLE : UIConstants.DISABLED_BUTTON_STYLE);
                recButton.setDisable(!appState.isApiKeyValid());
                transcriptionArea.setPromptText(UIConstants.TRANSCRIPT_PLACEHOLDER_IDLE);
                break;
        }
    }

    // File operations now delegated to FileOperationsService
    private void saveTranscription() {
        String transcription = transcriptionArea.getText().trim();
        FileOperationsService.FileOperationResult result =
                fileOperationsService.saveTranscription(transcription, primaryStage);

        // Result handling is done by the service through notifications
        // Additional logic could be added here if needed
    }

    private void selectAllTranscription() {
        fileOperationsService.selectAllText(transcriptionArea);
    }

    private void loadTranscription() {
        FileOperationsService.FileOperationResult result =
                fileOperationsService.loadTranscription(primaryStage);

        if (result.isSuccess()) {
            // The loaded content is stored in the message field of the result
            String loadedContent = result.getMessage();
            transcriptionArea.setText(loadedContent);
        }
        // Error handling is done by the service through notifications
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || !appState.isApiKeyValid() || appState.isChatBusy()) {
            return;
        }

        // Clear the input field immediately
        messageField.clear();

        // Add user message to conversation and display
        currentConversation.addUserMessage(message);
        appendChatMessage("You", message);

        // Update state to sending
        appState.setChatState(ApplicationState.ChatState.SENDING, "Sending message...");

        // Send to OpenAI with streaming
        openAIService.sendStreamingChatCompletion(
                currentConversation,
                this::onStreamingChunk,
                this::onStreamingComplete
        ).exceptionally(throwable -> {
            Platform.runLater(() -> {
                appState.setChatState(ApplicationState.ChatState.ERROR, throwable.getMessage());
                notificationService.showError("Failed to send message", throwable);
            });
            return null;
        });
    }

    /**
     * Handles streaming response chunks
     */
    private void onStreamingChunk(String chunk) {
        Platform.runLater(() -> {
            // Update state to streaming if not already
            if (!appState.isChatStreaming()) {
                appState.setChatState(ApplicationState.ChatState.STREAMING, "Receiving response...");
                // Start the assistant message
                chatArea.appendText("ChatGPT: ");
            }

            // Append the chunk to chat area
            chatArea.appendText(chunk);

            // Auto-scroll to bottom
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    /**
     * Handles completion of streaming response
     */
    private void onStreamingComplete(OpenAIService.ChatCompletionResult result) {
        Platform.runLater(() -> {
            if (result.isSuccess()) {
                // Add assistant message to conversation
                currentConversation.addAssistantMessage(result.getContent());

                // Add newlines for formatting
                chatArea.appendText("\n\n");

                // Update token usage
                appState.addTokensUsed(result.getTokensUsed());

                // Reset state to idle
                appState.setChatState(ApplicationState.ChatState.IDLE, "");

                notificationService.showSuccess("Response received (" + result.getTokensUsed() + " tokens)");
            } else {
                // Handle error
                appState.setChatState(ApplicationState.ChatState.ERROR, result.getMessage());
                chatArea.appendText("\n[Error: " + result.getMessage() + "]\n\n");
                notificationService.showError("Chat error", new RuntimeException(result.getMessage()));
            }

            // Auto-scroll to bottom
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    /**
     * Appends a message to the chat area with proper formatting
     */
    private void appendChatMessage(String sender, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        chatArea.appendText(sender + " (" + timestamp + "): " + message + "\n\n");
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    private void insertTranscript() {
        String transcript = transcriptionArea.getText().trim();
        if (!transcript.isEmpty()) {
            // If there's already text in the message field, add a space
            String currentText = messageField.getText();
            if (!currentText.isEmpty()) {
                messageField.setText(currentText + " " + transcript);
            } else {
                messageField.setText(transcript);
            }
            messageField.requestFocus();
            messageField.positionCaret(messageField.getText().length());
        }
    }

    private void clearChat() {
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Chat");
        alert.setHeaderText("Clear conversation history?");
        alert.setContentText("This will clear all messages in the current conversation. This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                chatArea.clear();
                currentConversation.clearConversation();
                appState.resetTokenCounter();
                appState.setChatState(ApplicationState.ChatState.IDLE, "");
                notificationService.showSuccess("Chat cleared");
            }
        });
    }

    /**
     * Cleanup resources on application shutdown
     */
    private void shutdown() {
        // Stop any active recording
        if (audioRecordingService.isRecording()) {
            audioRecordingService.forceStop();
        }

        // Stop real-time transcription service
        if (realTimeTranscriptionService != null) {
            realTimeTranscriptionService.shutdown();
        }

        notificationService.showInfo("Application shutdown complete");
        System.out.println("Application shutdown complete");
    }

    public static void main(String[] args) {
        launch(args);
    }
}