package com.pyjudge.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.pyjudge.dao.CourseDao;
import com.pyjudge.dao.MessageDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.db.Database;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.Problem;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.Reviewable;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.Topic;
import com.pyjudge.model.User;
import com.pyjudge.util.Slugs;
import com.pyjudge.util.Times;

/**
 * Creating, editing, and reviewing exercises and lessons.
 * Rules (shared by both kinds of content):
 * <ul>
 *   <li>instructor content starts as pending when approval is required; admin content publishes at once;</li>
 *   <li>an instructor editing approved/rejected content sends it back for review;</li>
 *   <li>a review decision and the message telling the author are saved in one transaction.</li>
 * </ul>
 */
public class ContentService {

    private final Database db;
    private final ProblemDao problems;
    private final CourseDao course;
    private final MessageDao messages;
    private final SettingsService settings;

    public ContentService(Database db, ProblemDao problems, CourseDao course, MessageDao messages, SettingsService settings) {
        this.db = db;
        this.problems = problems;
        this.course = course;
        this.messages = messages;
        this.settings = settings;
    }

    private boolean needsReview(User author) {
        return !author.isAdmin() && settings.flag(Setting.REQUIRE_APPROVAL);
    }

    // ---------------------------------------------------------------- exercises

    /** Form fields for an exercise. */
    public record ProblemForm(String title, String description, String difficulty, String timeLimit, String memoryLimit,
                              String tags, String starterCode) { }

    private void apply(Problem p, ProblemForm f) {
        List<String> errors = new ArrayList<>();
        if (f.title() == null || f.title().isBlank()) {
            errors.add("Title is required.");
        }
        if (f.description() == null || f.description().isBlank()) {
            errors.add("Description is required.");
        }
        Difficulty difficulty = null;
        try {
            difficulty = Difficulty.fromLabel(f.difficulty());
        } catch (IllegalArgumentException | NullPointerException e) {
            errors.add("Pick a valid difficulty.");
        }
        int time = parse(f.timeLimit(), -1);
        int memory = parse(f.memoryLimit(), -1);
        if (time < 1 || time > 10) {
            errors.add("Time limit must be 1–10 seconds.");
        }
        if (memory < 16 || memory > 1024) {
            errors.add("Memory limit must be 16–1024 MB.");
        }
        ValidationException.throwIfAny(errors);
        p.setTitle(f.title().trim());
        p.setDescription(f.description());
        p.setDifficulty(difficulty);
        p.setTimeLimit(time);
        p.setMemoryLimit(memory);
        p.setTags(cleanTags(f.tags()));
        p.setStarterCode(f.starterCode() == null || f.starterCode().isBlank() ? Problem.DEFAULT_STARTER : f.starterCode());
    }

    public Problem createProblem(User author, ProblemForm form) {
        requireAuthor(author);
        Problem p = new Problem();
        apply(p, form);
        p.setSlug(Slugs.unique(p.getTitle(), s -> problems.slugExists(s, 0)));
        p.setCreatedBy(author.getId());
        if (needsReview(author)) {
            p.setStatus(ContentStatus.PENDING);
        } else {
            approve(p, author);
        }
        return problems.save(p);
    }

    public Problem updateProblem(User editor, long id, ProblemForm form) {
        Problem p = editableProblem(editor, id);
        apply(p, form);
        p.setSlug(Slugs.unique(p.getTitle(), s -> problems.slugExists(s, p.getId())));
        if (needsReview(editor) && p.getStatus() != ContentStatus.PENDING) {
            p.setStatus(ContentStatus.PENDING);
            p.setReviewNote(null);
        }
        return problems.save(p);
    }

    public Problem editableProblem(User user, long id) {
        Problem p = problems.findById(id).orElseThrow(() -> new NotFoundException("Exercise"));
        if (!user.canEdit(p)) {
            throw new ForbiddenException();
        }
        return p;
    }

    public void deleteProblem(User user, long id) {
        editableProblem(user, id);
        problems.delete(id);
    }

