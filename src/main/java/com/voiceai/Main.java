package com.voiceai;

import com.voiceai.service.*;
import com.voiceai.state.ApplicationState;
import com.voiceai.ui.UIConstants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    // Services
    private NotificationService notificationService;
    private FileOperationsService fileOperationsService;
    private SettingsService settingsService;
    private OpenAIService openAIService;
    private AudioRecordingService audioRecordingService;
    private RealTimeTranscriptionService realTimeTranscriptionService;
    private AudioService audioService;
    private TranscriptionService transcriptionService;
    private ChatService chatService;
    private UIService uiService;

    // Application State
    private ApplicationState appState;

    // Reference to primary stage
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initialize all services
        initializeServices();

        // Load application settings
        loadApplicationSettings();

        // Set up state change listener
        appState.addStateChangeListener(this::onStateChanged);

        // Apply window geometry and title
        primaryStage.setTitle(UIConstants.APP_TITLE);
        applyWindowGeometry(primaryStage);

        // Create and show UI
        Scene scene = uiService.createAndShowUI(
                primaryStage,
                settingsService.getWindowWidth(),
                settingsService.getWindowHeight(),
                settingsService.getApiKey(),
                settingsService.isRealTimeTranscriptionEnabled()
        );

        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();

        // Initialize services with UI components
        initializeServiceUI();

        // Set up event handlers
        setupEventHandlers();

        // Show startup notification
        notificationService.showInfo("StorieS Maker initialized successfully");

        // Save window geometry on close
        primaryStage.setOnCloseRequest(event -> {
            saveWindowGeometry();
            shutdown();
        });
    }

    /**
     * Initializes all application services
     */
    private void initializeServices() {
        notificationService = new NotificationService();
        fileOperationsService = new FileOperationsService(notificationService);
        settingsService = new SettingsService(notificationService);
        openAIService = new OpenAIService();
        audioRecordingService = new AudioRecordingService();
        realTimeTranscriptionService = new RealTimeTranscriptionService(audioRecordingService, openAIService);
        appState = new ApplicationState();
        audioService = new AudioService(audioRecordingService, realTimeTranscriptionService, openAIService, notificationService, appState);
        transcriptionService = new TranscriptionService(audioService, settingsService, appState, notificationService);
        chatService = new ChatService(openAIService, notificationService, appState);
        uiService = new UIService();
    }

    /**
     * Initializes services with UI components
     */
    private void initializeServiceUI() {
        transcriptionService.initializeUI(
                uiService.getRecButton(),
                uiService.getTranscriptionStatusLabel(),
                uiService.getTranscriptionArea()
        );

        transcriptionService.updateApiKeyState(appState.isApiKeyValid());

        chatService.initializeUI(
                uiService.getChatArea(),
                uiService.getMessageField(),
                uiService.getSendButton(),
                uiService.getChatStatusLabel()
        );

        chatService.updateApiKeyState(appState.isApiKeyValid());
    }

    /**
     * Loads and applies application settings
     */
    private void loadApplicationSettings() {
        String apiKey = settingsService.getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            openAIService.setApiKey(apiKey);
            appState.setApiKeyValid(true);
            appState.setConnectionState(ApplicationState.ConnectionState.CONNECTED,
                    "API key loaded from settings");
        }

        notificationService.showInfo("Settings loaded from: " +
                settingsService.getConfigurationFileLocation());
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
     * Saves current window geometry
     */
    private void saveWindowGeometry() {
        if (primaryStage != null) {
            settingsService.saveWindowGeometry(
                    primaryStage.getX(), primaryStage.getY(),
                    primaryStage.getWidth(), primaryStage.getHeight()
            );
        }
    }

    /**
     * Sets up all event handlers
     */
    private void setupEventHandlers() {
        uiService.getTestConnectionButton().setOnAction(e -> testApiConnection());
        uiService.getSaveButton().setOnAction(e -> saveTranscription());
        uiService.getSelectAllButton().setOnAction(e -> transcriptionService.selectAllText());
        uiService.getLoadButton().setOnAction(e -> loadTranscription());
        uiService.getInsertTranscriptButton().setOnAction(e -> insertTranscript());
        uiService.getClearButton().setOnAction(e -> chatService.clearChat());
        uiService.getRealTimeCheckBox().setOnAction(e -> transcriptionService.setRealTimeTranscriptionEnabled(uiService.getRealTimeCheckBox().isSelected()));
    }

    /**
     * Handles application state changes
     */
    private void onStateChanged(ApplicationState state) {
        Platform.runLater(() -> {
            updateConnectionStatus();
            updateTokenCounter();
            transcriptionService.updateApiKeyState(state.isApiKeyValid());
            chatService.updateApiKeyState(state.isApiKeyValid());
        });
    }

    /**
     * Tests API connection
     */
    private void testApiConnection() {
        String inputApiKey = uiService.getApiKeyField().getText().trim();

        final String apiKey = inputApiKey.contains("•") && settingsService.hasApiKey()
                ? settingsService.getApiKey() : inputApiKey;

        if (apiKey.isEmpty()) {
            appState.setConnectionState(ApplicationState.ConnectionState.ERROR, "Please enter an API key");
            return;
        }

        uiService.getTestConnectionButton().setDisable(true);
        uiService.getTestConnectionButton().setText(UIConstants.TESTING_TEXT);
        appState.setConnectionState(ApplicationState.ConnectionState.TESTING, UIConstants.TESTING_TEXT);

        openAIService.validateApiKey(apiKey).thenAccept(result -> {
            Platform.runLater(() -> {
                if (result.isValid()) {
                    openAIService.setApiKey(apiKey);
                    settingsService.setApiKey(apiKey);
                    appState.setApiKeyValid(true);
                    appState.setConnectionState(ApplicationState.ConnectionState.CONNECTED, "Connected");
                    uiService.getApiKeyField().setText("•".repeat(Math.min(apiKey.length(), 32)));
                    uiService.getApiKeyField().setPromptText("API key saved");
                    notificationService.showSuccess("API Key validated and saved!");
                } else {
                    appState.setConnectionState(ApplicationState.ConnectionState.ERROR, result.getMessage());
                    notificationService.showError("API Key validation failed",
                            new RuntimeException(result.getMessage()));
                }
                uiService.getTestConnectionButton().setDisable(false);
                uiService.getTestConnectionButton().setText(UIConstants.TEST_CONNECTION_TEXT);
            });
        });
    }

    private void updateConnectionStatus() {
        String message = appState.getConnectionMessage();
        String color = switch (appState.getConnectionState()) {
            case CONNECTED -> UIConstants.SUCCESS_COLOR;
            case TESTING -> UIConstants.WARNING_COLOR;
            default -> UIConstants.ERROR_COLOR;
        };
        uiService.getConnectionStatus().setText(message);
        uiService.getConnectionStatus().setStyle(UIConstants.createStatusStyle(color));
    }

    private void updateTokenCounter() {
        int tokens = appState.getTokensUsedInSession();
        uiService.getTokenCounterLabel().setText("Tokens: " + tokens);
        String color = tokens > 5000 ? UIConstants.ERROR_COLOR :
                tokens > 2000 ? UIConstants.WARNING_COLOR :
                        tokens > 0 ? UIConstants.SUCCESS_COLOR : "#666";
        uiService.getTokenCounterLabel().setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
    }

    private void saveTranscription() {
        fileOperationsService.saveTranscription(transcriptionService.getTranscriptionText().trim(), primaryStage);
    }

    private void loadTranscription() {
        FileOperationsService.FileOperationResult result = fileOperationsService.loadTranscription(primaryStage);
        if (result.isSuccess()) {
            transcriptionService.setTranscriptionText(result.getMessage());
        }
    }

    private void insertTranscript() {
        chatService.insertTextToMessageField(transcriptionService.getTranscriptionText().trim());
    }

    private void shutdown() {
        if (audioService != null) audioService.shutdown();
        if (transcriptionService != null) transcriptionService.shutdown();
        if (chatService != null) chatService.shutdown();
        notificationService.showInfo("Application shutdown complete");
    }

    public static void main(String[] args) {
        launch(args);
    }
}