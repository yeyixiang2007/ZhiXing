package com.zhixing.navigation.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void adminShouldHaveManagePermission() {
        User admin = new Admin("admin", "hashed-password");
        Assertions.assertTrue(admin.canManageMap());
        Assertions.assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test
    void normalUserShouldNotHaveManagePermission() {
        User user = new NormalUser("guest", "hashed-password");
        Assertions.assertFalse(user.canManageMap());
        Assertions.assertEquals(Role.USER, user.getRole());
    }

    @Test
    void shouldRejectBlankUsername() {
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Admin("  ", "hashed-password")
        );
        Assertions.assertTrue(ex.getMessage().contains("username"));
    }
}