    public TestCase addTestCase(User user, long problemId, String input, String expected, boolean sample) {
        editableProblem(user, problemId);
        if (expected == null || expected.isBlank()) {
            throw new ValidationException("Expected output is required.");
        }
        return problems.addTestCase(problemId, new TestCase(input == null ? "" : input, expected, sample));
    }

    public long deleteTestCase(User user, long testCaseId) {
        TestCase tc = problems.findTestCase(testCaseId).orElseThrow(() -> new NotFoundException("Test case"));
        editableProblem(user, tc.getProblemId());
        problems.deleteTestCase(testCaseId);
        return tc.getProblemId();
    }

    /** Admin decision on an exercise. Approval requires test cases (otherwise it can't be judged). */
    public Problem reviewProblem(User admin, long id, boolean approve, String note) {
        requireAdmin(admin);
        Problem p = problems.findById(id).orElseThrow(() -> new NotFoundException("Exercise"));
        if (approve && p.getTestCases().isEmpty()) {
            throw new ValidationException("This exercise has no test cases yet, so it can't be judged.");
        }
        review(p, admin, approve, note, "exercise", p.getTitle(), () -> problems.save(p));
        return p;
    }

    // ---------------------------------------------------------------- lessons

    public record LessonForm(String title, String topicId, String minutes, String practiceTags, String content) { }

    private void apply(Lesson l, LessonForm f) {
        List<String> errors = new ArrayList<>();
        if (f.title() == null || f.title().isBlank()) {
            errors.add("Title is required.");
        }
        if (f.content() == null || f.content().isBlank()) {
            errors.add("Lesson content is required.");
        }
        Topic topic = course.topicById(parse(f.topicId(), -1)).orElse(null);
        if (topic == null) {
            errors.add("Pick a topic.");
        }
        int minutes = parse(f.minutes(), -1);
        if (minutes < 1 || minutes > 120) {
            errors.add("Reading time must be 1–120 minutes.");
        }
        ValidationException.throwIfAny(errors);
        l.setTitle(f.title().trim());
        l.setContent(f.content());
        l.setMinutes(minutes);
        l.setPracticeTags(cleanTags(f.practiceTags()));
        l.setTopicId(topic.getId());
    }

    public Lesson createLesson(User author, LessonForm form) {
        requireAuthor(author);
        Lesson l = new Lesson();
        apply(l, form);
        l.setSlug(Slugs.unique(l.getTitle(), s -> course.lessonSlugExists(l.getTopicId(), s, 0)));
        l.setSource(Lesson.Source.CUSTOM);
        l.setCreatedBy(author.getId());
        l.setPosition(1000 + (int) course.countLessonsInTopic(l.getTopicId()));
        if (needsReview(author)) {
            l.setStatus(ContentStatus.PENDING);
        } else {
            approve(l, author);
        }
        return course.saveLesson(l);
    }

    public Lesson updateLesson(User editor, long id, LessonForm form) {
        Lesson l = editableLesson(editor, id);
        apply(l, form);
        if (course.lessonSlugExists(l.getTopicId(), l.getSlug(), l.getId())) {
            l.setSlug(Slugs.unique(l.getTitle(), s -> course.lessonSlugExists(l.getTopicId(), s, l.getId())));
        }
        if (needsReview(editor) && l.getStatus() != ContentStatus.PENDING) {
            l.setStatus(ContentStatus.PENDING);
            l.setReviewNote(null);
        }
        return course.saveLesson(l);
    }

    public Lesson editableLesson(User user, long id) {
        Lesson l = course.lessonById(id).orElseThrow(() -> new NotFoundException("Lesson"));
        if (!l.isEditableBy(user)) {
            throw new ForbiddenException();
        }
        return l;
    }

    public void deleteLesson(User user, long id) {
        Lesson l = editableLesson(user, id);
        if (l.isBuiltin()) {
            throw new ValidationException("Built-in course lessons live in the curriculum files and can't be deleted here.");
        }
        course.deleteLesson(id);
    }

