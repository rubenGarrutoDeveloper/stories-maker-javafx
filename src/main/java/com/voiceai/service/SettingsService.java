package com.voiceai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Service for managing application settings and configuration.
 * Handles API keys, user preferences, and persistent configuration storage.
 */
public class SettingsService {

    private static final Logger logger = Logger.getLogger(SettingsService.class.getName());
    private static final String CONFIG_FILE_NAME = "stories-maker-config.json";
    private static final String APP_DIR_NAME = ".stories-maker";

    // Configuration keys
    private static final String API_KEY_KEY = "apiKey";
    private static final String THEME_KEY = "theme";
    private static final String LANGUAGE_KEY = "language";
    private static final String REAL_TIME_TRANSCRIPTION_KEY = "realTimeTranscription";
    private static final String DEFAULT_SAVE_PATH_KEY = "defaultSavePath";
    private static final String WINDOW_WIDTH_KEY = "windowWidth";
    private static final String WINDOW_HEIGHT_KEY = "windowHeight";
    private static final String WINDOW_X_KEY = "windowX";
    private static final String WINDOW_Y_KEY = "windowY";
    private static final String VERSION_KEY = "configVersion";

    // Default values
    private static final String DEFAULT_THEME = "light";
    private static final String DEFAULT_LANGUAGE = "it";
    private static final boolean DEFAULT_REAL_TIME_TRANSCRIPTION = true;
    private static final double DEFAULT_WINDOW_WIDTH = 1200.0;
    private static final double DEFAULT_WINDOW_HEIGHT = 800.0;
    private static final String CURRENT_CONFIG_VERSION = "1.0";

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final Path configFilePath;
    private ObjectNode configuration;

    /**
     * Current API key validation state
     */
    public enum ApiKeyState {
        NOT_SET,
        INVALID,
        VALID,
        VALIDATING
    }

    /**
     * Settings operation result
     */
    public static class SettingsResult {
        private final boolean success;
        private final String message;
        private final Exception exception;

        private SettingsResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.message = message;
            this.exception = exception;
        }

        public static SettingsResult success(String message) {
            return new SettingsResult(true, message, null);
        }

        public static SettingsResult failure(String message, Exception exception) {
            return new SettingsResult(false, message, exception);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Exception getException() {
            return exception;
        }
    }

    /**
     * Creates a new SettingsService
     *
     * @param notificationService the notification service for user feedback
     */
    public SettingsService(NotificationService notificationService) {
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper();
        this.configFilePath = getConfigFilePath();

        // Load or create configuration
        loadConfiguration();
    }

    /**
     * Gets the API key (decrypted)
     *
     * @return the API key or null if not set
     */
    public String getApiKey() {
        String encryptedKey = configuration.path(API_KEY_KEY).asText(null);
        if (encryptedKey == null || encryptedKey.trim().isEmpty()) {
            return null;
        }
        return decryptApiKey(encryptedKey);
    }

