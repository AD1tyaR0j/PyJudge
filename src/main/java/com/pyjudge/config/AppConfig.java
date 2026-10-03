package com.pyjudge.config;

import java.io.File;
import java.util.Objects;

/**
 * Immutable application settings. Values come from environment variables (or JVM -D properties)
 * so the same build can run locally, in tests, and in production.
 */
public final class AppConfig {

    private static volatile AppConfig current;

    private final int port;
    private final String dataDir;
    private final String databaseUrl;
    private final String pythonCommand;
    private final int judgeWorkers;
    private final boolean judgeSync;
    private final int passwordIterations;
    private final String webappDir;
    private final String classesDir;

    private AppConfig(Builder b) {
        this.port = b.port;
        this.dataDir = b.dataDir;
        this.databaseUrl = b.databaseUrl != null ? b.databaseUrl
                : "jdbc:sqlite:" + new File(b.dataDir, "pyjudge.db").getPath().replace('\\', '/');
        this.pythonCommand = b.pythonCommand;
        this.judgeWorkers = b.judgeWorkers;
        this.judgeSync = b.judgeSync;
        this.passwordIterations = b.passwordIterations;
        this.webappDir = b.webappDir;
        this.classesDir = b.classesDir;
    }

    public static AppConfig fromEnvironment() {
        return builder()
                .port(Integer.parseInt(env("PYJUDGE_PORT", "8080")))
                .dataDir(env("PYJUDGE_DATA_DIR", "data"))
                .databaseUrl(env("PYJUDGE_DB_URL", null))
                .pythonCommand(env("PYJUDGE_PYTHON", defaultPython()))
                .judgeWorkers(Integer.parseInt(env("PYJUDGE_JUDGE_WORKERS", "4")))
                .build();
    }

    private static String defaultPython() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "python" : "python3";
    }

    private static String env(String key, String fallback) {
        String v = System.getProperty(key, System.getenv(key));
        return v == null || v.isBlank() ? fallback : v;
    }

    /** Makes the configuration available to the web app (set by App before Tomcat starts). */
    public static void install(AppConfig config) {
        current = Objects.requireNonNull(config);
    }

    public static AppConfig current() {
        if (current == null) {
            current = fromEnvironment();
        }
        return current;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int port() { return port; }
    public String dataDir() { return dataDir; }
    public String databaseUrl() { return databaseUrl; }
    public String pythonCommand() { return pythonCommand; }
    public int judgeWorkers() { return judgeWorkers; }
    public boolean judgeSync() { return judgeSync; }
    public int passwordIterations() { return passwordIterations; }
    public String webappDir() { return webappDir; }
    public String classesDir() { return classesDir; }

    /** Builder pattern: lots of optional settings with sensible defaults. */
    public static final class Builder {
        private int port = 8080;
        private String dataDir = "data";
        private String databaseUrl;
        private String pythonCommand = defaultPython();
        private int judgeWorkers = 4;
        private boolean judgeSync;
        private int passwordIterations = 120_000;
        private String webappDir = "src/main/webapp";
        private String classesDir = "target/classes";

        public Builder port(int v) { port = v; return this; }
        public Builder dataDir(String v) { dataDir = v; return this; }
        public Builder databaseUrl(String v) { databaseUrl = v; return this; }
        public Builder pythonCommand(String v) { pythonCommand = v; return this; }
        public Builder judgeWorkers(int v) { judgeWorkers = v; return this; }
        public Builder judgeSync(boolean v) { judgeSync = v; return this; }
        public Builder passwordIterations(int v) { passwordIterations = v; return this; }
        public Builder webappDir(String v) { webappDir = v; return this; }
        public Builder classesDir(String v) { classesDir = v; return this; }

        public AppConfig build() {
            new File(dataDir).mkdirs();
            return new AppConfig(this);
        }
    }
}
