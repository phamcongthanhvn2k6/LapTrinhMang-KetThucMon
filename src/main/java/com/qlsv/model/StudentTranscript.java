package com.qlsv.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Hồ sơ Bảng điểm Tín chỉ Sinh viên.
 * Tách biệt rõ:
 * - Tín chỉ đăng ký (registeredCredits): Tổng số tín chỉ các môn đã học.
 * - Tín chỉ tích lũy (totalCredits / accumulatedCredits): Chỉ tính các học phần ĐẠT (Điểm chữ từ D trở lên, không bị F).
 */
public class StudentTranscript implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String fullName;
    private String className = "D21CQCN01-N";
    private String faculty = "Công nghệ Thông tin 1";
    private String academicYear = "2021-2026";
    private List<CourseGrade> grades = new ArrayList<>();
    private double cumulativeGpa10;
    private double cumulativeGpa4;
    private int totalCredits;      // Tín chỉ tích lũy đạt (Passed courses only)
    private int registeredCredits; // Tổng tín chỉ đăng ký
    private String academicRank;

    public StudentTranscript() {
    }

    public StudentTranscript(String studentId, String fullName, String className) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.className = className;
    }

    public void addCourseGrade(CourseGrade grade) {
        grades.add(grade);
        recalculateGpa();
    }

    public void recalculateGpa() {
        if (grades.isEmpty()) {
            cumulativeGpa10 = 0.0;
            cumulativeGpa4 = 0.0;
            totalCredits = 0;
            registeredCredits = 0;
            academicRank = "Chưa có điểm";
            return;
        }

        double sumWeighted10 = 0.0;
        double sumWeighted4 = 0.0;
        int regCredits = 0;
        int accCredits = 0;

        for (CourseGrade g : grades) {
            int c = g.getCredits();
            sumWeighted10 += g.getTotalScore() * c;
            sumWeighted4 += g.getGpa4() * c;
            regCredits += c;
            if (g.isPassed()) {
                accCredits += c;
            }
        }

        this.registeredCredits = regCredits;
        this.totalCredits = accCredits;
        this.cumulativeGpa10 = (regCredits > 0) ? Math.round((sumWeighted10 / regCredits) * 100.0) / 100.0 : 0.0;
        this.cumulativeGpa4 = (regCredits > 0) ? Math.round((sumWeighted4 / regCredits) * 100.0) / 100.0 : 0.0;

        if (cumulativeGpa4 >= 3.6) {
            academicRank = "Xuất sắc";
        } else if (cumulativeGpa4 >= 3.2) {
            academicRank = "Giỏi";
        } else if (cumulativeGpa4 >= 2.5) {
            academicRank = "Khá";
        } else if (cumulativeGpa4 >= 2.0) {
            academicRank = "Trung bình";
        } else if (cumulativeGpa4 >= 1.0) {
            academicRank = "Yếu";
        } else {
            academicRank = "Kém";
        }
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

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public List<CourseGrade> getGrades() {
        return grades;
    }

    public void setGrades(List<CourseGrade> grades) {
        this.grades = grades;
        recalculateGpa();
    }

    public double getCumulativeGpa10() {
        return cumulativeGpa10;
    }

    public double getCumulativeGpa4() {
        return cumulativeGpa4;
    }

    public int getTotalCredits() {
        return totalCredits;
    }

    public int getRegisteredCredits() {
        return registeredCredits;
    }

    public String getAcademicRank() {
        return academicRank;
    }
}
