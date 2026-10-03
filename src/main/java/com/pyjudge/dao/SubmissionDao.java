package com.pyjudge.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.pyjudge.db.Database;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Submission;
import com.pyjudge.model.Verdict;
import com.pyjudge.util.Times;

public class SubmissionDao extends JdbcDao implements Repository<Submission> {

    private static final String SELECT = "SELECT s.*, u.username, u.name AS user_name, p.title AS problem_title,"
            + " p.slug AS problem_slug, p.difficulty AS problem_difficulty, p.created_by AS problem_author,"
            + " g.name AS grader_name FROM submissions s"
            + " JOIN users u ON u.id = s.user_id JOIN problems p ON p.id = s.problem_id"
            + " LEFT JOIN users g ON g.id = s.graded_by";

    public SubmissionDao(Database db) {
        super(db);
    }

    static Submission map(ResultSet rs) throws SQLException {
        Submission s = new Submission();
        s.setId(rs.getLong("id"));
        s.setUserId(rs.getLong("user_id"));
        s.setProblemId(rs.getLong("problem_id"));
        s.setCode(rs.getString("code"));
        s.setVerdict(Verdict.valueOf(rs.getString("verdict")));
        s.setPassed(rs.getInt("passed"));
        s.setTotal(rs.getInt("total"));
        s.setRuntimeMs(getIntOrNull(rs, "runtime_ms"));
        s.setError(rs.getString("error"));
        s.setGrade(getIntOrNull(rs, "grade"));
        s.setFeedback(rs.getString("feedback"));
        s.setGradedBy(getLongOrNull(rs, "graded_by"));
        s.setGradedAt(getTime(rs, "graded_at"));
        s.setSubmittedAt(getTime(rs, "submitted_at"));
        s.setUsername(rs.getString("username"));
        String name = rs.getString("user_name");
        s.setUserDisplayName(name == null || name.isBlank() ? s.getUsername() : name);
        s.setProblemTitle(rs.getString("problem_title"));
        s.setProblemSlug(rs.getString("problem_slug"));
        s.setProblemDifficulty(Difficulty.fromLabel(rs.getString("problem_difficulty")));
        s.setProblemAuthorId(getLongOrNull(rs, "problem_author"));
        s.setGraderName(rs.getString("grader_name"));
        return s;
    }

    @Override
    public Optional<Submission> findById(long id) {
        return queryOne(SELECT + " WHERE s.id = ?", SubmissionDao::map, id);
    }

    @Override
    public List<Submission> findAll() {
        return query(SELECT + " ORDER BY s.id DESC", SubmissionDao::map);
    }

    @Override
    public Submission save(Submission s) {
        if (s.getId() == 0) {
            s.setSubmittedAt(Times.now());
            s.setId(insert("INSERT INTO submissions (user_id, problem_id, code, verdict, total, submitted_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?)", s.getUserId(), s.getProblemId(), s.getCode(), s.getVerdict(),
                    s.getTotal(), s.getSubmittedAt()));
        } else {
            saveResult(s);
        }
        return s;
    }

    /** Called by the judge worker thread once all tests have run. */
    public void saveResult(Submission s) {
        update("UPDATE submissions SET verdict = ?, passed = ?, total = ?, runtime_ms = ?, error = ? WHERE id = ?",
                s.getVerdict(), s.getPassed(), s.getTotal(), s.getRuntimeMs(), s.getError(), s.getId());
    }

    public void saveGrade(long id, int grade, String feedback, long graderId) {
        update("UPDATE submissions SET grade = ?, feedback = ?, graded_by = ?, graded_at = ? WHERE id = ?",
                grade, feedback, graderId, Times.now(), id);
    }

    @Override
    public boolean delete(long id) {
        return update("DELETE FROM submissions WHERE id = ?", id) == 1;
    }

    public List<Submission> byUser(long userId, int limit, int offset) {
        return query(SELECT + " WHERE s.user_id = ? ORDER BY s.id DESC LIMIT ? OFFSET ?", SubmissionDao::map,
                userId, limit, offset);
    }

