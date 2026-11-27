package com.qlsv.database;

import com.qlsv.database.dao.*;
import com.qlsv.model.*;

import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * Central Database Facade & Lifecycle Manager.
 * Orchestrates connection pooling via HikariCP and delegates persistence to specialized DAOs.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private SqlConfig currentConfig;
    private String desKey = "QLSV_KEY";
    private volatile boolean desKeyUnlocked = true; // Khi ServerGUI khởi chạy sẽ khóa lại (false) bắt buộc người dùng tự nhập khóa DES!

    private final StudentDAO studentDAO = new StudentDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final TranscriptDAO transcriptDAO = new TranscriptDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final AuditDAO auditDAO = new AuditDAO();

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

    public boolean isDesKeyUnlocked() {
        return desKeyUnlocked;
    }

    public void setDesKeyUnlocked(boolean unlocked) {
        this.desKeyUnlocked = unlocked;
    }

    private void ensureDesKeyUnlocked() throws Exception {
        if (!desKeyUnlocked) {
            throw new Exception("CHƯA MỞ KHÓA BẢO MẬT DES TRÊN SERVER: Quản trị viên cần nhập Khóa DES (8 ký tự) trên Máy chủ để mở khóa giải mã CSDL!");
        }
    }

    public synchronized boolean connect(SqlConfig config) throws Exception {
        this.currentConfig = config;
        ConnectionPool.init(config);

        try (Connection conn = ConnectionPool.getConnection()) {
            createTablesIfNotExists(conn, config.getDbType());
        }

        subjectDAO.initDefaultSubjects();
        accountDAO.initDefaultAccounts();
        return true;
    }

    private void createTablesIfNotExists(Connection connection, SqlConfig.DbType dbType) throws SQLException {
        if (connection == null) return;

        String autoIncId;
        switch (dbType) {
            case POSTGRESQL:
                autoIncId = "SERIAL PRIMARY KEY";
                break;
            case MYSQL:
                autoIncId = "INT AUTO_INCREMENT PRIMARY KEY";
                break;
            case SQLITE_EMBEDDED:
                autoIncId = "INTEGER PRIMARY KEY AUTOINCREMENT";
                break;
            case SQL_SERVER:
                autoIncId = "INT IDENTITY(1,1) PRIMARY KEY";
                break;
            case H2_EMBEDDED:
            default:
                autoIncId = "IDENTITY PRIMARY KEY";
                break;
        }

        try (Statement stmt = connection.createStatement()) {
            // 1. Table SinhVien
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS SinhVien (" +
                    "MaSV VARCHAR(50) PRIMARY KEY, " +
                    "HoTenEncrypted VARCHAR(500) NOT NULL, " +
                    "DiemToanEncrypted VARCHAR(200) NOT NULL, " +
                    "DiemVanEncrypted VARCHAR(200) NOT NULL, " +
                    "DiemAnhEncrypted VARCHAR(200) NOT NULL, " +
                    "DiemTB DOUBLE PRECISION, " +
                    "CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 2. Table MonHoc
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS MonHoc (" +
                    "MaMon VARCHAR(20) PRIMARY KEY, " +
                    "TenMon VARCHAR(150) NOT NULL, " +
                    "SoTinChi INT DEFAULT 3)");

            // 3. Table BangDiem
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS BangDiem (" +
                    "Id " + autoIncId + ", " +
                    "MaSV VARCHAR(50) NOT NULL, " +
                    "MaMon VARCHAR(20) NOT NULL, " +
                    "HocKy VARCHAR(20) DEFAULT 'HK1 2024-2025', " +
                    "DiemQuaTrinhEncrypted VARCHAR(200) NOT NULL, " +
                    "DiemThiEncrypted VARCHAR(200) NOT NULL, " +
                    "DiemTongKet DOUBLE PRECISION, " +
                    "DiemChu VARCHAR(5), " +
                    "DiemHe4 DOUBLE PRECISION)");

            // 4. Table TaiKhoan
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS TaiKhoan (" +
                    "TenDangNhap VARCHAR(50) PRIMARY KEY, " +
                    "MatKhauHash VARCHAR(250) NOT NULL, " +
                    "Salt VARCHAR(100) NOT NULL, " +
                    "VaiTro VARCHAR(20) NOT NULL, " +
                    "MaSV VARCHAR(50), " +
                    "HoTen VARCHAR(100), " +
                    "Email VARCHAR(100))");

            // 5. Table LichSuAudit
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS LichSuAudit (" +
                    "Id " + autoIncId + ", " +
                    "TenDangNhap VARCHAR(50), " +
                    "HanhDong VARCHAR(50) NOT NULL, " +
                    "ChiTiet VARCHAR(500), " +
                    "DiaChiIP VARCHAR(50), " +
                    "ThoiGian TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    public void insertUserAccount(String username, String rawPassword, UserAccount.Role role, String studentId, String fullName, String email) {
        accountDAO.insertUserAccount(username, rawPassword, role, studentId, fullName, email);
    }

    public UserAccount authenticateUser(String username, String password) {
        return accountDAO.authenticateUser(username, password);
    }

    public UserAccount registerNewAccount(String username, String rawPassword, UserAccount.Role role,
                                          String studentId, String fullName, String email) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        return accountDAO.registerNewAccount(username, rawPassword, role, studentId, fullName, email);
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        return accountDAO.changePassword(username, oldPassword, newPassword);
    }

    public List<String[]> getAllAccountsForInspection() {
        if (!isConnected()) {
            return List.of();
        }
        return accountDAO.getAllAccountsForInspection();
    }

    public void logAudit(String username, String action, String details, String ip) {
        auditDAO.logAudit(username, action, details, ip);
    }

    public List<AuditDAO.AuditEntry> getRecentAuditLogs(int limit) {
        return auditDAO.getRecentAuditLogs(limit);
    }

    public StudentTranscript getDetailedTranscript(String studentId) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        StudentResult student = getStudentDecrypted(studentId);
        return transcriptDAO.getDetailedTranscript(studentId, desKey, student);
    }

    public StudentTranscript updateCourseGrade(String studentId, String courseCode, double processScore, double examScore) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        StudentResult student = getStudentDecrypted(studentId);
        return transcriptDAO.updateCourseGrade(studentId, courseCode, processScore, examScore, desKey, student);
    }

    public int rotateEncryptionForAllRecords(String newKey, com.qlsv.security.SecurityManager.EncryptionAlgo newAlgo) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        String cleanNewKey = (newKey != null && !newKey.trim().isEmpty()) ? newKey.trim() : this.desKey;
        int count = studentDAO.rotateKeyAndReEncryptAll(this.desKey, cleanNewKey, newAlgo);
        this.desKey = cleanNewKey;
        return count;
    }

    public int restoreEncryptedSnapshot(List<EncryptedRecord> records) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        return studentDAO.restoreEncryptedSnapshot(records);
    }

    public int saveStudentBatch(List<StudentData> list) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        return studentDAO.saveStudentBatch(list, desKey);
    }

    public StudentResult saveStudentEncryptedAndCalculateResult(StudentData student) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        return studentDAO.saveStudentEncryptedAndCalculateResult(student, desKey);
    }

    public StudentResult getStudentDecrypted(String studentId) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        ensureDesKeyUnlocked();
        return studentDAO.getStudentDecrypted(studentId, desKey);
    }

    public boolean deleteStudent(String studentId) throws Exception {
        if (!isConnected()) {
            throw new Exception("CSDL chưa được kết nối!");
        }
        ensureDesKeyUnlocked();
        return studentDAO.deleteStudent(studentId);
    }

    public List<StudentResult> getAllStudentsDecrypted() throws Exception {
        if (!isConnected()) {
            return List.of();
        }
        ensureDesKeyUnlocked();
        return studentDAO.getAllStudentsDecrypted(desKey);
    }

    public List<StudentResult> searchStudents(String query) throws Exception {
        ensureDesKeyUnlocked();
        return studentDAO.searchStudents(query, desKey);
    }

    public com.qlsv.model.PageResult<StudentResult> getStudentsPaged(int page, int pageSize) throws Exception {
        if (!isConnected()) {
            return new com.qlsv.model.PageResult<>(List.of(), page, pageSize, 0);
        }
        ensureDesKeyUnlocked();
        return studentDAO.getStudentsPaged(page, pageSize, desKey);
    }

    public com.qlsv.model.PageResult<StudentResult> searchStudentsPaged(String query, int page, int pageSize) throws Exception {
        if (!isConnected()) {
            return new com.qlsv.model.PageResult<>(List.of(), page, pageSize, 0);
        }
        ensureDesKeyUnlocked();
        return studentDAO.searchStudentsPaged(query, page, pageSize, desKey);
    }

    public Map<String, Integer> getRankStatistics() throws Exception {
        ensureDesKeyUnlocked();
        return studentDAO.getRankStatistics(desKey);
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

    public List<EncryptedRecord> getAllRecordsForInspection() throws Exception {
        if (!isConnected()) {
            return List.of();
        }
        ensureDesKeyUnlocked();
        return studentDAO.getAllRecordsForInspection(desKey);
    }

    public boolean isConnected() {
        return ConnectionPool.isInitialized();
    }

    public Connection getConnection() throws SQLException {
        return ConnectionPool.getConnection();
    }

    public void closeConnection() {
        ConnectionPool.closePool();
    }
}
