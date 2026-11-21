package com.qlsv.database;

import java.sql.Connection;
import java.sql.SQLException;

import com.qlsv.model.SqlConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * High-performance Thread-Safe Connection Pool using HikariCP.
 * Provides independent connections to concurrent UDP Server worker threads.
 */
public class ConnectionPool {

    private static HikariDataSource dataSource;
    private static SqlConfig currentConfig;

    private ConnectionPool() {}

    public static synchronized void init(SqlConfig config) throws Exception {
        closePool();
        currentConfig = config;

        HikariConfig hikariConfig = new HikariConfig();
        String url;
        String driver;

        switch (config.getDbType()) {
            case SQL_SERVER:
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true;",
                        config.getHost(), config.getPort() > 0 ? config.getPort() : 1433, config.getDatabaseName());
                hikariConfig.setUsername(config.getUsername());
                hikariConfig.setPassword(config.getPassword());
                break;
            case MYSQL:
                driver = "com.mysql.cj.jdbc.Driver";
                url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                        config.getHost(), config.getPort() > 0 ? config.getPort() : 3306, config.getDatabaseName());
                hikariConfig.setUsername(config.getUsername());
                hikariConfig.setPassword(config.getPassword());
                break;
            case POSTGRESQL:
                driver = "org.postgresql.Driver";
                url = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require",
                        config.getHost(), config.getPort() > 0 ? config.getPort() : 5432,
                        (config.getDatabaseName() != null && !config.getDatabaseName().isEmpty()) ? config.getDatabaseName() : "postgres");
                hikariConfig.setUsername(config.getUsername());
                hikariConfig.setPassword(config.getPassword());
                break;
            case SQLITE_EMBEDDED:
                driver = "org.sqlite.JDBC";
                String dbName = (config.getDatabaseName() != null && !config.getDatabaseName().isEmpty())
                        ? config.getDatabaseName() : "qlsv_db";
                url = "jdbc:sqlite:" + dbName + ".db";
                break;
            case H2_EMBEDDED:
            default:
                driver = "org.h2.Driver";
                String h2Db = (config.getDatabaseName() != null && !config.getDatabaseName().isEmpty())
                        ? config.getDatabaseName() : "qlsv_h2";
                url = "jdbc:h2:./" + h2Db + ";DB_CLOSE_DELAY=-1";
                hikariConfig.setUsername(config.getUsername() != null ? config.getUsername() : "sa");
                hikariConfig.setPassword(config.getPassword() != null ? config.getPassword() : "");
                break;
        }

        hikariConfig.setDriverClassName(driver);
        hikariConfig.setJdbcUrl(url);

        // Tuning parameters for multi-threaded UDP Server (20 concurrent threads)
        if (config.getDbType() == SqlConfig.DbType.SQLITE_EMBEDDED) {
            // SQLite file-based locking requires smaller pool
            hikariConfig.setMaximumPoolSize(1);
        } else {
            hikariConfig.setMaximumPoolSize(15);
            hikariConfig.setMinimumIdle(2);
        }

        hikariConfig.setConnectionTimeout(15000); // 15s
        hikariConfig.setIdleTimeout(300000);      // 5 min
        hikariConfig.setMaxLifetime(600000);       // 10 min
        hikariConfig.setPoolName("PTIT-RUDP-HikariPool");

        try {
            dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            throw new Exception("Lỗi khởi tạo Connection Pool HikariCP (" + config.getDbType() + "): " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Connection Pool chưa được khởi tạo hoặc đã bị đóng!");
        }
        return dataSource.getConnection();
    }

    public static synchronized boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }

    public static SqlConfig getCurrentConfig() {
        return currentConfig;
    }

    public static synchronized void closePool() {
        if (dataSource != null) {
            try {
                if (!dataSource.isClosed()) {
                    dataSource.close();
                }
            } catch (Exception ignored) {}
            dataSource = null;
        }
    }
}

