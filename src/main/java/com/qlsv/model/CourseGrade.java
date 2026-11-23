package com.qlsv.model;

import java.io.Serializable;

/**
 * Mô hình Điểm Học Phần Tín Chỉ Chuẩn Đại Học (PTIT):
 * - Trọng số: 10% Chuyên cần + 20% Giữa kỳ + 70% Thi kết thúc (hoặc 30% Quá trình + 70% Thi).
 * - Quy tắc Cấm thi: Chuyên cần < 5.0 -> Điểm chữ F (GPA = 0.0, HỌC LẠI).
 * - Quy tắc Điểm liệt: Điểm thi kết thúc < 4.0 -> Điểm chữ F (GPA = 0.0, HỌC LẠI).
 */
public class CourseGrade implements Serializable {
    private static final long serialVersionUID = 1L;

    private String courseCode;
    private String courseName;
    private int credits;
    private double attendanceScore = -1.0; // 10% (Nếu >= 0)
    private double midtermScore;           // 20% (hoặc 30% Quá trình)
    private double finalScore;             // 70% Thi kết thúc
    private double totalScore;             // Thang điểm 10
    private String letterGrade;            // A+, A, B+, B, C+, C, D+, D, F
    private double gpa4;                   // Thang điểm 4.0

    public CourseGrade() {
    }

    public CourseGrade(String courseCode, String courseName, int credits, double midtermScore, double finalScore) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.attendanceScore = midtermScore;
        this.midtermScore = midtermScore;
        this.finalScore = finalScore;
        calculateTotal();
    }

    public CourseGrade(String courseCode, String courseName, int credits,
                       double attendanceScore, double midtermScore, double finalScore) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.attendanceScore = attendanceScore;
        this.midtermScore = midtermScore;
        this.finalScore = finalScore;
        calculateTotal();
    }

    public void calculateTotal() {
        if (attendanceScore >= 0.0) {
            this.totalScore = StudentResult.calculateWeightedScore(attendanceScore, midtermScore, finalScore);
        } else {
            this.totalScore = Math.round((midtermScore * 0.30 + finalScore * 0.70) * 100.0) / 100.0;
        }

        // Quy chế tín chỉ: Cấm thi (Chuyên cần < 5.0) hoặc Điểm liệt bài thi cuối kỳ (Thi < 4.0) -> Bắt buộc điểm F
        boolean examBanned = (attendanceScore >= 0.0 && attendanceScore < 5.0);
        boolean finalFailed = (finalScore < 4.0);

        if (examBanned || finalFailed || totalScore < 4.0) {
            letterGrade = "F";
            gpa4 = 0.0;
        } else if (totalScore >= 9.0) {
            letterGrade = "A+";
            gpa4 = 4.0;
        } else if (totalScore >= 8.5) {
            letterGrade = "A";
            gpa4 = 3.7;
        } else if (totalScore >= 8.0) {
            letterGrade = "B+";
            gpa4 = 3.5;
        } else if (totalScore >= 7.0) {
            letterGrade = "B";
            gpa4 = 3.0;
        } else if (totalScore >= 6.5) {
            letterGrade = "C+";
            gpa4 = 2.5;
        } else if (totalScore >= 5.5) {
            letterGrade = "C";
            gpa4 = 2.0;
        } else if (totalScore >= 5.0) {
            letterGrade = "D+";
            gpa4 = 1.5;
        } else {
            letterGrade = "D";
            gpa4 = 1.0;
        }
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public double getAttendanceScore() {
        return attendanceScore >= 0.0 ? attendanceScore : midtermScore;
    }

    public void setAttendanceScore(double attendanceScore) {
        this.attendanceScore = attendanceScore;
        calculateTotal();
    }

    public double getMidtermScore() {
        return midtermScore;
    }

    public void setMidtermScore(double midtermScore) {
        this.midtermScore = midtermScore;
        calculateTotal();
    }

    public double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
        calculateTotal();
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public void setLetterGrade(String letterGrade) {
        this.letterGrade = letterGrade;
    }

    public double getGpa4() {
        return gpa4;
    }

    public void setGpa4(double gpa4) {
        this.gpa4 = gpa4;
    }

    public double getScoreProcess() {
        return midtermScore;
    }

    public double getScoreExam() {
        return finalScore;
    }

    public boolean isPassed() {
        return !"F".equalsIgnoreCase(letterGrade) && gpa4 >= 1.0 && totalScore >= 4.0;
    }
}
