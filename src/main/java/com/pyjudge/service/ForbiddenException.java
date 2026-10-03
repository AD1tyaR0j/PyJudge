package com.pyjudge.service;

public class ForbiddenException extends PyJudgeException {

    public ForbiddenException() {
        super("You don't have permission to do that.");
    }

    public ForbiddenException(String message) {
        super(message);
    }

    @Override
    public int status() {
        return 403;
    }
}
