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
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.security.SecurityManager;

/**
 * Data Access Object for Student operations with DES Encryption.
 */
public class StudentDAO {

    public StudentResult saveStudentEncryptedAndCalculateResult(StudentData student, String desKey) throws Exception {
        String encFullName = SecurityManager.encrypt(student.getFullName(), desKey);
        String encToan = SecurityManager.encrypt(String.valueOf(student.getScoreMath()), desKey);
        String encVan = SecurityManager.encrypt(String.valueOf(student.getScoreLiterature()), desKey);
        String encAnh = SecurityManager.encrypt(String.valueOf(student.getScoreEnglish()), desKey);

        double avgScore = (student.getScoreMath() + student.getScoreLiterature() + student.getScoreEnglish()) / 3.0;
        avgScore = Math.round(avgScore * 100.0) / 100.0;

        try (Connection conn = ConnectionPool.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM SinhVien WHERE MaSV = ?";
            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
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
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
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
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setString(1, student.getStudentId());
                    stmt.setString(2, encFullName);
                    stmt.setString(3, encToan);
                    stmt.setString(4, encVan);
                    stmt.setString(5, encAnh);
                    stmt.setDouble(6, avgScore);
                    stmt.executeUpdate();
                }
            }
        }

        return getStudentDecrypted(student.getStudentId(), desKey);
    }

    public int saveStudentBatch(List<StudentData> list, String desKey) throws Exception {
        if (list == null || list.isEmpty()) return 0;
        int count = 0;

        try (Connection conn = ConnectionPool.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                for (StudentData s : list) {
                    saveStudentEncryptedAndCalculateResult(s, desKey);
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
        String querySql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB " +
                "FROM SinhVien WHERE MaSV = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(querySql)) {
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

    public boolean deleteStudent(String studentId) throws Exception {
        try (Connection conn = ConnectionPool.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // Cascade delete grades first
                try (PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM BangDiem WHERE MaSV = ?")) {
                    stmt2.setString(1, studentId);
                    stmt2.executeUpdate();
                }

                String sql = "DELETE FROM SinhVien WHERE MaSV = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, studentId);
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
                    r.getAcademicRank().toLowerCase().contains(q)) {
                filtered.add(r);
            }
        }
        return filtered;
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
            String rank = r.getAcademicRank();
            map.put(rank, map.getOrDefault(rank, 0) + 1);
        }
        return map;
    }

    public List<DatabaseManager.EncryptedRecord> getAllRecordsForInspection(String desKey) throws Exception {
        List<DatabaseManager.EncryptedRecord> list = new ArrayList<>();
        String sql = "SELECT MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB FROM SinhVien";

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

    private double parseDoubleSafe(String val, double def) {
        try {
            return Double.parseDouble(val);
        } catch (Exception e) {
            return def;
        }
    }
}

