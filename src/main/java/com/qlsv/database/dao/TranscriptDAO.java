package com.qlsv.database.dao;

import com.qlsv.database.ConnectionPool;
import com.qlsv.model.CourseGrade;
import com.qlsv.model.StudentResult;
import com.qlsv.model.StudentTranscript;
import com.qlsv.security.SecurityManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Data Access Object for Transcript & Grade calculations.
 */
public class TranscriptDAO {

    public StudentTranscript getDetailedTranscript(String studentId, String desKey, StudentResult student) throws Exception {
        StudentTranscript transcript = new StudentTranscript(student.getStudentId(), student.getFullName(), student.getClassName());

        String sql = "SELECT b.MaMon, m.TenMon, m.SoTinChi, b.DiemQuaTrinhEncrypted, b.DiemThiEncrypted, b.DiemTongKet, b.DiemChu, b.DiemHe4 " +
                "FROM BangDiem b JOIN MonHoc m ON b.MaMon = m.MaMon WHERE b.MaSV = ? ORDER BY b.MaMon ASC";

        boolean hasRecords = false;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasRecords = true;
                    String code = rs.getString("MaMon");
                    String name = rs.getString("TenMon");
                    int credits = rs.getInt("SoTinChi");
                    double midterm = parseDoubleSafe(SecurityManager.decrypt(rs.getString("DiemQuaTrinhEncrypted"), desKey), 8.0);
                    double finalExam = parseDoubleSafe(SecurityManager.decrypt(rs.getString("DiemThiEncrypted"), desKey), 8.0);

                    CourseGrade cg = new CourseGrade(code, name, credits, midterm, finalExam);
                    transcript.addCourseGrade(cg);
                }
            }
        }

        if (!hasRecords) {
            // Auto-populate 5 default courses from student subject scores
            double m = student.getScoreMath();
            double l = student.getScoreLiterature();
            double e = student.getScoreEnglish();
            double avg = student.getAverageScore();

            Object[][] sampleData = {
                    {"INT1339", "Lập Trình Mạng", 3, Math.min(10.0, m * 0.95), m},
                    {"INT1340", "Mã Hóa & An Toàn Thông Tin", 3, Math.min(10.0, l * 0.9), l},
                    {"INT1310", "Cơ Sở Dữ Liệu SQL", 3, Math.min(10.0, e * 0.92), e},
                    {"INT1306", "Cấu Trúc Dữ Liệu & Giải Thuật", 3, Math.min(10.0, avg * 0.93), avg},
                    {"INT1332", "Kiến Trúc Máy Tính", 3, 8.5, 9.0}
            };

            String insertSql = "INSERT INTO BangDiem (MaSV, MaMon, DiemQuaTrinhEncrypted, DiemThiEncrypted, DiemTongKet, DiemChu, DiemHe4) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = ConnectionPool.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Object[] row : sampleData) {
                    String code = (String) row[0];
                    String name = (String) row[1];
                    int credits = (Integer) row[2];
                    double mid = (Double) row[3];
                    double fin = (Double) row[4];

                    CourseGrade cg = new CourseGrade(code, name, credits, mid, fin);
                    transcript.addCourseGrade(cg);

                    ps.setString(1, studentId);
                    ps.setString(2, code);
                    ps.setString(3, SecurityManager.encrypt(String.valueOf(mid), desKey));
                    ps.setString(4, SecurityManager.encrypt(String.valueOf(fin), desKey));
                    ps.setDouble(5, cg.getTotalScore());
                    ps.setString(6, cg.getLetterGrade());
                    ps.setDouble(7, cg.getGpa4());
                    ps.executeUpdate();
                }
            }
        }

        return transcript;
    }

    private double parseDoubleSafe(String val, double def) {
        try {
            return Double.parseDouble(val);
        } catch (Exception e) {
            return def;
        }
    }
}
