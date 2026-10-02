package com.qlsv.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

public class AESEncryption {

    public static final String DEFAULT_KEY = "QLSV_AES_256_SECRET_KEY";
    private static final String SALT = "QLSV_AES_SALT_12345";
    private static final byte[] IV = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

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
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivspec = new IvParameterSpec(IV);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);

        byte[] encrypted = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String strToDecrypt, String passkey) throws Exception {
        if (strToDecrypt == null || strToDecrypt.isEmpty()) return strToDecrypt;
        String key = (passkey != null && !passkey.trim().isEmpty()) ? passkey.trim() : DEFAULT_KEY;

        SecretKeySpec secretKey = deriveKey(key);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivspec = new IvParameterSpec(IV);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);

        byte[] decoded = Base64.getDecoder().decode(strToDecrypt);
        byte[] original = cipher.doFinal(decoded);
        return new String(original, StandardCharsets.UTF_8);
    }
}
