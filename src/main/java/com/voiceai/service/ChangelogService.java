package com.voiceai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for reading and parsing changelog data
 */
public class ChangelogService {

    private static final Logger logger = Logger.getLogger(ChangelogService.class.getName());
    private static final String CHANGELOG_FILE = "/changelog.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ObjectMapper objectMapper;

    /**
     * Represents a single update entry
     */
    public static class ChangelogUpdate {
        private final UpdateType type;
        private final String description;

        public ChangelogUpdate(UpdateType type, String description) {
            this.type = type;
            this.description = description;
        }

        public UpdateType getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public String getIcon() {
            return type.getIcon();
        }

        @Override
        public String toString() {
            return type.getIcon() + " " + description;
        }
    }

    /**
     * Represents a version with its updates
     */
    public static class ChangelogVersion {
        private final String version;
        private final LocalDate date;
        private final List<ChangelogUpdate> updates;

        public ChangelogVersion(String version, LocalDate date, List<ChangelogUpdate> updates) {
            this.version = version;
            this.date = date;
            this.updates = updates;
        }

        public String getVersion() {
            return version;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getFormattedDate() {
            return date.format(DISPLAY_DATE_FORMATTER);
        }

        public List<ChangelogUpdate> getUpdates() {
            return new ArrayList<>(updates);
        }

        @Override
        public String toString() {
            return "Version " + version + " (" + getFormattedDate() + ")";
        }
    }

    /**
     * Types of updates
     */
    public enum UpdateType {
        FEATURE("feature", "✨", "Nuova funzionalità"),
        FIX("fix", "🪲", "Correzione bug"),
        IMPROVEMENT("improvement", "🔧", "Miglioramento"),
        SECURITY("security", "🔒", "Sicurezza"),
        BREAKING("breaking", "⚠️", "Breaking change");

        private final String value;
        private final String icon;
        private final String displayName;

        UpdateType(String value, String icon, String displayName) {
            this.value = value;
            this.icon = icon;
            this.displayName = displayName;
        }

        public String getValue() {
            return value;
        }

        public String getIcon() {
            return icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static UpdateType fromValue(String value) {
            for (UpdateType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return IMPROVEMENT; // Default fallback
        }
    }

    /**
     * Creates a new ChangelogService
     */
    public ChangelogService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads all changelog versions from the JSON file
     *
     * @return list of all changelog versions
     */
    public List<ChangelogVersion> loadChangelog() {
        List<ChangelogVersion> versions = new ArrayList<>();

        try (InputStream inputStream = getClass().getResourceAsStream(CHANGELOG_FILE)) {
            if (inputStream == null) {
                logger.warning("Changelog file not found: " + CHANGELOG_FILE);
                return versions;
            }

            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode versionsNode = root.get("versions");

            if (versionsNode != null && versionsNode.isArray()) {
                for (JsonNode versionNode : versionsNode) {
                    ChangelogVersion version = parseVersion(versionNode);
                    if (version != null) {
                        versions.add(version);
                    }
                }
            }

            logger.info("Loaded " + versions.size() + " changelog versions");

        } catch (Exception e) {
            logger.severe("Failed to load changelog: " + e.getMessage());
        }

        return versions;
    }

    /**
     * Gets the latest N versions from the changelog
     *
     * @param count number of versions to retrieve
     * @return list of latest versions
     */
    public List<ChangelogVersion> getLatestVersions(int count) {
        List<ChangelogVersion> allVersions = loadChangelog();

        if (allVersions.size() <= count) {
            return allVersions;
        }

        return allVersions.subList(0, count);
    }

    /**
     * Gets the changelog for a specific version
     *
     * @param version the version string (e.g., "1.0.0")
     * @return the changelog version or null if not found
     */
    public ChangelogVersion getVersionChangelog(String version) {
        List<ChangelogVersion> allVersions = loadChangelog();

        for (ChangelogVersion v : allVersions) {
            if (v.getVersion().equals(version)) {
                return v;
            }
        }

        return null;
    }

    /**
     * Parses a single version node from JSON
     */
    private ChangelogVersion parseVersion(JsonNode versionNode) {
        try {
            String version = versionNode.get("version").asText();
            String dateStr = versionNode.get("date").asText();
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);

            List<ChangelogUpdate> updates = new ArrayList<>();
            JsonNode updatesNode = versionNode.get("updates");

            if (updatesNode != null && updatesNode.isArray()) {
                for (JsonNode updateNode : updatesNode) {
                    ChangelogUpdate update = parseUpdate(updateNode);
                    if (update != null) {
                        updates.add(update);
                    }
                }
            }

            return new ChangelogVersion(version, date, updates);

        } catch (Exception e) {
            logger.warning("Failed to parse version node: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses a single update node from JSON
     */
    private ChangelogUpdate parseUpdate(JsonNode updateNode) {
        try {
            String typeStr = updateNode.get("type").asText();
            String description = updateNode.get("description").asText();

            UpdateType type = UpdateType.fromValue(typeStr);

            return new ChangelogUpdate(type, description);

        } catch (Exception e) {
            logger.warning("Failed to parse update node: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the changelog file exists
     *
     * @return true if the changelog file is accessible
     */
    public boolean isChangelogAvailable() {
        try (InputStream inputStream = getClass().getResourceAsStream(CHANGELOG_FILE)) {
            return inputStream != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets a summary of all updates across all versions
     *
     * @return total count of updates
     */
    public int getTotalUpdateCount() {
        List<ChangelogVersion> versions = loadChangelog();
        return versions.stream()
                .mapToInt(v -> v.getUpdates().size())
                .sum();
    }
}