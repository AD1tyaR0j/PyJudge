package com.pyjudge.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyjudge.db.Database;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.LessonProgress;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.Topic;
import com.pyjudge.util.Times;

/** Topics, lessons, quiz questions, and per-student lesson progress. */
public class CourseDao extends JdbcDao {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String LESSON_SELECT = "SELECT l.*, t.slug AS topic_slug, t.title AS topic_title,"
            + " u.name AS author_name FROM lessons l JOIN topics t ON t.id = l.topic_id"
            + " LEFT JOIN users u ON u.id = l.created_by";

    public CourseDao(Database db) {
        super(db);
    }

    // ---------------------------------------------------------------- mapping

    private static Topic mapTopic(ResultSet rs) throws SQLException {
        Topic t = new Topic();
        t.setId(rs.getLong("id"));
        t.setSlug(rs.getString("slug"));
        t.setTitle(rs.getString("title"));
        t.setLevel(rs.getString("level"));
        t.setSummary(rs.getString("summary"));
        t.setDescription(rs.getString("description"));
        t.setPosition(rs.getInt("position"));
        return t;
    }

    private static Lesson mapLesson(ResultSet rs) throws SQLException {
        Lesson l = new Lesson();
        l.setId(rs.getLong("id"));
        l.setTopicId(rs.getLong("topic_id"));
        l.setTopicSlug(rs.getString("topic_slug"));
        l.setTopicTitle(rs.getString("topic_title"));
        l.setSlug(rs.getString("slug"));
        l.setTitle(rs.getString("title"));
        l.setContent(rs.getString("content"));
        l.setMinutes(rs.getInt("minutes"));
        l.setPracticeTags(rs.getString("practice_tags"));
        l.setPosition(rs.getInt("position"));
        l.setSource(Lesson.Source.valueOf(rs.getString("source").toUpperCase()));
        l.setStatus(ContentStatus.fromCode(rs.getString("status")));
        l.setReviewNote(rs.getString("review_note"));
        l.setReviewedBy(getLongOrNull(rs, "reviewed_by"));
        l.setReviewedAt(getTime(rs, "reviewed_at"));
        l.setCreatedBy(getLongOrNull(rs, "created_by"));
        l.setAuthorName(rs.getString("author_name"));
        l.setCreatedAt(getTime(rs, "created_at"));
        l.setUpdatedAt(getTime(rs, "updated_at"));
        return l;
    }

    private static QuizQuestion mapQuestion(ResultSet rs) throws SQLException {
        QuizQuestion q = new QuizQuestion();
        q.setId(rs.getLong("id"));
        q.setLessonId(rs.getLong("lesson_id"));
        q.setPrompt(rs.getString("prompt"));
        try {
            q.setOptions(JSON.readValue(rs.getString("options_json"), new TypeReference<List<String>>() { }));
        } catch (JsonProcessingException e) {
            throw new SQLException("Bad options_json for question " + q.getId(), e);
        }
        q.setAnswer(rs.getInt("answer"));
        q.setExplanation(rs.getString("explanation"));
        q.setPosition(rs.getInt("position"));
        return q;
    }

    private static LessonProgress mapProgress(ResultSet rs) throws SQLException {
        LessonProgress p = new LessonProgress();
        p.setUserId(rs.getLong("user_id"));
        p.setLessonId(rs.getLong("lesson_id"));
        p.setCompletedAt(getTime(rs, "completed_at"));
        p.setQuizScore(getIntOrNull(rs, "quiz_score"));
        p.setQuizTotal(getIntOrNull(rs, "quiz_total"));
        p.setAttempts(rs.getInt("attempts"));
        return p;
    }

    // ---------------------------------------------------------------- topics

    /** All topics with their lessons (and quiz questions) attached, in course order. */
    public List<Topic> topicsWithLessons() {
        List<Topic> topics = query("SELECT * FROM topics ORDER BY position", CourseDao::mapTopic);
        Map<Long, Topic> byId = new HashMap<>();
        topics.forEach(t -> byId.put(t.getId(), t));
        Map<Long, List<QuizQuestion>> questions = new HashMap<>();
        for (QuizQuestion q : query("SELECT * FROM quiz_questions ORDER BY position, id", CourseDao::mapQuestion)) {
            questions.computeIfAbsent(q.getLessonId(), k -> new ArrayList<>()).add(q);
        }
        for (Lesson l : query(LESSON_SELECT + " ORDER BY l.position, l.id", CourseDao::mapLesson)) {
            l.setQuestions(questions.getOrDefault(l.getId(), new ArrayList<>()));
            Topic t = byId.get(l.getTopicId());
            if (t != null) {
                t.getLessons().add(l);
            }
        }
        return topics;
    }

