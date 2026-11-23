package com.qlsv.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.qlsv.database.ConnectionPool;
import com.qlsv.model.CourseGrade;
import com.qlsv.model.StudentResult;
import com.qlsv.model.StudentTranscript;
import com.qlsv.security.SecurityManager;

/**
 * Data Access Object for Transcript & Grade calculations.
 * Luôn đồng bộ hóa điểm học phần chính (INT1339 - Lập Trình Mạng) với bảng SinhVien.
 */
public class TranscriptDAO {

    public StudentTranscript getDetailedTranscript(String studentId, String desKey, StudentResult student) throws Exception {
        String cleanId = student.getStudentId().trim().toUpperCase();
        StudentTranscript transcript = new StudentTranscript(cleanId, student.getFullName(), student.getClassName());

        String sql = "SELECT b.MaMon, m.TenMon, m.SoTinChi, b.DiemQuaTrinhEncrypted, b.DiemThiEncrypted, b.DiemTongKet, b.DiemChu, b.DiemHe4 " +
                "FROM BangDiem b JOIN MonHoc m ON b.MaMon = m.MaMon WHERE b.MaSV = ? ORDER BY b.MaMon ASC";

        boolean hasRecords = false;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hasRecords = true;
                    String code = rs.getString("MaMon");
                    String name = rs.getString("TenMon");
                    int credits = rs.getInt("SoTinChi");

                    if ("INT1339".equalsIgnoreCase(code)) {
                        // Đồng bộ trực tiếp với 3 cột điểm Chuyên cần (10%), Giữa kỳ (20%), Cuối kỳ (70%) vừa chấm của sinh viên
                        CourseGrade cg = new CourseGrade(code, name, credits,
                                student.getScoreCC(), student.getScoreGK(), student.getScoreCK());
                        transcript.addCourseGrade(cg);
                    } else {
                        double midterm = parseDoubleSafe(SecurityManager.decrypt(rs.getString("DiemQuaTrinhEncrypted"), desKey), 8.0);
                        double finalExam = parseDoubleSafe(SecurityManager.decrypt(rs.getString("DiemThiEncrypted"), desKey), 8.0);
                        CourseGrade cg = new CourseGrade(code, name, credits, midterm, finalExam);
                        transcript.addCourseGrade(cg);
                    }
                }
            }
        }

        if (!hasRecords) {
            double cc = student.getScoreCC();
            double gk = student.getScoreGK();
            double ck = student.getScoreCK();
            double avg = student.getAverageScore();

            Object[][] sampleData = {
                    {"INT1306", "Cấu Trúc Dữ Liệu & Giải Thuật", 3, Math.max(4.0, Math.min(10.0, gk)), Math.max(4.0, Math.min(10.0, avg))},
                    {"INT1310", "Cơ Sở Dữ Liệu SQL", 3, Math.max(4.0, Math.min(10.0, cc)), Math.max(4.0, Math.min(10.0, ck))},
                    {"INT1332", "Kiến Trúc Máy Tính", 3, 8.5, 8.8},
                    {"INT1339", "Lập Trình Mạng", 3, gk, ck},
                    {"INT1340", "Mã Hóa & An Toàn Thông Tin", 3, Math.max(4.0, Math.min(10.0, gk * 0.95)), Math.max(4.0, Math.min(10.0, ck * 0.95))}
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

                    CourseGrade cg;
                    if ("INT1339".equals(code)) {
                        cg = new CourseGrade(code, name, credits, cc, gk, ck);
                    } else {
                        cg = new CourseGrade(code, name, credits, mid, fin);
                    }
                    transcript.addCourseGrade(cg);

                    ps.setString(1, cleanId);
                    ps.setString(2, code);
                    ps.setString(3, SecurityManager.encrypt(String.valueOf(mid), desKey));
                    ps.setString(4, SecurityManager.encrypt(String.valueOf(fin), desKey));
                    ps.setDouble(5, cg.getTotalScore());
                    ps.setString(6, cg.getLetterGrade());
                    ps.setDouble(7, cg.getGpa4());
                    ps.executeUpdate();
                }
            } catch (Exception ignored) {}
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
