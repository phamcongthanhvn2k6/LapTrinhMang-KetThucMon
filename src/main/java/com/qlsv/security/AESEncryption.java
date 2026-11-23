package com.qlsv.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Chuẩn mã hóa hiện đại AES-256-GCM (Authenticated Encryption with Associated Data)
 * kèm tương thích ngược AES-256-CBC.
 */
public class AESEncryption {

    public static final String DEFAULT_KEY = "QLSV_AES_256_SECRET_KEY";
    private static final String SALT = "QLSV_AES_SALT_12345";
    private static final String GCM_PREFIX = "GCM:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final byte[] LEGACY_IV = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    private static SecretKeySpec deriveKey(String passkey) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(passkey.toCharArray(), SALT.getBytes(StandardCharsets.UTF_8), 65536, 256);
        SecretKey secretKey = factory.generateSecret(spec);
        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }

    public static String encrypt(String strToEncrypt, String passkey) throws Exception {
        if (strToEncrypt == null || strToEncrypt.isEmpty()) return strToEncrypt;
        String key = (passkey != null && !passkey.trim().isEmpty()) ? passkey.trim() : DEFAULT_KEY;

        SecretKeySpec secretKey = deriveKey(key);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return GCM_PREFIX + Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String strToDecrypt, String passkey) throws Exception {
        if (strToDecrypt == null || strToDecrypt.isEmpty()) return strToDecrypt;
        String key = (passkey != null && !passkey.trim().isEmpty()) ? passkey.trim() : DEFAULT_KEY;
        SecretKeySpec secretKey = deriveKey(key);

        if (strToDecrypt.startsWith(GCM_PREFIX)) {
            String rawBase64 = strToDecrypt.substring(GCM_PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(rawBase64);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] original = cipher.doFinal(cipherText);
            return new String(original, StandardCharsets.UTF_8);
        }

        // Legacy AES-CBC fallback
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivspec = new IvParameterSpec(LEGACY_IV);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);

        byte[] decoded = Base64.getDecoder().decode(strToDecrypt);
        byte[] original = cipher.doFinal(decoded);
        return new String(original, StandardCharsets.UTF_8);
    }
}
