package com.voiceai.service;

import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for handling file operations throughout the application.
 * Manages save/load operations for transcriptions and other text content.
 */
public class FileOperationsService {

    private static final Logger logger = Logger.getLogger(FileOperationsService.class.getName());
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final NotificationService notificationService;

    /**
     * Supported file formats for transcription files
     */
    public enum FileFormat {
        TXT("Text Files", "*.txt", "txt"),
        MARKDOWN("Markdown Files", "*.md", "md"),
        JSON("JSON Files", "*.json", "json"),
        ALL("All Files", "*.*", "*");

        private final String description;
        private final String extension;
        private final String suffix;

        FileFormat(String description, String extension, String suffix) {
            this.description = description;
            this.extension = extension;
            this.suffix = suffix;
        }

        public String getDescription() {
            return description;
        }

        public String getExtension() {
            return extension;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    /**
     * Result class for file operations
     */
    public static class FileOperationResult {
        private final boolean success;
        private final String message;
        private final File file;
        private final Exception exception;

        private FileOperationResult(boolean success, String message, File file, Exception exception) {
            this.success = success;
            this.message = message;
            this.file = file;
            this.exception = exception;
        }

        public static FileOperationResult success(String message, File file) {
            return new FileOperationResult(true, message, file, null);
        }

        public static FileOperationResult failure(String message, Exception exception) {
            return new FileOperationResult(false, message, null, exception);
        }

        public static FileOperationResult cancelled() {
            return new FileOperationResult(false, "Operation cancelled by user", null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public File getFile() {
            return file;
        }

        public Exception getException() {
            return exception;
        }
    }

    /**
     * Creates a new FileOperationsService
     *
     * @param notificationService the notification service for user feedback
     */
    public FileOperationsService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Saves transcription text to a file with user-selected location
     *
     * @param content the text content to save
     * @param parentWindow the parent window for the file dialog
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult saveTranscription(String content, Window parentWindow) {
        return saveTranscription(content, parentWindow, "transcription");
    }

    /**
     * Saves transcription text to a file with custom base filename
     *
     * @param content the text content to save
     * @param parentWindow the parent window for the file dialog
     * @param baseFileName the base name for the file (timestamp will be added)
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult saveTranscription(String content, Window parentWindow, String baseFileName) {
        if (content == null || content.trim().isEmpty()) {
            notificationService.showWarning("No content to save");
            return FileOperationResult.failure("No content to save", null);
        }

        FileChooser fileChooser = createSaveFileChooser(baseFileName);
        File file = fileChooser.showSaveDialog(parentWindow);

        if (file == null) {
            return FileOperationResult.cancelled();
        }

        return saveContentToFile(content.trim(), file);
    }

    /**
     * Loads transcription text from a user-selected file
     *
     * @param parentWindow the parent window for the file dialog
     * @return FileOperationResult with loaded content or error information
     */
    public FileOperationResult loadTranscription(Window parentWindow) {
        FileChooser fileChooser = createLoadFileChooser();
        File file = fileChooser.showOpenDialog(parentWindow);

        if (file == null) {
            return FileOperationResult.cancelled();
        }

        return loadContentFromFile(file);
    }

    /**
     * Saves content directly to a specified file
     *
     * @param content the content to save
     * @param file the target file
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult saveContentToFile(String content, File file) {
        if (content == null) {
            content = "";
        }

        try {
            // Ensure parent directory exists
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (!dirCreated) {
                    String error = "Failed to create directory: " + parentDir.getAbsolutePath();
                    notificationService.showError(error);
                    return FileOperationResult.failure(error, null);
                }
            }

            // Write content to file using UTF-8 encoding
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            String successMessage = "File saved successfully: " + file.getName();
            notificationService.showSuccess(successMessage);
            logger.info("Successfully saved file: " + file.getAbsolutePath() + " (" + content.length() + " characters)");

            return FileOperationResult.success(successMessage, file);

        } catch (IOException e) {
            String errorMessage = "Failed to save file: " + e.getMessage();
            notificationService.showError(errorMessage, e);
            logger.severe("Failed to save file " + file.getAbsolutePath() + ": " + e.getMessage());

            return FileOperationResult.failure(errorMessage, e);
        }
    }

    /**
     * Loads content from a specified file
     *
     * @param file the file to load
     * @return FileOperationResult with loaded content or error information
     */
    public FileOperationResult loadContentFromFile(File file) {
        if (file == null || !file.exists()) {
            String error = "File does not exist";
            notificationService.showError(error);
            return FileOperationResult.failure(error, null);
        }

        if (!file.isFile()) {
            String error = "Selected path is not a file: " + file.getName();
            notificationService.showError(error);
            return FileOperationResult.failure(error, null);
        }

        if (!file.canRead()) {
            String error = "Cannot read file: " + file.getName();
            notificationService.showError(error);
            return FileOperationResult.failure(error, null);
        }

        try {
            // Read file content using UTF-8 encoding
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            String successMessage = "File loaded successfully: " + file.getName();
            notificationService.showSuccess(successMessage);
            logger.info("Successfully loaded file: " + file.getAbsolutePath() + " (" + content.length() + " characters)");

            // Return result with content stored in message field for now
            // In a more complex implementation, we might have a separate content field
            FileOperationResult result = FileOperationResult.success(content, file);
            return result;

        } catch (IOException e) {
            String errorMessage = "Failed to load file: " + e.getMessage();
            notificationService.showError(errorMessage, e);
            logger.severe("Failed to load file " + file.getAbsolutePath() + ": " + e.getMessage());

            return FileOperationResult.failure(errorMessage, e);
        }
    }

    /**
     * Selects all text in the provided TextArea
     *
     * @param textArea the TextArea to select all text in
     */
    public void selectAllText(TextArea textArea) {
        if (textArea != null) {
            textArea.selectAll();
            textArea.requestFocus();
            logger.info("Selected all text in TextArea");
        }
    }

    /**
     * Creates a FileChooser configured for saving transcription files
     *
     * @param baseFileName the base filename to use
     * @return configured FileChooser
     */
    private FileChooser createSaveFileChooser(String baseFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Transcription");

        // Generate filename with timestamp
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
        String fileName = (baseFileName != null && !baseFileName.trim().isEmpty())
                ? baseFileName.trim() + "_" + timestamp + ".txt"
                : "transcription_" + timestamp + ".txt";

        fileChooser.setInitialFileName(fileName);

        // Set extension filters
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(FileFormat.TXT.getDescription(), FileFormat.TXT.getExtension()),
                new FileChooser.ExtensionFilter(FileFormat.MARKDOWN.getDescription(), FileFormat.MARKDOWN.getExtension()),
                new FileChooser.ExtensionFilter(FileFormat.JSON.getDescription(), FileFormat.JSON.getExtension()),
                new FileChooser.ExtensionFilter(FileFormat.ALL.getDescription(), FileFormat.ALL.getExtension())
        );

        return fileChooser;
    }

    /**
     * Creates a FileChooser configured for loading transcription files
     *
     * @return configured FileChooser
     */
    private FileChooser createLoadFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Transcription");

        // Set extension filters (same as save, but no initial filename)
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(FileFormat.TXT.getDescription(), FileFormat.TXT.getExtension()),
                new FileChooser.ExtensionFilter(FileFormat.MARKDOWN.getDescription(), FileFormat.MARKDOWN.getExtension()),
                new FileChooser.ExtensionFilter(FileFormat.JSON.getDescription(), FileFormat.JSON.getExtension()),
                new FileChooser.ExtensionFilter(FileFormat.ALL.getDescription(), FileFormat.ALL.getExtension())
        );

        return fileChooser;
    }

