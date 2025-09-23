package com.voiceai.service;

import com.voiceai.service.OpenAIService.TranscriptionResult;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Service for managing real-time transcription with chunked audio processing
 */
public class RealTimeTranscriptionService {

    // Configuration
    private static final int CHUNK_DURATION_SECONDS = 5; // Process audio every 5 seconds
    private static final int OVERLAP_DURATION_MS = 500; // 500ms overlap between chunks
    private static final int MIN_AUDIO_LENGTH_MS = 1000; // Minimum 1 second of audio

    private final AudioRecordingService audioService;
    private final OpenAIService openAIService;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService transcriptionExecutor;

    private ScheduledFuture<?> chunkProcessorTask;
    private final AtomicBoolean isActive;
    private final AtomicInteger chunkCounter;

    private Consumer<String> onTranscriptionReceived;
    private Consumer<String> onError;

    // For tracking processed audio
    private long lastProcessedPosition = 0;
    private final Object positionLock = new Object();

    public RealTimeTranscriptionService(AudioRecordingService audioService, OpenAIService openAIService) {
        this.audioService = audioService;
        this.openAIService = openAIService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RealTimeTranscription-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.transcriptionExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "RealTimeTranscription-Worker");
            t.setDaemon(true);
            return t;
        });
        this.isActive = new AtomicBoolean(false);
        this.chunkCounter = new AtomicInteger(0);
    }

    /**
     * Starts real-time transcription
     * @param onTranscriptionReceived Callback for transcribed text chunks
     * @param onError Callback for errors
     */
    public void startRealTimeTranscription(
            Consumer<String> onTranscriptionReceived,
            Consumer<String> onError) {

        if (isActive.getAndSet(true)) {
            return; // Already active
        }

        this.onTranscriptionReceived = onTranscriptionReceived;
        this.onError = onError;
        this.chunkCounter.set(0);
        this.lastProcessedPosition = 0;

        // Start periodic chunk processing
        chunkProcessorTask = scheduler.scheduleWithFixedDelay(
                this::processAudioChunk,
                CHUNK_DURATION_SECONDS,
                CHUNK_DURATION_SECONDS,
                TimeUnit.SECONDS
        );

        System.out.println("Real-time transcription started");
    }

    /**
     * Stops real-time transcription
     */
    public void stopRealTimeTranscription() {
        if (!isActive.getAndSet(false)) {
            return; // Not active
        }

        // Cancel scheduled task
        if (chunkProcessorTask != null) {
            chunkProcessorTask.cancel(false);
            chunkProcessorTask = null;
        }

        // Process any remaining audio
        processRemainingAudio();

        System.out.println("Real-time transcription stopped");
    }

    /**
     * Processes a chunk of audio for transcription
     */
    private void processAudioChunk() {
        if (!isActive.get() || !audioService.isRecording()) {
            return;
        }

        try {
            // Get current audio data without stopping recording
            byte[] currentAudioData = audioService.getCurrentAudioData();

            if (currentAudioData == null || currentAudioData.length == 0) {
                return;
            }

            synchronized (positionLock) {
                // Calculate positions with overlap
                long currentLength = currentAudioData.length;

                if (currentLength <= lastProcessedPosition) {
                    return; // No new audio
                }

                // Calculate overlap position (go back 500ms worth of bytes)
                int bytesPerSecond = (int)(16000 * 2); // 16kHz * 2 bytes per sample
                int overlapBytes = (OVERLAP_DURATION_MS * bytesPerSecond) / 1000;
                long startPosition = Math.max(0, lastProcessedPosition - overlapBytes);

                // Extract chunk
                int chunkSize = (int)(currentLength - startPosition);

                // Check minimum audio length
                int minBytes = (MIN_AUDIO_LENGTH_MS * bytesPerSecond) / 1000;
                if (chunkSize < minBytes) {
                    return; // Not enough new audio
                }

                byte[] chunkData = new byte[chunkSize];
                System.arraycopy(currentAudioData, (int)startPosition, chunkData, 0, chunkSize);

                // Update position for next chunk
                lastProcessedPosition = currentLength;

                // Process chunk asynchronously
                int chunkNumber = chunkCounter.incrementAndGet();
                transcriptionExecutor.submit(() -> transcribeChunk(chunkData, chunkNumber));
            }

        } catch (Exception e) {
            System.err.println("Error processing audio chunk: " + e.getMessage());
            if (onError != null) {
                onError.accept("Chunk processing error: " + e.getMessage());
            }
        }
    }

    /**
     * Processes any remaining audio when stopping
     */
    private void processRemainingAudio() {
        try {
            byte[] finalAudioData = audioService.getCurrentAudioData();

            if (finalAudioData == null || finalAudioData.length == 0) {
                return;
            }

            synchronized (positionLock) {
                if (finalAudioData.length > lastProcessedPosition) {
                    // Process remaining audio from last position
                    int remainingSize = (int)(finalAudioData.length - lastProcessedPosition);
                    byte[] remainingData = new byte[remainingSize];
                    System.arraycopy(finalAudioData, (int)lastProcessedPosition,
                            remainingData, 0, remainingSize);

                    // Transcribe final chunk
                    int chunkNumber = chunkCounter.incrementAndGet();
                    CompletableFuture<Void> finalTranscription = CompletableFuture.runAsync(() ->
                            transcribeChunk(remainingData, chunkNumber), transcriptionExecutor);

                    // Wait for final transcription to complete (with timeout)
                    try {
                        finalTranscription.get(10, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        System.err.println("Final transcription timed out");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing remaining audio: " + e.getMessage());
        }
    }

    /**
     * Transcribes a single audio chunk
     */
    private void transcribeChunk(byte[] chunkData, int chunkNumber) {
        try {
            System.out.println("Transcribing chunk #" + chunkNumber +
                    " (" + chunkData.length + " bytes)");

            // Convert to WAV format
            byte[] wavData = audioService.convertToWavFormat(chunkData);

            // Send to OpenAI for transcription
            CompletableFuture<TranscriptionResult> future =
                    openAIService.transcribeAudio(wavData, "it");

            TranscriptionResult result = future.get(15, TimeUnit.SECONDS);

            if (result.isSuccess() && result.getText() != null &&
                    !result.getText().trim().isEmpty()) {

                String transcribedText = result.getText().trim();

                // Add a space after the text if it doesn't end with punctuation
                if (!transcribedText.matches(".*[.!?]$")) {
                    transcribedText += " ";
                }

                System.out.println("Chunk #" + chunkNumber + " transcribed: " +
                        transcribedText.substring(0, Math.min(50, transcribedText.length())) +
                        (transcribedText.length() > 50 ? "..." : ""));

                if (onTranscriptionReceived != null) {
                    onTranscriptionReceived.accept(transcribedText);
                }
            } else if (!result.isSuccess()) {
                System.err.println("Chunk #" + chunkNumber + " transcription failed: " +
                        result.getMessage());
                if (onError != null) {
                    onError.accept("Transcription error: " + result.getMessage());
                }
            }

        } catch (TimeoutException e) {
            System.err.println("Chunk #" + chunkNumber + " transcription timed out");
            if (onError != null) {
                onError.accept("Transcription timeout");
            }
        } catch (Exception e) {
            System.err.println("Error transcribing chunk #" + chunkNumber + ": " + e.getMessage());
            if (onError != null) {
                onError.accept("Transcription error: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if real-time transcription is active
     */
    public boolean isActive() {
        return isActive.get();
    }

    /**
     * Gets the number of chunks processed
     */
    public int getChunksProcessed() {
        return chunkCounter.get();
    }

    /**
     * Shuts down the service
     */
    public void shutdown() {
        stopRealTimeTranscription();
        scheduler.shutdown();
        transcriptionExecutor.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!transcriptionExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                transcriptionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            transcriptionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}