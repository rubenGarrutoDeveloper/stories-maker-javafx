package com.voiceai;

import com.voiceai.service.AudioRecordingService;
import com.voiceai.service.AudioService;
import com.voiceai.service.ChatService;
import com.voiceai.service.FileOperationsService;
import com.voiceai.service.NotificationService;
import com.voiceai.service.OpenAIService;
import com.voiceai.service.RealTimeTranscriptionService;
import com.voiceai.service.SettingsService;
import com.voiceai.service.TranscriptionService;
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

public class Main extends Application {

    // Services
    private OpenAIService openAIService;
    private AudioRecordingService audioRecordingService;
    private RealTimeTranscriptionService realTimeTranscriptionService;
    private NotificationService notificationService;
    private FileOperationsService fileOperationsService;
    private SettingsService settingsService;
    private AudioService audioService;
    private TranscriptionService transcriptionService;
    private ChatService chatService;

    // Application State
    private ApplicationState appState;

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

        // Initialize AudioService
        audioService = new AudioService(
                audioRecordingService,
                realTimeTranscriptionService,
                openAIService,
                notificationService,
                appState
        );

        // Initialize TranscriptionService
        transcriptionService = new TranscriptionService(
                audioService,
                settingsService,
                appState,
                notificationService
        );

        // Initialize ChatService
        chatService = new ChatService(
                openAIService,
                notificationService,
                appState
        );

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

        // Initialize services with UI components
        initializeServices();

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
     * Initializes services with UI components
     */
    private void initializeServices() {
        transcriptionService.initializeUI(recButton, transcriptionStatusLabel, transcriptionArea);
        transcriptionService.updateApiKeyState(appState.isApiKeyValid());

        chatService.initializeUI(chatArea, messageField, sendButton, chatStatusLabel);
        chatService.updateApiKeyState(appState.isApiKeyValid());
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
        Platform.runLater(() -> {
            updateUIState();
            // Notify services of API key state changes
            transcriptionService.updateApiKeyState(state.isApiKeyValid());
            chatService.updateApiKeyState(state.isApiKeyValid());
        });
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

        // Real-time transcription checkbox
        realTimeCheckBox = new CheckBox("Real-time");
        realTimeCheckBox.setSelected(useRealTimeTranscription);
        realTimeCheckBox.setStyle("-fx-font-size: 12px;");
        realTimeCheckBox.setOnAction(e -> {
            useRealTimeTranscription = realTimeCheckBox.isSelected();
            transcriptionService.setRealTimeTranscriptionEnabled(useRealTimeTranscription);
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
        // Test Connection button
        testConnectionButton.setOnAction(e -> testApiConnection());

        // Recording button is handled by TranscriptionService
        // Send message button is handled by ChatService

        // Save button
        saveButton.setOnAction(e -> saveTranscription());

        // Select All button
        selectAllButton.setOnAction(e -> selectAllTranscription());

        // Load button
        loadButton.setOnAction(e -> loadTranscription());

        // Insert transcript button - uses both services
        insertTranscriptButton.setOnAction(e -> insertTranscript());

        // Clear chat button - delegates to ChatService
        clearButton.setOnAction(e -> chatService.clearChat());
    }

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

        // Update token counter
        updateTokenCounter();

        // Chat and transcription UI updates are now handled by their respective services
    }

    // File operations delegated to services
    private void saveTranscription() {
        String transcription = transcriptionService.getTranscriptionText().trim();
        fileOperationsService.saveTranscription(transcription, primaryStage);
    }

    private void selectAllTranscription() {
        transcriptionService.selectAllText();
    }

    private void loadTranscription() {
        FileOperationsService.FileOperationResult result =
                fileOperationsService.loadTranscription(primaryStage);

        if (result.isSuccess()) {
            String loadedContent = result.getMessage();
            transcriptionService.setTranscriptionText(loadedContent);
        }
    }

    private void insertTranscript() {
        String transcript = transcriptionService.getTranscriptionText().trim();
        chatService.insertTextToMessageField(transcript);
    }

    /**
     * Cleanup resources on application shutdown
     */
    private void shutdown() {
        // Stop audio service
        if (audioService != null) {
            audioService.shutdown();
        }

        // Stop transcription service
        if (transcriptionService != null) {
            transcriptionService.shutdown();
        }

        // Stop chat service
        if (chatService != null) {
            chatService.shutdown();
        }

        notificationService.showInfo("Application shutdown complete");
        System.out.println("Application shutdown complete");
    }

    public static void main(String[] args) {
        launch(args);
    }
}