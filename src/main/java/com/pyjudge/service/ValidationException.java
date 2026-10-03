package com.pyjudge.service;

import java.util.List;

/** User input was rejected. Carries every problem found so the form can show them all at once. */
public class ValidationException extends PyJudgeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join(" ", errors));
        this.errors = List.copyOf(errors);
    }

    public ValidationException(String error) {
        this(List.of(error));
    }

    public List<String> errors() {
        return errors;
    }

    @Override
    public int status() {
        return 400;
    }

    /** Throws if any errors were collected. */
    public static void throwIfAny(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
