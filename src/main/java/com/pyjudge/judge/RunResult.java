package com.pyjudge.judge;

/** What happened when one program ran on one input. */
public final class RunResult {

    public enum Status { OK, TLE, MLE, RE, CE }

    private final Status status;
    private final String stdout;
    private final String stderr;
    private final Integer exitCode;
    private final long timeMs;

    public RunResult(Status status, String stdout, String stderr, Integer exitCode, long timeMs) {
        this.status = status;
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
        this.timeMs = timeMs;
    }

    public Status status() { return status; }
    public String stdout() { return stdout; }
    public String stderr() { return stderr; }
    public Integer exitCode() { return exitCode; }
    public long timeMs() { return timeMs; }
}
