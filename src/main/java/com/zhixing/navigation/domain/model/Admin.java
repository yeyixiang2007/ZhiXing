package com.zhixing.navigation.domain.model;

public final class Admin extends User {
    public Admin(String username, String passwordHash) {
        super(username, passwordHash, Role.ADMIN);
    }
}

