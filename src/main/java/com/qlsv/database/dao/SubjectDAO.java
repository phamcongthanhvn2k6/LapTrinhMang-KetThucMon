package com.qlsv.database.dao;

import com.qlsv.database.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Data Access Object for Subject/Course operations.
 */
public class SubjectDAO {

    public void initDefaultSubjects() {
        String checkCourse = "SELECT COUNT(*) FROM MonHoc";
        try (Connection conn = ConnectionPool.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(checkCourse)) {

            if (rs.next() && rs.getInt(1) == 0) {
                String insertCourse = "INSERT INTO MonHoc (MaMon, TenMon, SoTinChi) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertCourse)) {
                    Object[][] courses = {
                            {"INT1339", "Lập Trình Mạng", 3},
                            {"INT1340", "Mã Hóa & An Toàn Thông Tin", 3},
                            {"INT1310", "Cơ Sở Dữ Liệu SQL", 3},
                            {"INT1306", "Cấu Trúc Dữ Liệu & Giải Thuật", 3},
                            {"INT1332", "Kiến Trúc Máy Tính", 3}
                    };
                    for (Object[] c : courses) {
                        ps.setString(1, (String) c[0]);
                        ps.setString(2, (String) c[1]);
                        ps.setInt(3, (Integer) c[2]);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
