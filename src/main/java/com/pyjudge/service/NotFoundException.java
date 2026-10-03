package com.pyjudge.service;

public class NotFoundException extends PyJudgeException {

    public NotFoundException(String what) {
        super(what + " not found.");
    }

    @Override
    public int status() {
        return 404;
    }
}
