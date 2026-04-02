package com.zhixing.navigation.domain.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {
    private static final String PREFIX = "sha256$";

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        String password = requirePassword(rawPassword);
        return PREFIX + sha256Hex(password);
    }

    public static boolean matches(String rawPassword, String encodedHash) {
        if (!isEncodedHash(encodedHash)) {
            return false;
        }
        String candidate = hash(rawPassword);
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                encodedHash.trim().getBytes(StandardCharsets.UTF_8)
        );
    }

    public static boolean isEncodedHash(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith(PREFIX)) {
            return false;
        }
        String hashPart = trimmed.substring(PREFIX.length());
        return hashPart.matches("[0-9a-f]{64}");
    }

    private static String requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return rawPassword;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}

