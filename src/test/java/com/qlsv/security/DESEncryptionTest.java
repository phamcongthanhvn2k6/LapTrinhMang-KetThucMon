package com.qlsv.security;

import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;

public class DESEncryptionTest {

    public static void main(String[] args) {
        System.out.println("========== BẮT ĐẦU KIỂM THỬ THUẬT TOÁN MÃ HÓA DES ==========");

        try {
            String originalText = "Nguyễn Văn A - 8.5";
            String key = "QLSV_KEY";

            // 1. Encrypt
            String encrypted = DESEncryption.encrypt(originalText, key);
            System.out.println("1. Chuỗi ban đầu  : " + originalText);
            System.out.println("2. Mã hóa DES     : " + encrypted);

            // 2. Decrypt
            String decrypted = DESEncryption.decrypt(encrypted, key);
            System.out.println("3. Giải mã DES    : " + decrypted);

            if (originalText.equals(decrypted)) {
                System.out.println("===> KẾT QUẢ MÃ HÓA & GIẢI MÃ DES: THÀNH CÔNG (MATCH!)");
            } else {
                System.err.println("===> KẾT QUẢ MÃ HÓA & GIẢI MÃ DES: THẤT BẠI!");
                System.exit(1);
            }

            // 3. Test Student Average Calculation
            System.out.println("\n========== BẮT ĐẦU KIỂM THỬ TÍNH ĐIỂM TRUNG BÌNH ==========");
            StudentData student = new StudentData("SV001", "Trần Thị B", 8.0, 7.5, 9.0);
            double avg = (student.getScoreMath() + student.getScoreLiterature() + student.getScoreEnglish()) / 3.0;
            avg = Math.round(avg * 100.0) / 100.0;

            StudentResult result = new StudentResult(student.getStudentId(), student.getFullName(), avg);
            System.out.println("Sinh viên: " + result.getFullName() + " (" + result.getStudentId() + ")");
            System.out.println("Điểm Toán: 8.0 | Điểm Văn: 7.5 | Điểm Anh: 9.0");
            System.out.println("Điểm Trung Bình = (8.0 + 7.5 + 9.0) / 3 = " + result.getAverageScore());

            if (Math.abs(result.getAverageScore() - 8.17) < 0.01) {
                System.out.println("===> TÍNH ĐIỂM TRUNG BÌNH: THÀNH CÔNG (8.17)");
            } else {
                System.err.println("===> TÍNH ĐIỂM TRUNG BÌNH THẤT BẠI: " + result.getAverageScore());
                System.exit(1);
            }

            System.out.println("\n========== TẤT CẢ KIỂM THỬ ĐƠN VỊ HOÀN HẢO! ==========");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
