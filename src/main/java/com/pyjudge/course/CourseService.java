package com.pyjudge.course;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.pyjudge.dao.CourseDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.LessonProgress;
import com.pyjudge.model.Problem;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.Topic;
import com.pyjudge.model.User;
import com.pyjudge.service.ForbiddenException;
import com.pyjudge.service.NotFoundException;
import com.pyjudge.service.ValidationException;

/** The learning side of the platform: course map, lessons, quizzes, progress, and the curriculum sync. */
public class CourseService {

    /** Fraction of quiz answers needed to complete a lesson. */
    public static final double PASS_RATIO = 0.5;

    private final CourseDao course;
    private final ProblemDao problems;

    public CourseService(CourseDao course, ProblemDao problems) {
        this.course = course;
        this.problems = problems;
    }

    /** Topics with approved lessons only, plus the viewer's progress (empty for guests). */
    public CourseOverview overview(User user) {
        Map<Long, LessonProgress> progress = user == null ? Map.of() : course.progressFor(user.getId());
        List<CourseOverview.TopicProgress> topics = new ArrayList<>();
        Lesson next = null;
        for (Topic t : course.topicsWithLessons()) {
            List<Lesson> lessons = t.getApprovedLessons();
            if (lessons.isEmpty()) {
                continue;
            }
            int done = 0;
            for (Lesson l : lessons) {
                LessonProgress p = progress.get(l.getId());
                if (p != null && p.isCompleted()) {
                    done++;
                } else if (next == null) {
                    next = l;
                }
            }
            topics.add(new CourseOverview.TopicProgress(t, lessons, done));
        }
        return new CourseOverview(topics, progress, next);
    }

    public CourseOverview.TopicProgress topic(String slug, User user) {
        return overview(user).getTopics().stream().filter(t -> t.getTopic().getSlug().equals(slug)).findFirst()
                .orElseThrow(() -> new NotFoundException("Topic"));
    }

    public Lesson lesson(String topicSlug, String lessonSlug, User user) {
        Lesson l = course.lesson(topicSlug, lessonSlug).orElseThrow(() -> new NotFoundException("Lesson"));
        if (!l.isVisibleTo(user)) {
            throw new NotFoundException("Lesson");
        }
        return l;
    }

    /** [previous, next] approved lessons across the whole course (either may be null). */
    public Lesson[] neighbours(Lesson lesson) {
        List<Lesson> ordered = course.topicsWithLessons().stream().flatMap(t -> t.getApprovedLessons().stream()).toList();
        int i = -1;
        for (int k = 0; k < ordered.size(); k++) {
            if (ordered.get(k).getId() == lesson.getId()) {
                i = k;
            }
        }
        if (i < 0) {
            return new Lesson[]{null, null};
        }
        return new Lesson[]{i > 0 ? ordered.get(i - 1) : null, i + 1 < ordered.size() ? ordered.get(i + 1) : null};
    }

    /** The result of checking a quiz: score, pass/fail, and per-question feedback. */
    public record QuizResult(int score, int total, boolean passed, boolean saved, boolean completed,
                             Integer best, List<QuestionResult> results) { }

    public record QuestionResult(long id, Integer chosen, int answer, boolean correct, String explanationHtml) { }

    /** Grades a quiz. Guests get feedback; logged-in users also get their progress recorded. */
    public QuizResult gradeQuiz(Lesson lesson, Map<Long, Integer> answers, User user) {
        List<QuizQuestion> questions = lesson.getQuestions();
        if (questions.isEmpty()) {
            throw new ValidationException("This lesson has no quiz.");
        }
        List<QuestionResult> results = new ArrayList<>();
        int score = 0;
        for (QuizQuestion q : questions) {
            Integer chosen = answers.get(q.getId());
            boolean correct = q.isCorrect(chosen);
            if (correct) {
                score++;
            }
            results.add(new QuestionResult(q.getId(), chosen, q.getAnswer(), correct,
                    q.getExplanation().isBlank() ? "" : MarkdownRenderer.renderInline(q.getExplanation())));
        }
        int total = questions.size();
        boolean passed = score >= total * PASS_RATIO;
        if (user == null) {
            return new QuizResult(score, total, passed, false, false, null, results);
        }
        LessonProgress p = course.recordAttempt(user.getId(), lesson.getId(), score, total, passed);
        return new QuizResult(score, total, passed, true, p.isCompleted(), p.getQuizScore(), results);
    }

