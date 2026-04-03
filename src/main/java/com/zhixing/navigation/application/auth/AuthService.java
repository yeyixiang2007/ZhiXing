package com.zhixing.navigation.application.auth;

import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.security.PasswordHasher;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;
import com.zhixing.navigation.domain.model.Role;
import com.zhixing.navigation.domain.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 30_000L;

    private final PersistenceService persistenceService;
    private final Map<String, Integer> failedAttempts;
    private final Map<String, Long> lockedUntilEpochMs;

    public AuthService(PersistenceService persistenceService) {
        this.persistenceService = Objects.requireNonNull(persistenceService, "persistenceService must not be null");
        this.failedAttempts = new HashMap<String, Integer>();
        this.lockedUntilEpochMs = new HashMap<String, Long>();
    }

    public User login(String username, String rawPassword) {
        String target = requireText(username, "username");
        requireText(rawPassword, "password");
        String key = target.toLowerCase();
        long now = System.currentTimeMillis();
        checkRateLimit(key, now);

        List<User> users = persistenceService.loadUsersOrDefault();
        for (User user : users) {
            if (user.getUsername().equals(target)) {
                if (!PasswordHasher.matches(rawPassword, user.getPasswordHash())) {
                    recordFailure(key, now);
                    throw new AuthenticationException("Invalid username or password.");
                }
                clearFailures(key);
                return user;
            }
        }
        recordFailure(key, now);
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

    private void checkRateLimit(String key, long now) {
        synchronized (this) {
            Long lockedUntil = lockedUntilEpochMs.get(key);
            if (lockedUntil == null) {
                return;
            }
            if (lockedUntil.longValue() <= now) {
                lockedUntilEpochMs.remove(key);
                return;
            }
            long waitSeconds = Math.max(1L, (lockedUntil.longValue() - now + 999L) / 1000L);
            throw new AuthenticationException("Too many failed attempts. Please retry in " + waitSeconds + " seconds.");
        }
    }

    private void recordFailure(String key, long now) {
        synchronized (this) {
            int count = failedAttempts.containsKey(key) ? failedAttempts.get(key).intValue() + 1 : 1;
            failedAttempts.put(key, Integer.valueOf(count));
            if (count >= MAX_FAILED_ATTEMPTS) {
                lockedUntilEpochMs.put(key, Long.valueOf(now + LOCK_DURATION_MS));
                failedAttempts.remove(key);
            }
        }
    }

    private void clearFailures(String key) {
        synchronized (this) {
            failedAttempts.remove(key);
            lockedUntilEpochMs.remove(key);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
