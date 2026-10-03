package com.pyjudge.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.pyjudge.Services;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.service.NotFoundException;
import com.pyjudge.service.StatsService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Instructor area (admins see everything):
 * /instructor, /instructor/exercises, /instructor/lessons, /instructor/submissions, /instructor/reports
 */
@WebServlet({"/instructor", "/instructor/*"})
public class InstructorServlet extends BaseServlet {

    private static final int PAGE = 25;

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireAuthor(req);
        Services s = services();
        Long authorFilter = user.isAdmin() ? null : user.getId();
        List<Problem> mine = s.problems.search(null, null, null, null, authorFilter);
        List<Long> ids = mine.stream().map(Problem::getId).toList();
        String route = path.isEmpty() ? "" : path.get(0);

        switch (route) {
            case "" -> {
                req.setAttribute("exercises", mine);
                req.setAttribute("approved", mine.stream().filter(Problem::isApproved).count());
                req.setAttribute("pending", mine.stream().filter(p -> p.getStatus() == ContentStatus.PENDING).count());
                req.setAttribute("rejected", mine.stream().filter(p -> p.getStatus() == ContentStatus.REJECTED).toList());
                req.setAttribute("toGrade", s.submissions.countForProblems(ids, false));
                req.setAttribute("students", s.submissions.countDistinctStudents(ids));
                req.setAttribute("queue", s.submissions.forProblems(ids, false, null, 8, 0));
                req.setAttribute("inbox", s.messaging.inbox(user, 5));
                req.setAttribute("senders", s.messaging.conversations(user));
                req.setAttribute("lessonCount", s.courseDao.lessons(authorFilter).size());
                render(req, resp, "instructor/dashboard");
            }
            case "exercises" -> {
                req.setAttribute("exercises", mine);
                req.setAttribute("tests", s.problems.testCaseCounts());
                req.setAttribute("acceptance", s.submissions.acceptanceStats());
                render(req, resp, "instructor/exercises");
            }
            case "lessons" -> {
                req.setAttribute("lessons", s.courseDao.lessons(authorFilter));
                req.setAttribute("stats", s.courseDao.lessonStats());
                render(req, resp, "instructor/lessons");
            }
            case "submissions" -> {
                String show = param(req, "show").isEmpty() ? "ungraded" : param(req, "show");
                Boolean graded = switch (show) {
                    case "graded" -> true;
                    case "all" -> null;
                    default -> false;
                };
                Long exercise = longParam(req, "exercise");
                int page = Math.max(1, intParam(req, "page", 1));
                req.setAttribute("submissions", s.submissions.forProblems(ids, graded, exercise, PAGE, (page - 1) * PAGE));
                req.setAttribute("show", show);
                req.setAttribute("exercise", exercise);
                req.setAttribute("exercises", mine);
                req.setAttribute("page", page);
                req.setAttribute("hasMore", s.submissions.forProblems(ids, graded, exercise, 1, page * PAGE).size() == 1);
                render(req, resp, "instructor/submissions");
            }
            case "reports" -> {
                List<Problem> live = mine.stream().filter(Problem::isApproved).toList();
                List<StatsService.ExerciseReport> exerciseRows = s.stats.exerciseReports(live);
                List<StatsService.ExerciseReport> attempted = exerciseRows.stream().filter(r -> r.getAcceptance() != null).toList();
                req.setAttribute("exerciseRows", exerciseRows);
                req.setAttribute("studentRows", s.stats.studentReports(live));
                req.setAttribute("chart", Map.of(
                        "labels", attempted.stream().map(r -> r.getProblem().getTitle()).toList(),
                        "acceptance", attempted.stream().map(StatsService.ExerciseReport::getAcceptance).toList()));
                render(req, resp, "instructor/reports");
            }
            default -> throw new NotFoundException("Page");
        }
    }

    /** Lessons visible in the lessons list for this user (helper for views). */
    static boolean editable(Lesson l, User u) {
        return l.isEditableBy(u);
    }
}
