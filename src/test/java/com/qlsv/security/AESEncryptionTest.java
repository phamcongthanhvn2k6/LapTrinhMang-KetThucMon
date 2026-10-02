package com.qlsv.security;

public class AESEncryptionTest {
    public static void main(String[] args) {
        System.out.println("========== KIỂM THỬ THUẬT TOÁN MÃ HÓA BẢO MẬT AES-256 ==========");
        try {
            String original = "Nguyễn Văn An - Điểm TB 9.5";
            String key = "THIEN_MA_AES_KEY";

            String encrypted = AESEncryption.encrypt(original, key);
            System.out.println("1. Chuỗi ban đầu  : " + original);
            System.out.println("2. Mã hóa AES-256 : " + encrypted);

            String decrypted = AESEncryption.decrypt(encrypted, key);
            System.out.println("3. Giải mã AES-256: " + decrypted);

            if (original.equals(decrypted)) {
                System.out.println("===> KẾT QUẢ MÃ HÓA & GIẢI MÃ AES-256: THÀNH CÔNG (MATCH!)");
            } else {
                throw new Exception("Lỗi: Mã hóa/Giải mã AES-256 không trùng khớp!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
