package com.pyjudge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.pyjudge.config.AppConfig;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;

/** Builds an isolated application (fresh SQLite file, synchronous judge, fast password hashing) for each test. */
public final class TestSupport {

    private TestSupport() {
    }

    public static AppConfig config() {
        try {
            Path dir = Files.createTempDirectory("pyjudge-test");
            return AppConfig.builder()
                    .port(0)
                    .dataDir(dir.toString())
                    .judgeSync(true)
                    .judgeWorkers(2)
                    .passwordIterations(1_000)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Services services() {
        return new Services(config());
    }

    public static User user(Services s, String username, Role role) {
        return s.auth.createUser(username, capitalize(username), username + "@example.com", "password123", role);
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