    public long countByUser(long userId) {
        return queryLong("SELECT COUNT(*) FROM submissions WHERE user_id = ?", userId);
    }

    public Optional<Submission> latestFor(long userId, long problemId) {
        return queryOne(SELECT + " WHERE s.user_id = ? AND s.problem_id = ? ORDER BY s.id DESC LIMIT 1",
                SubmissionDao::map, userId, problemId);
    }

    public List<Submission> recent(int limit) {
        return query(SELECT + " ORDER BY s.id DESC LIMIT ?", SubmissionDao::map, limit);
    }

    /** Instructor review queue: submissions on the given exercises, optionally only (un)graded ones. */
    public List<Submission> forProblems(Collection<Long> problemIds, Boolean graded, Long problemId, int limit, int offset) {
        if (problemIds.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder(SELECT + " WHERE s.verdict <> 'PENDING' AND s.problem_id IN ("
                + placeholders(problemIds.size()) + ")");
        List<Object> params = new ArrayList<>(problemIds);
        if (graded != null) {
            sql.append(graded ? " AND s.grade IS NOT NULL" : " AND s.grade IS NULL");
        }
        if (problemId != null) {
            sql.append(" AND s.problem_id = ?");
            params.add(problemId);
        }
        sql.append(" ORDER BY s.id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return query(sql.toString(), SubmissionDao::map, params.toArray());
    }

    public long countForProblems(Collection<Long> problemIds, Boolean graded) {
        if (problemIds.isEmpty()) {
            return 0;
        }
        String extra = graded == null ? "" : graded ? " AND grade IS NOT NULL" : " AND grade IS NULL";
        return queryLong("SELECT COUNT(*) FROM submissions WHERE verdict <> 'PENDING' AND problem_id IN ("
                + placeholders(problemIds.size()) + ")" + extra, problemIds.toArray());
    }

    public long countDistinctStudents(Collection<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return 0;
        }
        return queryLong("SELECT COUNT(DISTINCT user_id) FROM submissions WHERE problem_id IN ("
                + placeholders(problemIds.size()) + ")", problemIds.toArray());
    }

    public long countPendingByUser(long userId) {
        return queryLong("SELECT COUNT(*) FROM submissions WHERE user_id = ? AND verdict = 'PENDING'", userId);
    }

    public Set<Long> solvedProblemIds(long userId) {
        return new TreeSet<>(query("SELECT DISTINCT problem_id FROM submissions WHERE user_id = ? AND verdict = 'AC'",
                rs -> rs.getLong(1), userId));
    }

    public Set<Long> attemptedProblemIds(long userId) {
        return new TreeSet<>(query("SELECT DISTINCT problem_id FROM submissions WHERE user_id = ?", rs -> rs.getLong(1), userId));
    }

    /** Problems the user solved within a time window (challenges). */
    public Set<Long> solvedBetween(long userId, Collection<Long> problemIds, LocalDateTime from, LocalDateTime to) {
        if (problemIds.isEmpty()) {
            return Set.of();
        }
        Object[] params = concat(new Object[]{userId, from, to}, problemIds);
        return new TreeSet<>(query("SELECT DISTINCT problem_id FROM submissions WHERE user_id = ? AND verdict = 'AC'"
                + " AND submitted_at BETWEEN ? AND ? AND problem_id IN (" + placeholders(problemIds.size()) + ")",
                rs -> rs.getLong(1), params));
    }

    /** problem_id -> {judged, accepted, distinct students, distinct solvers}. */
    public Map<Long, long[]> acceptanceStats() {
        return queryMap("SELECT problem_id, COUNT(*), SUM(CASE WHEN verdict = 'AC' THEN 1 ELSE 0 END),"
                        + " COUNT(DISTINCT user_id), COUNT(DISTINCT CASE WHEN verdict = 'AC' THEN user_id END)"
                        + " FROM submissions WHERE verdict <> 'PENDING' GROUP BY problem_id",
                rs -> rs.getLong(1), rs -> new long[]{rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)});
    }

