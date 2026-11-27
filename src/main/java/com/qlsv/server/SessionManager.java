package com.qlsv.server;

import com.qlsv.model.UserAccount;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý Phiên đăng nhập RBAC, Chống tấn công Brute-force (Rate-Limiting) & Trạng thái Khóa bảng điểm học kỳ.
 */
public class SessionManager {

    private static SessionManager instance;
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, LoginAttemptRecord> loginAttempts = new ConcurrentHashMap<>();

    // Bộ chống Spam & DoS Flood toàn cục theo IP / Tài khoản (Sliding Window & Cooldown)
    private final Map<String, SlidingWindowBucket> packetRateBuckets = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindowBucket> mutatingRateBuckets = new ConcurrentHashMap<>();
    private final Map<String, Long> actionCooldowns = new ConcurrentHashMap<>();

    private static final long SESSION_TIMEOUT_MS = 4 * 60 * 60 * 1000L; // 4 hours
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 60 * 1000L; // 60 seconds lockout

    // Ngưỡng chống Spam RUDP (Anti-Spam / DoS Rate Limiting Thresholds)
    public static final int MAX_PACKETS_PER_10_SEC = 120;      // Tối đa 120 gói/10s trên mỗi IP (Cho phép Benchmark 50 gói, chặn Flood >120 gói)
    public static final int MAX_MUTATIONS_PER_10_SEC = 18;     // Tối đa 18 lệnh Ghi/Sửa/Xóa/Đăng ký trong 10 giây
    public static final long SPAM_BAN_DURATION_MS = 30 * 1000L; // Khóa IP/Tài khoản spam trong 30 giây

    public static class SlidingWindowBucket {
        long windowStartMs = System.currentTimeMillis();
        int count = 0;
        long bannedUntilMs = 0;
    }

    // Trạng thái Khóa sổ điểm học kỳ (Gradebook Lock Workflow)
    private volatile boolean gradebookLocked = false;
    private volatile String gradebookLockedBy = "";

    public static class LoginAttemptRecord {
        int failedCount = 0;
        long lockedUntil = 0;
    }

    public static class UserSession {
        private final String token;
        private final String username;
        private final UserAccount.Role role;
        private final String studentId;
        private final String fullName;
        private long lastActive;

        public UserSession(String token, String username, UserAccount.Role role, String studentId, String fullName) {
            this.token = token;
            this.username = username;
            this.role = role;
            this.studentId = studentId;
            this.fullName = fullName;
            this.lastActive = System.currentTimeMillis();
        }

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }

        public UserAccount.Role getRole() {
            return role;
        }

        public String getStudentId() {
            return studentId;
        }

