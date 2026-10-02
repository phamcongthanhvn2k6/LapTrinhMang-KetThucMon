package com.qlsv.model;

import java.io.Serializable;

public class SqlConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum DbType {
        SQL_SERVER,
        MYSQL,
        POSTGRESQL,
        H2_EMBEDDED,
        SQLITE_EMBEDDED
    }

    private DbType dbType = DbType.SQL_SERVER;
    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;

    public SqlConfig() {
    }

    public SqlConfig(DbType dbType, String host, int port, String databaseName, String username, String password) {
        this.dbType = dbType;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }

    public DbType getDbType() {
        return dbType;
    }

    public void setDbType(DbType dbType) {
        this.dbType = dbType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "SqlConfig{" +
                "dbType=" + dbType +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", databaseName='" + databaseName + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