    public Map<Long, Double> averageGrades() {
        return queryMap("SELECT problem_id, AVG(grade) FROM submissions WHERE grade IS NOT NULL GROUP BY problem_id",
                rs -> rs.getLong(1), rs -> rs.getDouble(2));
    }

    /** Distinct (user, problem difficulty) pairs of accepted solves, for scoring. Optional window and problem set. */
    public List<long[]> solvePairs(Collection<Long> problemIds, LocalDateTime from, LocalDateTime to) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT s.user_id, s.problem_id, p.difficulty FROM submissions s"
                + " JOIN problems p ON p.id = s.problem_id JOIN users u ON u.id = s.user_id"
                + " WHERE s.verdict = 'AC' AND p.status = 'approved' AND u.blocked = 0");
        List<Object> params = new ArrayList<>();
        if (problemIds != null) {
            if (problemIds.isEmpty()) {
                return List.of();
            }
            sql.append(" AND s.problem_id IN (").append(placeholders(problemIds.size())).append(")");
            params.addAll(problemIds);
        }
        if (from != null) {
            sql.append(" AND s.submitted_at BETWEEN ? AND ?");
            params.add(from);
            params.add(to);
        }
        return query(sql.toString(), rs -> new long[]{rs.getLong(1), rs.getLong(2),
                Difficulty.fromLabel(rs.getString(3)).getPoints()}, params.toArray());
    }

    public Map<String, Long> verdictCounts(Long userId) {
        return userId == null
                ? queryMap("SELECT verdict, COUNT(*) FROM submissions GROUP BY verdict", rs -> rs.getString(1), rs -> rs.getLong(2))
                : queryMap("SELECT verdict, COUNT(*) FROM submissions WHERE user_id = ? GROUP BY verdict",
                        rs -> rs.getString(1), rs -> rs.getLong(2), userId);
    }

    public long count() {
        return queryLong("SELECT COUNT(*) FROM submissions");
    }

    /** (submitted_at, verdict, user_id) since a date, for the activity charts. */
    public List<Object[]> activitySince(LocalDateTime since) {
        return query("SELECT submitted_at, verdict, user_id FROM submissions WHERE submitted_at >= ?",
                rs -> new Object[]{getTime(rs, "submitted_at"), rs.getString("verdict"), rs.getLong("user_id")}, since);
    }

    public List<LocalDateTime> submissionTimes(long userId) {
        return query("SELECT submitted_at FROM submissions WHERE user_id = ?", rs -> getTime(rs, "submitted_at"), userId);
    }

    /** problem_id -> first accepted time, for the "solved over time" chart. */
    public Map<Long, LocalDateTime> firstSolveTimes(long userId) {
        return queryMap("SELECT problem_id, MIN(submitted_at) FROM submissions WHERE user_id = ? AND verdict = 'AC'"
                + " GROUP BY problem_id", rs -> rs.getLong(1), rs -> Times.fromDb(rs.getString(2)), userId);
    }

    public List<Submission> gradedForUser(long userId, int limit) {
        return query(SELECT + " WHERE s.user_id = ? AND s.grade IS NOT NULL ORDER BY s.graded_at DESC LIMIT ?",
                SubmissionDao::map, userId, limit);
    }

    public Double averageGradeForUser(long userId) {
        return queryOne("SELECT AVG(grade) FROM submissions WHERE user_id = ? AND grade IS NOT NULL",
                rs -> {
                    double v = rs.getDouble(1);
                    return rs.wasNull() ? null : v;
                }, userId).orElse(null);
    }

    /** Rows (user_id, problem_id, verdict, grade, submitted_at) for per-student reports. */
    public List<Object[]> reportRows(Collection<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return List.of();
        }
        return query("SELECT user_id, problem_id, verdict, grade, submitted_at FROM submissions WHERE verdict <> 'PENDING'"
                        + " AND problem_id IN (" + placeholders(problemIds.size()) + ")",
                rs -> new Object[]{rs.getLong(1), rs.getLong(2), rs.getString(3), getIntOrNull(rs, "grade"),
                        getTime(rs, "submitted_at")}, problemIds.toArray());
    }
}
