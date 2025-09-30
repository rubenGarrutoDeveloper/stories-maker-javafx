package com.voiceai.service;

import com.voiceai.state.ApplicationState;
import com.voiceai.constant.UIConstants;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.util.logging.Logger;

/**
 * Service for managing transcription UI state and interactions.
 * Handles recording button management, status updates, and transcription area coordination.
 */
public class TranscriptionService {

    private static final Logger logger = Logger.getLogger(TranscriptionService.class.getName());

    private final AudioService audioService;
    private final SettingsService settingsService;
    private final ApplicationState appState;
    private final NotificationService notificationService;

    // UI Component references
    private Button recordingButton;
    private Label statusLabel;
    private TextArea transcriptionArea;

    // State tracking
    private boolean isApiKeyValid = false;

    /**
     * Interface for transcription events that external components can listen to
     */
    public interface TranscriptionEventCallback {
        void onRecordingToggleRequested();
    }

    private TranscriptionEventCallback eventCallback;

    /**
     * Creates a new TranscriptionService
     *
     * @param audioService the audio service for recording operations
     * @param settingsService the settings service for configuration
     * @param appState the application state
     * @param notificationService the notification service
     */
    public TranscriptionService(AudioService audioService,
                                SettingsService settingsService,
                                ApplicationState appState,
                                NotificationService notificationService) {
        this.audioService = audioService;
        this.settingsService = settingsService;
        this.appState = appState;
        this.notificationService = notificationService;

        // Set up audio service callbacks to update transcription UI
        setupAudioServiceCallbacks();
    }

    /**
     * Sets up callbacks with AudioService for UI updates
     */
    private void setupAudioServiceCallbacks() {
        audioService.setCallbacks(
                // Status callback
                this::updateStatus,

                // Transcription callback
                new AudioService.TranscriptionCallback() {
                    @Override
                    public void onTranscriptionReceived(String text) {
                        Platform.runLater(() -> appendTranscriptionText(text));
                    }

                    @Override
                    public void onTranscriptionError(String error) {
                        Platform.runLater(() -> {
                            logger.warning("Transcription error: " + error);
                            updateStatus("Error: " + error);
                        });
                    }
                },

                // Recording state callback
                state -> Platform.runLater(() -> updateRecordingButtonState(state))
        );
    }

    /**
     * Initializes the transcription service with UI components
     *
     * @param recordingButton the recording button
     * @param statusLabel the status label
     * @param transcriptionArea the transcription text area
     */
    public void initializeUI(Button recordingButton, Label statusLabel, TextArea transcriptionArea) {
        this.recordingButton = recordingButton;
        this.statusLabel = statusLabel;
        this.transcriptionArea = transcriptionArea;

        // Set up recording button event handler
        if (recordingButton != null) {
            recordingButton.setOnAction(e -> handleRecordingButtonClick());
        }

        // Initial UI state
        updateUIState();

        logger.info("TranscriptionService UI initialized");
    }

    /**
     * Sets the callback for transcription events
     *
     * @param callback the callback for external event handling
     */
    public void setEventCallback(TranscriptionEventCallback callback) {
        this.eventCallback = callback;
    }

