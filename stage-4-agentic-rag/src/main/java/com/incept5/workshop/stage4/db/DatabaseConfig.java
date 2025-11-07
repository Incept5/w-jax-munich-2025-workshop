package com.incept5.workshop.stage4.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Database configuration using HikariCP connection pooling.
 * 
 * Provides high-performance connection pooling for PostgreSQL with pgvector.
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    // Connection settings - configurable via environment variables for workshop sharing
    // Note: Using methods instead of static finals to read env vars at call time (not class load time)
    private static String getDefaultJdbcUrl() {
        return System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/workshop_rag");
    }
    
    private static String getDefaultUsername() {
        return System.getenv().getOrDefault("DB_USER", "workshop");
    }
    
    private static String getDefaultPassword() {
        return System.getenv().getOrDefault("DB_PASSWORD", "workshop123");
    }
    
    /**
     * Create a DataSource with default configuration.
     * Reads environment variables at call time for maximum flexibility.
     */
    public static DataSource createDataSource() {
        String jdbcUrl = getDefaultJdbcUrl();
        String username = getDefaultUsername();
        String password = getDefaultPassword();
        
        logger.debug("Database config from environment: DB_URL={}", System.getenv("DB_URL"));
        logger.debug("Resolved JDBC URL: {}", jdbcUrl);
        
        return createDataSource(jdbcUrl, username, password);
    }
    
    /**
     * Create a DataSource with custom configuration.
     */
    public static DataSource createDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        
        // Connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        
        // Pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);  // 30 seconds
        config.setIdleTimeout(600000);       // 10 minutes
        config.setMaxLifetime(1800000);      // 30 minutes
        
        // Performance settings
        config.setAutoCommit(true);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // Pool name for monitoring
        config.setPoolName("Stage4-PgVector-Pool");
        
        logger.info("Creating HikariCP DataSource for: {}", jdbcUrl);
        
        return new HikariDataSource(config);
    }
}
