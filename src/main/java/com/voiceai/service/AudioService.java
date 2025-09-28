package com.voiceai.service;

import com.voiceai.state.ApplicationState;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Service for coordinating audio recording and transcription operations.
 * Manages the complex interaction between AudioRecordingService and RealTimeTranscriptionService.
 */
public class AudioService {

    private static final Logger logger = Logger.getLogger(AudioService.class.getName());

    private final AudioRecordingService audioRecordingService;
    private final RealTimeTranscriptionService realTimeTranscriptionService;
    private final OpenAIService openAIService;
    private final NotificationService notificationService;
    private final ApplicationState appState;

    // Callback interfaces for UI communication
    public interface AudioStatusCallback {
        void onStatusUpdate(String status);
    }

    public interface TranscriptionCallback {
        void onTranscriptionReceived(String text);
        void onTranscriptionError(String error);
    }

    public interface RecordingStateCallback {
        void onRecordingStateChanged(ApplicationState.RecordingState state);
    }

    /**
     * Recording configuration
     */
    public static class RecordingConfig {
        private final boolean useRealTimeTranscription;
        private final String language;
        private final double chunkDurationSeconds;

        public RecordingConfig(boolean useRealTimeTranscription, String language, double chunkDurationSeconds) {
            this.useRealTimeTranscription = useRealTimeTranscription;
            this.language = language;
            this.chunkDurationSeconds = chunkDurationSeconds;
        }

        public boolean isUseRealTimeTranscription() {
            return useRealTimeTranscription;
        }

        public String getLanguage() {
            return language;
        }

        public double getChunkDurationSeconds() {
            return chunkDurationSeconds;
        }

        // Default configuration
        public static RecordingConfig getDefault() {
            return new RecordingConfig(true, "it", 3.0);
        }
    }

    /**
     * Audio operation result
     */
    public static class AudioResult {
        private final boolean success;
        private final String message;
        private final byte[] audioData;
        private final Exception exception;
        private final int chunksProcessed;

        private AudioResult(boolean success, String message, byte[] audioData, Exception exception, int chunksProcessed) {
            this.success = success;
            this.message = message;
            this.audioData = audioData;
            this.exception = exception;
            this.chunksProcessed = chunksProcessed;
        }

        public static AudioResult success(String message, byte[] audioData) {
            return new AudioResult(true, message, audioData, null, 0);
        }

        public static AudioResult success(String message, int chunksProcessed) {
            return new AudioResult(true, message, null, null, chunksProcessed);
        }

        public static AudioResult failure(String message, Exception exception) {
            return new AudioResult(false, message, null, exception, 0);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public byte[] getAudioData() { return audioData; }
        public Exception getException() { return exception; }
        public int getChunksProcessed() { return chunksProcessed; }
    }

    // Current recording state
    private RecordingConfig currentConfig;
    private AudioStatusCallback statusCallback;
    private TranscriptionCallback transcriptionCallback;
    private RecordingStateCallback stateCallback;
    private boolean isRecordingActive = false;

    /**
     * Creates a new AudioService
     *
     * @param audioRecordingService the audio recording service
     * @param realTimeTranscriptionService the real-time transcription service
     * @param openAIService the OpenAI service for transcription
     * @param notificationService the notification service
     * @param appState the application state
     */
    public AudioService(AudioRecordingService audioRecordingService,
                        RealTimeTranscriptionService realTimeTranscriptionService,
                        OpenAIService openAIService,
                        NotificationService notificationService,
                        ApplicationState appState) {
        this.audioRecordingService = audioRecordingService;
        this.realTimeTranscriptionService = realTimeTranscriptionService;
        this.openAIService = openAIService;
        this.notificationService = notificationService;
        this.appState = appState;
    }

    /**
     * Sets up callbacks for UI communication
     *
     * @param statusCallback callback for status updates
     * @param transcriptionCallback callback for transcription events
     * @param stateCallback callback for recording state changes
     */
    public void setCallbacks(AudioStatusCallback statusCallback,
                             TranscriptionCallback transcriptionCallback,
                             RecordingStateCallback stateCallback) {
        this.statusCallback = statusCallback;
        this.transcriptionCallback = transcriptionCallback;
        this.stateCallback = stateCallback;
    }

