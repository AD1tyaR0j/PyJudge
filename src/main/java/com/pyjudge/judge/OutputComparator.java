package com.pyjudge.judge;

import java.util.ArrayList;
import java.util.List;

/** Compares program output with the expected answer, ignoring trailing spaces and blank lines. */
public final class OutputComparator {

    private OutputComparator() {
    }

    public static String normalize(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            lines.add(line.stripTrailing());
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines);
    }

    public static boolean matches(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }
}
