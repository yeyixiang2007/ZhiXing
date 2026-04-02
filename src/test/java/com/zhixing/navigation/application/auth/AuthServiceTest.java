package com.zhixing.navigation.application.auth;

import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.NormalUser;
import com.zhixing.navigation.domain.model.User;
import com.zhixing.navigation.domain.security.PasswordHasher;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class AuthServiceTest {

    @Test
    void shouldLoginAdminSuccessfully() throws IOException {
        AuthService authService = createAuthServiceWithUsers();

        Admin admin = authService.loginAdmin("admin", "admin123");

        Assertions.assertEquals("admin", admin.getUsername());
    }

    @Test
    void shouldRejectWrongPassword() throws IOException {
        AuthService authService = createAuthServiceWithUsers();

        Assertions.assertThrows(AuthenticationException.class, () -> authService.loginAdmin("admin", "wrong"));
    }

    @Test
    void shouldRejectNonAdminLoginToAdminPortal() throws IOException {
        AuthService authService = createAuthServiceWithUsers();

        Assertions.assertThrows(AuthorizationException.class, () -> authService.loginAdmin("guest", "guest123"));
    }

    private AuthService createAuthServiceWithUsers() throws IOException {
        Path dir = Files.createTempDirectory("zhixing-auth-test");
        PersistenceService persistenceService = new PersistenceService(dir);

        List<User> users = new ArrayList<User>();
        users.add(new Admin("admin", PasswordHasher.hash("admin123")));
        users.add(new NormalUser("guest", PasswordHasher.hash("guest123")));
        persistenceService.saveUsers(users);

        return new AuthService(persistenceService);
    }
}