    /**
     * Starts recording with the specified configuration
     *
     * @param config the recording configuration
     * @return CompletableFuture that completes when recording setup is finished
     */
    public CompletableFuture<AudioResult> startRecording(RecordingConfig config) {
        if (isRecordingActive) {
            String error = "Recording is already active";
            notificationService.showWarning(error);
            return CompletableFuture.completedFuture(AudioResult.failure(error, null));
        }

        // Check microphone availability
        if (!AudioRecordingService.isMicrophoneAvailable()) {
            String error = "Microphone not available";
            notificationService.showError(error);
            updateStatus(error);
            return CompletableFuture.completedFuture(AudioResult.failure(error, null));
        }

        this.currentConfig = config;
        this.isRecordingActive = true;

        // Update state and UI
        updateRecordingState(ApplicationState.RecordingState.RECORDING);
        updateStatus("Starting recording...");

        logger.info("Starting recording with config: realTime=" + config.isUseRealTimeTranscription() +
                ", language=" + config.getLanguage());

        // Start recording and capture audio
        return audioRecordingService.startRecording()
                .thenCompose(v -> {
                    // If real-time transcription is enabled, start it
                    if (config.isUseRealTimeTranscription()) {
                        Platform.runLater(() -> {
                            updateStatus("Real-time transcription active");
                            realTimeTranscriptionService.startRealTimeTranscription(
                                    this::handleRealTimeTranscription,
                                    this::handleRealTimeTranscriptionError
                            );
                        });
                    } else {
                        Platform.runLater(() -> updateStatus("Recording..."));
                    }

                    // Start capturing audio data
                    return audioRecordingService.captureAudio();
                })
                .thenApply(audioData -> {
                    // This is called when recording completes
                    Platform.runLater(() -> {
                        if (!config.isUseRealTimeTranscription()) {
                            // Process the entire audio at once if not using real-time
                            updateStatus("Processing final transcription...");
                            processNonRealTimeAudio(audioData);
                        } else {
                            // Real-time transcription already handled the audio
                            int chunks = realTimeTranscriptionService.getChunksProcessed();
                            updateStatus("Recording completed");
                            notificationService.showSuccess("Recording stopped. Processed " + chunks + " chunks");
                        }

                        isRecordingActive = false;
                        updateRecordingState(ApplicationState.RecordingState.IDLE);
                    });

                    int chunks = config.isUseRealTimeTranscription() ?
                            realTimeTranscriptionService.getChunksProcessed() : 1;
                    return AudioResult.success("Recording completed successfully", chunks);
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        String error = "Recording failed: " + throwable.getMessage();
                        notificationService.showError(error, throwable);
                        updateStatus("Recording failed");
                        isRecordingActive = false;
                        updateRecordingState(ApplicationState.RecordingState.IDLE);
                    });
                    return AudioResult.failure("Recording failed", (Exception) throwable);
                });
    }

    /**
     * Stops the current recording
     *
     * @return CompletableFuture that completes when recording is stopped
     */
    public CompletableFuture<AudioResult> stopRecording() {
        if (!isRecordingActive || !audioRecordingService.isRecording()) {
            String error = "No active recording to stop";
            return CompletableFuture.completedFuture(AudioResult.failure(error, null));
        }

        // Update UI to show processing state
        updateRecordingState(ApplicationState.RecordingState.STOPPING);
        updateStatus("Stopping recording...");

        logger.info("Stopping recording...");

        // Stop real-time transcription if active
        if (currentConfig.isUseRealTimeTranscription() && realTimeTranscriptionService.isActive()) {
            realTimeTranscriptionService.stopRealTimeTranscription();
        }

        // Stop recording
        return audioRecordingService.stopRecording()
                .thenApply(audioData -> {
                    Platform.runLater(() -> {
                        if (currentConfig.isUseRealTimeTranscription()) {
                            // Real-time already processed everything
                            int chunks = realTimeTranscriptionService.getChunksProcessed();
                            notificationService.showSuccess("Recording stopped. Processed " + chunks + " chunks");
                            updateStatus("Completed (" + chunks + " chunks)");
                        } else {
                            // Process the entire recording now
                            updateStatus("Processing transcription...");
                            processNonRealTimeAudio(audioData);
                        }

                        isRecordingActive = false;
                        updateRecordingState(ApplicationState.RecordingState.IDLE);
                    });

                    int chunks = currentConfig.isUseRealTimeTranscription() ?
                            realTimeTranscriptionService.getChunksProcessed() : 1;
                    return AudioResult.success("Recording stopped successfully", chunks);
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        String error = "Failed to stop recording: " + throwable.getMessage();
                        notificationService.showError(error, throwable);
                        updateStatus("Stop failed");
                        isRecordingActive = false;
                        updateRecordingState(ApplicationState.RecordingState.IDLE);
                    });
                    return AudioResult.failure("Failed to stop recording", (Exception) throwable);
                });
    }

    /**
     * Toggles recording state (start if idle, stop if recording)
     *
     * @param config the recording configuration to use if starting
     * @return CompletableFuture that completes when the toggle operation is finished
     */
    public CompletableFuture<AudioResult> toggleRecording(RecordingConfig config) {
        if (isRecordingActive) {
            return stopRecording();
        } else {
            return startRecording(config);
        }
    }

    /**
     * Checks if recording is currently active
     *
     * @return true if recording is active
     */
    public boolean isRecordingActive() {
        return isRecordingActive;
    }

    /**
     * Gets the current recording duration
     *
     * @return the recording duration in seconds, or 0 if not recording
     */
    public double getCurrentRecordingDuration() {
        return audioRecordingService.getCurrentRecordingDuration();
    }

    /**
     * Forces stop of all audio operations (for emergency shutdown)
     */
    public void forceStop() {
        try {
            if (realTimeTranscriptionService.isActive()) {
                realTimeTranscriptionService.stopRealTimeTranscription();
            }

            if (audioRecordingService.isRecording()) {
                audioRecordingService.forceStop();
            }

            isRecordingActive = false;
            updateRecordingState(ApplicationState.RecordingState.IDLE);
            updateStatus("");

            logger.info("Audio service force stopped");

        } catch (Exception e) {
            logger.severe("Error during force stop: " + e.getMessage());
        }
    }

    /**
     * Shutdown the audio service and cleanup resources
     */
    public void shutdown() {
        forceStop();

        if (realTimeTranscriptionService != null) {
            realTimeTranscriptionService.shutdown();
        }

        logger.info("Audio service shutdown complete");
    }

    /**
     * Handles real-time transcription text chunks
     */
    private void handleRealTimeTranscription(String text) {
        // Ensure transcription callback happens on JavaFX Application Thread
        Platform.runLater(() -> {
            if (transcriptionCallback != null) {
                transcriptionCallback.onTranscriptionReceived(text);
            }

            // Update status with recording duration
            double duration = audioRecordingService.getCurrentRecordingDuration();
            updateStatus(String.format("Recording... (%.1fs)", duration));
        });
    }

    /**
     * Handles real-time transcription errors
     */
    private void handleRealTimeTranscriptionError(String error) {
        logger.warning("Real-time transcription error: " + error);

        // Ensure error callback happens on JavaFX Application Thread
        Platform.runLater(() -> {
            if (transcriptionCallback != null) {
                transcriptionCallback.onTranscriptionError(error);
            }

            updateStatus("Error: " + error);
        });
    }

    /**
     * Processes audio data for non-real-time transcription
     */
    private void processNonRealTimeAudio(byte[] audioData) {
        if (audioData.length > 0) {
            notificationService.showInfo("Processing " + audioData.length + " bytes of audio");
            updateStatus("Transcribing...");

            // Transcribe audio using Whisper API
            openAIService.transcribeAudio(audioData)
                    .thenAccept(result -> {
                        Platform.runLater(() -> {
                            if (result.isSuccess()) {
                                if (transcriptionCallback != null) {
                                    transcriptionCallback.onTranscriptionReceived(result.getText() + "\n\n");
                                }
                                notificationService.showSuccess("Transcription completed!");
                                updateStatus("Transcription complete");
                            } else {
                                if (transcriptionCallback != null) {
                                    transcriptionCallback.onTranscriptionReceived("[Transcription failed: " + result.getMessage() + "]\n\n");
                                }
                                notificationService.showError("Transcription failed", new RuntimeException(result.getMessage()));
                                updateStatus("Transcription failed");
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            if (transcriptionCallback != null) {
                                transcriptionCallback.onTranscriptionReceived("[Transcription error: " + throwable.getMessage() + "]\n\n");
                            }
                            notificationService.showError("Transcription error", throwable);
                            updateStatus("Error");
                        });
                        return null;
                    });
        } else {
            notificationService.showWarning("No audio data captured");
            updateStatus("");
        }
    }

    /**
     * Updates the status through callback
     */
    private void updateStatus(String status) {
        if (statusCallback != null) {
            // Ensure UI updates happen on JavaFX Application Thread
            Platform.runLater(() -> statusCallback.onStatusUpdate(status));
        }
    }

    /**
     * Updates the recording state through callback and application state
     */
    private void updateRecordingState(ApplicationState.RecordingState state) {
        // Application state updates can happen on any thread
        appState.setRecordingState(state);

        if (stateCallback != null) {
            // Ensure UI updates happen on JavaFX Application Thread
            Platform.runLater(() -> stateCallback.onRecordingStateChanged(state));
        }
    }

    /**
     * Gets current configuration
     *
     * @return the current recording configuration or null if not set
     */
    public RecordingConfig getCurrentConfig() {
        return currentConfig;
    }

    /**
     * Validates if the audio service is ready for recording
     *
     * @return true if ready for recording
     */
    public boolean isReadyForRecording() {
        return AudioRecordingService.isMicrophoneAvailable() && !isRecordingActive;
    }

    /**
     * Gets statistics about the current or last recording session
     *
     * @return recording statistics
     */
    public RecordingStats getRecordingStats() {
        return new RecordingStats(
                getCurrentRecordingDuration(),
                realTimeTranscriptionService.getChunksProcessed(),
                isRecordingActive,
                currentConfig != null ? currentConfig.isUseRealTimeTranscription() : false
        );
    }

    /**
     * Recording statistics data class
     */
    public static class RecordingStats {
        private final double duration;
        private final int chunksProcessed;
        private final boolean isActive;
        private final boolean isRealTime;

        public RecordingStats(double duration, int chunksProcessed, boolean isActive, boolean isRealTime) {
            this.duration = duration;
            this.chunksProcessed = chunksProcessed;
            this.isActive = isActive;
            this.isRealTime = isRealTime;
        }

        public double getDuration() { return duration; }
        public int getChunksProcessed() { return chunksProcessed; }
        public boolean isActive() { return isActive; }
        public boolean isRealTime() { return isRealTime; }
    }
}