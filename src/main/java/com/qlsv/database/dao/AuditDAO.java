package com.qlsv.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.qlsv.database.ConnectionPool;

/**
 * Data Access Object for Immutable Security & Grade Audit Trail Logging.
 */
public class AuditDAO {

    public static class AuditEntry {
        public int id;
        public String username;
        public String action;
        public String details;
        public String ipAddress;
        public String timestamp;

        public AuditEntry() {}

        public AuditEntry(int id, String username, String action, String details, String ipAddress, String timestamp) {
            this.id = id;
            this.username = username;
            this.action = action;
            this.details = details;
            this.ipAddress = ipAddress;
            this.timestamp = timestamp;
        }
    }

    public void logAudit(String username, String action, String details, String ip) {
        String sql = "INSERT INTO LichSuAudit (TenDangNhap, HanhDong, ChiTiet, DiaChiIP) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, action);
            ps.setString(3, details != null && details.length() > 480 ? details.substring(0, 480) : details);
            ps.setString(4, ip);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    public List<AuditEntry> getRecentAuditLogs(int limit) {
        List<AuditEntry> list = new ArrayList<>();
        if (limit <= 0) limit = 50;
        String sql = "SELECT Id, TenDangNhap, HanhDong, ChiTiet, DiaChiIP, ThoiGian FROM LichSuAudit ORDER BY Id DESC LIMIT ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AuditEntry(
                            rs.getInt("Id"),
                            rs.getString("TenDangNhap"),
                            rs.getString("HanhDong"),
                            rs.getString("ChiTiet"),
                            rs.getString("DiaChiIP"),
                            String.valueOf(rs.getTimestamp("ThoiGian"))
                    ));
                }
            }
        } catch (Exception ignored) {}
        return list;
    }
}
