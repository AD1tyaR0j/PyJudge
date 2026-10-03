package com.pyjudge.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** All timestamps are UTC and stored as 'yyyy-MM-dd HH:mm:ss' text. */
public final class Times {

    public static final DateTimeFormatter DB = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private Times() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    public static String toDb(LocalDateTime t) {
        return t == null ? null : DB.format(t);
    }

    public static LocalDateTime fromDb(String s) {
        return s == null || s.isBlank() ? null : LocalDateTime.parse(s.length() > 19 ? s.substring(0, 19) : s, DB);
    }

    public static String display(LocalDateTime t) {
        return t == null ? "" : DISPLAY.format(t);
    }
}
