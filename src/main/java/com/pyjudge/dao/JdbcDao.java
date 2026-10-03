package com.pyjudge.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.pyjudge.db.Database;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Role;
import com.pyjudge.model.Verdict;
import com.pyjudge.util.Times;

/**
 * Shared JDBC plumbing. Every query goes through a {@link PreparedStatement} with bound parameters,
 * so user input is never concatenated into SQL.
 */
public abstract class JdbcDao {

    protected final Database db;

    protected JdbcDao(Database db) {
        this.db = db;
    }

    /** Maps the current row of a ResultSet to an object. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = prepare(c, sql, false, params); ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
                return out;
            }
        });
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    protected long queryLong(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    /** Two-column query to an ordered map, e.g. "SELECT status, COUNT(*) ... GROUP BY status". */
    protected <K, V> Map<K, V> queryMap(String sql, RowMapper<K> key, RowMapper<V> value, Object... params) {
        Map<K, V> out = new LinkedHashMap<>();
        db.withConnection(c -> {
            try (PreparedStatement ps = prepare(c, sql, false, params); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(key.map(rs), value.map(rs));
                }
            }
            return null;
        });
        return out;
    }

    protected int update(String sql, Object... params) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = prepare(c, sql, false, params)) {
                return ps.executeUpdate();
            }
        });
    }

    /** Runs an INSERT and returns the generated primary key. */
    protected long insert(String sql, Object... params) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = prepare(c, sql, true, params)) {
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
                throw new SQLException("No generated key for: " + sql);
            }
        });
    }

    /** "?, ?, ?" for an IN (...) clause with {@code n} parameters (at least one, so the SQL stays valid). */
    protected static String placeholders(int n) {
        return n <= 0 ? "NULL" : String.join(", ", java.util.Collections.nCopies(n, "?"));
    }

    protected static Object[] concat(Object[] head, Collection<?> tail) {
        List<Object> all = new ArrayList<>(List.of(head));
        all.addAll(tail);
        return all.toArray();
    }

    private PreparedStatement prepare(Connection c, String sql, boolean keys, Object... params) throws SQLException {
        PreparedStatement ps = keys ? c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            bind(ps, i + 1, params[i]);
        }
        return ps;
    }

    private static void bind(PreparedStatement ps, int index, Object value) throws SQLException {
        switch (value) {
            case null -> ps.setNull(index, Types.NULL);
            case LocalDateTime t -> ps.setString(index, Times.toDb(t));
            case Boolean b -> ps.setInt(index, b ? 1 : 0);
            case Role r -> ps.setString(index, r.code());
            case ContentStatus s -> ps.setString(index, s.code());
            case Difficulty d -> ps.setString(index, d.getLabel());
            case Verdict v -> ps.setString(index, v.name());
            case Enum<?> e -> ps.setString(index, e.name().toLowerCase());
            default -> ps.setObject(index, value);
        }
    }

    // ---- ResultSet helpers for nullable columns ----

    protected static Long getLongOrNull(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    protected static Integer getIntOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    protected static LocalDateTime getTime(ResultSet rs, String col) throws SQLException {
        return Times.fromDb(rs.getString(col));
    }

    protected static String joinIds(Collection<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
