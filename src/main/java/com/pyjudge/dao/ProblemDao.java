package com.pyjudge.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.pyjudge.db.Database;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.TestCase;
import com.pyjudge.util.Times;

public class ProblemDao extends JdbcDao implements Repository<Problem> {

    private static final String SELECT = "SELECT p.*, u.name AS author_name FROM problems p"
            + " LEFT JOIN users u ON u.id = p.created_by";

    public ProblemDao(Database db) {
        super(db);
    }

    static Problem map(ResultSet rs) throws SQLException {
        Problem p = new Problem();
        p.setId(rs.getLong("id"));
        p.setTitle(rs.getString("title"));
        p.setSlug(rs.getString("slug"));
        p.setDescription(rs.getString("description"));
        p.setDifficulty(Difficulty.fromLabel(rs.getString("difficulty")));
        p.setTimeLimit(rs.getInt("time_limit"));
        p.setMemoryLimit(rs.getInt("memory_limit"));
        p.setTags(rs.getString("tags"));
        p.setStarterCode(rs.getString("starter_code"));
        p.setStatus(ContentStatus.fromCode(rs.getString("status")));
        p.setReviewNote(rs.getString("review_note"));
        p.setReviewedBy(getLongOrNull(rs, "reviewed_by"));
        p.setReviewedAt(getTime(rs, "reviewed_at"));
        p.setCreatedBy(getLongOrNull(rs, "created_by"));
        p.setAuthorName(rs.getString("author_name"));
        p.setCreatedAt(getTime(rs, "created_at"));
        p.setUpdatedAt(getTime(rs, "updated_at"));
        return p;
    }

    private static TestCase mapCase(ResultSet rs) throws SQLException {
        TestCase tc = new TestCase(rs.getString("input"), rs.getString("expected_output"), rs.getInt("is_sample") == 1);
        tc.setId(rs.getLong("id"));
        tc.setProblemId(rs.getLong("problem_id"));
        return tc;
    }

    private Problem withCases(Problem p) {
        p.setTestCases(testCases(p.getId()));
        return p;
    }

    @Override
    public Optional<Problem> findById(long id) {
        return queryOne(SELECT + " WHERE p.id = ?", ProblemDao::map, id).map(this::withCases);
    }

    public Optional<Problem> findBySlug(String slug) {
        return queryOne(SELECT + " WHERE p.slug = ?", ProblemDao::map, slug).map(this::withCases);
    }

    public Optional<Problem> findByTitle(String title) {
        return queryOne(SELECT + " WHERE p.title = ?", ProblemDao::map, title);
    }

    @Override
    public List<Problem> findAll() {
        return query(SELECT + " ORDER BY p.id", ProblemDao::map);
    }

    /** Filtered listing used by the exercise list and the admin content table. Null filters are ignored. */
    public List<Problem> search(ContentStatus status, Difficulty difficulty, String tag, String text, Long authorId) {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append(" AND p.status = ?");
            params.add(status);
        }
        if (difficulty != null) {
            sql.append(" AND p.difficulty = ?");
            params.add(difficulty);
        }
        if (tag != null && !tag.isBlank()) {
            sql.append(" AND (',' || REPLACE(p.tags, ' ', '') || ',') LIKE ?");
            params.add("%," + tag.trim().replace(" ", "") + ",%");
        }
        if (text != null && !text.isBlank()) {
            sql.append(" AND p.title LIKE ?");
            params.add("%" + text.trim() + "%");
        }
        if (authorId != null) {
            sql.append(" AND p.created_by = ?");
            params.add(authorId);
        }
        sql.append(" ORDER BY p.id");
        return query(sql.toString(), ProblemDao::map, params.toArray());
    }

    public List<Problem> approved() {
        return search(ContentStatus.APPROVED, null, null, null, null);
    }

    public List<TestCase> testCases(long problemId) {
        return query("SELECT * FROM test_cases WHERE problem_id = ? ORDER BY id", ProblemDao::mapCase, problemId);
    }

    public Map<Long, Long> testCaseCounts() {
        return queryMap("SELECT problem_id, COUNT(*) FROM test_cases GROUP BY problem_id",
                rs -> rs.getLong(1), rs -> rs.getLong(2));
    }

    /**
     * Inserts or updates the exercise. A new exercise is saved together with its test cases
     * in one transaction, so a failure never leaves an exercise without its tests.
     */
    @Override
    public Problem save(Problem p) {
        p.setUpdatedAt(Times.now());
        if (p.getId() == 0) {
            p.setCreatedAt(p.getUpdatedAt());
            db.inTransaction(c -> {
                long id = insert("INSERT INTO problems (title, slug, description, difficulty, time_limit, memory_limit,"
                                + " tags, starter_code, status, review_note, reviewed_by, reviewed_at, created_by,"
                                + " created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        p.getTitle(), p.getSlug(), p.getDescription(), p.getDifficulty(), p.getTimeLimit(),
                        p.getMemoryLimit(), p.getTags(), p.getStarterCode(), p.getStatus(), p.getReviewNote(),
                        p.getReviewedBy(), p.getReviewedAt(), p.getCreatedBy(), p.getCreatedAt(), p.getUpdatedAt());
                p.setId(id);
                for (TestCase tc : p.getTestCases()) {
                    addTestCase(id, tc);
                }
            });
        } else {
            update("UPDATE problems SET title = ?, slug = ?, description = ?, difficulty = ?, time_limit = ?,"
                            + " memory_limit = ?, tags = ?, starter_code = ?, status = ?, review_note = ?,"
                            + " reviewed_by = ?, reviewed_at = ?, updated_at = ? WHERE id = ?",
                    p.getTitle(), p.getSlug(), p.getDescription(), p.getDifficulty(), p.getTimeLimit(),
                    p.getMemoryLimit(), p.getTags(), p.getStarterCode(), p.getStatus(), p.getReviewNote(),
                    p.getReviewedBy(), p.getReviewedAt(), p.getUpdatedAt(), p.getId());
        }
        return p;
    }

    public TestCase addTestCase(long problemId, TestCase tc) {
        tc.setProblemId(problemId);
        tc.setId(insert("INSERT INTO test_cases (problem_id, input, expected_output, is_sample) VALUES (?, ?, ?, ?)",
                problemId, tc.getInput(), tc.getExpectedOutput(), tc.isSample()));
        return tc;
    }

    public Optional<TestCase> findTestCase(long id) {
        return queryOne("SELECT * FROM test_cases WHERE id = ?", ProblemDao::mapCase, id);
    }

    public void deleteTestCase(long id) {
        update("DELETE FROM test_cases WHERE id = ?", id);
    }

    /** Test cases and submissions go with it (ON DELETE CASCADE). */
    @Override
    public boolean delete(long id) {
        return update("DELETE FROM problems WHERE id = ?", id) == 1;
    }

    public boolean slugExists(String slug, long exceptId) {
        return queryLong("SELECT COUNT(*) FROM problems WHERE slug = ? AND id <> ?", slug, exceptId) > 0;
    }

    public Map<String, Long> countByStatus() {
        return queryMap("SELECT status, COUNT(*) FROM problems GROUP BY status", rs -> rs.getString(1), rs -> rs.getLong(2));
    }

    public long countApproved() {
        return queryLong("SELECT COUNT(*) FROM problems WHERE status = 'approved'");
    }

    /** Distinct tags across approved exercises, for the topic filter. */
    public List<String> approvedTags() {
        return approved().stream().flatMap(p -> p.getTagList().stream()).distinct().sorted().toList();
    }
}
