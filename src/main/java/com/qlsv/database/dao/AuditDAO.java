package com.qlsv.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.qlsv.database.ConnectionPool;

/**
 * Data Access Object for Audit Logging.
 */
public class AuditDAO {

    public void logAudit(String username, String action, String details, String ip) {
        String sql = "INSERT INTO LichSuAudit (TenDangNhap, HanhDong, ChiTiet, DiaChiIP) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.setString(4, ip);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}

