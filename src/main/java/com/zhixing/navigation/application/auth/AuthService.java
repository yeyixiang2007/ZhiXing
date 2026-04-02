package com.zhixing.navigation.application.auth;

import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Role;
import com.zhixing.navigation.domain.model.User;
import com.zhixing.navigation.domain.security.PasswordHasher;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;

import java.util.List;
import java.util.Objects;

public class AuthService {
    private final PersistenceService persistenceService;

    public AuthService(PersistenceService persistenceService) {
        this.persistenceService = Objects.requireNonNull(persistenceService, "persistenceService must not be null");
    }

    public User login(String username, String rawPassword) {
        String target = requireText(username, "username");
        requireText(rawPassword, "password");

        List<User> users = persistenceService.loadUsersOrDefault();
        for (User user : users) {
            if (user.getUsername().equals(target)) {
                if (!PasswordHasher.matches(rawPassword, user.getPasswordHash())) {
                    throw new AuthenticationException("Invalid username or password.");
                }
                return user;
            }
        }
        throw new AuthenticationException("Invalid username or password.");
    }

    public Admin loginAdmin(String username, String rawPassword) {
        User user = login(username, rawPassword);
        if (user.getRole() != Role.ADMIN) {
            throw new AuthorizationException("Permission denied: admin role required.");
        }
        return new Admin(user.getUsername(), user.getPasswordHash());
    }

    public void requireAdmin(User operator) {
        Objects.requireNonNull(operator, "operator must not be null");
        if (!operator.canManageMap()) {
            throw new AuthorizationException("Permission denied: admin role required.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

