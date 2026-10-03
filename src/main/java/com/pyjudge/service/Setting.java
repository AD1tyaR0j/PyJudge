package com.pyjudge.service;

/** Every system setting an admin can change, with its type, default, and allowed range. */
public enum Setting {
    SITE_NAME(Type.TEXT, "PyJudge", "Site name", "Shown in the header and page titles.", 0, 0),
    ANNOUNCEMENT(Type.TEXT, "", "Announcement banner", "Shown at the top of every page. Leave empty to hide.", 0, 0),
    REGISTRATION_OPEN(Type.FLAG, "true", "Open registration", "Let new students sign up on their own.", 0, 0),
    REQUIRE_APPROVAL(Type.FLAG, "true", "Require approval",
            "Instructor exercises and lessons must be approved by an admin before students see them.", 0, 0),
    MESSAGING_ENABLED(Type.FLAG, "true", "Messaging", "Let students and instructors message each other.", 0, 0),
    LEADERBOARD_ENABLED(Type.FLAG, "true", "Leaderboard", "Show the public leaderboard.", 0, 0),
    DEFAULT_TIME_LIMIT(Type.NUMBER, "2", "Default time limit (s)", "Used for new exercises and the Run button.", 1, 10),
    DEFAULT_MEMORY_LIMIT(Type.NUMBER, "128", "Default memory limit (MB)", "Used for new exercises and the Run button.", 16, 1024),
    MAX_PENDING_SUBMISSIONS(Type.NUMBER, "3", "Queued submissions per student",
            "How many submissions one student may have waiting for the judge.", 1, 20);

    public enum Type { TEXT, FLAG, NUMBER }

    private final Type type;
    private final String defaultValue;
    private final String label;
    private final String help;
    private final int min;
    private final int max;

    Setting(Type type, String defaultValue, String label, String help, int min, int max) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.label = label;
        this.help = help;
        this.min = min;
        this.max = max;
    }

    /** Key used in the database and in form fields, e.g. "default_time_limit". */
    public String key() {
        return name().toLowerCase();
    }

    public String getKey() { return key(); }
    public Type getType() { return type; }
    public String getDefaultValue() { return defaultValue; }
    public String getLabel() { return label; }
    public String getHelp() { return help; }
    public int getMin() { return min; }
    public int getMax() { return max; }
}
