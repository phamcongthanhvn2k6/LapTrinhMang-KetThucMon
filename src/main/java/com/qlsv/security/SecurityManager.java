package com.qlsv.security;

public class SecurityManager {

    public enum EncryptionAlgo {
        DES,
        AES_256
    }

    private static EncryptionAlgo currentAlgo = EncryptionAlgo.DES;

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
        try {
            if (currentAlgo == EncryptionAlgo.AES_256) {
                return AESEncryption.decrypt(ciphertext, passkey);
            } else {
                return DESEncryption.decrypt(ciphertext, passkey);
            }
        } catch (Exception e) {
            // Fallback try DES or AES if algo changed
            try {
                return DESEncryption.decrypt(ciphertext, passkey);
            } catch (Exception ex) {
                return AESEncryption.decrypt(ciphertext, passkey);
            }
        }
    }
}
