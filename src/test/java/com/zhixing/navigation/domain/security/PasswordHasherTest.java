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
}

