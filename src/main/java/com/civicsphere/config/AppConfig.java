package com.civicsphere.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application configuration loader.
 * Reads configurations from db.properties with environment variable override support.
 */
public class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties properties = new Properties();

    static {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                properties.load(is);
                LOGGER.info("db.properties successfully loaded.");
            } else {
                LOGGER.warning("db.properties not found on classpath, using defaults.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load db.properties", e);
        }
    }

    public static String getProperty(String key, String defaultValue) {
        // Environment variable override (e.g. db.user -> CIVICSPHERE_DB_USER)
        String envKey = "CIVICSPHERE_" + key.replace('.', '_').toUpperCase();
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        return properties.getProperty(key, defaultValue);
    }

    public static String getDbDriver() {
        return getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
    }

    public static String getDbUrl() {
        return getProperty("db.url", "jdbc:mysql://localhost:3306/civicsphere_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    }

    public static String getDbUser() {
        return getProperty("db.user", "root");
    }

    public static String getDbPassword() {
        return getProperty("db.password", "ramyarani");
    }

    public static String getUploadDir() {
        return getProperty("upload.dir", "uploads");
    }

    public static double getDistanceThresholdMeters() {
        try {
            return Double.parseDouble(getProperty("recurrence.distance.threshold.meters", "100.0"));
        } catch (NumberFormatException e) {
            return 100.0;
        }
    }

    public static double getRecurrenceScoreThreshold() {
        try {
            return Double.parseDouble(getProperty("recurrence.score.threshold", "70.0"));
        } catch (NumberFormatException e) {
            return 70.0;
        }
    }
}

