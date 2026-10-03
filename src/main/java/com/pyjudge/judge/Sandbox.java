package com.pyjudge.judge;

/**
 * Runs untrusted Python code with limits. Implementations decide *how* it is isolated
 * (a local process today; a container would be another implementation).
 */
public interface Sandbox {

    /** Output shown after "Run" is capped so huge prints can't flood the page. */
    int DISPLAY_OUTPUT_LIMIT = 64 * 1024;

    /** Judging compares the full answer, which can legitimately be megabytes. */
    int JUDGE_OUTPUT_LIMIT = 16 * 1024 * 1024;

    RunResult run(String code, String stdin, int timeLimitSeconds, int memoryLimitMb, int maxOutputBytes);
}
