package com.qlsv.util;

import java.util.regex.Pattern;

import com.qlsv.model.StudentData;

/**
 * Bộ Kiểm tra & Chuẩn hóa Dữ liệu Đầu vào Tập trung (Client & Server-side Validation).
 * Ngăn chặn lỗi trùng khóa phân biệt hoa-thường, lỗi nhập dấu phẩy thập phân ('8,5'),
 * lỗi giá trị NaN/Infinity và dữ liệu rác.
 */
public class InputValidator {

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^[A-Z0-9_-]{3,25}$");
    private static final Pattern HAS_LETTER_PATTERN = Pattern.compile(".*\\p{L}+.*");
    private static final Pattern INVALID_NAME_CHARS = Pattern.compile(".*[0-9<>\"';\\\\/|@#$%^&*=+~`]+.*");

    public static String normalizeAndValidateStudentId(String rawId) throws IllegalArgumentException {
        if (rawId == null || rawId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sinh viên không được để trống!");
        }
        String normalized = rawId.trim().toUpperCase();
        if (!STUDENT_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Mã sinh viên '" + rawId + "' không hợp lệ! (Chỉ chấp nhận chữ cái, chữ số, dài 3-25 ký tự, VD: SV001 hoặc B21DCCN001)");
        }
        return normalized;
    }

    public static String normalizeAndValidateFullName(String rawName) throws IllegalArgumentException {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên sinh viên không được để trống!");
        }
        String normalized = rawName.trim().replaceAll("\\s+", " ");
        if (normalized.length() < 2 || normalized.length() > 100) {
            throw new IllegalArgumentException("Họ và tên sinh viên phải có độ dài từ 2 đến 100 ký tự!");
        }
        if (!HAS_LETTER_PATTERN.matcher(normalized).matches() || INVALID_NAME_CHARS.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Họ và tên sinh viên '" + rawName + "' không hợp lệ (Không được chứa chữ số hoặc ký tự đặc biệt)!");
        }
        return normalized;
    }

    public static double parseAndValidateScore(String rawScore, String fieldLabel) throws IllegalArgumentException {
        if (rawScore == null || rawScore.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " không được để trống!");
        }
        // Hỗ trợ bàn phím tiếng Việt gõ dấu phẩy thập phân (VD: "8,5" -> "8.5")
        String cleaned = rawScore.trim().replace(',', '.');
        double val;
        try {
            val = Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldLabel + " ('" + rawScore + "') phải là số thực hợp lệ (VD: 8.5 hoặc 8,5)!");
        }
        return validateScoreValue(val, fieldLabel);
    }

    public static double validateScoreValue(double val, String fieldLabel) throws IllegalArgumentException {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            throw new IllegalArgumentException(fieldLabel + " không hợp lệ (NaN/Infinite)!");
        }
        if (val < 0.0 || val > 10.0) {
            throw new IllegalArgumentException(fieldLabel + " (" + val + ") phải nằm trong khoảng từ 0.0 đến 10.0!");
        }
        return Math.round(val * 100.0) / 100.0;
    }

    public static StudentData validateAndNormalizeStudentData(StudentData data) throws IllegalArgumentException {
        if (data == null) {
            throw new IllegalArgumentException("Dữ liệu sinh viên rỗng!");
        }
        String cleanId = normalizeAndValidateStudentId(data.getStudentId());
        String cleanName = normalizeAndValidateFullName(data.getFullName());
        double cleanCC = validateScoreValue(data.getScoreMath(), "Điểm Chuyên cần (10%)");
        double cleanGK = validateScoreValue(data.getScoreLiterature(), "Điểm Giữa kỳ (20%)");
        double cleanCK = validateScoreValue(data.getScoreEnglish(), "Điểm Thi kết thúc (70%)");

        data.setStudentId(cleanId);
        data.setFullName(cleanName);
        data.setScoreMath(cleanCC);
        data.setScoreLiterature(cleanGK);
        data.setScoreEnglish(cleanCK);
        return data;
    }
}