    /**
     * Handles recording button click events
     */
    private void handleRecordingButtonClick() {
        if (!isApiKeyValid) {
            notificationService.showWarning("Please validate API key before recording");
            return;
        }

        // Create recording configuration from current settings
        AudioService.RecordingConfig config = new AudioService.RecordingConfig(
                settingsService.isRealTimeTranscriptionEnabled(),
                settingsService.getLanguage(),
                3.0 // chunk duration seconds
        );

        // Delegate to AudioService for actual recording operations
        audioService.toggleRecording(config)
                .thenAccept(result -> {
                    Platform.runLater(() -> {
                        if (!result.isSuccess() && result.getException() != null) {
                            // Additional error handling if needed
                            logger.warning("Recording operation failed: " + result.getMessage());
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        notificationService.showError("Recording operation failed", throwable);
                    });
                    return null;
                });

        // Notify external components if callback is set
        if (eventCallback != null) {
            eventCallback.onRecordingToggleRequested();
        }
    }

    /**
     * Updates the transcription status label
     *
     * @param status the status message to display
     */
    public void updateStatus(String status) {
        if (statusLabel != null) {
            Platform.runLater(() -> {
                if (status == null || status.isEmpty()) {
                    statusLabel.setText("");
                } else {
                    statusLabel.setText("• " + status);
                }
            });
        }
    }

    /**
     * Appends text to the transcription area
     *
     * @param text the text to append
     */
    public void appendTranscriptionText(String text) {
        if (transcriptionArea != null && text != null) {
            transcriptionArea.appendText(text);
            // Auto-scroll to bottom
            transcriptionArea.setScrollTop(Double.MAX_VALUE);
        }
    }

    /**
     * Clears the transcription area
     */
    public void clearTranscription() {
        if (transcriptionArea != null) {
            transcriptionArea.clear();
            notificationService.showInfo("Transcription cleared");
        }
    }

    /**
     * Gets the current transcription text
     *
     * @return the transcription text or empty string if no area is set
     */
    public String getTranscriptionText() {
        if (transcriptionArea != null) {
            return transcriptionArea.getText();
        }
        return "";
    }

    /**
     * Sets the transcription text
     *
     * @param text the text to set
     */
    public void setTranscriptionText(String text) {
        if (transcriptionArea != null) {
            transcriptionArea.setText(text != null ? text : "");
        }
    }

    /**
     * Selects all text in the transcription area
     */
    public void selectAllText() {
        if (transcriptionArea != null) {
            transcriptionArea.selectAll();
            transcriptionArea.requestFocus();
        }
    }

    /**
     * Updates the recording button state based on the current recording state
     *
     * @param recordingState the current recording state
     */
    private void updateRecordingButtonState(ApplicationState.RecordingState recordingState) {
        if (recordingButton == null) return;

        switch (recordingState) {
            case RECORDING:
                recordingButton.setText(UIConstants.STOP_BUTTON_TEXT);
                recordingButton.setStyle(UIConstants.DANGER_BUTTON_STYLE);
                recordingButton.setDisable(false);
                updateTranscriptionAreaPlaceholder(UIConstants.TRANSCRIPT_PLACEHOLDER_RECORDING);
                break;

            case STOPPING:
                recordingButton.setText(UIConstants.STOPPING_BUTTON_TEXT);
                recordingButton.setStyle(UIConstants.DANGER_BUTTON_STYLE);
                recordingButton.setDisable(true);
                updateTranscriptionAreaPlaceholder(UIConstants.TRANSCRIPT_PLACEHOLDER_RECORDING);
                break;

            case IDLE:
            default:
                recordingButton.setText(UIConstants.REC_BUTTON_TEXT);
                recordingButton.setStyle(isApiKeyValid ?
                        UIConstants.PRIMARY_BUTTON_STYLE : UIConstants.DISABLED_BUTTON_STYLE);
                recordingButton.setDisable(!isApiKeyValid);
                updateTranscriptionAreaPlaceholder(UIConstants.TRANSCRIPT_PLACEHOLDER_IDLE);
                break;
        }
    }

    /**
     * Updates the transcription area placeholder text
     *
     * @param placeholder the placeholder text
     */
    private void updateTranscriptionAreaPlaceholder(String placeholder) {
        if (transcriptionArea != null) {
            transcriptionArea.setPromptText(placeholder);
        }
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
        ApplicationState.RecordingState currentState = appState.getRecordingState();
        updateRecordingButtonState(currentState);
    }

    /**
     * Checks if recording is currently active
     *
     * @return true if recording is active
     */
    public boolean isRecording() {
        return audioService.isRecordingActive();
    }

    /**
     * Gets the current recording duration
     *
     * @return the recording duration in seconds
     */
    public double getCurrentRecordingDuration() {
        return audioService.getCurrentRecordingDuration();
    }

    /**
     * Gets recording statistics
     *
     * @return the recording statistics
     */
    public AudioService.RecordingStats getRecordingStats() {
        return audioService.getRecordingStats();
    }

    /**
     * Forces stop of current recording (emergency stop)
     */
    public void forceStopRecording() {
        audioService.forceStop();
        notificationService.showWarning("Recording force stopped");
    }

    /**
     * Checks if the transcription service is ready for recording
     *
     * @return true if ready for recording
     */
    public boolean isReadyForRecording() {
        return isApiKeyValid && audioService.isReadyForRecording();
    }

    /**
     * Gets the current real-time transcription setting
     *
     * @return true if real-time transcription is enabled
     */
    public boolean isRealTimeTranscriptionEnabled() {
        return settingsService.isRealTimeTranscriptionEnabled();
    }

    /**
     * Updates the real-time transcription setting
     *
     * @param enabled true to enable real-time transcription
     */
    public void setRealTimeTranscriptionEnabled(boolean enabled) {
        settingsService.setRealTimeTranscriptionEnabled(enabled);
    }

    /**
     * Gets configuration information for display
     *
     * @return formatted configuration string
     */
    public String getConfigurationInfo() {
        return String.format("Language: %s, Real-time: %s, API Key: %s",
                settingsService.getLanguage(),
                settingsService.isRealTimeTranscriptionEnabled() ? "Enabled" : "Disabled",
                isApiKeyValid ? "Valid" : "Invalid"
        );
    }

    /**
     * Validates the current transcription configuration
     *
     * @return validation result with any issues found
     */
    public ValidationResult validateConfiguration() {
        if (!isApiKeyValid) {
            return ValidationResult.failure("API key is not valid");
        }

        if (!audioService.isReadyForRecording()) {
            return ValidationResult.failure("Audio service is not ready for recording");
        }

        String language = settingsService.getLanguage();
        if (language == null || language.trim().isEmpty()) {
            return ValidationResult.failure("Language setting is not configured");
        }

        return ValidationResult.success("Configuration is valid");
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
     * Shutdown the transcription service
     */
    public void shutdown() {
        // Clean up any resources if needed
        logger.info("TranscriptionService shutdown complete");
    }
}