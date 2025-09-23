package com.voiceai.service;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecordingService {

    public enum RecordingState {
        IDLE,
        RECORDING,
        PROCESSING
    }

    // Audio format compatible with OpenAI Whisper
    private static final float SAMPLE_RATE = 16000.0f;  // 16kHz
    private static final int SAMPLE_SIZE_IN_BITS = 16;   // 16-bit
    private static final int CHANNELS = 1;               // Mono
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    private final AudioFormat audioFormat;
    private TargetDataLine targetDataLine;
    private ByteArrayOutputStream recordedAudioStream;
    private CompletableFuture<byte[]> recordingTask;
    private final AtomicBoolean isRecording;
    private volatile RecordingState currentState;

    public AudioRecordingService() {
        this.audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,
                SAMPLE_SIZE_IN_BITS,
                CHANNELS,
                (SAMPLE_SIZE_IN_BITS / 8) * CHANNELS, // frameSize
                SAMPLE_RATE,  // frameRate
                BIG_ENDIAN
        );

        this.isRecording = new AtomicBoolean(false);
        this.currentState = RecordingState.IDLE;
    }

    /**
     * Starts audio recording asynchronously
     * @return CompletableFuture that completes when recording is started
     */
    public CompletableFuture<Void> startRecording() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (isRecording.get()) {
                    throw new IllegalStateException("Recording is already in progress");
                }

                currentState = RecordingState.RECORDING;

                // Get and configure the microphone line
                DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

                if (!AudioSystem.isLineSupported(dataLineInfo)) {
                    throw new UnsupportedOperationException("Audio format not supported by system");
                }

                targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
                targetDataLine.open(audioFormat);

                // Initialize audio buffer
                recordedAudioStream = new ByteArrayOutputStream();

                // Start recording
                targetDataLine.start();
                isRecording.set(true);

                System.out.println("Recording started...");

            } catch (LineUnavailableException e) {
                currentState = RecordingState.IDLE;
                throw new RuntimeException("Microphone not available: " + e.getMessage(), e);
            } catch (Exception e) {
                currentState = RecordingState.IDLE;
                throw new RuntimeException("Failed to start recording: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Stops recording and returns the recorded audio data
     * @return CompletableFuture containing the recorded audio bytes
     */
    public CompletableFuture<byte[]> stopRecording() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isRecording.get()) {
                    throw new IllegalStateException("No recording in progress");
                }

                currentState = RecordingState.PROCESSING;

                // Stop recording
                isRecording.set(false);

                if (targetDataLine != null) {
                    targetDataLine.stop();
                    targetDataLine.close();
                }

                // Get recorded data
                byte[] audioData = recordedAudioStream != null ?
                        recordedAudioStream.toByteArray() : new byte[0];

                // Cleanup
                cleanup();

                currentState = RecordingState.IDLE;

                System.out.println("Recording stopped. Captured " + audioData.length + " bytes");
                return audioData;

            } catch (Exception e) {
                currentState = RecordingState.IDLE;
                cleanup();
                throw new RuntimeException("Failed to stop recording: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Starts the recording process that captures audio data
     * Should be called after startRecording() completes successfully
     * @return CompletableFuture that completes when recording is stopped
     */
    public CompletableFuture<byte[]> captureAudio() {
        recordingTask = CompletableFuture.supplyAsync(() -> {
            byte[] buffer = new byte[4096]; // 4KB buffer

            try {
                while (isRecording.get() && targetDataLine != null) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);

                    if (bytesRead > 0 && recordedAudioStream != null) {
                        recordedAudioStream.write(buffer, 0, bytesRead);
                    }
                }

                return recordedAudioStream != null ? recordedAudioStream.toByteArray() : new byte[0];

            } catch (Exception e) {
                System.err.println("Error during audio capture: " + e.getMessage());
                return new byte[0];
            }
        });

        return recordingTask;
    }

    /**
     * Gets the current recording state
     */
    public RecordingState getRecordingState() {
        return currentState;
    }

    /**
     * Checks if currently recording
     */
    public boolean isRecording() {
        return isRecording.get();
    }

    /**
     * Gets the audio format used for recording
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * Converts recorded audio bytes to WAV format for API compatibility
     * @param audioData Raw PCM audio data
     * @return WAV formatted audio data
     */
    public byte[] convertToWavFormat(byte[] audioData) {
        try {
            ByteArrayOutputStream wavStream = new ByteArrayOutputStream();

            // WAV header (44 bytes)
            writeWavHeader(wavStream, audioData.length);

            // Audio data
            wavStream.write(audioData);

            return wavStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to convert to WAV format", e);
        }
    }

    private void writeWavHeader(ByteArrayOutputStream out, int audioDataLength) throws IOException {
        int fileLength = audioDataLength + 44 - 8; // Total file size - 8 bytes
        int byteRate = (int) (SAMPLE_RATE * CHANNELS * SAMPLE_SIZE_IN_BITS / 8);
        int blockAlign = CHANNELS * SAMPLE_SIZE_IN_BITS / 8;

        // RIFF header
        out.write("RIFF".getBytes());
        writeInt(out, fileLength);
        out.write("WAVE".getBytes());

        // fmt chunk
        out.write("fmt ".getBytes());
        writeInt(out, 16); // fmt chunk size
        writeShort(out, 1); // audio format (PCM)
        writeShort(out, CHANNELS);
        writeInt(out, (int) SAMPLE_RATE);
        writeInt(out, byteRate);
        writeShort(out, blockAlign);
        writeShort(out, SAMPLE_SIZE_IN_BITS);

        // data chunk
        out.write("data".getBytes());
        writeInt(out, audioDataLength);
    }

    private void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private void writeShort(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    /**
     * Emergency stop - cancels recording immediately
     */
    public void forceStop() {
        isRecording.set(false);
        currentState = RecordingState.IDLE;

        if (recordingTask != null && !recordingTask.isDone()) {
            recordingTask.cancel(true);
        }

        cleanup();
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        try {
            if (targetDataLine != null && targetDataLine.isOpen()) {
                targetDataLine.stop();
                targetDataLine.close();
            }

            if (recordedAudioStream != null) {
                recordedAudioStream.close();
                recordedAudioStream = null;
            }

        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        } finally {
            targetDataLine = null;
        }
    }

    /**
     * Checks if microphone is available
     */
    public static boolean isMicrophoneAvailable() {
        AudioFormat testFormat = new AudioFormat(16000.0f, 16, 1, true, false);
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, testFormat);
        return AudioSystem.isLineSupported(dataLineInfo);
    }
}
