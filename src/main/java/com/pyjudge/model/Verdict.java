package com.pyjudge.model;

/** The judge's decision on a submission. */
public enum Verdict {
    PENDING("Judging"),
    AC("Accepted"),
    WA("Wrong Answer"),
    TLE("Time Limit Exceeded"),
    MLE("Memory Limit Exceeded"),
    RE("Runtime Error"),
    CE("Syntax Error"),
    IE("Internal Error");

    private final String label;

    Verdict(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
    public String getCode() { return name(); }
    public String getCss() { return name().toLowerCase(); }
    public boolean isAccepted() { return this == AC; }
    public boolean isFinal() { return this != PENDING; }
}
