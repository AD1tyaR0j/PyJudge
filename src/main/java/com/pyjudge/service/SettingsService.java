package com.pyjudge.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pyjudge.dao.SettingsDao;

/**
 * Typed access to system settings. Values are cached in a thread-safe map (every request reads them)
 * and the cache is refreshed whenever an admin saves.
 */
public class SettingsService {

    private final SettingsDao dao;
    private final Map<Setting, String> cache = new ConcurrentHashMap<>();

    public SettingsService(SettingsDao dao) {
        this.dao = dao;
        reload();
    }

    private void reload() {
        Map<String, String> stored = dao.all();
        for (Setting s : Setting.values()) {
            cache.put(s, stored.getOrDefault(s.key(), s.getDefaultValue()));
        }
    }

    public String text(Setting s) {
        return cache.getOrDefault(s, s.getDefaultValue());
    }

    public boolean flag(Setting s) {
        return Boolean.parseBoolean(text(s));
    }

    public int number(Setting s) {
        try {
            return Integer.parseInt(text(s));
        } catch (NumberFormatException e) {
            return Integer.parseInt(s.getDefaultValue());
        }
    }

    /** All values keyed by setting key, e.g. for JSP: ${site.site_name}. */
    public Map<String, Object> asMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Setting s : Setting.values()) {
            out.put(s.key(), switch (s.getType()) {
                case FLAG -> flag(s);
                case NUMBER -> number(s);
                case TEXT -> text(s);
            });
        }
        return out;
    }

    /**
     * Validates and saves settings from a submitted form (unchecked checkboxes are simply absent).
     *
     * @throws ValidationException listing every invalid field; nothing is saved in that case
     */
    public void update(Map<String, String> form) {
        List<String> errors = new ArrayList<>();
        Map<Setting, String> next = new EnumMap<>(Setting.class);
        for (Setting s : Setting.values()) {
            String raw = form.get(s.key());
            switch (s.getType()) {
                case FLAG -> next.put(s, String.valueOf(raw != null));
                case NUMBER -> {
                    try {
                        int v = Integer.parseInt(raw == null ? "" : raw.trim());
                        if (v < s.getMin() || v > s.getMax()) {
                            errors.add(s.getLabel() + " must be between " + s.getMin() + " and " + s.getMax() + ".");
                        } else {
                            next.put(s, String.valueOf(v));
                        }
                    } catch (NumberFormatException e) {
                        errors.add(s.getLabel() + " must be a whole number.");
                    }
                }
                case TEXT -> {
                    String v = raw == null ? "" : raw.trim();
                    if (s == Setting.SITE_NAME && v.isEmpty()) {
                        errors.add("Site name can't be empty.");
                    } else {
                        next.put(s, v.length() > 300 ? v.substring(0, 300) : v);
                    }
                }
            }
        }
        ValidationException.throwIfAny(errors);
        Map<String, String> rows = new LinkedHashMap<>();
        next.forEach((k, v) -> rows.put(k.key(), v));
        dao.saveAll(rows);
        cache.putAll(next);
    }
}
