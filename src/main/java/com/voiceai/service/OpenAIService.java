package com.voiceai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.voiceai.model.ChatMessage;
import com.voiceai.model.Conversation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class OpenAIService {

    private static final String OPENAI_BASE_URL = "https://api.openai.com/v1";
    private static final String MODELS_ENDPOINT = "/models";
    private static final String TRANSCRIPTIONS_ENDPOINT = "/audio/transcriptions";
    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";

    // Chat model configuration
    private static final String DEFAULT_CHAT_MODEL = "gpt-3.5-turbo";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 2000;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String apiKey;

    public OpenAIService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sets the API key for OpenAI requests
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Validates the API key by making a request to the models endpoint
     * Returns a CompletableFuture for async handling
     */
    public CompletableFuture<ApiValidationResult> validateApiKey(String testApiKey) {
        if (testApiKey == null || testApiKey.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    new ApiValidationResult(false, "API key cannot be empty")
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_BASE_URL + MODELS_ENDPOINT))
                .header("Authorization", "Bearer " + testApiKey.trim())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> processValidationResponse(response))
                .exceptionally(throwable -> new ApiValidationResult(false,
                        "Network error: " + throwable.getMessage()));
    }

    /**
     * Sends a chat completion request to OpenAI
     * @param conversation The conversation context
     * @return CompletableFuture containing the chat response
     */
    public CompletableFuture<ChatCompletionResult> sendChatCompletion(Conversation conversation) {
        return sendChatCompletion(conversation, DEFAULT_CHAT_MODEL, DEFAULT_TEMPERATURE, DEFAULT_MAX_TOKENS);
    }

    /**
     * Sends a chat completion request with custom parameters
     */
    public CompletableFuture<ChatCompletionResult> sendChatCompletion(
            Conversation conversation,
            String model,
            double temperature,
            int maxTokens) {

        if (!hasValidApiKey()) {
            return CompletableFuture.completedFuture(
                    new ChatCompletionResult(false, "", "No valid API key set", 0)
            );
        }

        try {
            // Create request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("stream", false); // We'll implement streaming separately

            // Add messages
            ArrayNode messagesArray = objectMapper.createArrayNode();
            for (ChatMessage message : conversation.getMessagesForAPI()) {
                ObjectNode messageNode = objectMapper.createObjectNode();
                messageNode.put("role", message.getRole().getValue());
                messageNode.put("content", message.getContent());
                messagesArray.add(messageNode);
            }
            requestBody.set("messages", messagesArray);

            String requestBodyString = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_BASE_URL + CHAT_COMPLETIONS_ENDPOINT))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60)) // Longer timeout for chat responses
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::processChatCompletionResponse)
                    .exceptionally(throwable -> new ChatCompletionResult(false, "",
                            "Network error: " + throwable.getMessage(), 0));

        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    new ChatCompletionResult(false, "",
                            "Failed to prepare request: " + e.getMessage(), 0)
            );
        }
    }

    /**
     * Sends a streaming chat completion request
     * @param conversation The conversation context
     * @param onChunk Callback for each streaming chunk
     * @param onComplete Callback when streaming is complete
     * @return CompletableFuture that completes when streaming starts
     */
    public CompletableFuture<Void> sendStreamingChatCompletion(
            Conversation conversation,
            Consumer<String> onChunk,
            Consumer<ChatCompletionResult> onComplete) {

        if (!hasValidApiKey()) {
            ChatCompletionResult errorResult = new ChatCompletionResult(false, "", "No valid API key set", 0);
            onComplete.accept(errorResult);
            return CompletableFuture.completedFuture(null);
        }

        try {
            // Create request body for streaming
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", DEFAULT_CHAT_MODEL);
            requestBody.put("temperature", DEFAULT_TEMPERATURE);
            requestBody.put("max_tokens", DEFAULT_MAX_TOKENS);
            requestBody.put("stream", true);

            // Add messages
            ArrayNode messagesArray = objectMapper.createArrayNode();
            for (ChatMessage message : conversation.getMessagesForAPI()) {
                ObjectNode messageNode = objectMapper.createObjectNode();
                messageNode.put("role", message.getRole().getValue());
                messageNode.put("content", message.getContent());
                messagesArray.add(messageNode);
            }
            requestBody.set("messages", messagesArray);

            String requestBodyString = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_BASE_URL + CHAT_COMPLETIONS_ENDPOINT))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                    .thenAccept(response -> processStreamingResponse(response, onChunk, onComplete))
                    .exceptionally(throwable -> {
                        ChatCompletionResult errorResult = new ChatCompletionResult(false, "",
                                "Network error: " + throwable.getMessage(), 0);
                        onComplete.accept(errorResult);
                        return null;
                    });

        } catch (Exception e) {
            ChatCompletionResult errorResult = new ChatCompletionResult(false, "",
                    "Failed to prepare request: " + e.getMessage(), 0);
            onComplete.accept(errorResult);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Processes streaming response from OpenAI
     */
    private void processStreamingResponse(
            HttpResponse<Stream<String>> response,
            Consumer<String> onChunk,
            Consumer<ChatCompletionResult> onComplete) {

        if (response.statusCode() != 200) {
            ChatCompletionResult errorResult = new ChatCompletionResult(false, "",
                    "HTTP " + response.statusCode(), 0);
            onComplete.accept(errorResult);
            return;
        }

        StringBuilder fullContent = new StringBuilder();
        AtomicInteger totalTokens = new AtomicInteger();

        try {
            response.body().forEach(line -> {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();

                    if ("[DONE]".equals(data)) {
                        // Streaming complete
                        ChatCompletionResult result = new ChatCompletionResult(true,
                                fullContent.toString(), "Success", totalTokens.get());
                        onComplete.accept(result);
                        return;
                    }

                    try {
                        JsonNode dataNode = objectMapper.readTree(data);
                        JsonNode choices = dataNode.get("choices");

                        if (choices != null && choices.isArray() && choices.size() > 0) {
                            JsonNode firstChoice = choices.get(0);
                            JsonNode delta = firstChoice.get("delta");

                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").asText();
                                fullContent.append(content);
                                onChunk.accept(content);
                            }
                        }

                        // Extract token usage if available
                        JsonNode usage = dataNode.get("usage");
                        if (usage != null && usage.has("total_tokens")) {
                            totalTokens.set(usage.get("total_tokens").asInt());
                        }

                    } catch (Exception e) {
                        System.err.println("Error parsing streaming data: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            ChatCompletionResult errorResult = new ChatCompletionResult(false, "",
                    "Error processing stream: " + e.getMessage(), 0);
            onComplete.accept(errorResult);
        }
    }

    /**
     * Processes the HTTP response for chat completion
     */
    private ChatCompletionResult processChatCompletionResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();

        switch (statusCode) {
            case 200:
                try {
                    JsonNode jsonResponse = objectMapper.readTree(response.body());

                    // Extract response content
                    JsonNode choices = jsonResponse.get("choices");
                    if (choices == null || !choices.isArray() || choices.size() == 0) {
                        return new ChatCompletionResult(false, "", "No response choices found", 0);
                    }

                    JsonNode firstChoice = choices.get(0);
                    JsonNode message = firstChoice.get("message");
                    String content = message.get("content").asText().trim();

                    // Extract token usage
                    int totalTokens = 0;
                    JsonNode usage = jsonResponse.get("usage");
                    if (usage != null && usage.has("total_tokens")) {
                        totalTokens = usage.get("total_tokens").asInt();
                    }

                    return new ChatCompletionResult(true, content, "Success", totalTokens);

                } catch (Exception e) {
                    return new ChatCompletionResult(false, "", "Failed to parse response: " + e.getMessage(), 0);
                }

            case 400:
                return new ChatCompletionResult(false, "", "Bad request - invalid parameters or messages", 0);

            case 401:
                return new ChatCompletionResult(false, "", "Invalid API key", 0);

            case 429:
                return new ChatCompletionResult(false, "", "Rate limit exceeded - please try again later", 0);

            case 500:
            case 502:
            case 503:
                return new ChatCompletionResult(false, "", "OpenAI service temporarily unavailable", 0);

            default:
                return new ChatCompletionResult(false, "", "Unexpected error (HTTP " + statusCode + ")", 0);
        }
    }

    /**
     * Result class for chat completion responses
     */
    public static class ChatCompletionResult {
        private final boolean success;
        private final String content;
        private final String message;
        private final int tokensUsed;

        public ChatCompletionResult(boolean success, String content, String message, int tokensUsed) {
            this.success = success;
            this.content = content;
            this.message = message;
            this.tokensUsed = tokensUsed;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getContent() {
            return content;
        }

        public String getMessage() {
            return message;
        }

        public int getTokensUsed() {
            return tokensUsed;
        }

        @Override
        public String toString() {
            return "ChatCompletionResult{success=" + success + ", content='" + content +
                    "', message='" + message + "', tokensUsed=" + tokensUsed + "}";
        }
    }

    // ===== EXISTING TRANSCRIPTION METHODS =====

    /**
     * Transcribes audio using OpenAI Whisper API
     * @param audioData Raw audio bytes (will be converted to WAV)
     * @param language Language code (e.g., "it" for Italian, "en" for English)
     * @return CompletableFuture containing the transcribed text
     */
    public CompletableFuture<TranscriptionResult> transcribeAudio(byte[] audioData, String language) {
        if (!hasValidApiKey()) {
            return CompletableFuture.completedFuture(
                    new TranscriptionResult(false, "", "No valid API key set")
            );
        }

        if (audioData == null || audioData.length == 0) {
            return CompletableFuture.completedFuture(
                    new TranscriptionResult(false, "", "No audio data provided")
            );
        }

        try {
            // Convert to WAV format (create AudioRecordingService instance for conversion)
            AudioRecordingService audioService = new AudioRecordingService();
            byte[] wavData = audioService.convertToWavFormat(audioData);

            // Create multipart request
            String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();
            byte[] multipartBody = createMultipartBody(wavData, language != null ? language : "it", boundary);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_BASE_URL + TRANSCRIPTIONS_ENDPOINT))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(30)) // Longer timeout for file upload
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(this::processTranscriptionResponse)
                    .exceptionally(throwable -> new TranscriptionResult(false, "",
                            "Network error: " + throwable.getMessage()));

        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    new TranscriptionResult(false, "", "Failed to prepare request: " + e.getMessage())
            );
        }
    }

    /**
     * Transcribes audio with default Italian language
     */
    public CompletableFuture<TranscriptionResult> transcribeAudio(byte[] audioData) {
        return transcribeAudio(audioData, "it");
    }

    /**
     * Creates multipart/form-data body for Whisper API
     */
    private byte[] createMultipartBody(byte[] audioData, String language, String boundary) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String lineEnd = "\r\n";

        // Audio file part
        body.write(("--" + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
        body.write("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"".getBytes(StandardCharsets.UTF_8));
        body.write(lineEnd.getBytes(StandardCharsets.UTF_8));
        body.write("Content-Type: audio/wav".getBytes(StandardCharsets.UTF_8));
        body.write((lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
        body.write(audioData);
        body.write(lineEnd.getBytes(StandardCharsets.UTF_8));

        // Model part
        body.write(("--" + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
        body.write("Content-Disposition: form-data; name=\"model\"".getBytes(StandardCharsets.UTF_8));
        body.write((lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
        body.write("whisper-1".getBytes(StandardCharsets.UTF_8));
        body.write(lineEnd.getBytes(StandardCharsets.UTF_8));

        // Language part
        body.write(("--" + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
        body.write("Content-Disposition: form-data; name=\"language\"".getBytes(StandardCharsets.UTF_8));
        body.write((lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
        body.write(language.getBytes(StandardCharsets.UTF_8));
        body.write(lineEnd.getBytes(StandardCharsets.UTF_8));

        // Response format part
        body.write(("--" + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
        body.write("Content-Disposition: form-data; name=\"response_format\"".getBytes(StandardCharsets.UTF_8));
        body.write((lineEnd + lineEnd).getBytes(StandardCharsets.UTF_8));
        body.write("json".getBytes(StandardCharsets.UTF_8));
        body.write(lineEnd.getBytes(StandardCharsets.UTF_8));

        // End boundary
        body.write(("--" + boundary + "--" + lineEnd).getBytes(StandardCharsets.UTF_8));

        return body.toByteArray();
    }

    /**
     * Processes the HTTP response from Whisper API
     */
    private TranscriptionResult processTranscriptionResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();

        switch (statusCode) {
            case 200:
                try {
                    JsonNode jsonResponse = objectMapper.readTree(response.body());
                    String transcribedText = jsonResponse.get("text").asText();
                    return new TranscriptionResult(true, transcribedText.trim(), "Success");
                } catch (Exception e) {
                    return new TranscriptionResult(false, "", "Failed to parse response: " + e.getMessage());
                }

            case 400:
                return new TranscriptionResult(false, "", "Bad request - invalid audio format or parameters");

            case 401:
                return new TranscriptionResult(false, "", "Invalid API key");

            case 413:
                return new TranscriptionResult(false, "", "Audio file too large (max 25MB)");

            case 429:
                return new TranscriptionResult(false, "", "Rate limit exceeded");

            case 500:
            case 502:
            case 503:
                return new TranscriptionResult(false, "", "OpenAI service temporarily unavailable");

            default:
                return new TranscriptionResult(false, "", "Unexpected error (HTTP " + statusCode + ")");
        }
    }

    /**
     * Result class for audio transcription
     */
    public static class TranscriptionResult {
        private final boolean success;
        private final String text;
        private final String message;

        public TranscriptionResult(boolean success, String text, String message) {
            this.success = success;
            this.text = text;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getText() {
            return text;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "TranscriptionResult{success=" + success + ", text='" + text + "', message='" + message + "'}";
        }
    }

    /**
     * Processes the HTTP response for API validation
     */
    private ApiValidationResult processValidationResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();

        switch (statusCode) {
            case 200:
                // Success - API key is valid
                try {
                    JsonNode jsonResponse = objectMapper.readTree(response.body());
                    JsonNode data = jsonResponse.get("data");
                    int modelCount = data != null ? data.size() : 0;
                    return new ApiValidationResult(true,
                            "Connected successfully! " + modelCount + " models available.");
                } catch (Exception e) {
                    return new ApiValidationResult(true, "Connected successfully!");
                }

            case 401:
                return new ApiValidationResult(false, "Invalid API key");

            case 403:
                return new ApiValidationResult(false, "API key doesn't have required permissions");

            case 429:
                return new ApiValidationResult(false, "Rate limit exceeded - try again later");

            case 500:
            case 502:
            case 503:
                return new ApiValidationResult(false, "OpenAI service temporarily unavailable");

            default:
                return new ApiValidationResult(false,
                        "Unexpected error (HTTP " + statusCode + ")");
        }
    }

    /**
     * Checks if the service has a valid API key set
     */
    public boolean hasValidApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Gets the current API key (masked for security)
     */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 8) {
            return "Not set";
        }
        return "sk-..." + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Result class for API validation
     */
    public static class ApiValidationResult {
        private final boolean valid;
        private final String message;

        public ApiValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "ApiValidationResult{valid=" + valid + ", message='" + message + "'}";
        }
    }

    /**
     * Test method to check service configuration
     */
    public void testService() {
        System.out.println("OpenAI Service initialized");
        System.out.println("Base URL: " + OPENAI_BASE_URL);
        System.out.println("HTTP Client timeout: 10s");
    }
}