package com.qlsv.model;

import java.io.Serializable;

public class StudentResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String fullName;
    private double averageScore;

    public StudentResult() {
    }

    public StudentResult(String studentId, String fullName, double averageScore) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.averageScore = averageScore;
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

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    @Override
    public String toString() {
        return "StudentResult{" +
                "studentId='" + studentId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", averageScore=" + averageScore +
                '}';
    }
}
