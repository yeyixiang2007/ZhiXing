package com.zhixing.navigation.domain.model;

import java.util.Objects;

public abstract class User {
    private final String username;
    private final String passwordHash;
    private final Role role;

    protected User(String username, String passwordHash, Role role) {
        this.username = requireText(username, "username");
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean canManageMap() {
        return role == Role.ADMIN;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

