package com.voiceai.util;

/**
 * Utility class containing application version information
 */
public class AppVersion {

    /**
     * Current application version
     * Format: MAJOR.MINOR.PATCH
     */
    public static final String VERSION = "1.0.0";

    /**
     * Application build date (can be updated during build process)
     */
    public static final String BUILD_DATE = "2025-09-30";

    /**
     * Application name
     */
    public static final String APP_NAME = "StorieS Maker";

    /**
     * Full version string with build information
     */
    public static final String FULL_VERSION = VERSION + " (Build: " + BUILD_DATE + ")";

    // Private constructor to prevent instantiation
    private AppVersion() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the version string
     *
     * @return the version string
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Gets the full version string with build date
     *
     * @return the full version string
     */
    public static String getFullVersion() {
        return FULL_VERSION;
    }

    /**
     * Gets the build date
     *
     * @return the build date
     */
    public static String getBuildDate() {
        return BUILD_DATE;
    }

    /**
     * Gets the application name
     *
     * @return the application name
     */
    public static String getAppName() {
        return APP_NAME;
    }

    /**
     * Checks if this is a development version
     *
     * @return true if version contains "SNAPSHOT" or "dev"
     */
    public static boolean isDevelopmentVersion() {
        return VERSION.contains("SNAPSHOT") || VERSION.contains("dev");
    }

    /**
     * Gets the major version number
     *
     * @return the major version number
     */
    public static int getMajorVersion() {
        try {
            return Integer.parseInt(VERSION.split("\\.")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Gets the minor version number
     *
     * @return the minor version number
     */
    public static int getMinorVersion() {
        try {
            return Integer.parseInt(VERSION.split("\\.")[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Gets the patch version number
     *
     * @return the patch version number
     */
    public static int getPatchVersion() {
        try {
            String[] parts = VERSION.split("\\.");
            if (parts.length >= 3) {
                // Remove any non-numeric suffix (like -SNAPSHOT)
                return Integer.parseInt(parts[2].replaceAll("[^0-9].*", ""));
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Compares this version with another version string
     *
     * @param otherVersion the version to compare with
     * @return negative if this version is older, 0 if equal, positive if newer
     */
    public static int compareVersion(String otherVersion) {
        String[] thisParts = VERSION.split("\\.");
        String[] otherParts = otherVersion.split("\\.");

        int maxLength = Math.max(thisParts.length, otherParts.length);

        for (int i = 0; i < maxLength; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i].replaceAll("[^0-9].*", "")) : 0;
            int otherPart = i < otherParts.length ?
                    Integer.parseInt(otherParts[i].replaceAll("[^0-9].*", "")) : 0;

            if (thisPart != otherPart) {
                return thisPart - otherPart;
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        return FULL_VERSION;
    }
}