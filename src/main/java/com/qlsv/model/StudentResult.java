package com.qlsv.model;

import java.io.Serializable;

public class StudentResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String fullName;
    private String className = "D21CQCN01-N";
    private String dateOfBirth = "2003-01-01";
    private String gender = "Nam";
    private String email = "";
    private String academicRank = "Chưa xếp loại";
    private double averageScore;
    private double scoreMath;
    private double scoreLiterature;
    private double scoreEnglish;

    public StudentResult() {
    }

    public StudentResult(String studentId, String fullName, double averageScore) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.averageScore = averageScore;
        this.academicRank = calculateRank(averageScore);
    }

    public StudentResult(String studentId, String fullName, String className, String dateOfBirth, String gender, 
                         String email, double averageScore, String academicRank) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.className = className;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.email = email;
        this.averageScore = averageScore;
        this.academicRank = (academicRank != null && !academicRank.isEmpty()) ? academicRank : calculateRank(averageScore);
    }

    public static String calculateRank(double gpa) {
        if (gpa >= 9.0) return "Xuất sắc";
        if (gpa >= 8.0) return "Giỏi";
        if (gpa >= 6.5) return "Khá";
        if (gpa >= 5.0) return "Trung bình";
        return "Yếu";
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAcademicRank() {
        return academicRank;
    }

    public void setAcademicRank(String academicRank) {
        this.academicRank = academicRank;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public double getScoreMath() {
        return scoreMath;
    }

    public void setScoreMath(double scoreMath) {
        this.scoreMath = scoreMath;
    }

    public double getScoreLiterature() {
        return scoreLiterature;
    }

    public void setScoreLiterature(double scoreLiterature) {
        this.scoreLiterature = scoreLiterature;
    }

    public double getScoreEnglish() {
        return scoreEnglish;
    }

    public void setScoreEnglish(double scoreEnglish) {
        this.scoreEnglish = scoreEnglish;
    }

    @Override
    public String toString() {
        return "StudentResult{" +
                "studentId='" + studentId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", className='" + className + '\'' +
                ", averageScore=" + averageScore +
                ", academicRank='" + academicRank + '\'' +
                '}';
    }
}
