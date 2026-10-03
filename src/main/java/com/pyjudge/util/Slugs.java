package com.pyjudge.util;

import java.text.Normalizer;
import java.util.function.Predicate;

/** URL-friendly identifiers: "Two Sum!" -> "two-sum". */
public final class Slugs {

    private Slugs() {
    }

    public static String slugify(String text) {
        String ascii = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKD).replaceAll("[^\\p{ASCII}]", "");
        String slug = ascii.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "item" : slug;
    }

    /** Appends -2, -3, ... until {@code taken} says the slug is free. */
    public static String unique(String text, Predicate<String> taken) {
        String base = slugify(text);
        String slug = base;
        for (int n = 2; taken.test(slug); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }
}
