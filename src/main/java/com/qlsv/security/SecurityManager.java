package com.qlsv.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Trung tâm Quản lý Bảo mật Đa lớp (Dual-Cipher Engine: DES & AES-256-GCM + HMAC-SHA256 Packet Signing).
 */
public class SecurityManager {

    public enum EncryptionAlgo {
        DES("DES (56-bit Chuẩn Đề Bài)"),
        AES_256("AES-256-GCM (Chuẩn Đại Học Hiện Đại)");

        private final String displayName;

        EncryptionAlgo(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static EncryptionAlgo currentAlgo = EncryptionAlgo.DES;
    private static final String PACKET_HMAC_SECRET = "PTIT_RUDP_HMAC_SHA256_MASTER_SECRET_2025";

    public static EncryptionAlgo getCurrentAlgo() {
        return currentAlgo;
    }

    public static void setCurrentAlgo(EncryptionAlgo algo) {
        if (algo != null) {
            currentAlgo = algo;
        }
    }

    public static String encrypt(String data, String passkey) throws Exception {
        if (data == null) return null;
        if (currentAlgo == EncryptionAlgo.AES_256) {
            return AESEncryption.encrypt(data, passkey);
        } else {
            return DESEncryption.encrypt(data, passkey);
        }
    }

    public static String decrypt(String ciphertext, String passkey) throws Exception {
        if (ciphertext == null) return null;
        if (ciphertext.startsWith("GCM:")) {
            return AESEncryption.decrypt(ciphertext, passkey);
        }
        try {
            if (currentAlgo == EncryptionAlgo.AES_256) {
                return AESEncryption.decrypt(ciphertext, passkey);
            } else {
                return DESEncryption.decrypt(ciphertext, passkey);
            }
        } catch (Exception e) {
            // Automatic fallback between DES and AES so existing records never fail to decrypt
            try {
                return DESEncryption.decrypt(ciphertext, passkey);
            } catch (Exception ex) {
                return AESEncryption.decrypt(ciphertext, passkey);
            }
        }
    }

    /**
     * Ký số gói tin RUDP bằng thuật toán HMAC-SHA256 để chống sửa đổi dữ liệu trên đường truyền (Anti-Tampering).
     */
    public static String computeHmacSha256(String data) {
        return computeHmacSha256(data, PACKET_HMAC_SECRET);
    }

    public static String computeHmacSha256(String data, String secret) {
        if (data == null) return "";
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    (secret != null ? secret : PACKET_HMAC_SECRET).getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256Hmac.init(secretKey);
            byte[] macBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(macBytes);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Xác thực chữ ký số HMAC-SHA256 theo thời gian hằng số (Constant-time comparison) chống Timing Attack.
     */
    public static boolean verifyHmacSha256(String data, String signature) {
        if (signature == null || signature.isEmpty()) return false;
        String computed = computeHmacSha256(data);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
