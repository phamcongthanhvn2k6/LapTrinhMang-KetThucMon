package com.qlsv.model;

import java.io.Serializable;

/**
 * Kết quả Học tập & Xét Học vụ Sinh viên theo Quy chế Đào tạo Tín chỉ Đại học (PTIT / Bộ GD&ĐT).
 * Trọng số: Chuyên cần (10%) + Giữa kỳ (20%) + Thi kết thúc học phần (70%).
 * Áp dụng quy tắc:
 * - Cấm thi khi Điểm Chuyên Cần < 5.0
 * - Điểm liệt bài thi kết thúc khi Điểm Thi < 4.0
 * - Cảnh báo học vụ 3 mức & Xét Học bổng Khuyến khích Học tập (KKHT).
 */
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
    private double scoreMath;       // Chuyên cần (10%)
    private double scoreLiterature; // Giữa kỳ (20%)
    private double scoreEnglish;    // Thi kết thúc (70%)

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

    /**
     * Tính điểm tổng kết học phần theo trọng số tín chỉ chuẩn PTIT: 10% CC + 20% GK + 70% CK.
     */
    public static double calculateWeightedScore(double cc, double gk, double ck) {
        double raw = cc * 0.10 + gk * 0.20 + ck * 0.70;
        return Math.round(raw * 100.0) / 100.0;
    }

    public static String calculateRank(double gpa) {
        if (gpa >= 9.0) return "Xuất sắc";
        if (gpa >= 8.0) return "Giỏi";
        if (gpa >= 6.5) return "Khá";
        if (gpa >= 5.0) return "Trung bình";
        return "Yếu";
    }

    /**
     * Kiểm tra sinh viên có bị Cấm thi do điểm chuyên cần < 5.0 hay không.
     */
    public boolean isExamBanned() {
        return scoreMath > 0.0 && scoreMath < 5.0;
    }

    /**
     * Kiểm tra sinh viên có bị điểm liệt bài thi cuối kỳ (< 4.0) hay không.
     */
    public boolean isFinalExamFailed() {
        return scoreEnglish > 0.0 && scoreEnglish < 4.0;
    }

    /**
     * Kiểm tra điều kiện xét Học bổng Khuyến khích Học tập (KKHT) chuẩn Đại học:
     * GPA hệ 4 >= 3.2, Chuyên cần >= 8.0, Giữa kỳ >= 7.0, Cuối kỳ >= 7.0, không bị cấm thi.
     */
    public boolean isScholarshipEligible() {
        if (isExamBanned() || isFinalExamFailed()) return false;
        return getGpa4() >= 3.2 && scoreMath >= 8.0 && scoreLiterature >= 7.0 && scoreEnglish >= 7.0;
    }

    public String getScholarshipTier() {
        if (!isScholarshipEligible()) return "Không đạt";
        if (getGpa4() >= 3.6 && averageScore >= 9.0) {
            return "Học bổng Xuất sắc (120% học phí)";
        }
        return "Học bổng Giỏi (100% học phí)";
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

    public String getId() {
        return studentId;
    }

    public String getName() {
        return fullName;
    }

    public double getScoreCC() {
        return scoreMath;
    }

    public void setScoreCC(double scoreCC) {
        this.scoreMath = scoreCC;
    }

    public double getScoreGK() {
        return scoreLiterature;
    }

    public void setScoreGK(double scoreGK) {
        this.scoreLiterature = scoreGK;
    }

    public double getScoreCK() {
        return scoreEnglish;
    }

    public void setScoreCK(double scoreCK) {
        this.scoreEnglish = scoreCK;
    }

    public double getGpa4() {
        if (isExamBanned() || isFinalExamFailed()) {
            return 0.0;
        }
        double s = averageScore;
        if (s >= 8.5) return 4.0;
        if (s >= 8.0) return 3.5;
        if (s >= 7.0) return 3.0;
        if (s >= 6.5) return 2.5;
        if (s >= 5.5) return 2.0;
        if (s >= 5.0) return 1.5;
        if (s >= 4.0) return 1.0;
        return 0.0;
    }

    public String getLetterGrade() {
        if (isExamBanned() || isFinalExamFailed()) {
            return "F";
        }
        double s = averageScore;
        if (s >= 9.0) return "A+";
        if (s >= 8.5) return "A";
        if (s >= 8.0) return "B+";
        if (s >= 7.0) return "B";
        if (s >= 6.5) return "C+";
        if (s >= 5.5) return "C";
        if (s >= 5.0) return "D+";
        if (s >= 4.0) return "D";
        return "F";
    }

    public String getAcademicRankCredit() {
        double g = getGpa4();
        if (g >= 3.6) return "Xuất sắc";
        if (g >= 3.2) return "Giỏi";
        if (g >= 2.5) return "Khá";
        if (g >= 2.0) return "Trung bình";
        if (g >= 1.0) return "Yếu";
        return "Kém";
    }

    public String getAcademicStatus() {
        if (isExamBanned()) {
            return "Cấm thi (CC < 5.0)";
        }
        if (isFinalExamFailed()) {
            return "Học lại (Thi < 4.0)";
        }
        double gpa4 = getGpa4();
        if (averageScore < 4.0 || gpa4 < 1.2) {
            return "Cảnh báo Mức 2";
        }
        if (gpa4 < 2.0) {
            return "Cảnh báo Mức 1";
        }
        if (isScholarshipEligible()) {
            return "Đạt Học Bổng KKHT";
        }
        return "Đang học (Đạt)";
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
