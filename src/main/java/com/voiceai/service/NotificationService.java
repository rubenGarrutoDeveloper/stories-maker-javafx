package com.voiceai.service;

import javafx.application.Platform;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service for handling user notifications throughout the application.
 * Currently provides console-based notifications with support for future UI enhancements.
 */
public class NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // Queue for managing notifications (for future enhancements)
    private final BlockingQueue<Notification> notificationQueue;
    private final boolean enableLogging;

    /**
     * Types of notifications supported by the service
     */
    public enum NotificationType {
        SUCCESS("SUCCESS", "✓"),
        ERROR("ERROR", "✗"),
        WARNING("WARNING", "⚠"),
        INFO("INFO", "ℹ");

        private final String label;
        private final String symbol;

        NotificationType(String label, String symbol) {
            this.label = label;
            this.symbol = symbol;
        }

        public String getLabel() {
            return label;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    /**
     * Internal notification data structure
     */
    private static class Notification {
        final NotificationType type;
        final String message;
        final String details;
        final LocalDateTime timestamp;

        Notification(NotificationType type, String message, String details) {
            this.type = type;
            this.message = message != null ? message : "";
            this.details = details;
            this.timestamp = LocalDateTime.now();
        }
    }

    /**
     * Creates a new NotificationService with logging enabled
     */
    public NotificationService() {
        this(true);
    }

    /**
     * Creates a new NotificationService with configurable logging
     *
     * @param enableLogging whether to enable console logging
     */
    public NotificationService(boolean enableLogging) {
        this.enableLogging = enableLogging;
        this.notificationQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Shows a simple notification with message and type
     *
     * @param message the notification message
     * @param type the notification type (SUCCESS, ERROR, WARNING, INFO)
     */
    public void showNotification(String message, String type) {
        NotificationType notificationType;
        try {
            notificationType = NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            notificationType = NotificationType.INFO;
            logger.warning("Unknown notification type: " + type + ". Using INFO instead.");
        }

        showNotification(message, notificationType);
    }

    /**
     * Shows a notification with the specified type
     *
     * @param message the notification message
     * @param type the notification type
     */
    public void showNotification(String message, NotificationType type) {
        showNotification(message, type, null);
    }

    /**
     * Shows a detailed notification with additional details
     *
     * @param message the main notification message
     * @param type the notification type
     * @param details additional details or context
     */
    public void showNotification(String message, NotificationType type, String details) {
        if (message == null || message.trim().isEmpty()) {
            logger.warning("Attempted to show notification with empty message");
            return;
        }

        Notification notification = new Notification(type, message.trim(), details);

        // Add to queue for future processing
        notificationQueue.offer(notification);

        // Process immediately for now (console output)
        processNotification(notification);
    }

    /**
     * Shows a success notification
     *
     * @param message the success message
     */
    public void showSuccess(String message) {
        showNotification(message, NotificationType.SUCCESS);
    }

    /**
     * Shows an error notification
     *
     * @param message the error message
     */
    public void showError(String message) {
        showNotification(message, NotificationType.ERROR);
    }

    /**
     * Shows a warning notification
     *
     * @param message the warning message
     */
    public void showWarning(String message) {
        showNotification(message, NotificationType.WARNING);
    }

    /**
     * Shows an info notification
     *
     * @param message the info message
     */
    public void showInfo(String message) {
        showNotification(message, NotificationType.INFO);
    }

    /**
     * Shows an error notification with exception details
     *
     * @param message the error message
     * @param throwable the exception that caused the error
     */
    public void showError(String message, Throwable throwable) {
        String details = throwable != null ? throwable.getMessage() : null;
        showNotification(message, NotificationType.ERROR, details);

        // Log the full exception for debugging
        if (throwable != null) {
            logger.log(Level.SEVERE, "Error notification with exception: " + message, throwable);
        }
    }

    /**
     * Processes a notification (currently console output, designed for future UI enhancement)
     *
     * @param notification the notification to process
     */
    private void processNotification(Notification notification) {
        // Ensure UI updates happen on JavaFX Application Thread
        if (Platform.isFxApplicationThread()) {
            displayNotification(notification);
        } else {
            Platform.runLater(() -> displayNotification(notification));
        }
    }

    /**
     * Displays the notification (console implementation)
     *
     * @param notification the notification to display
     */
    private void displayNotification(Notification notification) {
        if (!enableLogging) {
            return;
        }

        String timestamp = notification.timestamp.format(TIMESTAMP_FORMAT);
        String symbol = notification.type.getSymbol();
        String typeLabel = notification.type.getLabel();

        // Format: [HH:mm:ss.SSS] ✓ SUCCESS: Message
        StringBuilder output = new StringBuilder();
        output.append("[").append(timestamp).append("] ");
        output.append(symbol).append(" ");
        output.append(typeLabel).append(": ");
        output.append(notification.message);

        if (notification.details != null && !notification.details.trim().isEmpty()) {
            output.append(" (").append(notification.details.trim()).append(")");
        }

        // Use appropriate logging level
        switch (notification.type) {
            case ERROR:
                logger.severe(output.toString());
                System.err.println(output.toString());
                break;
            case WARNING:
                logger.warning(output.toString());
                System.out.println(output.toString());
                break;
            case SUCCESS:
            case INFO:
            default:
                logger.info(output.toString());
                System.out.println(output.toString());
                break;
        }
    }

    /**
     * Gets the number of pending notifications in the queue
     *
     * @return the number of pending notifications
     */
    public int getPendingNotificationCount() {
        return notificationQueue.size();
    }

    /**
     * Clears all pending notifications
     */
    public void clearPendingNotifications() {
        notificationQueue.clear();
        logger.info("Cleared all pending notifications");
    }

    /**
     * Checks if logging is enabled
     *
     * @return true if logging is enabled
     */
    public boolean isLoggingEnabled() {
        return enableLogging;
    }

    /**
     * Future method placeholder for UI-based notifications
     * This method is designed to be enhanced when implementing toast notifications,
     * popup dialogs, or other UI-based notification systems.
     *
     * @param notification the notification to show in UI
     * @param durationMs the duration to show the notification (for toast-style notifications)
     */
    public void showUINotification(Notification notification, int durationMs) {
        // TODO: Implement UI-based notifications (toast, popup, etc.)
        // For now, delegate to console output
        processNotification(notification);
    }
}