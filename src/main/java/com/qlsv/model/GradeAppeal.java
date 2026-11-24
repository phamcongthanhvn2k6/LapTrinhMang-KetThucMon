package com.qlsv.model;

import java.io.Serializable;

/**
 * Mô hình Đơn Phúc Khảo Điểm Học Phần Hai Chiều (Student <-> Teacher/Academic Board).
 */
public class GradeAppeal implements Serializable {
    private static final long serialVersionUID = 1L;

    private int appealId;
    private String studentId;
    private String courseCode;
    private String reason;
    private String status;      // "CHỜ DUYỆT", "ĐÃ DUYỆT NÂNG ĐIỂM", "TỪ CHỐI"
    private String teacherNote; // Phản hồi của Giảng viên / Hội đồng chấm phúc khảo
    private String timestamp;

    public GradeAppeal() {
    }

    public GradeAppeal(int appealId, String studentId, String courseCode, String reason,
                       String status, String teacherNote, String timestamp) {
        this.appealId = appealId;
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.reason = reason;
        this.status = status;
        this.teacherNote = teacherNote;
        this.timestamp = timestamp;
    }

    public int getAppealId() {
        return appealId;
    }

    public void setAppealId(int appealId) {
        this.appealId = appealId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTeacherNote() {
        return teacherNote;
    }

    public void setTeacherNote(String teacherNote) {
        this.teacherNote = teacherNote;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