    /**
     * Sets and encrypts the API key
     *
     * @param apiKey the API key to store
     * @return SettingsResult indicating success or failure
     */
    public SettingsResult setApiKey(String apiKey) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                configuration.remove(API_KEY_KEY);
                notificationService.showInfo("API key cleared");
            } else {
                String encryptedKey = encryptApiKey(apiKey.trim());
                configuration.put(API_KEY_KEY, encryptedKey);
                notificationService.showInfo("API key updated");
            }

            return saveConfiguration();

        } catch (Exception e) {
            String error = "Failed to set API key: " + e.getMessage();
            notificationService.showError(error, e);
            return SettingsResult.failure(error, e);
        }
    }

    /**
     * Checks if an API key is currently set
     *
     * @return true if API key is set
     */
    public boolean hasApiKey() {
        return getApiKey() != null;
    }

    /**
     * Gets the current theme setting
     *
     * @return the theme name
     */
    public String getTheme() {
        return configuration.path(THEME_KEY).asText(DEFAULT_THEME);
    }

    /**
     * Sets the theme preference
     *
     * @param theme the theme name (light, dark, etc.)
     * @return SettingsResult indicating success or failure
     */
    public SettingsResult setTheme(String theme) {
        if (theme == null || theme.trim().isEmpty()) {
            theme = DEFAULT_THEME;
        }

        configuration.put(THEME_KEY, theme.trim().toLowerCase());
        notificationService.showInfo("Theme changed to: " + theme);
        return saveConfiguration();
    }

    /**
     * Gets the current language setting
     *
     * @return the language code
     */
    public String getLanguage() {
        return configuration.path(LANGUAGE_KEY).asText(DEFAULT_LANGUAGE);
    }

    /**
     * Sets the language preference
     *
     * @param language the language code (it, en, es, fr, de)
     * @return SettingsResult indicating success or failure
     */
    public SettingsResult setLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            language = DEFAULT_LANGUAGE;
        }

        configuration.put(LANGUAGE_KEY, language.trim().toLowerCase());
        notificationService.showInfo("Language changed to: " + language);
        return saveConfiguration();
    }

    /**
     * Gets the real-time transcription preference
     *
     * @return true if real-time transcription is enabled
     */
    public boolean isRealTimeTranscriptionEnabled() {
        return configuration.path(REAL_TIME_TRANSCRIPTION_KEY).asBoolean(DEFAULT_REAL_TIME_TRANSCRIPTION);
    }

    /**
     * Sets the real-time transcription preference
     *
     * @param enabled true to enable real-time transcription
     * @return SettingsResult indicating success or failure
     */
    public SettingsResult setRealTimeTranscriptionEnabled(boolean enabled) {
        configuration.put(REAL_TIME_TRANSCRIPTION_KEY, enabled);
        notificationService.showInfo("Real-time transcription " + (enabled ? "enabled" : "disabled"));
        return saveConfiguration();
    }

    /**
     * Gets the default save path
     *
     * @return the default save path or null if not set
     */
    public String getDefaultSavePath() {
        return configuration.path(DEFAULT_SAVE_PATH_KEY).asText(null);
    }

    /**
     * Sets the default save path
     *
     * @param path the default save path
     * @return SettingsResult indicating success or failure
     */
    public SettingsResult setDefaultSavePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            configuration.remove(DEFAULT_SAVE_PATH_KEY);
        } else {
            configuration.put(DEFAULT_SAVE_PATH_KEY, path.trim());
        }
        return saveConfiguration();
    }

    /**
     * Gets the window width preference
     *
     * @return the window width
     */
    public double getWindowWidth() {
        return configuration.path(WINDOW_WIDTH_KEY).asDouble(DEFAULT_WINDOW_WIDTH);
    }

    /**
     * Gets the window height preference
     *
     * @return the window height
     */
    public double getWindowHeight() {
        return configuration.path(WINDOW_HEIGHT_KEY).asDouble(DEFAULT_WINDOW_HEIGHT);
    }

    /**
     * Gets the window X position preference
     *
     * @return the window X position or -1 if not set
     */
    public double getWindowX() {
        return configuration.path(WINDOW_X_KEY).asDouble(-1);
    }

    /**
     * Gets the window Y position preference
     *
     * @return the window Y position or -1 if not set
     */
    public double getWindowY() {
        return configuration.path(WINDOW_Y_KEY).asDouble(-1);
    }

    /**
     * Saves window position and size
     *
     * @param x window X position
     * @param y window Y position
     * @param width window width
     * @param height window height
     * @return SettingsResult indicating success or failure
     */
    public SettingsResult saveWindowGeometry(double x, double y, double width, double height) {
        configuration.put(WINDOW_X_KEY, x);
        configuration.put(WINDOW_Y_KEY, y);
        configuration.put(WINDOW_WIDTH_KEY, width);
        configuration.put(WINDOW_HEIGHT_KEY, height);

        return saveConfiguration();
    }

    /**
     * Resets all settings to defaults
     *
     * @return SettingsResult indicating success or failure
     */
    public SettingsResult resetToDefaults() {
        try {
            configuration = objectMapper.createObjectNode();
            applyDefaults();

            SettingsResult result = saveConfiguration();
            if (result.isSuccess()) {
                notificationService.showSuccess("Settings reset to defaults");
            }
            return result;

        } catch (Exception e) {
            String error = "Failed to reset settings: " + e.getMessage();
            notificationService.showError(error, e);
            return SettingsResult.failure(error, e);
        }
    }

    /**
     * Exports settings to a file
     *
     * @param exportPath the path to export to
     * @return SettingsResult indicating success or failure
     */
    public SettingsResult exportSettings(String exportPath) {
        try {
            // Create a copy without the API key for security
            ObjectNode exportConfig = configuration.deepCopy();
            exportConfig.remove(API_KEY_KEY);

            File exportFile = new File(exportPath);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(exportFile, exportConfig);

            notificationService.showSuccess("Settings exported successfully");
            return SettingsResult.success("Settings exported to: " + exportPath);

        } catch (Exception e) {
            String error = "Failed to export settings: " + e.getMessage();
            notificationService.showError(error, e);
            return SettingsResult.failure(error, e);
        }
    }

    /**
     * Gets the configuration file path
     *
     * @return the configuration file path
     */
    private Path getConfigFilePath() {
        // Use user home directory for config storage
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, APP_DIR_NAME);

        // Create app directory if it doesn't exist
        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            logger.warning("Failed to create app directory: " + e.getMessage());
            // Fallback to current directory
            return Paths.get(CONFIG_FILE_NAME);
        }

        return appDir.resolve(CONFIG_FILE_NAME);
    }

    /**
     * Loads configuration from file or creates default
     */
    private void loadConfiguration() {
        try {
            if (Files.exists(configFilePath)) {
                String configJson = Files.readString(configFilePath, StandardCharsets.UTF_8);
                JsonNode loadedConfig = objectMapper.readTree(configJson);

                if (loadedConfig.isObject()) {
                    configuration = (ObjectNode) loadedConfig;

                    // Check for version migration
                    String configVersion = configuration.path(VERSION_KEY).asText("");
                    if (!CURRENT_CONFIG_VERSION.equals(configVersion)) {
                        migrateConfiguration(configVersion);
                    }

                    logger.info("Configuration loaded successfully from: " + configFilePath);
                } else {
                    throw new IOException("Invalid configuration format");
                }
            } else {
                // Create default configuration
                configuration = objectMapper.createObjectNode();
                applyDefaults();
                saveConfiguration();
                logger.info("Created default configuration at: " + configFilePath);
            }

        } catch (Exception e) {
            logger.warning("Failed to load configuration: " + e.getMessage());
            notificationService.showWarning("Failed to load settings, using defaults");

            // Create default configuration
            configuration = objectMapper.createObjectNode();
            applyDefaults();
        }
    }

    /**
     * Applies default configuration values
     */
    private void applyDefaults() {
        configuration.put(THEME_KEY, DEFAULT_THEME);
        configuration.put(LANGUAGE_KEY, DEFAULT_LANGUAGE);
        configuration.put(REAL_TIME_TRANSCRIPTION_KEY, DEFAULT_REAL_TIME_TRANSCRIPTION);
        configuration.put(WINDOW_WIDTH_KEY, DEFAULT_WINDOW_WIDTH);
        configuration.put(WINDOW_HEIGHT_KEY, DEFAULT_WINDOW_HEIGHT);
        configuration.put(VERSION_KEY, CURRENT_CONFIG_VERSION);
    }

    /**
     * Migrates configuration from older versions
     *
     * @param oldVersion the old configuration version
     */
    private void migrateConfiguration(String oldVersion) {
        logger.info("Migrating configuration from version " + oldVersion + " to " + CURRENT_CONFIG_VERSION);

        // Add migration logic here as needed
        // For now, just update the version
        configuration.put(VERSION_KEY, CURRENT_CONFIG_VERSION);

        notificationService.showInfo("Configuration migrated to version " + CURRENT_CONFIG_VERSION);
    }

    /**
     * Saves configuration to file
     *
     * @return SettingsResult indicating success or failure
     */
    private SettingsResult saveConfiguration() {
        try {
            // Ensure parent directory exists
            Files.createDirectories(configFilePath.getParent());

            // Write configuration to file
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(configFilePath.toFile(), configuration);

            logger.info("Configuration saved successfully to: " + configFilePath);
            return SettingsResult.success("Configuration saved");

        } catch (Exception e) {
            String error = "Failed to save configuration: " + e.getMessage();
            logger.severe(error);
            return SettingsResult.failure(error, e);
        }
    }

    /**
     * Simple encryption for API key (Base64 encoding for now)
     * Note: This is basic obfuscation, not secure encryption
     *
     * @param apiKey the API key to encrypt
     * @return the encrypted API key
     */
    private String encryptApiKey(String apiKey) {
        // Simple Base64 encoding with a basic XOR cipher
        byte[] keyBytes = apiKey.getBytes(StandardCharsets.UTF_8);
        byte[] xorKey = "StoriesMaker2024".getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] ^= xorKey[i % xorKey.length];
        }

        return Base64.getEncoder().encodeToString(keyBytes);
    }

    /**
     * Simple decryption for API key (Base64 decoding)
     *
     * @param encryptedKey the encrypted API key
     * @return the decrypted API key
     */
    private String decryptApiKey(String encryptedKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptedKey);
            byte[] xorKey = "StoriesMaker2024".getBytes(StandardCharsets.UTF_8);

            for (int i = 0; i < keyBytes.length; i++) {
                keyBytes[i] ^= xorKey[i % xorKey.length];
            }

            return new String(keyBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.warning("Failed to decrypt API key: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the configuration file location for display purposes
     *
     * @return the configuration file path as a string
     */
    public String getConfigurationFileLocation() {
        return configFilePath.toString();
    }

    /**
     * Validates the configuration integrity
     *
     * @return true if configuration is valid
     */
    public boolean validateConfiguration() {
        try {
            // Basic validation checks
            if (configuration == null) {
                return false;
            }

            // Check for required version field
            if (!configuration.has(VERSION_KEY)) {
                return false;
            }

            // Validate theme value
            String theme = getTheme();
            if (theme == null || theme.trim().isEmpty()) {
                return false;
            }

            // Validate language value
            String language = getLanguage();
            if (language == null || language.trim().isEmpty()) {
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.warning("Configuration validation failed: " + e.getMessage());
            return false;
        }
    }
}