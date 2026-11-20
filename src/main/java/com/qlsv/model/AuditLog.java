package com.qlsv.model;

import java.io.Serializable;
import java.util.Date;

public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String username;
    private String action; // LOGIN, ADD_STUDENT, UPDATE_STUDENT, DELETE_STUDENT, IMPORT_EXCEL, etc.
    private String details;
    private String clientIp;
    private Date timestamp;

    public AuditLog() {
        this.timestamp = new Date();
    }

    public AuditLog(String username, String action, String details, String clientIp) {
        this.username = username;
        this.action = action;
        this.details = details;
        this.clientIp = clientIp;
        this.timestamp = new Date();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
