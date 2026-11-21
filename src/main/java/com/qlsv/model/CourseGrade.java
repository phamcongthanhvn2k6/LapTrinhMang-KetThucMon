package com.qlsv.model;

import java.io.Serializable;

public class CourseGrade implements Serializable {
    private static final long serialVersionUID = 1L;

    private String courseCode;
    private String courseName;
    private int credits;
    private double midtermScore; // 30%
    private double finalScore;   // 70%
    private double totalScore;   // 10-scale
    private String letterGrade;  // A+, A, B+...
    private double gpa4;         // 4.0 scale

    public CourseGrade() {
    }

    public CourseGrade(String courseCode, String courseName, int credits, double midtermScore, double finalScore) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.midtermScore = midtermScore;
        this.finalScore = finalScore;
        calculateTotal();
    }

    public void calculateTotal() {
        this.totalScore = Math.round((midtermScore * 0.3 + finalScore * 0.7) * 100.0) / 100.0;
        if (totalScore >= 9.0) {
            letterGrade = "A+"; gpa4 = 4.0;
        } else if (totalScore >= 8.5) {
            letterGrade = "A";  gpa4 = 3.8;
        } else if (totalScore >= 8.0) {
            letterGrade = "B+"; gpa4 = 3.5;
        } else if (totalScore >= 7.0) {
            letterGrade = "B";  gpa4 = 3.0;
        } else if (totalScore >= 6.5) {
            letterGrade = "C+"; gpa4 = 2.5;
        } else if (totalScore >= 5.5) {
            letterGrade = "C";  gpa4 = 2.0;
        } else if (totalScore >= 5.0) {
            letterGrade = "D+"; gpa4 = 1.5;
        } else if (totalScore >= 4.0) {
            letterGrade = "D";  gpa4 = 1.0;
        } else {
            letterGrade = "F";  gpa4 = 0.0;
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
        return totalScore >= 4.0;
    }
}
