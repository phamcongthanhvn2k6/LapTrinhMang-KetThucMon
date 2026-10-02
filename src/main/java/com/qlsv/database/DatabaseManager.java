package com.qlsv.database;

import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.security.SecurityManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;
    private SqlConfig currentConfig;
    private String desKey = "QLSV_KEY";

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
            case POSTGRESQL:
                driver = "org.postgresql.Driver";
                url = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require",
                        config.getHost(), config.getPort() > 0 ? config.getPort() : 5432, 
                        (config.getDatabaseName() != null && !config.getDatabaseName().isEmpty()) ? config.getDatabaseName() : "postgres");
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

    public synchronized StudentResult saveStudentEncryptedAndCalculateResult(StudentData student) throws Exception {
        if (connection == null || connection.isClosed()) {
            throw new Exception("CSDL chưa được kết nối!");
        }

        String encFullName = SecurityManager.encrypt(student.getFullName(), desKey);
        String encToan = SecurityManager.encrypt(String.valueOf(student.getScoreMath()), desKey);
        String encVan = SecurityManager.encrypt(String.valueOf(student.getScoreLiterature()), desKey);
        String encAnh = SecurityManager.encrypt(String.valueOf(student.getScoreEnglish()), desKey);

        double avgScore = (student.getScoreMath() + student.getScoreLiterature() + student.getScoreEnglish()) / 3.0;
        avgScore = Math.round(avgScore * 100.0) / 100.0;

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

        return getStudentDecrypted(student.getStudentId());
    }

    public synchronized StudentResult getStudentDecrypted(String studentId) throws Exception {
        String querySql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB " +
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

                    String fullName = SecurityManager.decrypt(encFullName, desKey);
                    double scoreMath = parseDoubleSafe(SecurityManager.decrypt(encToan, desKey), 0.0);
                    double scoreVan = parseDoubleSafe(SecurityManager.decrypt(encVan, desKey), 0.0);
                    double scoreAnh = parseDoubleSafe(SecurityManager.decrypt(encAnh, desKey), 0.0);

                    double avg = (scoreMath + scoreVan + scoreAnh) / 3.0;
                    avg = Math.round(avg * 100.0) / 100.0;

                    StudentResult result = new StudentResult(id, fullName, avg);
                    result.setScoreMath(scoreMath);
                    result.setScoreLiterature(scoreVan);
                    result.setScoreEnglish(scoreAnh);
                    return result;
                }
            }
        }
        throw new Exception("Không tìm thấy sinh viên có mã: " + studentId);
    }

    public synchronized boolean deleteStudent(String studentId) throws Exception {
        if (connection == null || connection.isClosed()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        String sql = "DELETE FROM SinhVien WHERE MaSV = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    public synchronized List<StudentResult> getAllStudentsDecrypted() throws Exception {
        List<StudentResult> list = new ArrayList<>();
        if (connection == null || connection.isClosed()) {
            return list;
        }
        String sql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB FROM SinhVien ORDER BY MaSV ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("MaSV");
                String encName = rs.getString("HoTenEncrypted");
                String encToan = rs.getString("DiemToanEncrypted");
                String encVan = rs.getString("DiemVanEncrypted");
                String encAnh = rs.getString("DiemAnhEncrypted");

                String fullName;
                try {
                    fullName = SecurityManager.decrypt(encName, desKey);
                } catch (Exception ex) {
                    fullName = "[Lỗi giải mã]";
                }

                double scoreMath = parseDoubleSafe(SecurityManager.decrypt(encToan, desKey), 0.0);
                double scoreVan = parseDoubleSafe(SecurityManager.decrypt(encVan, desKey), 0.0);
                double scoreAnh = parseDoubleSafe(SecurityManager.decrypt(encAnh, desKey), 0.0);
                double avg = rs.getDouble("DiemTB");

                StudentResult res = new StudentResult(id, fullName, avg);
                res.setScoreMath(scoreMath);
                res.setScoreLiterature(scoreVan);
                res.setScoreEnglish(scoreAnh);
                list.add(res);
            }
        }
        return list;
    }

    public synchronized List<StudentResult> searchStudents(String query) throws Exception {
        List<StudentResult> all = getAllStudentsDecrypted();
        if (query == null || query.trim().isEmpty()) {
            return all;
        }
        String q = query.trim().toLowerCase();
        List<StudentResult> filtered = new ArrayList<>();
        for (StudentResult r : all) {
            if (r.getStudentId().toLowerCase().contains(q) || r.getFullName().toLowerCase().contains(q) || r.getAcademicRank().toLowerCase().contains(q)) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    public synchronized Map<String, Integer> getRankStatistics() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("Xuất sắc", 0);
        map.put("Giỏi", 0);
        map.put("Khá", 0);
        map.put("Trung bình", 0);
        map.put("Yếu", 0);

        List<StudentResult> list = getAllStudentsDecrypted();
        for (StudentResult r : list) {
            String rank = r.getAcademicRank();
            map.put(rank, map.getOrDefault(rank, 0) + 1);
        }
        return map;
    }

    private double parseDoubleSafe(String val, double def) {
        try {
            return Double.parseDouble(val);
        } catch (Exception e) {
            return def;
        }
    }

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
                    rec.hoTenDecrypted = SecurityManager.decrypt(rec.hoTenEncrypted, desKey);
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
