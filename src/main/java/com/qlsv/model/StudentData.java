package com.qlsv.model;

import java.io.Serializable;

public class StudentData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String fullName;
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
                ", scoreMath=" + scoreMath +
                ", scoreLiterature=" + scoreLiterature +
                ", scoreEnglish=" + scoreEnglish +
                '}';
    }
}