    public List<Topic> topics() {
        return query("SELECT * FROM topics ORDER BY position", CourseDao::mapTopic);
    }

    public Optional<Topic> topicBySlug(String slug) {
        return queryOne("SELECT * FROM topics WHERE slug = ?", CourseDao::mapTopic, slug);
    }

    public Optional<Topic> topicById(long id) {
        return queryOne("SELECT * FROM topics WHERE id = ?", CourseDao::mapTopic, id);
    }

    public Topic saveTopic(Topic t) {
        Optional<Topic> existing = topicBySlug(t.getSlug());
        if (existing.isPresent()) {
            t.setId(existing.get().getId());
            update("UPDATE topics SET title = ?, level = ?, summary = ?, description = ?, position = ? WHERE id = ?",
                    t.getTitle(), t.getLevel(), t.getSummary(), t.getDescription(), t.getPosition(), t.getId());
        } else {
            t.setId(insert("INSERT INTO topics (slug, title, level, summary, description, position) VALUES (?, ?, ?, ?, ?, ?)",
                    t.getSlug(), t.getTitle(), t.getLevel(), t.getSummary(), t.getDescription(), t.getPosition()));
        }
        return t;
    }

    // ---------------------------------------------------------------- lessons

    private Lesson withQuestions(Lesson l) {
        l.setQuestions(questions(l.getId()));
        return l;
    }

    public Optional<Lesson> lesson(String topicSlug, String lessonSlug) {
        return queryOne(LESSON_SELECT + " WHERE t.slug = ? AND l.slug = ?", CourseDao::mapLesson, topicSlug, lessonSlug)
                .map(this::withQuestions);
    }

    public Optional<Lesson> lessonById(long id) {
        return queryOne(LESSON_SELECT + " WHERE l.id = ?", CourseDao::mapLesson, id).map(this::withQuestions);
    }

    public List<Lesson> lessons(Long authorId) {
        String where = authorId == null ? "" : " WHERE l.created_by = ?";
        Object[] params = authorId == null ? new Object[0] : new Object[]{authorId};
        return query(LESSON_SELECT + where + " ORDER BY t.position, l.position, l.id", CourseDao::mapLesson, params)
                .stream().map(this::withQuestions).toList();
    }

    public List<Lesson> pendingLessons() {
        return query(LESSON_SELECT + " WHERE l.status = 'pending' ORDER BY l.updated_at", CourseDao::mapLesson)
                .stream().map(this::withQuestions).toList();
    }

    public boolean lessonSlugExists(long topicId, String slug, long exceptId) {
        return queryLong("SELECT COUNT(*) FROM lessons WHERE topic_id = ? AND slug = ? AND id <> ?", topicId, slug, exceptId) > 0;
    }

    public long countLessonsInTopic(long topicId) {
        return queryLong("SELECT COUNT(*) FROM lessons WHERE topic_id = ?", topicId);
    }

    public Lesson saveLesson(Lesson l) {
        l.setUpdatedAt(Times.now());
        if (l.getId() == 0) {
            l.setCreatedAt(l.getUpdatedAt());
            l.setId(insert("INSERT INTO lessons (topic_id, slug, title, content, minutes, practice_tags, position, source,"
                            + " status, review_note, reviewed_by, reviewed_at, created_by, created_at, updated_at)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    l.getTopicId(), l.getSlug(), l.getTitle(), l.getContent(), l.getMinutes(), l.getPracticeTags(),
                    l.getPosition(), l.getSource(), l.getStatus(), l.getReviewNote(), l.getReviewedBy(), l.getReviewedAt(),
                    l.getCreatedBy(), l.getCreatedAt(), l.getUpdatedAt()));
        } else {
            update("UPDATE lessons SET topic_id = ?, slug = ?, title = ?, content = ?, minutes = ?, practice_tags = ?,"
                            + " position = ?, status = ?, review_note = ?, reviewed_by = ?, reviewed_at = ?, updated_at = ?"
                            + " WHERE id = ?",
                    l.getTopicId(), l.getSlug(), l.getTitle(), l.getContent(), l.getMinutes(), l.getPracticeTags(),
                    l.getPosition(), l.getStatus(), l.getReviewNote(), l.getReviewedBy(), l.getReviewedAt(),
                    l.getUpdatedAt(), l.getId());
        }
        return l;
    }

    public boolean deleteLesson(long id) {
        return update("DELETE FROM lessons WHERE id = ?", id) == 1;
    }

    public Map<String, Long> lessonCountByStatus() {
        return queryMap("SELECT status, COUNT(*) FROM lessons GROUP BY status", rs -> rs.getString(1), rs -> rs.getLong(2));
    }