    /**
     * Adds a quiz question. {@code options} may contain blanks (unused form rows);
     * {@code answerIndex} refers to the original row and is remapped after blanks are dropped.
     */
    public QuizQuestion addQuestion(User user, long lessonId, String prompt, List<String> options, Integer answerIndex,
                                    String explanation) {
        Lesson l = editableLesson(user, lessonId);
        List<Integer> filled = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i) != null && !options.get(i).isBlank()) {
                filled.add(i);
            }
        }
        if (prompt == null || prompt.isBlank() || filled.size() < 2) {
            throw new ValidationException("A question needs a prompt and at least two options.");
        }
        if (answerIndex == null || !filled.contains(answerIndex)) {
            throw new ValidationException("Mark which (non-empty) option is correct.");
        }
        QuizQuestion q = new QuizQuestion(prompt.trim(), filled.stream().map(i -> options.get(i).trim()).toList(),
                filled.indexOf(answerIndex), explanation);
        q.setPosition(l.getQuestions().size());
        return course.addQuestion(lessonId, q);
    }

    public long deleteQuestion(User user, long questionId) {
        QuizQuestion q = course.question(questionId).orElseThrow(() -> new NotFoundException("Question"));
        editableLesson(user, q.getLessonId());
        course.deleteQuestion(questionId);
        return q.getLessonId();
    }

    public Lesson reviewLesson(User admin, long id, boolean approve, String note) {
        requireAdmin(admin);
        Lesson l = course.lessonById(id).orElseThrow(() -> new NotFoundException("Lesson"));
        review(l, admin, approve, note, "lesson", l.getTitle(), () -> course.saveLesson(l));
        return l;
    }

    // ---------------------------------------------------------------- shared review logic

    private void approve(Problem p, User reviewer) {
        p.setStatus(ContentStatus.APPROVED);
        p.setReviewedBy(reviewer.getId());
        p.setReviewedAt(Times.now());
    }

    private void approve(Lesson l, User reviewer) {
        l.setStatus(ContentStatus.APPROVED);
        l.setReviewedBy(reviewer.getId());
        l.setReviewedAt(Times.now());
    }

    /** Sets the decision on the content, saves it, and messages the author, all or nothing. */
    private void review(Reviewable content, User admin, boolean approve, String note, String kind, String title,
                        Runnable save) {
        String cleanNote = note == null ? "" : note.trim();
        if (!approve && cleanNote.isEmpty()) {
            throw new ValidationException("Add a note so the instructor knows what to change.");
        }
        ContentStatus status = approve ? ContentStatus.APPROVED : ContentStatus.REJECTED;
        if (content instanceof Problem p) {
            p.setStatus(status);
            p.setReviewNote(cleanNote.isEmpty() ? null : cleanNote);
            p.setReviewedBy(admin.getId());
            p.setReviewedAt(Times.now());
        } else if (content instanceof Lesson l) {
            l.setStatus(status);
            l.setReviewNote(cleanNote.isEmpty() ? null : cleanNote);
            l.setReviewedBy(admin.getId());
            l.setReviewedAt(Times.now());
        }
        db.inTransaction(c -> {
            save.run();
            Long author = content.getCreatedBy();
            if (author != null && author != admin.getId()) {
                String body = "Your " + kind + " \"" + title + "\" was "
                        + (approve ? "approved and published." : "returned for changes.")
                        + (cleanNote.isEmpty() ? "" : "\n\nReviewer note: " + cleanNote);
                messages.send(admin.getId(), author, body, null);
            }
        });
    }

    // ---------------------------------------------------------------- helpers

    private static void requireAuthor(User user) {
        if (user == null || !user.canAuthor()) {
            throw new ForbiddenException();
        }
    }

    private static void requireAdmin(User user) {
        if (user == null || !user.isAdmin()) {
            throw new ForbiddenException();
        }
    }

    static int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static String cleanTags(String raw) {
        return raw == null ? "" : Arrays.stream(raw.split(",")).map(String::trim).filter(t -> !t.isEmpty())
                .collect(Collectors.joining(", "));
    }
}
