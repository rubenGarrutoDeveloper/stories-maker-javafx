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
    private static final String AUDIO_SOURCE_KEY = "audioSourceName";
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

    public enum ApiKeyState {
        NOT_SET,
        INVALID,
        VALID,
        VALIDATING
    }

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

    public SettingsService(NotificationService notificationService) {
        this.notificationService = notificationService;
        this.objectMapper = new ObjectMapper();
        this.configFilePath = getConfigFilePath();

        loadConfiguration();
    }

    public String getApiKey() {
        String encryptedKey = configuration.path(API_KEY_KEY).asText(null);
        if (encryptedKey == null || encryptedKey.trim().isEmpty()) {
            return null;
        }
        return decryptApiKey(encryptedKey);
    }

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

    public boolean hasApiKey() {
        return getApiKey() != null;
    }

    public String getTheme() {
        return configuration.path(THEME_KEY).asText(DEFAULT_THEME);
    }

    public SettingsResult setTheme(String theme) {
        if (theme == null || theme.trim().isEmpty()) {
            theme = DEFAULT_THEME;
        }

        configuration.put(THEME_KEY, theme.trim().toLowerCase());
        notificationService.showInfo("Theme changed to: " + theme);
        return saveConfiguration();
    }

    public String getLanguage() {
        return configuration.path(LANGUAGE_KEY).asText(DEFAULT_LANGUAGE);
    }

    public SettingsResult setLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            language = DEFAULT_LANGUAGE;
        }

        configuration.put(LANGUAGE_KEY, language.trim().toLowerCase());
        notificationService.showInfo("Language changed to: " + language);
        return saveConfiguration();
    }

    public boolean isRealTimeTranscriptionEnabled() {
        return configuration.path(REAL_TIME_TRANSCRIPTION_KEY).asBoolean(DEFAULT_REAL_TIME_TRANSCRIPTION);
    }

    public SettingsResult setRealTimeTranscriptionEnabled(boolean enabled) {
        configuration.put(REAL_TIME_TRANSCRIPTION_KEY, enabled);
        notificationService.showInfo("Real-time transcription " + (enabled ? "enabled" : "disabled"));
        return saveConfiguration();
    }

    /**
     * Gets the saved audio source name
     */
    public String getAudioSourceName() {
        return configuration.path(AUDIO_SOURCE_KEY).asText(null);
    }

    /**
     * Sets the audio source name to be saved
     */
    public SettingsResult setAudioSourceName(String audioSourceName) {
        if (audioSourceName == null || audioSourceName.trim().isEmpty()) {
            configuration.remove(AUDIO_SOURCE_KEY);
        } else {
            configuration.put(AUDIO_SOURCE_KEY, audioSourceName.trim());
            notificationService.showInfo("Audio source saved: " + audioSourceName);
        }
        return saveConfiguration();
    }

    public String getDefaultSavePath() {
        return configuration.path(DEFAULT_SAVE_PATH_KEY).asText(null);
    }

    public SettingsResult setDefaultSavePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            configuration.remove(DEFAULT_SAVE_PATH_KEY);
        } else {
            configuration.put(DEFAULT_SAVE_PATH_KEY, path.trim());
        }
        return saveConfiguration();
    }

    public double getWindowWidth() {
        return configuration.path(WINDOW_WIDTH_KEY).asDouble(DEFAULT_WINDOW_WIDTH);
    }

    public double getWindowHeight() {
        return configuration.path(WINDOW_HEIGHT_KEY).asDouble(DEFAULT_WINDOW_HEIGHT);
    }

    public double getWindowX() {
        return configuration.path(WINDOW_X_KEY).asDouble(-1);
    }

    public double getWindowY() {
        return configuration.path(WINDOW_Y_KEY).asDouble(-1);
    }

    public SettingsResult saveWindowGeometry(double x, double y, double width, double height) {
        configuration.put(WINDOW_X_KEY, x);
        configuration.put(WINDOW_Y_KEY, y);
        configuration.put(WINDOW_WIDTH_KEY, width);
        configuration.put(WINDOW_HEIGHT_KEY, height);

        return saveConfiguration();
    }

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

    public SettingsResult exportSettings(String exportPath) {
        try {
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

    private Path getConfigFilePath() {
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, APP_DIR_NAME);

        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            logger.warning("Failed to create app directory: " + e.getMessage());
            return Paths.get(CONFIG_FILE_NAME);
        }

        return appDir.resolve(CONFIG_FILE_NAME);
    }

    private void loadConfiguration() {
        try {
            if (Files.exists(configFilePath)) {
                String configJson = Files.readString(configFilePath, StandardCharsets.UTF_8);
                JsonNode loadedConfig = objectMapper.readTree(configJson);

                if (loadedConfig.isObject()) {
                    configuration = (ObjectNode) loadedConfig;

                    String configVersion = configuration.path(VERSION_KEY).asText("");
                    if (!CURRENT_CONFIG_VERSION.equals(configVersion)) {
                        migrateConfiguration(configVersion);
                    }

                    logger.info("Configuration loaded successfully from: " + configFilePath);
                } else {
                    throw new IOException("Invalid configuration format");
                }
            } else {
                configuration = objectMapper.createObjectNode();
                applyDefaults();
                saveConfiguration();
                logger.info("Created default configuration at: " + configFilePath);
            }

        } catch (Exception e) {
            logger.warning("Failed to load configuration: " + e.getMessage());
            notificationService.showWarning("Failed to load settings, using defaults");

            configuration = objectMapper.createObjectNode();
            applyDefaults();
        }
    }

    private void applyDefaults() {
        configuration.put(THEME_KEY, DEFAULT_THEME);
        configuration.put(LANGUAGE_KEY, DEFAULT_LANGUAGE);
        configuration.put(REAL_TIME_TRANSCRIPTION_KEY, DEFAULT_REAL_TIME_TRANSCRIPTION);
        configuration.put(WINDOW_WIDTH_KEY, DEFAULT_WINDOW_WIDTH);
        configuration.put(WINDOW_HEIGHT_KEY, DEFAULT_WINDOW_HEIGHT);
        configuration.put(VERSION_KEY, CURRENT_CONFIG_VERSION);
    }

    private void migrateConfiguration(String oldVersion) {
        logger.info("Migrating configuration from version " + oldVersion + " to " + CURRENT_CONFIG_VERSION);

        configuration.put(VERSION_KEY, CURRENT_CONFIG_VERSION);

        notificationService.showInfo("Configuration migrated to version " + CURRENT_CONFIG_VERSION);
    }

    private SettingsResult saveConfiguration() {
        try {
            Files.createDirectories(configFilePath.getParent());

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

    private String encryptApiKey(String apiKey) {
        byte[] keyBytes = apiKey.getBytes(StandardCharsets.UTF_8);
        byte[] xorKey = "StoriesMaker2024".getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] ^= xorKey[i % xorKey.length];
        }

        return Base64.getEncoder().encodeToString(keyBytes);
    }

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

    public String getConfigurationFileLocation() {
        return configFilePath.toString();
    }

    public boolean validateConfiguration() {
        try {
            if (configuration == null) {
                return false;
            }

            if (!configuration.has(VERSION_KEY)) {
                return false;
            }

            String theme = getTheme();
            if (theme == null || theme.trim().isEmpty()) {
                return false;
            }

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