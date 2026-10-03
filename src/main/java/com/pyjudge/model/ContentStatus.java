package com.pyjudge.model;

public enum ContentStatus {
    PENDING("pending", "Pending review"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Rejected");

    private final String code;
    private final String label;

    ContentStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() { return code; }
    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static ContentStatus fromCode(String code) {
        for (ContentStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + code);
    }
}
