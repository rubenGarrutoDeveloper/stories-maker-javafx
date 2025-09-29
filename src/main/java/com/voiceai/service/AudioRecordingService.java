package com.voiceai.service;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AudioRecordingService {

    public enum RecordingState {
        IDLE,
        RECORDING,
        PROCESSING
    }

    /**
     * Represents an available audio input source
     */
    public static class AudioSource {
        private final Mixer.Info mixerInfo;
        private final String displayName;
        private final boolean isSystemAudio;

        public AudioSource(Mixer.Info mixerInfo, String displayName, boolean isSystemAudio) {
            this.mixerInfo = mixerInfo;
            this.displayName = displayName;
            this.isSystemAudio = isSystemAudio;
        }

        public Mixer.Info getMixerInfo() {
            return mixerInfo;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isSystemAudio() {
            return isSystemAudio;
        }

        @Override
        public String toString() {
            return displayName + (isSystemAudio ? " (System Audio)" : "");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AudioSource that = (AudioSource) o;
            return mixerInfo.getName().equals(that.mixerInfo.getName());
        }

        @Override
        public int hashCode() {
            return mixerInfo.getName().hashCode();
        }
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

    // Thread-safe access to audio data
    private final ReadWriteLock audioDataLock = new ReentrantReadWriteLock();
    private byte[] currentAudioBuffer = new byte[0];

    // Selected audio source
    private AudioSource selectedAudioSource;

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

        // Try to select default audio source
        List<AudioSource> sources = getAvailableAudioSources();
        if (!sources.isEmpty()) {
            this.selectedAudioSource = sources.get(0);
        }
    }

    /**
     * Gets all available audio input sources on the system
     */
    public List<AudioSource> getAvailableAudioSources() {
        List<AudioSource> sources = new ArrayList<>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            // Check if this mixer supports recording (has target lines)
            Line.Info[] targetLineInfos = mixer.getTargetLineInfo();

            for (Line.Info lineInfo : targetLineInfos) {
                if (lineInfo instanceof DataLine.Info) {
                    DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;

                    // Check if this line supports our audio format (or can be adapted)
                    if (dataLineInfo.isFormatSupported(audioFormat) ||
                            canAdaptFormat(mixer, dataLineInfo)) {

                        String mixerName = mixerInfo.getName();
                        String description = mixerInfo.getDescription();

                        // Determine if this is system audio (stereo mix, loopback, etc.)
                        boolean isSystemAudio = isSystemAudioSource(mixerName, description);

                        // Create display name
                        String displayName = createDisplayName(mixerName, description);

                        AudioSource source = new AudioSource(mixerInfo, displayName, isSystemAudio);

                        // Avoid duplicates
                        if (!sources.contains(source)) {
                            sources.add(source);
                        }
                        break; // Only add each mixer once
                    }
                }
            }
        }

        return sources;
    }

    /**
     * Checks if a mixer can adapt to our required format
     */
    private boolean canAdaptFormat(Mixer mixer, DataLine.Info lineInfo) {
        try {
            // Get supported formats
            AudioFormat[] formats = lineInfo.getFormats();

            for (AudioFormat format : formats) {
                // Check if we can convert this format to our target format
                if (format.getSampleRate() == AudioSystem.NOT_SPECIFIED ||
                        format.getSampleRate() >= SAMPLE_RATE) {
                    if (format.getChannels() == AudioSystem.NOT_SPECIFIED ||
                            format.getChannels() <= 2) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return false;
    }

    /**
     * Determines if an audio source is system audio based on name/description
     */
    private boolean isSystemAudioSource(String name, String description) {
        String combinedLower = (name + " " + description).toLowerCase();

        return combinedLower.contains("stereo mix") ||
                combinedLower.contains("stereomix") ||
                combinedLower.contains("wave out") ||
                combinedLower.contains("loopback") ||
                combinedLower.contains("what u hear") ||
                combinedLower.contains("what you hear") ||
                combinedLower.contains("record what you hear") ||
                combinedLower.contains("rec. playback") ||
                combinedLower.contains("monitor") && combinedLower.contains("mix");
    }

    /**
     * Creates a user-friendly display name for an audio source
     */
    private String createDisplayName(String name, String description) {
        // Remove redundant information
        if (description != null && !description.trim().isEmpty() &&
                !description.equals(name) && !description.equals("Direct Audio Device")) {
            return name + " - " + description;
        }
        return name;
    }

    /**
     * Sets the audio source to use for recording
     */
    public void setAudioSource(AudioSource audioSource) {
        if (isRecording.get()) {
            throw new IllegalStateException("Cannot change audio source while recording");
        }
        this.selectedAudioSource = audioSource;
        System.out.println("Audio source set to: " + audioSource.getDisplayName());
    }

    /**
     * Gets the currently selected audio source
     */
    public AudioSource getSelectedAudioSource() {
        return selectedAudioSource;
    }

    /**
     * Gets the default audio source (first available)
     */
    public AudioSource getDefaultAudioSource() {
        List<AudioSource> sources = getAvailableAudioSources();
        return sources.isEmpty() ? null : sources.get(0);
    }

    /**
     * Starts audio recording asynchronously using the selected audio source
     */
    public CompletableFuture<Void> startRecording() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (isRecording.get()) {
                    throw new IllegalStateException("Recording is already in progress");
                }

                if (selectedAudioSource == null) {
                    throw new IllegalStateException("No audio source selected");
                }

                currentState = RecordingState.RECORDING;

                // Get the mixer for the selected audio source
                Mixer mixer = AudioSystem.getMixer(selectedAudioSource.getMixerInfo());

                // Create data line info
                DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

                // Try to get the line from the selected mixer
                try {
                    targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
                } catch (LineUnavailableException e) {
                    // If exact format not supported, try to find a compatible format
                    targetDataLine = getCompatibleLine(mixer);
                    if (targetDataLine == null) {
                        throw new UnsupportedOperationException(
                                "Selected audio source does not support the required format: " +
                                        selectedAudioSource.getDisplayName());
                    }
                }

                targetDataLine.open(audioFormat);

                // Initialize audio buffer
                recordedAudioStream = new ByteArrayOutputStream();

                // Clear the current buffer
                audioDataLock.writeLock().lock();
                try {
                    currentAudioBuffer = new byte[0];
                } finally {
                    audioDataLock.writeLock().unlock();
                }

                // Start recording
                targetDataLine.start();
                isRecording.set(true);

                System.out.println("Recording started using: " + selectedAudioSource.getDisplayName());

            } catch (LineUnavailableException e) {
                currentState = RecordingState.IDLE;
                throw new RuntimeException("Audio source not available: " + e.getMessage(), e);
            } catch (Exception e) {
                currentState = RecordingState.IDLE;
                throw new RuntimeException("Failed to start recording: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Tries to get a compatible target line from the mixer
     */
    private TargetDataLine getCompatibleLine(Mixer mixer) {
        Line.Info[] lineInfos = mixer.getTargetLineInfo();

        for (Line.Info info : lineInfos) {
            if (info instanceof DataLine.Info) {
                DataLine.Info dataLineInfo = (DataLine.Info) info;
                AudioFormat[] formats = dataLineInfo.getFormats();

                for (AudioFormat format : formats) {
                    try {
                        // Try to open with this format
                        TargetDataLine line = (TargetDataLine) mixer.getLine(
                                new DataLine.Info(TargetDataLine.class, format));

                        // If successful, we can use this line
                        return line;
                    } catch (Exception e) {
                        // Try next format
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gets the current audio data without stopping the recording
     */
    public byte[] getCurrentAudioData() {
        audioDataLock.readLock().lock();
        try {
            return currentAudioBuffer.clone();
        } finally {
            audioDataLock.readLock().unlock();
        }
    }

    /**
     * Stops recording and returns the recorded audio data
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
     */
    public CompletableFuture<byte[]> captureAudio() {
        recordingTask = CompletableFuture.supplyAsync(() -> {
            byte[] buffer = new byte[4096];

            try {
                while (isRecording.get() && targetDataLine != null) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);

                    if (bytesRead > 0 && recordedAudioStream != null) {
                        recordedAudioStream.write(buffer, 0, bytesRead);

                        audioDataLock.writeLock().lock();
                        try {
                            byte[] newAudioData = recordedAudioStream.toByteArray();
                            currentAudioBuffer = newAudioData;
                        } finally {
                            audioDataLock.writeLock().unlock();
                        }
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

    public RecordingState getRecordingState() {
        return currentState;
    }

    public boolean isRecording() {
        return isRecording.get();
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public double getCurrentRecordingDuration() {
        audioDataLock.readLock().lock();
        try {
            if (currentAudioBuffer.length == 0) {
                return 0.0;
            }

            int bytesPerSecond = (int)(SAMPLE_RATE * CHANNELS * (SAMPLE_SIZE_IN_BITS / 8));
            return (double) currentAudioBuffer.length / bytesPerSecond;
        } finally {
            audioDataLock.readLock().unlock();
        }
    }

    public byte[] convertToWavFormat(byte[] audioData) {
        try {
            ByteArrayOutputStream wavStream = new ByteArrayOutputStream();
            writeWavHeader(wavStream, audioData.length);
            wavStream.write(audioData);
            return wavStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert to WAV format", e);
        }
    }

    private void writeWavHeader(ByteArrayOutputStream out, int audioDataLength) throws IOException {
        int fileLength = audioDataLength + 44 - 8;
        int byteRate = (int) (SAMPLE_RATE * CHANNELS * SAMPLE_SIZE_IN_BITS / 8);
        int blockAlign = CHANNELS * SAMPLE_SIZE_IN_BITS / 8;

        out.write("RIFF".getBytes());
        writeInt(out, fileLength);
        out.write("WAVE".getBytes());

        out.write("fmt ".getBytes());
        writeInt(out, 16);
        writeShort(out, 1);
        writeShort(out, CHANNELS);
        writeInt(out, (int) SAMPLE_RATE);
        writeInt(out, byteRate);
        writeShort(out, blockAlign);
        writeShort(out, SAMPLE_SIZE_IN_BITS);

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

    public void forceStop() {
        isRecording.set(false);
        currentState = RecordingState.IDLE;

        if (recordingTask != null && !recordingTask.isDone()) {
            recordingTask.cancel(true);
        }

        cleanup();
    }

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

            audioDataLock.writeLock().lock();
            try {
                currentAudioBuffer = new byte[0];
            } finally {
                audioDataLock.writeLock().unlock();
            }

        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        } finally {
            targetDataLine = null;
        }
    }

    /**
     * Checks if any audio input source is available
     */
    public static boolean isMicrophoneAvailable() {
        AudioFormat testFormat = new AudioFormat(16000.0f, 16, 1, true, false);
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, testFormat);
        return AudioSystem.isLineSupported(dataLineInfo);
    }

    /**
     * Tests if a specific audio source is working
     */
    public boolean testAudioSource(AudioSource source) {
        try {
            Mixer mixer = AudioSystem.getMixer(source.getMixerInfo());
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

            TargetDataLine testLine = (TargetDataLine) mixer.getLine(dataLineInfo);
            testLine.open(audioFormat);
            testLine.close();

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}