package com.zhixing.navigation.domain.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void shouldHashAndVerifyPassword() {
        String encoded = PasswordHasher.hash("admin123");

        Assertions.assertTrue(PasswordHasher.isEncodedHash(encoded));
        Assertions.assertTrue(PasswordHasher.matches("admin123", encoded));
        Assertions.assertFalse(PasswordHasher.matches("wrong", encoded));
    }

    @Test
    void shouldRejectLegacySha256Hash() {
        String legacy = "sha256$240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
        Assertions.assertFalse(PasswordHasher.isEncodedHash(legacy));
        Assertions.assertFalse(PasswordHasher.matches("admin123", legacy));
    }
}
