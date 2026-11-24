package com.qlsv.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.qlsv.database.ConnectionPool;
import com.qlsv.database.DatabaseManager;
import com.qlsv.model.CourseGrade;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.security.SecurityManager;
import com.qlsv.util.InputValidator;

/**
 * Data Access Object for Student operations with Dual Encryption (DES / AES-256-GCM),
 * Single-Connection ACID Batch Transactions, and University Credit Weighting (10% CC + 20% GK + 70% CK).
 */
public class StudentDAO {

    public StudentResult saveStudentEncryptedAndCalculateResult(StudentData student, String desKey) throws Exception {
        InputValidator.validateAndNormalizeStudentData(student);
        try (Connection conn = ConnectionPool.getConnection()) {
            boolean origAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                StudentResult res = upsertStudentOnConnection(conn, student, desKey);
                conn.commit();
                return res;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(origAuto);
            }
        }
    }

    /**
     * Thực thi Upsert trên cùng 1 Connection (Tránh lỗi lồng Connection gây mất tính toàn vẹn Transaction và cạn kiệt Pool).
     * Đồng thời đồng bộ điểm học phần sang bảng BangDiem để Cổng Sinh Viên luôn hiển thị điểm mới nhất.
     */
    private StudentResult upsertStudentOnConnection(Connection conn, StudentData student, String desKey) throws Exception {
        String cleanId = student.getStudentId().trim().toUpperCase();
        String encFullName = SecurityManager.encrypt(student.getFullName(), desKey);
        String encToan = SecurityManager.encrypt(String.valueOf(student.getScoreMath()), desKey);
        String encVan = SecurityManager.encrypt(String.valueOf(student.getScoreLiterature()), desKey);
        String encAnh = SecurityManager.encrypt(String.valueOf(student.getScoreEnglish()), desKey);

        double avgScore = StudentResult.calculateWeightedScore(
                student.getScoreMath(),
                student.getScoreLiterature(),
                student.getScoreEnglish()
        );

        String checkSql = "SELECT COUNT(*) FROM SinhVien WHERE MaSV = ?";
        boolean exists = false;
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, cleanId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    exists = true;
                }
            }
        }

        if (exists) {
            String updateSql = "UPDATE SinhVien SET HoTenEncrypted = ?, DiemToanEncrypted = ?, " +
                    "DiemVanEncrypted = ?, DiemAnhEncrypted = ?, DiemTB = ? WHERE MaSV = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, encFullName);
                stmt.setString(2, encToan);
                stmt.setString(3, encVan);
                stmt.setString(4, encAnh);
                stmt.setDouble(5, avgScore);
                stmt.setString(6, cleanId);
                stmt.executeUpdate();
            }
        } else {
            String insertSql = "INSERT INTO SinhVien (MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, cleanId);
                stmt.setString(2, encFullName);
                stmt.setString(3, encToan);
                stmt.setString(4, encVan);
                stmt.setString(5, encAnh);
                stmt.setDouble(6, avgScore);
                stmt.executeUpdate();
            }
        }

        // Đồng bộ điểm học phần chính (INT1339 - Lập Trình Mạng) trong bảng BangDiem nếu đã tồn tại
        try {
            double processScore = Math.round(((student.getScoreMath() + student.getScoreLiterature() * 2.0) / 3.0) * 100.0) / 100.0;
            CourseGrade cg = new CourseGrade("INT1339", "Lập Trình Mạng", 3,
                    student.getScoreMath(), student.getScoreLiterature(), student.getScoreEnglish());
            String syncBangDiemSql = "UPDATE BangDiem SET DiemQuaTrinhEncrypted = ?, DiemThiEncrypted = ?, " +
                    "DiemTongKet = ?, DiemChu = ?, DiemHe4 = ? WHERE MaSV = ? AND MaMon = 'INT1339'";
            try (PreparedStatement psSync = conn.prepareStatement(syncBangDiemSql)) {
                psSync.setString(1, SecurityManager.encrypt(String.valueOf(processScore), desKey));
                psSync.setString(2, SecurityManager.encrypt(String.valueOf(student.getScoreEnglish()), desKey));
                psSync.setDouble(3, cg.getTotalScore());
                psSync.setString(4, cg.getLetterGrade());
                psSync.setDouble(5, cg.getGpa4());
                psSync.setString(6, cleanId);
                psSync.executeUpdate();
            }
        } catch (Exception ignored) {}

        StudentResult result = new StudentResult(cleanId, student.getFullName(), avgScore);
        result.setScoreMath(student.getScoreMath());
        result.setScoreLiterature(student.getScoreLiterature());
        result.setScoreEnglish(student.getScoreEnglish());
        return result;
    }

    public int saveStudentBatch(List<StudentData> list, String desKey) throws Exception {
        if (list == null || list.isEmpty()) return 0;
        for (StudentData s : list) {
            InputValidator.validateAndNormalizeStudentData(s);
        }

        int count = 0;
        try (Connection conn = ConnectionPool.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                for (StudentData s : list) {
                    upsertStudentOnConnection(conn, s, desKey);
                    count++;
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
        return count;
    }

    public StudentResult getStudentDecrypted(String studentId, String desKey) throws Exception {
        String cleanId = (studentId != null) ? studentId.trim().toUpperCase() : "";
        String querySql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB " +
                "FROM SinhVien WHERE MaSV = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(querySql)) {
            stmt.setString(1, cleanId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("MaSV");
                    String encFullName = rs.getString("HoTenEncrypted");
                    String encToan = rs.getString("DiemToanEncrypted");
                    String encVan = rs.getString("DiemVanEncrypted");
                    String encAnh = rs.getString("DiemAnhEncrypted");

                    String fullName = SecurityManager.decrypt(encFullName, desKey);
                    double dbAvg = rs.getDouble("DiemTB");
                    double scoreMath = parseDoubleSafe(SecurityManager.decrypt(encToan, desKey), dbAvg);
                    double scoreVan = parseDoubleSafe(SecurityManager.decrypt(encVan, desKey), dbAvg);
                    double scoreAnh = parseDoubleSafe(SecurityManager.decrypt(encAnh, desKey), dbAvg);

                    double avg = StudentResult.calculateWeightedScore(scoreMath, scoreVan, scoreAnh);

                    StudentResult result = new StudentResult(id, fullName, avg);
                    result.setScoreMath(scoreMath);
                    result.setScoreLiterature(scoreVan);
                    result.setScoreEnglish(scoreAnh);
                    return result;
                }
            }
        }
        throw new Exception("Không tìm thấy sinh viên có mã: " + cleanId);
    }

    public boolean deleteStudent(String studentId) throws Exception {
        String cleanId = (studentId != null) ? studentId.trim().toUpperCase() : "";
        try (Connection conn = ConnectionPool.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM BangDiem WHERE MaSV = ?")) {
                    stmt2.setString(1, cleanId);
                    stmt2.executeUpdate();
                }

                String sql = "DELETE FROM SinhVien WHERE MaSV = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, cleanId);
                    int rows = stmt.executeUpdate();
                    conn.commit();
                    return rows > 0;
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public List<StudentResult> getAllStudentsDecrypted(String desKey) throws Exception {
        List<StudentResult> list = new ArrayList<>();
        String sql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB FROM SinhVien ORDER BY MaSV ASC";

        try (Connection conn = ConnectionPool.getConnection();
             Statement stmt = conn.createStatement();
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
                    fullName = "[SV " + id + "]";
                }

                double avg = rs.getDouble("DiemTB");
                double scoreMath = avg > 0 ? avg : 8.0;
                double scoreVan = avg > 0 ? avg : 8.0;
                double scoreAnh = avg > 0 ? avg : 8.0;

                try {
                    if (encToan != null) scoreMath = parseDoubleSafe(SecurityManager.decrypt(encToan, desKey), avg);
                } catch (Exception ignored) {}
                try {
                    if (encVan != null) scoreVan = parseDoubleSafe(SecurityManager.decrypt(encVan, desKey), avg);
                } catch (Exception ignored) {}
                try {
                    if (encAnh != null) scoreAnh = parseDoubleSafe(SecurityManager.decrypt(encAnh, desKey), avg);
                } catch (Exception ignored) {}

                double weightedAvg = StudentResult.calculateWeightedScore(scoreMath, scoreVan, scoreAnh);
                StudentResult res = new StudentResult(id, fullName, weightedAvg);
                res.setScoreMath(scoreMath);
                res.setScoreLiterature(scoreVan);
                res.setScoreEnglish(scoreAnh);
                list.add(res);
            }
        }
        return list;
    }

    public List<StudentResult> searchStudents(String query, String desKey) throws Exception {
        List<StudentResult> all = getAllStudentsDecrypted(desKey);
        if (query == null || query.trim().isEmpty()) {
            return all;
        }
        String q = query.trim().toLowerCase();
        List<StudentResult> filtered = new ArrayList<>();
        for (StudentResult r : all) {
            if (r.getStudentId().toLowerCase().contains(q) ||
                    r.getFullName().toLowerCase().contains(q) ||
                    r.getAcademicRankCredit().toLowerCase().contains(q) ||
                    r.getAcademicStatus().toLowerCase().contains(q)) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    public com.qlsv.model.PageResult<StudentResult> getStudentsPaged(int page, int pageSize, String desKey) throws Exception {
        if (page < 1) page = 1;
        if (pageSize <= 0) pageSize = 20;

        int totalCount = 0;
        String countSql = "SELECT COUNT(*) FROM SinhVien";
        try (Connection conn = ConnectionPool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                totalCount = rs.getInt(1);
            }
        }

        int offset = (page - 1) * pageSize;
        List<StudentResult> list = new ArrayList<>();
        String sql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB FROM SinhVien ORDER BY MaSV ASC LIMIT ? OFFSET ?";

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
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
                        fullName = "[SV " + id + "]";
                    }

                    double avg = rs.getDouble("DiemTB");
                    double scoreMath = avg > 0 ? avg : 8.0;
                    double scoreVan = avg > 0 ? avg : 8.0;
                    double scoreAnh = avg > 0 ? avg : 8.0;

                    try {
                        if (encToan != null) scoreMath = parseDoubleSafe(SecurityManager.decrypt(encToan, desKey), avg);
                    } catch (Exception ignored) {}
                    try {
                        if (encVan != null) scoreVan = parseDoubleSafe(SecurityManager.decrypt(encVan, desKey), avg);
                    } catch (Exception ignored) {}
                    try {
                        if (encAnh != null) scoreAnh = parseDoubleSafe(SecurityManager.decrypt(encAnh, desKey), avg);
                    } catch (Exception ignored) {}

                    double weightedAvg = StudentResult.calculateWeightedScore(scoreMath, scoreVan, scoreAnh);
                    StudentResult res = new StudentResult(id, fullName, weightedAvg);
                    res.setScoreMath(scoreMath);
                    res.setScoreLiterature(scoreVan);
                    res.setScoreEnglish(scoreAnh);
                    list.add(res);
                }
            }
        }
        return new com.qlsv.model.PageResult<>(list, page, pageSize, totalCount);
    }

    public com.qlsv.model.PageResult<StudentResult> searchStudentsPaged(String query, int page, int pageSize, String desKey) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            return getStudentsPaged(page, pageSize, desKey);
        }
        List<StudentResult> filtered = searchStudents(query, desKey);
        if (page < 1) page = 1;
        if (pageSize <= 0) pageSize = 20;

        int fromIndex = Math.min((page - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<StudentResult> sub = filtered.subList(fromIndex, toIndex);
        return new com.qlsv.model.PageResult<>(sub, page, pageSize, filtered.size());
    }

    public Map<String, Integer> getRankStatistics(String desKey) throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("Xuất sắc", 0);
        map.put("Giỏi", 0);
        map.put("Khá", 0);
        map.put("Trung bình", 0);
        map.put("Yếu", 0);

        List<StudentResult> list = getAllStudentsDecrypted(desKey);
        for (StudentResult r : list) {
            String rank = r.getAcademicRankCredit();
            if ("Kém".equals(rank)) {
                rank = "Yếu";
            }
            map.put(rank, map.getOrDefault(rank, 0) + 1);
        }
        return map;
    }

    public List<DatabaseManager.EncryptedRecord> getAllRecordsForInspection(String desKey) throws Exception {
        List<DatabaseManager.EncryptedRecord> list = new ArrayList<>();
        String sql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB FROM SinhVien ORDER BY MaSV ASC";

        try (Connection conn = ConnectionPool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                DatabaseManager.EncryptedRecord rec = new DatabaseManager.EncryptedRecord();
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

    /**
     * Xoay khóa mật mã và tái mã hóa toàn bộ bản ghi trong CSDL (Live Cryptographic Key & Algorithm Rotation)
     * trong một giao dịch ACID duy nhất.
     */
    public int rotateKeyAndReEncryptAll(String oldKey, String newKey, SecurityManager.EncryptionAlgo newAlgo) throws Exception {
        List<StudentResult> allStudents = getAllStudentsDecrypted(oldKey);
        SecurityManager.setCurrentAlgo(newAlgo);

        int rotatedCount = 0;
        try (Connection conn = ConnectionPool.getConnection()) {
            boolean origAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String updateSql = "UPDATE SinhVien SET HoTenEncrypted = ?, DiemToanEncrypted = ?, DiemVanEncrypted = ?, DiemAnhEncrypted = ? WHERE MaSV = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    for (StudentResult s : allStudents) {
                        ps.setString(1, SecurityManager.encrypt(s.getFullName(), newKey));
                        ps.setString(2, SecurityManager.encrypt(String.valueOf(s.getScoreCC()), newKey));
                        ps.setString(3, SecurityManager.encrypt(String.valueOf(s.getScoreGK()), newKey));
                        ps.setString(4, SecurityManager.encrypt(String.valueOf(s.getScoreCK()), newKey));
                        ps.setString(5, s.getStudentId());
                        ps.addBatch();
                        rotatedCount++;
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(origAuto);
            }
        }
        return rotatedCount;
    }

    private double parseDoubleSafe(String val, double def) {
        try {
            return Double.parseDouble(val);
        } catch (Exception e) {
            return def;
        }
    }
}