    /**
     * Validates if a file path is safe to write to
     *
     * @param file the file to validate
     * @return true if the file path is safe
     */
    public boolean isFilePathSafe(File file) {
        if (file == null) {
            return false;
        }

        try {
            Path filePath = file.toPath().toAbsolutePath().normalize();
            Path userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
            Path userDocuments = userHome.resolve("Documents").normalize();
            Path userDesktop = userHome.resolve("Desktop").normalize();
            Path currentDir = Path.of(".").toAbsolutePath().normalize();

            // Allow writes to user home, documents, desktop, and current directory
            return filePath.startsWith(userHome) ||
                    filePath.startsWith(userDocuments) ||
                    filePath.startsWith(userDesktop) ||
                    filePath.startsWith(currentDir);

        } catch (Exception e) {
            logger.warning("Failed to validate file path safety: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the file extension from a filename
     *
     * @param fileName the filename
     * @return the extension (without dot) or empty string if none
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }

    /**
     * Generates a safe filename by removing or replacing invalid characters
     *
     * @param fileName the original filename
     * @return a safe filename for the current operating system
     */
    public String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "untitled";
        }

        // Remove or replace invalid characters for most file systems
        String sanitized = fileName.trim()
                .replaceAll("[<>:\"/\\\\|?*]", "_")  // Replace invalid chars with underscore
                .replaceAll("\\s+", "_")              // Replace whitespace with underscore
                .replaceAll("_{2,}", "_");            // Replace multiple underscores with single

        // Remove leading/trailing underscores
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        // Ensure it's not empty after sanitization
        if (sanitized.isEmpty()) {
            sanitized = "untitled";
        }

        // Limit length to reasonable size (most filesystems support 255 chars)
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }

        return sanitized;
    }
}