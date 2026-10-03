package com.pyjudge.model;

/** The three kinds of account. The database stores the lowercase {@link #code()}. */
public enum Role {
    STUDENT("student", "Student"),
    INSTRUCTOR("instructor", "Instructor"),
    ADMIN("admin", "Admin");

    private final String code;
    private final String label;

    Role(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() { return code; }
    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static Role fromCode(String code) {
        for (Role r : values()) {
            if (r.code.equalsIgnoreCase(code)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + code);
    }
}
