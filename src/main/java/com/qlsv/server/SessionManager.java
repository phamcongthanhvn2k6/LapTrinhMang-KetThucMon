package com.qlsv.server;

import com.qlsv.model.UserAccount;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static SessionManager instance;
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_MS = 4 * 60 * 60 * 1000L; // 4 hours

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

    public synchronized String createSession(UserAccount account) {
        String token = UUID.randomUUID().toString();
        UserSession session = new UserSession(token, account.getUsername(), account.getRole(), account.getStudentId(), account.getFullName());
        activeSessions.put(token, session);
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
