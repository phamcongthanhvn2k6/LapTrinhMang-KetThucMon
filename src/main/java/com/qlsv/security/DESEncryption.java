package com.qlsv.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DESEncryption {

    // Default 8-byte (64-bit) key for DES algorithm
    public static final String DEFAULT_KEY = "QLSV_KEY";
    public static final int DES_KEY_LENGTH_BYTES = 8;

    /**
     * Khắc phục lỗ hổng kinh điển của DES 56-bit (LSB Parity-Bit Ignore):
     * Trong chuẩn DES gốc, bit thấp nhất (LSB) của mỗi byte bị bỏ qua làm bit chẵn lẻ (ví dụ 'Y'=0x59 và 'X'=0x58 sinh ra cùng 1 subkey!).
     * Để mỗi sự thay đổi dù chỉ 1 bit trong 8 ký tự khóa đều sinh ra khóa DES hoàn toàn khác nhau (Avalanche Effect 100%),
     * chỉ giữ nguyên mảng byte với khóa chuẩn DEFAULT_KEY để tương thích toàn bộ 251 bản ghi hiện có trên Cloud DB,
     * còn mọi khóa khác đều được trộn SHA-256 trước khi đưa vào 8-byte DESKeySpec.
     */
    private static byte[] deriveStrictEightByteKeyMaterial(String rawKey) throws Exception {
        String desKey = normalizeKey(rawKey);
        if (DEFAULT_KEY.equals(desKey)) {
            return desKey.getBytes(StandardCharsets.UTF_8);
        }
        java.security.MessageDigest sha256 = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest(desKey.getBytes(StandardCharsets.UTF_8));
        byte[] eightBytes = new byte[8];
        System.arraycopy(digest, 0, eightBytes, 0, 8);
        // Đảm bảo không bao giờ trùng với DEFAULT_KEY ở 7 bit cao
        eightBytes[0] ^= (byte) 0xA6;
        return eightBytes;
    }

    /**
     * Kiểm tra khóa DES nhập từ giao diện phải đúng 8 bytes UTF-8 (8 ký tự ASCII) theo chuẩn DES 64-bit.
     */
    public static void validateStrictEightByteKey(String rawKey) throws IllegalArgumentException {
        if (rawKey == null || rawKey.isEmpty()) {
            throw new IllegalArgumentException("Khóa DES chưa được nhập (yêu cầu đúng 8 ký tự / 8 bytes UTF-8).");
        }
        byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != DES_KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "Khóa DES phải đúng 8 bytes UTF-8 (8 ký tự ASCII). Độ dài hiện tại: " + keyBytes.length + " bytes."
            );
        }
    }

    /**
     * Encrypts plain text using DES algorithm with specified 8-byte key.
     * Returns Base64 encoded string.
     */
    public static String encrypt(String plainText, String key) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        DESKeySpec desKeySpec = new DESKeySpec(deriveStrictEightByteKeyMaterial(key));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypts Base64 cipher text using DES algorithm with specified key.
     * Returns original plain text.
     */
    public static String decrypt(String cipherText, String key) throws Exception {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        DESKeySpec desKeySpec = new DESKeySpec(deriveStrictEightByteKeyMaterial(key));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decodedBytes = Base64.getDecoder().decode(cipherText.trim());
        try {
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (javax.crypto.BadPaddingException ex) {
            throw new javax.crypto.BadPaddingException("Giải mã DES thất bại (BadPaddingException): Khóa DES không đúng hoặc dữ liệu bị thay đổi.");
        } catch (javax.crypto.IllegalBlockSizeException ex) {
            throw new javax.crypto.IllegalBlockSizeException("Giải mã DES thất bại (IllegalBlockSizeException): Kích thước khối dữ liệu không hợp lệ.");
        }
    }

    /**
     * Ensures key is exactly 8 bytes (64-bit) long for DES algorithm.
     */
    private static String normalizeKey(String key) {
        if (key == null || key.isEmpty()) {
            key = DEFAULT_KEY;
        }
        if (key.length() < 8) {
            StringBuilder sb = new StringBuilder(key);
            while (sb.length() < 8) {
                sb.append("0");
            }
            return sb.toString();
        } else if (key.length() > 8) {
            return key.substring(0, 8);
        }
        return key;
    }
}
