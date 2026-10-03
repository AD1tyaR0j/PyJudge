package com.pyjudge.service;

import java.util.List;

import com.pyjudge.dao.MessageDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.db.Database;
import com.pyjudge.judge.JudgeService;
import com.pyjudge.judge.RunResult;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;

/** Running code, submitting solutions, and instructor grading. */
public class SubmissionService {

    public static final int MAX_CODE = 64 * 1024;
    public static final int MAX_INPUT = 64 * 1024;

    private final Database db;
    private final SubmissionDao submissions;
    private final ProblemDao problems;
    private final MessageDao messages;
    private final JudgeService judge;
    private final SettingsService settings;

    public SubmissionService(Database db, SubmissionDao submissions, ProblemDao problems, MessageDao messages,
                             JudgeService judge, SettingsService settings) {
        this.db = db;
        this.submissions = submissions;
        this.problems = problems;
        this.messages = messages;
        this.judge = judge;
        this.settings = settings;
    }

    private static void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("Code cannot be empty.");
        }
        if (code.length() > MAX_CODE) {
            throw new ValidationException("Code is too long.");
        }
    }

    public Problem visibleProblem(long id, User user) {
        Problem p = problems.findById(id).orElseThrow(() -> new NotFoundException("Exercise"));
        if (!p.isVisibleTo(user)) {
            throw new NotFoundException("Exercise");
        }
        return p;
    }

    /** "Run" with custom input. Uses the exercise's limits when one is given, otherwise the defaults. */
    public RunResult run(String code, String stdin, Long problemId, User user) {
        validateCode(code);
        if (stdin != null && stdin.length() > MAX_INPUT) {
            throw new ValidationException("Input is too long.");
        }
        int time = settings.number(Setting.DEFAULT_TIME_LIMIT);
        int memory = settings.number(Setting.DEFAULT_MEMORY_LIMIT);
        if (problemId != null) {
            Problem p = problems.findById(problemId).orElse(null);
            if (p != null && p.isVisibleTo(user)) {
                time = p.getTimeLimit();
                memory = p.getMemoryLimit();
            }
        }
        return judge.runInteractive(code, stdin == null ? "" : stdin, time, memory);
    }

    /** Saves a submission and queues it for the judge. The verdict arrives asynchronously. */
    public Submission submit(long problemId, String code, User user) {
        validateCode(code);
        Problem p = visibleProblem(problemId, user);
        if (submissions.countPendingByUser(user.getId()) >= settings.number(Setting.MAX_PENDING_SUBMISSIONS)) {
            throw new ValidationException("You already have submissions being judged. Please wait.");
        }
        Submission s = new Submission();
        s.setUserId(user.getId());
        s.setProblemId(p.getId());
        s.setCode(code);
        s.setTotal(p.getTestCases().size());
        submissions.save(s);
        judge.enqueue(s.getId());
        return submissions.findById(s.getId()).orElseThrow();
    }

    /** Owners can always see their submission; authors of the exercise (and admins) can review it. */
    public boolean canView(Submission s, User user) {
        if (user == null) {
            return false;
        }
        if (s.getUserId() == user.getId() || user.isAdmin()) {
            return true;
        }
        return user.canAuthor() && s.getProblemAuthorId() != null && s.getProblemAuthorId() == user.getId();
    }

    public boolean canGrade(Submission s, User user) {
        return user != null && (user.isAdmin()
                || (user.canAuthor() && s.getProblemAuthorId() != null && s.getProblemAuthorId() == user.getId()));
    }

    public Submission viewable(long id, User user) {
        Submission s = submissions.findById(id).orElseThrow(() -> new NotFoundException("Submission"));
        if (!canView(s, user)) {
            throw new ForbiddenException();
        }
        return s;
    }

    /** Saves the grade and notifies the student, in one transaction. */
    public void grade(long submissionId, String gradeText, String feedback, User grader) {
        Submission s = submissions.findById(submissionId).orElseThrow(() -> new NotFoundException("Submission"));
        if (!canGrade(s, grader)) {
            throw new ForbiddenException();
        }
        int grade;
        try {
            grade = Integer.parseInt(gradeText == null ? "" : gradeText.trim());
        } catch (NumberFormatException e) {
            grade = -1;
        }
        if (grade < 0 || grade > 100) {
            throw new ValidationException("Grade must be a whole number from 0 to 100.");
        }
        String fb = feedback == null || feedback.isBlank() ? null : feedback.trim();
        boolean first = !s.isGraded();
        int g = grade;
        db.inTransaction(c -> {
            submissions.saveGrade(s.getId(), g, fb, grader.getId());
            String body = "Your submission #" + s.getId() + " for \"" + s.getProblemTitle() + "\" was "
                    + (first ? "graded" : "regraded") + ": " + g + "/100." + (fb == null ? "" : "\n\n" + fb);
            messages.send(grader.getId(), s.getUserId(), body, s.getId());
        });
    }

    /** Problems an author's queue covers: their own exercises, or every exercise for admins. */
    public List<Long> reviewableProblemIds(User user) {
        return problems.search(null, null, null, null, user.isAdmin() ? null : user.getId()).stream()
                .map(Problem::getId).toList();
    }
}
