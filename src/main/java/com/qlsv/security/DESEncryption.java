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
        String desKey = normalizeKey(key);
        DESKeySpec desKeySpec = new DESKeySpec(desKey.getBytes(StandardCharsets.UTF_8));
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
        String desKey = normalizeKey(key);
        DESKeySpec desKeySpec = new DESKeySpec(desKey.getBytes(StandardCharsets.UTF_8));
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
