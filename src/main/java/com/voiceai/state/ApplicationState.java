package com.voiceai.state;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the application's state including API key validation,
 * recording status, and state change notifications
 */
public class ApplicationState {

    public enum RecordingState {
        IDLE,
        RECORDING,
        STOPPING,
        PROCESSING
    }

    public enum ConnectionState {
        DISCONNECTED,
        TESTING,
        CONNECTED,
        ERROR
    }

    // State variables
    private boolean apiKeyValid = false;
    private RecordingState recordingState = RecordingState.IDLE;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private String connectionMessage = "Disconnected";
    private String lastError = null;

    // State change listeners
    private final List<Consumer<ApplicationState>> stateChangeListeners = new ArrayList<>();

    /**
     * Gets the current API key validation status
     */
    public boolean isApiKeyValid() {
        return apiKeyValid;
    }

    /**
     * Sets the API key validation status
     */
    public void setApiKeyValid(boolean valid) {
        if (this.apiKeyValid != valid) {
            this.apiKeyValid = valid;
            if (valid) {
                setConnectionState(ConnectionState.CONNECTED, "API Key validated successfully");
            } else {
                setConnectionState(ConnectionState.DISCONNECTED, "Invalid API key");
            }
            notifyStateChange();
        }
    }

    /**
     * Gets the current recording state
     */
    public RecordingState getRecordingState() {
        return recordingState;
    }

    /**
     * Sets the recording state
     */
    public void setRecordingState(RecordingState state) {
        if (this.recordingState != state) {
            this.recordingState = state;
            notifyStateChange();
        }
    }

    /**
     * Gets the current connection state
     */
    public ConnectionState getConnectionState() {
        return connectionState;
    }

    /**
     * Sets the connection state with a message
     */
    public void setConnectionState(ConnectionState state, String message) {
        boolean changed = false;

        if (this.connectionState != state) {
            this.connectionState = state;
            changed = true;
        }

        if (!message.equals(this.connectionMessage)) {
            this.connectionMessage = message;
            changed = true;
        }

        if (changed) {
            notifyStateChange();
        }
    }

    /**
     * Gets the connection status message
     */
    public String getConnectionMessage() {
        return connectionMessage;
    }

    /**
     * Sets an error message
     */
    public void setError(String error) {
        this.lastError = error;
        notifyStateChange();
    }

    /**
     * Gets the last error message
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Clears the last error
     */
    public void clearError() {
        if (lastError != null) {
            lastError = null;
            notifyStateChange();
        }
    }

    // Convenience methods for recording state
    public boolean isRecording() {
        return recordingState == RecordingState.RECORDING;
    }

    public boolean isRecordingStopping() {
        return recordingState == RecordingState.STOPPING;
    }

    public boolean isProcessing() {
        return recordingState == RecordingState.PROCESSING;
    }

    public boolean isRecordingIdle() {
        return recordingState == RecordingState.IDLE;
    }

    // Convenience methods for connection state
    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }

    public boolean isConnecting() {
        return connectionState == ConnectionState.TESTING;
    }

    public boolean isDisconnected() {
        return connectionState == ConnectionState.DISCONNECTED;
    }

    public boolean hasConnectionError() {
        return connectionState == ConnectionState.ERROR;
    }

    /**
     * Adds a state change listener
     */
    public void addStateChangeListener(Consumer<ApplicationState> listener) {
        stateChangeListeners.add(listener);
    }

    /**
     * Removes a state change listener
     */
    public void removeStateChangeListener(Consumer<ApplicationState> listener) {
        stateChangeListeners.remove(listener);
    }

    /**
     * Notifies all listeners of state change
     */
    private void notifyStateChange() {
        for (Consumer<ApplicationState> listener : stateChangeListeners) {
            try {
                listener.accept(this);
            } catch (Exception e) {
                System.err.println("Error in state change listener: " + e.getMessage());
            }
        }
    }

    /**
     * Resets the application state to initial values
     */
    public void reset() {
        apiKeyValid = false;
        recordingState = RecordingState.IDLE;
        connectionState = ConnectionState.DISCONNECTED;
        connectionMessage = "Disconnected";
        lastError = null;
        notifyStateChange();
    }

    /**
     * Gets a summary of the current state for debugging
     */
    @Override
    public String toString() {
        return "ApplicationState{" +
                "apiKeyValid=" + apiKeyValid +
                ", recordingState=" + recordingState +
                ", connectionState=" + connectionState +
                ", connectionMessage='" + connectionMessage + '\'' +
                ", lastError='" + lastError + '\'' +
                '}';
    }
}