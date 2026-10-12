package com.pds.logistics.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC connection manager.
 * Reads database credentials from {@code db.properties} on the classpath.
 * Provides a simple connection factory; use one connection per transaction.
 */
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private static String url;
    private static String username;
    private static String password;

    static {
        try (InputStream is = DatabaseConfig.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (is == null) {
                // Fallback defaults for development
                url      = "jdbc:mysql://localhost:3306/pds_logistics?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
                username = "root";
                password = "";
                log.warn("db.properties not found – using development defaults");
            } else {
                Properties props = new Properties();
                props.load(is);
                url      = props.getProperty("db.url");
                username = props.getProperty("db.username");
                password = props.getProperty("db.password");
            }

            // Ensure the MySQL driver is registered
            Class.forName("com.mysql.cj.jdbc.Driver");
            log.info("DatabaseConfig initialised: {}", url);

        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Failed to initialise DatabaseConfig: " + e.getMessage());
        }
    }

    /**
     * Opens and returns a new JDBC connection.
     * Caller is responsible for closing the connection.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Tests the connection. Returns true if successful.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(3);
        } catch (SQLException e) {
            log.error("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /** Returns the configured JDBC URL (for display / diagnostics) */
    public static String getUrl() { return url; }

    private DatabaseConfig() {}
}