        public String getFullName() {
            return fullName;
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - lastActive) > SESSION_TIMEOUT_MS;
        }

        public void touch() {
            this.lastActive = System.currentTimeMillis();
        }
    }

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Brute-force Rate Limiting
    public synchronized long getRemainingLockoutSeconds(String key) {
        if (key == null) return 0;
        LoginAttemptRecord rec = loginAttempts.get(key.toLowerCase());
        if (rec == null) return 0;
        long now = System.currentTimeMillis();
        if (rec.lockedUntil > now) {
            return (rec.lockedUntil - now + 999) / 1000;
        }
        return 0;
    }

    public synchronized int recordLoginFailure(String key) {
        if (key == null) return 0;
        String k = key.toLowerCase();
        LoginAttemptRecord rec = loginAttempts.computeIfAbsent(k, x -> new LoginAttemptRecord());
        long now = System.currentTimeMillis();
        if (rec.lockedUntil > 0 && now >= rec.lockedUntil) {
            rec.failedCount = 0;
            rec.lockedUntil = 0;
        }
        rec.failedCount++;
        if (rec.failedCount >= MAX_FAILED_ATTEMPTS) {
            rec.lockedUntil = now + LOCKOUT_DURATION_MS;
        }
        return rec.failedCount;
    }

    public synchronized void clearLoginFailures(String key) {
        if (key != null) {
            loginAttempts.remove(key.toLowerCase());
        }
    }

    // Gradebook Lock Workflow
    public boolean isGradebookLocked() {
        return gradebookLocked;
    }

    public void setGradebookLocked(boolean locked, String operator) {
        this.gradebookLocked = locked;
        this.gradebookLockedBy = operator != null ? operator : "Admin";
    }

    public String getGradebookLockedBy() {
        return gradebookLockedBy;
    }

    public synchronized String createSession(UserAccount account) {
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession(token, account.getUsername(), account.getRole(), account.getStudentId(), account.getFullName());
        activeSessions.put(token, session);
        clearLoginFailures(account.getUsername());
        return token;
    }

    public UserSession getSession(String token) {
        if (token == null) return null;
        UserSession s = activeSessions.get(token);
        if (s != null) {
            if (s.isExpired()) {
                activeSessions.remove(token);
                return null;
            }
            s.touch();
        }
        return s;
    }

    public boolean isAuthorized(String token, UserAccount.Role... requiredRoles) {
        UserSession s = getSession(token);
        if (s == null) return false;
        if (s.getRole() == UserAccount.Role.ADMIN) return true; // Admin has all rights
        for (UserAccount.Role r : requiredRoles) {
            if (s.getRole() == r) return true;
        }
        return false;
    }

    public void invalidate(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }

    /**
     * Kiểm tra chống Spam / UDP Flood toàn cục cho mọi gói tin đến theo IP hoặc Session.
     * @return null nếu hợp lệ, hoặc chuỗi thông báo lỗi nếu vi phạm ngưỡng Anti-Spam.
     */
    public synchronized String checkPacketSpamLimit(String clientKey, boolean isMutatingCommand) {
        if (clientKey == null || clientKey.isEmpty()) clientKey = "127.0.0.1";
        long now = System.currentTimeMillis();

        // 1. Kiểm tra tốc độ gói tin tổng thể (Chống UDP Packet Flood / DoS)
        SlidingWindowBucket pktBucket = packetRateBuckets.computeIfAbsent(clientKey, k -> new SlidingWindowBucket());
        if (pktBucket.bannedUntilMs > now) {
            long remainSec = (pktBucket.bannedUntilMs - now + 999) / 1000;
            return String.format("CHẶN SPAM / DoS FLOOD (IDS): Phát hiện gửi quá %d gói tin/10s! Tạm khóa truy cập trong %d giây.",
                    MAX_PACKETS_PER_10_SEC, remainSec);
        }
        if (now - pktBucket.windowStartMs > 10_000L) {
            pktBucket.windowStartMs = now;
            pktBucket.count = 0;
        }
        pktBucket.count++;
        if (pktBucket.count > MAX_PACKETS_PER_10_SEC) {
            pktBucket.bannedUntilMs = now + SPAM_BAN_DURATION_MS;
            return String.format("CHẶN SPAM / DoS FLOOD (IDS): Vượt ngưỡng %d gói tin RUDP trong 10 giây! Đã kích hoạt khóa bảo vệ 30 giây.",
                    MAX_PACKETS_PER_10_SEC);
        }

        // 2. Kiểm tra tốc độ các lệnh Ghi/Sửa/Xóa/Đăng ký (Chống Spam thao tác CSDL)
        if (isMutatingCommand) {
            SlidingWindowBucket mutBucket = mutatingRateBuckets.computeIfAbsent(clientKey, k -> new SlidingWindowBucket());
            if (mutBucket.bannedUntilMs > now) {
                long remainSec = (mutBucket.bannedUntilMs - now + 999) / 1000;
                return String.format("CHẶN SPAM THAO TÁC CSDL: Bạn thao tác Ghi/Sửa quá nhanh (>%d lệnh/10s)! Vui lòng chờ %d giây.",
                        MAX_MUTATIONS_PER_10_SEC, remainSec);
            }
            if (now - mutBucket.windowStartMs > 10_000L) {
                mutBucket.windowStartMs = now;
                mutBucket.count = 0;
            }
            mutBucket.count++;
            if (mutBucket.count > MAX_MUTATIONS_PER_10_SEC) {
                mutBucket.bannedUntilMs = now + SPAM_BAN_DURATION_MS;
                return String.format("CHẶN SPAM THAO TÁC CSDL: Phát hiện %d lệnh Ghi/Sửa liên tiếp trong 10 giây! Tạm khóa thao tác ghi trong 30 giây.",
                        mutBucket.count);
            }
        }
        return null;
    }

    /**
     * Kiểm tra thời gian chờ (Cooldown) giữa 2 lần thực hiện liên tiếp của một hành động nhạy cảm
     * (VD: Đăng ký tài khoản mới, Gửi đơn phúc khảo, Đăng ký tín chỉ, Phát thông báo toàn trường).
     * @return null nếu hợp lệ, hoặc thông báo lỗi nếu đang trong thời gian Cooldown.
     */
    public synchronized String checkActionCooldown(String actorKey, String actionName, long cooldownMs) {
        if (actorKey == null || actorKey.isEmpty()) actorKey = "anonymous";
        String compositeKey = actorKey.toLowerCase() + ":" + actionName;
        long now = System.currentTimeMillis();
        Long allowedAt = actionCooldowns.get(compositeKey);
        if (allowedAt != null && allowedAt > now) {
            long remainSec = Math.max(1, (allowedAt - now + 999) / 1000);
            return String.format("CHỐNG SPAM (%s): Thao tác quá nhanh! Vui lòng chờ %d giây trước khi thực hiện lại.",
                    actionName, remainSec);
        }
        actionCooldowns.put(compositeKey, now + cooldownMs);
        return null;
    }

    public synchronized void clearSpamState(String clientKey) {
        if (clientKey != null) {
            packetRateBuckets.remove(clientKey);
            mutatingRateBuckets.remove(clientKey);
        }
        actionCooldowns.clear();
    }
}
