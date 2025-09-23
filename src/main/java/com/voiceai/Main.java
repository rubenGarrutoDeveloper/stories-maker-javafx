package com.voiceai;

import com.voiceai.service.AudioRecordingService;
import com.voiceai.service.OpenAIService;
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

    // State
    private boolean isApiKeyValid = false;
    private boolean isCurrentlyRecording = false;

    @Override
    public void start(Stage primaryStage) {
        // Initialize services
        openAIService = new OpenAIService();
        audioRecordingService = new AudioRecordingService();

        primaryStage.setTitle("StorieS Maker");

        // Create main layout
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // API Configuration Section
        VBox apiSection = createApiSection();

        // Main Content - Two panels
        HBox mainContent = createMainContent();

        root.getChildren().addAll(apiSection, mainContent);

        Scene scene = new Scene(root, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();

        // Initialize event handlers
        setupEventHandlers();

        // Update UI state
        updateUIState();
    }

    private VBox createApiSection() {
        VBox apiSection = new VBox(10);

        Label titleLabel = new Label("StorieS Maker");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #6b46c1;");

        HBox apiKeyBox = new HBox(10);
        apiKeyBox.setAlignment(Pos.CENTER_LEFT);

        apiKeyField = new TextField();
        apiKeyField.setPromptText("OpenAI API Key");
        apiKeyField.setPrefWidth(300);
        apiKeyField.setStyle("-fx-padding: 8px; -fx-border-radius: 5px;");

        testConnectionButton = new Button("Test Connection");
        testConnectionButton.setStyle("-fx-background-color: #e5e7eb; -fx-border-radius: 5px; -fx-padding: 8px 15px;");

        connectionStatus = new Label("Disconnected");
        connectionStatus.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 5px 10px; -fx-border-radius: 15px;");

        apiKeyBox.getChildren().addAll(apiKeyField, testConnectionButton, connectionStatus);
        apiSection.getChildren().addAll(titleLabel, apiKeyBox);

        return apiSection;
    }

    private HBox createMainContent() {
        HBox mainContent = new HBox(20);
        mainContent.setAlignment(Pos.TOP_CENTER);

        // Left Panel - Transcription
        VBox transcriptionPanel = createTranscriptionPanel();
        transcriptionPanel.setPrefWidth(580);

        // Right Panel - Chat GPT
        VBox chatPanel = createChatPanel();
        chatPanel.setPrefWidth(580);

        mainContent.getChildren().addAll(transcriptionPanel, chatPanel);

        return mainContent;
    }

    private VBox createTranscriptionPanel() {
        VBox panel = new VBox(15);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label("TRANSCRIPT");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        header.getChildren().add(headerLabel);

        // Control buttons
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        recButton = new Button("🔴 REC");
        recButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");

        saveButton = new Button("💾 SAVE");
        saveButton.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");

        selectAllButton = new Button("📋 SELECT ALL");
        selectAllButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");

        loadButton = new Button("📂 LOAD");
        loadButton.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");

        controls.getChildren().addAll(recButton, saveButton, selectAllButton, loadButton);

        // Transcription area
        transcriptionArea = new TextArea();
        transcriptionArea.setPromptText("Premi REC per iniziare la trascrizione...");
        transcriptionArea.setPrefHeight(400);
        transcriptionArea.setWrapText(true);
        transcriptionArea.setStyle("-fx-control-inner-background: white; -fx-border-color: #d1d5db; -fx-border-radius: 5px;");

        panel.getChildren().addAll(header, controls, transcriptionArea);
        panel.setStyle("-fx-background-color: white; -fx-border-radius: 10px; -fx-padding: 20px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        return panel;
    }

    private VBox createChatPanel() {
        VBox panel = new VBox(15);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label("💬 CHAT GPT");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        header.getChildren().add(headerLabel);

        // Chat area
        chatArea = new TextArea();
        chatArea.setPromptText("Scrivi un messaggio per iniziare...");
        chatArea.setPrefHeight(300);
        chatArea.setWrapText(true);
        chatArea.setEditable(false);
        chatArea.setStyle("-fx-control-inner-background: white; -fx-border-color: #d1d5db; -fx-border-radius: 5px;");

        // Message input section
        VBox inputSection = new VBox(10);

        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER);

        messageField = new TextField();
        messageField.setPromptText("Scrivi un messaggio...");
        messageField.setPrefWidth(450);
        messageField.setStyle("-fx-padding: 8px; -fx-border-radius: 5px;");

        sendButton = new Button("➤");
        sendButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 12px;");

        messageBox.getChildren().addAll(messageField, sendButton);

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);

        insertTranscriptButton = new Button("📝 Insert Transcript");
        insertTranscriptButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");

        clearButton = new Button("🗑️ Clear");
        clearButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");

        actionButtons.getChildren().addAll(insertTranscriptButton, clearButton);

        inputSection.getChildren().addAll(messageBox, actionButtons);

        panel.getChildren().addAll(header, chatArea, inputSection);
        panel.setStyle("-fx-background-color: white; -fx-border-radius: 10px; -fx-padding: 20px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        return panel;
    }

    private void setupEventHandlers() {
        // Test Connection button
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

    // Event handler methods
    private void testApiConnection() {
        String apiKey = apiKeyField.getText().trim();

        if (apiKey.isEmpty()) {
            updateConnectionStatus(false, "Please enter an API key");
            return;
        }

        // Disable button and show loading state
        testConnectionButton.setDisable(true);
        testConnectionButton.setText("Testing...");
        updateConnectionStatus(false, "Testing...", "#f59e0b"); // Orange color

        // Validate API key asynchronously
        openAIService.validateApiKey(apiKey)
                .thenAccept(result -> {
                    // Update UI on JavaFX Application Thread
                    Platform.runLater(() -> {
                        isApiKeyValid = result.isValid();

                        if (result.isValid()) {
                            // Store the valid API key
                            openAIService.setApiKey(apiKey);
                            updateConnectionStatus(true, result.getMessage());
                            showNotification("API Key validated successfully!", "SUCCESS");
                        } else {
                            updateConnectionStatus(false, result.getMessage());
                            showNotification("API Key validation failed: " + result.getMessage(), "ERROR");
                        }

                        // Re-enable button and update UI state
                        testConnectionButton.setDisable(false);
                        testConnectionButton.setText("Test Connection");
                        updateUIState();
                    });
                })
                .exceptionally(throwable -> {
                    // Handle any unexpected errors
                    Platform.runLater(() -> {
                        updateConnectionStatus(false, "Validation failed: " + throwable.getMessage());
                        showNotification("Connection test failed", "ERROR");
                        testConnectionButton.setDisable(false);
                        testConnectionButton.setText("Test Connection");
                    });
                    return null;
                });
    }

    /**
     * Updates the connection status label
     */
    private void updateConnectionStatus(boolean isConnected, String message) {
        updateConnectionStatus(isConnected, message, null);
    }

    /**
     * Updates the connection status label with custom color
     */
    private void updateConnectionStatus(boolean isConnected, String message, String customColor) {
        String color = customColor;
        if (color == null) {
            color = isConnected ? "#10b981" : "#ef4444"; // Green for success, red for error
        }

        connectionStatus.setText(message);
        connectionStatus.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 5px 10px; -fx-border-radius: 15px;");
    }

    /**
     * Shows a temporary notification (simple console output for now)
     */
    private void showNotification(String message, String type) {
        System.out.println("[" + type + "] " + message);
        // TODO: Implement proper notification system (toast, alert, etc.)
    }

    /**
     * Updates UI components based on current application state
     */
    private void updateUIState() {
        // Enable/disable recording button based on API key validity
        recButton.setDisable(!isApiKeyValid);

        // Update button text and style based on API key validity
        if (isApiKeyValid) {
            recButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");
        } else {
            recButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");
        }

        // Enable/disable send button based on API key validity
        sendButton.setDisable(!isApiKeyValid);
        if (isApiKeyValid) {
            sendButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 12px;");
        } else {
            sendButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 12px;");
        }
    }

    private void toggleRecording() {
        if (!isCurrentlyRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    // Add these new methods:
    private void startRecording() {
        // Check microphone availability
        if (!AudioRecordingService.isMicrophoneAvailable()) {
            showNotification("Microphone not available", "ERROR");
            return;
        }

        // Update UI immediately
        isCurrentlyRecording = true;
        updateRecordingUI();

        // Start recording asynchronously
        audioRecordingService.startRecording()
                .thenCompose(v -> {
                    // Start capturing audio data
                    return audioRecordingService.captureAudio();
                })
                .thenAccept(audioData -> {
                    // This will be called when recording stops
                    Platform.runLater(() -> {
                        if (audioData.length > 0) {
                            showNotification("Recording completed: " + audioData.length + " bytes captured", "SUCCESS");
                            transcriptionArea.appendText("[Processing " + audioData.length + " bytes...]\n");

                            // Transcribe audio using Whisper API
                            openAIService.transcribeAudio(audioData)
                                    .thenAccept(result -> {
                                        Platform.runLater(() -> {
                                            if (result.isSuccess()) {
                                                transcriptionArea.appendText(result.getText() + "\n\n");
                                                showNotification("Transcription completed!", "SUCCESS");
                                            } else {
                                                transcriptionArea.appendText("[Transcription failed: " + result.getMessage() + "]\n\n");
                                                showNotification("Transcription failed: " + result.getMessage(), "ERROR");
                                            }
                                        });
                                    })
                                    .exceptionally(throwable -> {
                                        Platform.runLater(() -> {
                                            transcriptionArea.appendText("[Transcription error: " + throwable.getMessage() + "]\n\n");
                                            showNotification("Transcription error", "ERROR");
                                        });
                                        return null;
                                    });
                        } else {
                            showNotification("No audio data captured", "WARNING");
                        }

                        isCurrentlyRecording = false;
                        updateRecordingUI();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showNotification("Recording failed: " + throwable.getMessage(), "ERROR");
                        isCurrentlyRecording = false;
                        updateRecordingUI();
                    });
                    return null;
                });
    }

    private void stopRecording() {
        if (audioRecordingService.isRecording()) {
            // Update UI to show processing state
            recButton.setText("⏹️ STOPPING...");
            recButton.setDisable(true);

            audioRecordingService.stopRecording()
                    .thenAccept(audioData -> {
                        Platform.runLater(() -> {
                            if (audioData.length > 0) {
                                showNotification("Recording stopped: " + audioData.length + " bytes captured", "SUCCESS");
                                transcriptionArea.appendText("[Processing " + audioData.length + " bytes...]\n");

                                // Transcribe audio using Whisper API
                                openAIService.transcribeAudio(audioData)
                                        .thenAccept(result -> {
                                            Platform.runLater(() -> {
                                                if (result.isSuccess()) {
                                                    transcriptionArea.appendText(result.getText() + "\n\n");
                                                    showNotification("Transcription completed!", "SUCCESS");
                                                } else {
                                                    transcriptionArea.appendText("[Transcription failed: " + result.getMessage() + "]\n\n");
                                                    showNotification("Transcription failed: " + result.getMessage(), "ERROR");
                                                }
                                            });
                                        })
                                        .exceptionally(throwable -> {
                                            Platform.runLater(() -> {
                                                transcriptionArea.appendText("[Transcription error: " + throwable.getMessage() + "]\n\n");
                                                showNotification("Transcription error", "ERROR");
                                            });
                                            return null;
                                        });
                            }

                            isCurrentlyRecording = false;
                            updateRecordingUI();
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            showNotification("Failed to stop recording: " + throwable.getMessage(), "ERROR");
                            isCurrentlyRecording = false;
                            updateRecordingUI();
                        });
                        return null;
                    });
        }
    }

    private void updateRecordingUI() {
        if (isCurrentlyRecording) {
            recButton.setText("⏹️ STOP");
            recButton.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");
            transcriptionArea.setPromptText("🎤 Recording in progress...");
        } else {
            recButton.setText("🔴 REC");
            recButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-border-radius: 5px; -fx-padding: 8px 15px;");
            transcriptionArea.setPromptText("Premi REC per iniziare la trascrizione...");
            recButton.setDisable(false);
        }
    }



    private void saveTranscription() {
        // TODO: Implement save functionality
        System.out.println("Saving transcription...");
    }

    private void selectAllTranscription() {
        transcriptionArea.selectAll();
    }

    private void loadTranscription() {
        // TODO: Implement load functionality
        System.out.println("Loading transcription...");
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // TODO: Implement ChatGPT integration
            chatArea.appendText("You: " + message + "\n\n");
            messageField.clear();
            System.out.println("Sending message: " + message);
        }
    }

    private void insertTranscript() {
        String transcript = transcriptionArea.getText();
        if (!transcript.isEmpty()) {
            messageField.setText(transcript);
        }
    }

    private void clearChat() {
        chatArea.clear();
    }

    public static void main(String[] args) {
        launch(args);
    }
}