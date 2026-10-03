package com.pyjudge.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.pyjudge.db.Database;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;
import com.pyjudge.util.Times;

public class UserDao extends JdbcDao implements Repository<User> {

    private static final String COLUMNS =
            "id, username, name, email, bio, password_hash, role, blocked, created_at, last_seen_at";

    public UserDao(Database db) {
        super(db);
    }

    /** Builds the right User subclass (Student / Instructor / Admin) from the role column. */
    static User map(ResultSet rs) throws SQLException {
        User u = User.forRole(Role.fromCode(rs.getString("role")));
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setBio(rs.getString("bio"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setBlocked(rs.getInt("blocked") == 1);
        u.setCreatedAt(getTime(rs, "created_at"));
        u.setLastSeenAt(getTime(rs, "last_seen_at"));
        return u;
    }

    @Override
    public Optional<User> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", UserDao::map, id);
    }

    public Optional<User> findByUsername(String username) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE username = ?", UserDao::map, username);
    }

    /** Login accepts either the username or the email address. */
    public Optional<User> findByLogin(String identifier) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE username = ? OR email = ?", UserDao::map,
                identifier, identifier.toLowerCase());
    }

    public Optional<User> findByEmail(String email) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE email = ?", UserDao::map, email.toLowerCase());
    }

    @Override
    public List<User> findAll() {
        return query("SELECT " + COLUMNS + " FROM users ORDER BY id", UserDao::map);
    }

    /** Admin user list with optional role filter and free-text search. */
    public List<User> search(Role role, String text) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM users WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (text != null && !text.isBlank()) {
            sql.append(" AND (username LIKE ? OR name LIKE ? OR email LIKE ?)");
            String like = "%" + text.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY id");
        return query(sql.toString(), UserDao::map, params.toArray());
    }

    public List<User> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return query("SELECT " + COLUMNS + " FROM users WHERE id IN (" + placeholders(ids.size()) + ")",
                UserDao::map, ids.toArray());
    }

    @Override
    public User save(User u) {
        if (u.getId() == 0) {
            if (u.getCreatedAt() == null) {
                u.setCreatedAt(Times.now());
            }
            long id = insert("INSERT INTO users (username, name, email, bio, password_hash, role, blocked, created_at)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    u.getUsername(), u.getName(), u.getEmail().toLowerCase(), u.getBio(), u.getPasswordHash(),
                    u.getRole(), u.isBlocked(), u.getCreatedAt());
            u.setId(id);
        } else {
            update("UPDATE users SET name = ?, email = ?, bio = ?, password_hash = ?, role = ?, blocked = ? WHERE id = ?",
                    u.getName(), u.getEmail().toLowerCase(), u.getBio(), u.getPasswordHash(), u.getRole(),
                    u.isBlocked(), u.getId());
        }
        return u;
    }

    /** Changing role means a different subclass: reload after this. */
    public void updateRole(long userId, Role role) {
        update("UPDATE users SET role = ? WHERE id = ?", role, userId);
    }

    public void touchLastSeen(long userId) {
        update("UPDATE users SET last_seen_at = ? WHERE id = ?", Times.now(), userId);
    }

    /** Related rows are removed or detached by the schema's ON DELETE rules. */
    @Override
    public boolean delete(long id) {
        return update("DELETE FROM users WHERE id = ?", id) == 1;
    }

    public boolean usernameTaken(String username, long exceptId) {
        return queryLong("SELECT COUNT(*) FROM users WHERE username = ? AND id <> ?", username, exceptId) > 0;
    }

    public boolean emailTaken(String email, long exceptId) {
        return queryLong("SELECT COUNT(*) FROM users WHERE email = ? AND id <> ?", email.toLowerCase(), exceptId) > 0;
    }

    public Map<String, Long> countByRole() {
        return queryMap("SELECT role, COUNT(*) FROM users GROUP BY role", rs -> rs.getString(1), rs -> rs.getLong(2));
    }

    public long count() {
        return queryLong("SELECT COUNT(*) FROM users");
    }

    public List<java.time.LocalDateTime> signupTimesSince(java.time.LocalDateTime since) {
        return query("SELECT created_at FROM users WHERE created_at >= ?", rs -> getTime(rs, "created_at"), since);
    }
}
