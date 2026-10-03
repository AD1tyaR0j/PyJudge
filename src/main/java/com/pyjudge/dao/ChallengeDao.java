package com.pyjudge.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.pyjudge.db.Database;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.Problem;
import com.pyjudge.util.Times;

public class ChallengeDao extends JdbcDao implements Repository<Challenge> {

    private static final String SELECT = "SELECT c.*, u.name AS author_name FROM challenges c"
            + " LEFT JOIN users u ON u.id = c.created_by";

    public ChallengeDao(Database db) {
        super(db);
    }

    private static Challenge map(ResultSet rs) throws SQLException {
        Challenge c = new Challenge();
        c.setId(rs.getLong("id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setStartAt(getTime(rs, "start_at"));
        c.setEndAt(getTime(rs, "end_at"));
        c.setCreatedBy(getLongOrNull(rs, "created_by"));
        c.setAuthorName(rs.getString("author_name"));
        c.setCreatedAt(getTime(rs, "created_at"));
        return c;
    }

    private Challenge withProblems(Challenge c) {
        c.setProblems(query("SELECT p.*, NULL AS author_name FROM problems p JOIN challenge_problems cp"
                + " ON cp.problem_id = p.id WHERE cp.challenge_id = ? ORDER BY p.id", ProblemDao::map, c.getId()));
        return c;
    }

    @Override
    public Optional<Challenge> findById(long id) {
        return queryOne(SELECT + " WHERE c.id = ?", ChallengeDao::map, id).map(this::withProblems);
    }

    @Override
    public List<Challenge> findAll() {
        return query(SELECT + " ORDER BY c.start_at DESC", ChallengeDao::map).stream().map(this::withProblems).toList();
    }

    /** Saves the challenge and replaces its exercise list atomically. */
    @Override
    public Challenge save(Challenge c) {
        db.inTransaction(conn -> {
            if (c.getId() == 0) {
                c.setCreatedAt(Times.now());
                c.setId(insert("INSERT INTO challenges (title, description, start_at, end_at, created_by, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?)", c.getTitle(), c.getDescription(), c.getStartAt(), c.getEndAt(),
                        c.getCreatedBy(), c.getCreatedAt()));
            } else {
                update("UPDATE challenges SET title = ?, description = ?, start_at = ?, end_at = ? WHERE id = ?",
                        c.getTitle(), c.getDescription(), c.getStartAt(), c.getEndAt(), c.getId());
                update("DELETE FROM challenge_problems WHERE challenge_id = ?", c.getId());
            }
            for (Problem p : c.getProblems()) {
                update("INSERT INTO challenge_problems (challenge_id, problem_id) VALUES (?, ?)", c.getId(), p.getId());
            }
        });
        return c;
    }

    @Override
    public boolean delete(long id) {
        return update("DELETE FROM challenges WHERE id = ?", id) == 1;
    }

    public long count() {
        return queryLong("SELECT COUNT(*) FROM challenges");
    }
}
