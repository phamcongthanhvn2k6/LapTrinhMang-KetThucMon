package com.qlsv.database;

import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.security.DESEncryption;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;
    private SqlConfig currentConfig;
    private String desKey = DESEncryption.DEFAULT_KEY;

    private DatabaseManager() {
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public String getDesKey() {
        return desKey;
    }

    public void setDesKey(String desKey) {
        if (desKey != null && !desKey.trim().isEmpty()) {
            this.desKey = desKey.trim();
        }
    }

    public synchronized boolean connect(SqlConfig config) throws Exception {
        closeConnection();
        this.currentConfig = config;
        String url;
        String driver;

        switch (config.getDbType()) {
            case SQL_SERVER:
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true;",
                        config.getHost(), config.getPort() > 0 ? config.getPort() : 1433, config.getDatabaseName());
                break;
            case MYSQL:
                driver = "com.mysql.cj.jdbc.Driver";
                url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                        config.getHost(), config.getPort() > 0 ? config.getPort() : 3306, config.getDatabaseName());
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
                break;
        }

        try {
            Class.forName(driver);
            if (config.getDbType() == SqlConfig.DbType.SQLITE_EMBEDDED) {
                connection = DriverManager.getConnection(url);
            } else {
                connection = DriverManager.getConnection(url, config.getUsername(), config.getPassword());
            }

            // Create table if not exists
            createTableIfNotExists();
            return true;
        } catch (Exception e) {
            throw new Exception("Không thể kết nối CSDL (" + config.getDbType() + "): " + e.getMessage(), e);
        }
    }

    private void createTableIfNotExists() throws SQLException {
        if (connection == null) return;

        String sql;
        if (currentConfig.getDbType() == SqlConfig.DbType.SQL_SERVER) {
            sql = "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[SinhVien]') AND type in (N'U')) " +
                  "CREATE TABLE SinhVien (" +
                  "MaSV VARCHAR(50) PRIMARY KEY, " +
                  "HoTenEncrypted NVARCHAR(500) NOT NULL, " +
                  "DiemToanEncrypted VARCHAR(200) NOT NULL, " +
                  "DiemVanEncrypted VARCHAR(200) NOT NULL, " +
                  "DiemAnhEncrypted VARCHAR(200) NOT NULL, " +
                  "DiemTB FLOAT, " +
                  "CreatedAt DATETIME DEFAULT GETDATE())";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS SinhVien (" +
                  "MaSV VARCHAR(50) PRIMARY KEY, " +
                  "HoTenEncrypted VARCHAR(500) NOT NULL, " +
                  "DiemToanEncrypted VARCHAR(200) NOT NULL, " +
                  "DiemVanEncrypted VARCHAR(200) NOT NULL, " +
                  "DiemAnhEncrypted VARCHAR(200) NOT NULL, " +
                  "DiemTB DOUBLE, " +
                  "CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * Encrypts student scores & info using DES and stores them in Database.
     * Then reads back, decrypts using DES, calculates average, and returns StudentResult.
     */
    public synchronized StudentResult saveStudentEncryptedAndCalculateResult(StudentData student) throws Exception {
        if (connection == null || connection.isClosed()) {
            throw new Exception("CSDL chưa được kết nối!");
        }

        // Encrypt with DES algorithm
        String encFullName = DESEncryption.encrypt(student.getFullName(), desKey);
        String encToan = DESEncryption.encrypt(String.valueOf(student.getScoreMath()), desKey);
        String encVan = DESEncryption.encrypt(String.valueOf(student.getScoreLiterature()), desKey);
        String encAnh = DESEncryption.encrypt(String.valueOf(student.getScoreEnglish()), desKey);

        // Calculate average score
        double avgScore = (student.getScoreMath() + student.getScoreLiterature() + student.getScoreEnglish()) / 3.0;
        avgScore = Math.round(avgScore * 100.0) / 100.0;

        // Check if student exists
        String checkSql = "SELECT COUNT(*) FROM SinhVien WHERE MaSV = ?";
        boolean exists = false;
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, student.getStudentId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    exists = true;
                }
            }
        }

        if (exists) {
            String updateSql = "UPDATE SinhVien SET HoTenEncrypted = ?, DiemToanEncrypted = ?, " +
                    "DiemVanEncrypted = ?, DiemAnhEncrypted = ?, DiemTB = ? WHERE MaSV = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                stmt.setString(1, encFullName);
                stmt.setString(2, encToan);
                stmt.setString(3, encVan);
                stmt.setString(4, encAnh);
                stmt.setDouble(5, avgScore);
                stmt.setString(6, student.getStudentId());
                stmt.executeUpdate();
            }
        } else {
            String insertSql = "INSERT INTO SinhVien (MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
                stmt.setString(1, student.getStudentId());
                stmt.setString(2, encFullName);
                stmt.setString(3, encToan);
                stmt.setString(4, encVan);
                stmt.setString(5, encAnh);
                stmt.setDouble(6, avgScore);
                stmt.executeUpdate();
            }
        }

        // Retrieve from Database and decrypt DES data to form final StudentResult
        return getStudentDecrypted(student.getStudentId());
    }

    /**
     * Reads student from Database, decrypts DES data, and returns StudentResult.
     */
    public synchronized StudentResult getStudentDecrypted(String studentId) throws Exception {
        String querySql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted " +
                "FROM SinhVien WHERE MaSV = ?";
        try (PreparedStatement stmt = connection.prepareStatement(querySql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("MaSV");
                    String encFullName = rs.getString("HoTenEncrypted");
                    String encToan = rs.getString("DiemToanEncrypted");
                    String encVan = rs.getString("DiemVanEncrypted");
                    String encAnh = rs.getString("DiemAnhEncrypted");

                    // Decrypt DES
                    String fullName = DESEncryption.decrypt(encFullName, desKey);
                    double scoreMath = Double.parseDouble(DESEncryption.decrypt(encToan, desKey));
                    double scoreVan = Double.parseDouble(DESEncryption.decrypt(encVan, desKey));
                    double scoreAnh = Double.parseDouble(DESEncryption.decrypt(encAnh, desKey));

                    double avg = (scoreMath + scoreVan + scoreAnh) / 3.0;
                    avg = Math.round(avg * 100.0) / 100.0;

                    return new StudentResult(id, fullName, avg);
                }
            }
        }
        throw new Exception("Không tìm thấy sinh viên có mã: " + studentId);
    }

    /**
     * Helper struct for displaying encrypted database content on Server dashboard.
     */
    public static class EncryptedRecord {
        public String maSV;
        public String hoTenEncrypted;
        public String diemToanEncrypted;
        public String diemVanEncrypted;
        public String diemAnhEncrypted;
        public String hoTenDecrypted;
        public double diemTB;
    }

    public synchronized List<EncryptedRecord> getAllRecordsForInspection() throws Exception {
        List<EncryptedRecord> list = new ArrayList<>();
        if (connection == null || connection.isClosed()) {
            return list;
        }

        String sql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB FROM SinhVien";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                EncryptedRecord rec = new EncryptedRecord();
                rec.maSV = rs.getString("MaSV");
                rec.hoTenEncrypted = rs.getString("HoTenEncrypted");
                rec.diemToanEncrypted = rs.getString("DiemToanEncrypted");
                rec.diemVanEncrypted = rs.getString("DiemVanEncrypted");
                rec.diemAnhEncrypted = rs.getString("DiemAnhEncrypted");
                rec.diemTB = rs.getDouble("DiemTB");

                try {
                    rec.hoTenDecrypted = DESEncryption.decrypt(rec.hoTenEncrypted, desKey);
                } catch (Exception ex) {
                    rec.hoTenDecrypted = "[Giải mã lỗi: " + ex.getMessage() + "]";
                }
                list.add(rec);
            }
        }
        return list;
    }

    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }
}
