package com.pyjudge.db;

import java.sql.SQLException;

/** Unchecked wrapper so callers aren't forced to handle SQLException at every layer. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(SQLException cause) {
        super(cause.getMessage(), cause);
    }

    public DataAccessException(String message) {
        super(message);
    }

    /** True when the failure was a UNIQUE constraint (e.g. a duplicate username). */
    public boolean isUniqueViolation() {
        String msg = String.valueOf(getMessage()).toUpperCase();
        return msg.contains("UNIQUE") || msg.contains("DUPLICATE");
    }
}
