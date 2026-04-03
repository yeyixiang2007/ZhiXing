package com.zhixing.navigation.domain.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final String PBKDF2_PREFIX = "pbkdf2$";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        String password = requirePassword(rawPassword);
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BYTES);
        return PBKDF2_PREFIX + PBKDF2_ITERATIONS + "$" + toHex(salt) + "$" + toHex(derived);
    }

    public static boolean matches(String rawPassword, String encodedHash) {
        if (!isEncodedHash(encodedHash)) {
            return false;
        }
        String candidate = requirePassword(rawPassword);
        return verifyPbkdf2(candidate, encodedHash.trim());
    }

    public static boolean isEncodedHash(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith(PBKDF2_PREFIX)) {
            return false;
        }
        String[] parts = trimmed.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        if (!"pbkdf2".equals(parts[0])) {
            return false;
        }
        if (!parts[1].matches("[0-9]{4,7}")) {
            return false;
        }
        if (!parts[2].matches("[0-9a-f]+") || !parts[3].matches("[0-9a-f]+")) {
            return false;
        }
        return parts[2].length() >= SALT_BYTES * 2 && parts[3].length() >= KEY_BYTES * 2;
    }

    private static String requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return rawPassword;
    }

    private static boolean verifyPbkdf2(String rawPassword, String encodedHash) {
        String[] parts = encodedHash.split("\\$");
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return false;
        }
        byte[] salt = fromHex(parts[2]);
        byte[] expected = fromHex(parts[3]);
        byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations, expected.length);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBytes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBytes * 8);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return keyFactory.generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("PBKDF2 hashing failed", ex);
        }
    }

    private static String toHex(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte value : data) {
            builder.append(String.format(Locale.ROOT, "%02x", value));
        }
        return builder.toString();
    }

    private static byte[] fromHex(String hex) {
        if (hex == null || (hex.length() % 2) != 0) {
            throw new IllegalArgumentException("invalid hex");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("invalid hex");
            }
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }
}