    /** Lessons without a quiz are completed by reading them. */
    public void markComplete(Lesson lesson, User user) {
        if (!lesson.getQuestions().isEmpty()) {
            throw new ValidationException("Pass the quiz to complete this lesson.");
        }
        course.recordAttempt(user.getId(), lesson.getId(), null, null, true);
    }

    /** Approved exercises sharing a tag with the lesson: unsolved and easier ones first. */
    public List<Problem> practiceFor(Lesson lesson, Set<Long> solved, int limit) {
        Set<String> tags = new HashSet<>(lesson.getPracticeTagList());
        if (tags.isEmpty()) {
            return List.of();
        }
        return problems.approved().stream()
                .filter(p -> p.getTagList().stream().anyMatch(tags::contains))
                .sorted(Comparator.comparing((Problem p) -> solved.contains(p.getId()))
                        .thenComparing(Problem::getDifficulty).thenComparing(Problem::getId))
                .limit(limit).toList();
    }

    public List<Problem> practiceForTopic(CourseOverview.TopicProgress topic, Set<Long> solved, int limit) {
        List<Problem> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Lesson l : topic.getLessons()) {
            for (Problem p : practiceFor(l, solved, 3)) {
                if (seen.add(p.getId())) {
                    out.add(p);
                }
            }
        }
        return out.stream().limit(limit).toList();
    }

    // ---------------------------------------------------------------- curriculum sync

    public record SyncReport(int topicsCreated, int lessonsCreated, int lessonsUpdated) { }

    /** Creates or refreshes built-in topics and lessons. Lessons written by instructors are never touched. */
    public SyncReport syncCurriculum() {
        int topicsCreated = 0;
        int created = 0;
        int updated = 0;
        for (CurriculumLoader.TopicSpec spec : CurriculumLoader.load()) {
            boolean isNew = course.topicBySlug(spec.slug()).isEmpty();
            Topic t = new Topic();
            t.setSlug(spec.slug());
            t.setTitle(spec.title());
            t.setLevel(spec.level());
            t.setSummary(spec.summary());
            t.setDescription(spec.description());
            t.setPosition(spec.position());
            course.saveTopic(t);
            if (isNew) {
                topicsCreated++;
            }
            for (CurriculumLoader.LessonSpec ls : spec.lessons()) {
                Optional<Lesson> existing = course.lesson(spec.slug(), ls.slug());
                if (existing.isPresent() && !existing.get().isBuiltin()) {
                    continue;
                }
                Lesson l = existing.orElseGet(Lesson::new);
                if (existing.isEmpty()) {
                    l.setTopicId(t.getId());
                    l.setSlug(ls.slug());
                    l.setSource(Lesson.Source.BUILTIN);
                    l.setStatus(ContentStatus.APPROVED);
                    created++;
                } else {
                    updated++;
                }
                l.setTitle(ls.title());
                l.setContent(ls.content());
                l.setMinutes(ls.minutes());
                l.setPracticeTags(ls.practice());
                l.setPosition(ls.position());
                course.saveLesson(l);
                course.replaceQuestions(l.getId(), ls.quiz());
            }
        }
        return new SyncReport(topicsCreated, created, updated);
    }

    /** Guard for editing pages: built-in lessons are admin-only. */
    public Lesson editableLesson(long id, User user) {
        Lesson l = course.lessonById(id).orElseThrow(() -> new NotFoundException("Lesson"));
        if (!l.isEditableBy(user)) {
            throw new ForbiddenException();
        }
        return l;
    }
}
