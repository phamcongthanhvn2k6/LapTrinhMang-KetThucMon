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

    private static final long SESSION_TIMEOUT_MS = 4 * 60 * 60 * 1000L; // 4 hours
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 60 * 1000L; // 60 seconds lockout

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
}
