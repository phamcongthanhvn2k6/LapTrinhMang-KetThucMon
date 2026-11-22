package com.qlsv.model;

import java.io.Serializable;

public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Role {
        ADMIN("Quản trị viên"),
        GIANGVIEN("Giảng viên"),
        HOCSINH("Học sinh / Sinh viên");

        private final String displayName;
        Role(String displayName) {
            this.displayName = displayName;
        }
        public String getDisplayName() {
            return displayName;
        }
    }

    private String username;
    private String passwordHash;
    private String salt;
    private Role role;
    private String studentId; // If HOCSINH
    private String fullName;
    private String email;

    public UserAccount() {
    }

    public UserAccount(String username, String passwordHash, String salt, Role role, String studentId, String fullName, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.studentId = studentId;
        this.fullName = fullName;
        this.email = email;
    }

    public UserAccount(String username, String fullName, Role role, String studentId) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.studentId = studentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