    // ---------------------------------------------------------------- quiz questions

    public List<QuizQuestion> questions(long lessonId) {
        return query("SELECT * FROM quiz_questions WHERE lesson_id = ? ORDER BY position, id", CourseDao::mapQuestion, lessonId);
    }

    public QuizQuestion addQuestion(long lessonId, QuizQuestion q) {
        q.setLessonId(lessonId);
        try {
            q.setId(insert("INSERT INTO quiz_questions (lesson_id, prompt, options_json, answer, explanation, position)"
                            + " VALUES (?, ?, ?, ?, ?, ?)", lessonId, q.getPrompt(), JSON.writeValueAsString(q.getOptions()),
                    q.getAnswer(), q.getExplanation(), q.getPosition()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return q;
    }

    /** Replaces all questions of a lesson in one transaction (used by the curriculum sync). */
    public void replaceQuestions(long lessonId, List<QuizQuestion> questions) {
        db.inTransaction(c -> {
            update("DELETE FROM quiz_questions WHERE lesson_id = ?", lessonId);
            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion q = questions.get(i);
                q.setPosition(i);
                addQuestion(lessonId, q);
            }
        });
    }

    public Optional<QuizQuestion> question(long id) {
        return queryOne("SELECT * FROM quiz_questions WHERE id = ?", CourseDao::mapQuestion, id);
    }

    public void deleteQuestion(long id) {
        update("DELETE FROM quiz_questions WHERE id = ?", id);
    }

    // ---------------------------------------------------------------- progress

    public Map<Long, LessonProgress> progressFor(long userId) {
        Map<Long, LessonProgress> out = new HashMap<>();
        query("SELECT * FROM lesson_progress WHERE user_id = ?", CourseDao::mapProgress, userId)
                .forEach(p -> out.put(p.getLessonId(), p));
        return out;
    }

    /**
     * Records a quiz attempt: keeps the best score, counts attempts, and marks the lesson complete
     * the first time it's passed. Read-modify-write happens in one transaction.
     */
    public LessonProgress recordAttempt(long userId, long lessonId, Integer score, Integer total, boolean complete) {
        return db.inTransaction(c -> {
            LessonProgress p = queryOne("SELECT * FROM lesson_progress WHERE user_id = ? AND lesson_id = ?",
                    CourseDao::mapProgress, userId, lessonId).orElse(null);
            if (p == null) {
                insert("INSERT INTO lesson_progress (user_id, lesson_id, attempts, updated_at) VALUES (?, ?, 0, ?)",
                        userId, lessonId, Times.now());
                p = new LessonProgress();
                p.setUserId(userId);
                p.setLessonId(lessonId);
            }
            if (total != null) {
                p.setAttempts(p.getAttempts() + 1);
                if (p.getQuizScore() == null || score > p.getQuizScore()) {
                    p.setQuizScore(score);
                    p.setQuizTotal(total);
                }
            }
            if (complete && p.getCompletedAt() == null) {
                p.setCompletedAt(Times.now());
            }
            update("UPDATE lesson_progress SET completed_at = ?, quiz_score = ?, quiz_total = ?, attempts = ?, updated_at = ?"
                            + " WHERE user_id = ? AND lesson_id = ?", p.getCompletedAt(), p.getQuizScore(), p.getQuizTotal(),
                    p.getAttempts(), Times.now(), userId, lessonId);
            return p;
        });
    }

    public List<java.time.LocalDateTime> completionTimes(long userId) {
        return query("SELECT completed_at FROM lesson_progress WHERE user_id = ? AND completed_at IS NOT NULL",
                rs -> getTime(rs, "completed_at"), userId);
    }

    /** lesson_id -> {completions, quiz points scored, quiz points possible}. */
    public Map<Long, long[]> lessonStats() {
        return queryMap("SELECT lesson_id, SUM(CASE WHEN completed_at IS NOT NULL THEN 1 ELSE 0 END),"
                        + " COALESCE(SUM(quiz_score), 0), COALESCE(SUM(quiz_total), 0) FROM lesson_progress GROUP BY lesson_id",
                rs -> rs.getLong(1), rs -> new long[]{rs.getLong(2), rs.getLong(3), rs.getLong(4)});
    }

    public long totalCompletions() {
        return queryLong("SELECT COUNT(*) FROM lesson_progress WHERE completed_at IS NOT NULL");
    }

    /** user_id -> lessons completed, for instructor reports. */
    public Map<Long, Long> completionsByUser() {
        return queryMap("SELECT user_id, COUNT(*) FROM lesson_progress WHERE completed_at IS NOT NULL GROUP BY user_id",
                rs -> rs.getLong(1), rs -> rs.getLong(2));
    }
}
