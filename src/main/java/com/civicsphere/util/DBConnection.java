package com.civicsphere.util;

import com.civicsphere.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database connection utility for CivicSphere.
 * Manages JDBC connections to MySQL.
 */
public class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    static {
        try {
            Class.forName(AppConfig.getDbDriver());
            LOGGER.info("MySQL JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load MySQL JDBC Driver: " + AppConfig.getDbDriver(), e);
        }
    }

    /**
     * Gets a new JDBC database connection.
     * @return Connection object
     * @throws SQLException on connection failure
     */
    public static Connection getConnection() throws SQLException {
        String url = AppConfig.getDbUrl();
        String user = AppConfig.getDbUser();
        String password = AppConfig.getDbPassword();
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Closes AutoCloseable resources safely without throwing checked exceptions.
     */
    public static void close(AutoCloseable... resources) {
        if (resources == null) return;
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error closing resource", e);
                }
            }
        }
    }
}

