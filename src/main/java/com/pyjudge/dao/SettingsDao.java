package com.pyjudge.dao;

import java.util.Map;

import com.pyjudge.db.Database;

/** Key/value store for system settings. */
public class SettingsDao extends JdbcDao {

    public SettingsDao(Database db) {
        super(db);
    }

    public Map<String, String> all() {
        return queryMap("SELECT key, value FROM settings", rs -> rs.getString(1), rs -> rs.getString(2));
    }

    /** Writes every value in one transaction: either all settings change or none do. */
    public void saveAll(Map<String, String> values) {
        db.inTransaction(c -> {
            for (Map.Entry<String, String> e : values.entrySet()) {
                update("INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value",
                        e.getKey(), e.getValue());
            }
        });
    }
}
