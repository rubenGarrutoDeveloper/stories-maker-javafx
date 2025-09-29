package com.voiceai;

import com.voiceai.service.*;
import com.voiceai.state.ApplicationState;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

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

        // Initialize audio source selector
        initializeAudioSourceSelector();

        // Show startup notification
        notificationService.showInfo("StorieS Maker initialized successfully");

        // Save window geometry on close
        primaryStage.setOnCloseRequest(event -> {
            saveWindowGeometry();
            shutdown();
        });
    }

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

    private void applyWindowGeometry(Stage stage) {
        double x = settingsService.getWindowX();
        double y = settingsService.getWindowY();

        if (x >= 0 && y >= 0) {
            stage.setX(x);
            stage.setY(y);
        }
    }

    private void saveWindowGeometry() {
        if (primaryStage != null) {
            settingsService.saveWindowGeometry(
                    primaryStage.getX(), primaryStage.getY(),
                    primaryStage.getWidth(), primaryStage.getHeight()
            );
        }
    }

    private void setupEventHandlers() {
        uiService.getTestConnectionButton().setOnAction(e -> testApiConnection());
        uiService.getSaveButton().setOnAction(e -> saveTranscription());
        uiService.getSelectAllButton().setOnAction(e -> transcriptionService.selectAllText());
        uiService.getLoadButton().setOnAction(e -> loadTranscription());
        uiService.getInsertTranscriptButton().setOnAction(e -> insertTranscript());
        uiService.getClearButton().setOnAction(e -> chatService.clearChat());
        uiService.getRealTimeCheckBox().setOnAction(e -> transcriptionService.setRealTimeTranscriptionEnabled(uiService.getRealTimeCheckBox().isSelected()));

        // Audio source selection handlers
        uiService.getAudioSourceComboBox().setOnAction(e -> onAudioSourceSelected());
        uiService.getRefreshSourcesButton().setOnAction(e -> refreshAudioSources());
    }

    /**
     * Initializes the audio source selector with available sources
     */
    private void initializeAudioSourceSelector() {
        List<AudioRecordingService.AudioSource> sources = audioRecordingService.getAvailableAudioSources();

        if (sources.isEmpty()) {
            notificationService.showWarning("No audio input sources found on this system");
            uiService.getAudioSourceComboBox().setDisable(true);
            return;
        }

        // Populate combo box
        uiService.getAudioSourceComboBox().getItems().clear();
        uiService.getAudioSourceComboBox().getItems().addAll(sources);

        // Try to restore saved audio source
        String savedSourceName = settingsService.getAudioSourceName();
        AudioRecordingService.AudioSource selectedSource = null;

        if (savedSourceName != null) {
            // Find the saved source
            for (AudioRecordingService.AudioSource source : sources) {
                if (source.getDisplayName().equals(savedSourceName)) {
                    selectedSource = source;
                    break;
                }
            }
        }

        // If no saved source or saved source not found, use the first one
        if (selectedSource == null && !sources.isEmpty()) {
            selectedSource = sources.get(0);
        }

        // Set the selected source
        if (selectedSource != null) {
            uiService.getAudioSourceComboBox().setValue(selectedSource);
            audioRecordingService.setAudioSource(selectedSource);

            notificationService.showInfo("Audio source: " + selectedSource.getDisplayName());
        }

        // Show info about system audio availability
        boolean hasSystemAudio = sources.stream().anyMatch(AudioRecordingService.AudioSource::isSystemAudio);

        // Count microphones and system audio sources
        long micCount = sources.stream().filter(s -> !s.isSystemAudio()).count();
        long sysAudioCount = sources.stream().filter(AudioRecordingService.AudioSource::isSystemAudio).count();

        notificationService.showSuccess("Found " + sources.size() + " audio source(s): " +
                micCount + " microphone(s), " + sysAudioCount + " system audio");

        if (!hasSystemAudio) {
            notificationService.showWarning("⚠️ System audio (PC audio) not detected!");
            notificationService.showInfo("To record PC audio: Open 'Sound Settings' → 'Sound Control Panel' → " +
                    "'Recording' tab → Right-click → 'Show Disabled Devices' → " +
                    "Enable 'Stereo Mix' or 'Wave Out Mix'");
        } else {
            notificationService.showSuccess("✓ System audio capture is available! You can record PC audio.");
        }
    }

    /**
     * Handles audio source selection change
     */
    private void onAudioSourceSelected() {
        AudioRecordingService.AudioSource selected = uiService.getAudioSourceComboBox().getValue();

        if (selected != null) {
            try {
                audioRecordingService.setAudioSource(selected);
                settingsService.setAudioSourceName(selected.getDisplayName());

                String sourceType = selected.isSystemAudio() ? "System Audio" : "Microphone";
                notificationService.showSuccess("Audio source changed to: " + selected.getDisplayName() + " (" + sourceType + ")");

                // Test the audio source
                if (audioRecordingService.testAudioSource(selected)) {
                    notificationService.showInfo("Audio source is working correctly");
                } else {
                    notificationService.showWarning("Warning: Selected audio source may not be working properly");
                }

            } catch (Exception e) {
                notificationService.showError("Failed to set audio source", e);
            }
        }
    }

    /**
     * Refreshes the list of available audio sources
     */
    private void refreshAudioSources() {
        notificationService.showInfo("Refreshing audio sources...");

        // Get current selection to try to restore it
        AudioRecordingService.AudioSource currentSelection = uiService.getAudioSourceComboBox().getValue();

        // Reload sources
        List<AudioRecordingService.AudioSource> sources = audioRecordingService.getAvailableAudioSources();

        if (sources.isEmpty()) {
            notificationService.showWarning("No audio input sources found");
            uiService.getAudioSourceComboBox().setDisable(true);
            return;
        }

        // Update combo box
        uiService.getAudioSourceComboBox().getItems().clear();
        uiService.getAudioSourceComboBox().getItems().addAll(sources);
        uiService.getAudioSourceComboBox().setDisable(false);

        // Try to restore previous selection
        if (currentSelection != null) {
            AudioRecordingService.AudioSource match = sources.stream()
                    .filter(s -> s.getDisplayName().equals(currentSelection.getDisplayName()))
                    .findFirst()
                    .orElse(null);

            if (match != null) {
                uiService.getAudioSourceComboBox().setValue(match);
                audioRecordingService.setAudioSource(match);
            } else {
                // Previous source not available, select first one
                uiService.getAudioSourceComboBox().setValue(sources.get(0));
                audioRecordingService.setAudioSource(sources.get(0));
                notificationService.showWarning("Previous audio source no longer available");
            }
        } else if (!sources.isEmpty()) {
            uiService.getAudioSourceComboBox().setValue(sources.get(0));
            audioRecordingService.setAudioSource(sources.get(0));
        }

        notificationService.showSuccess("Found " + sources.size() + " audio source(s)");

        // Report if system audio is available
        boolean hasSystemAudio = sources.stream().anyMatch(AudioRecordingService.AudioSource::isSystemAudio);
        if (hasSystemAudio) {
            notificationService.showInfo("System audio capture is available");
        }
    }

    private void onStateChanged(ApplicationState state) {
        Platform.runLater(() -> {
            updateConnectionStatus();
            updateTokenCounter();
            transcriptionService.updateApiKeyState(state.isApiKeyValid());
            chatService.updateApiKeyState(state.isApiKeyValid());
        });
    }

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