package com.pyjudge.web;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyjudge.course.MarkdownRenderer;
import com.pyjudge.util.Times;

/**
 * Static helpers exposed to JSP as EL functions (see WEB-INF/pyjudge.tld), e.g. {@code ${h:e(user.name)}}.
 * JSP EL does not escape output by default, so every user-supplied value goes through {@link #e}.
 */
public final class Html {

    private static final ObjectMapper JSON = new ObjectMapper();

    private Html() {
    }

    /** HTML-escape any value (null becomes ""). */
    public static String e(Object value) {
        return value == null ? "" : MarkdownRenderer.escape(String.valueOf(value));
    }

    public static String dt(LocalDateTime t) {
        return Times.display(t);
    }

    public static String date(LocalDateTime t) {
        return t == null ? "" : t.toLocalDate().toString();
    }

    /** Value for an HTML datetime-local input. */
    public static String inputTime(LocalDateTime t) {
        return t == null ? "" : Times.INPUT.format(t);
    }

    public static String md(String markdown) {
        return MarkdownRenderer.render(markdown);
    }

    public static String mdInline(String markdown) {
        return MarkdownRenderer.renderInline(markdown);
    }

    public static String lesson(String markdown) {
        return MarkdownRenderer.renderLesson(markdown);
    }

    /** JSON safe to place inside a {@code <script>} element. */
    public static String json(Object value) {
        try {
            return JSON.writeValueAsString(value).replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
        } catch (JsonProcessingException e) {
            return "null";
        }
    }

    public static int pct(Number part, Number whole) {
        if (part == null || whole == null || whole.doubleValue() == 0) {
            return 0;
        }
        return (int) Math.round(100.0 * part.doubleValue() / whole.doubleValue());
    }

    /** true if the collection/array/string contains the value (EL has no `in` operator). */
    public static boolean has(java.util.Collection<?> collection, Object value) {
        if (collection == null || value == null) {
            return false;
        }
        if (value instanceof Number n) {
            return collection.contains(n.longValue()) || collection.contains(n.intValue());
        }
        return collection.contains(value);
    }

    public static String plural(Number n, String word) {
        long v = n == null ? 0 : n.longValue();
        return v + " " + word + (v == 1 ? "" : "s");
    }
}
