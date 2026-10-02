package com.qlsv.model;

import java.io.Serializable;

public class StudentData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String fullName;
    private String className = "D21CQCN01-N";
    private String dateOfBirth = "2003-01-01";
    private String gender = "Nam";
    private String email = "";
    private String phoneNumber = "";
    private String academicRank = "Chưa xếp loại";
    private String status = "Đang học";

    private double scoreMath;
    private double scoreLiterature;
    private double scoreEnglish;

    public StudentData() {
    }

    public StudentData(String studentId, String fullName, double scoreMath, double scoreLiterature, double scoreEnglish) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.scoreMath = scoreMath;
        this.scoreLiterature = scoreLiterature;
        this.scoreEnglish = scoreEnglish;
        this.email = studentId.toLowerCase() + "@ptit.edu.vn";
    }

    public StudentData(String studentId, String fullName, String className, String dateOfBirth, String gender, 
                       String email, String phoneNumber, double scoreMath, double scoreLiterature, double scoreEnglish) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.className = className;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.scoreMath = scoreMath;
        this.scoreLiterature = scoreLiterature;
        this.scoreEnglish = scoreEnglish;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAcademicRank() {
        return academicRank;
    }

    public void setAcademicRank(String academicRank) {
        this.academicRank = academicRank;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        return "StudentData{" +
                "studentId='" + studentId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", className='" + className + '\'' +
                ", academicRank='" + academicRank + '\'' +
                ", scoreMath=" + scoreMath +
                ", scoreLiterature=" + scoreLiterature +
                ", scoreEnglish=" + scoreEnglish +
                '}';
    }
}
