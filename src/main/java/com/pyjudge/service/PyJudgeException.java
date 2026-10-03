package com.pyjudge.service;

/** Base class for the application's own (expected) errors. Servlets map each subclass to an HTTP status. */
public abstract class PyJudgeException extends RuntimeException {

    protected PyJudgeException(String message) {
        super(message);
    }

    /** HTTP status this error should produce. */
    public abstract int status();
}
