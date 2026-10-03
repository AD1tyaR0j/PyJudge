package com.pyjudge.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pyjudge.Services;
import com.pyjudge.course.CourseOverview;
import com.pyjudge.course.CourseService;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.User;
import com.pyjudge.service.NotFoundException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The Python course:
 * <pre>
 *   GET  /learn                          course map
 *   GET  /learn/{topic}                  topic page
 *   GET  /learn/{topic}/{lesson}         lesson with playgrounds and quiz
 *   POST /learn/{topic}/{lesson}/quiz    grade quiz (JSON)
 *   POST /learn/{topic}/{lesson}/complete  mark a quiz-less lesson as read (JSON)
 * </pre>
 */
@WebServlet({"/learn", "/learn/*"})
public class LearnServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        Services s = services();
        User user = currentUser(req);
        CourseOverview course = s.course.overview(user);
        req.setAttribute("course", course);
        Set<Long> solved = user == null ? Set.of() : s.submissions.solvedProblemIds(user.getId());
        req.setAttribute("solved", solved);

        if (path.isEmpty()) {
            render(req, resp, "learn/index");
            return;
        }
        if (path.size() == 1) {
            CourseOverview.TopicProgress topic = s.course.topic(path.get(0), user);
            req.setAttribute("info", topic);
            req.setAttribute("practice", s.course.practiceForTopic(topic, solved, 6));
            render(req, resp, "learn/topic");
            return;
        }
        Lesson lesson = s.course.lesson(path.get(0), path.get(1), user);
        Lesson[] around = s.course.neighbours(lesson);
        CourseOverview.TopicProgress topic = course.getTopics().stream()
                .filter(t -> t.getTopic().getId() == lesson.getTopicId()).findFirst().orElse(null);
        req.setAttribute("lesson", lesson);
        req.setAttribute("topic", topic);
        req.setAttribute("siblings", topic != null && lesson.isApproved() ? topic.getLessons() : List.of(lesson));
        req.setAttribute("progress", course.getProgress().get(lesson.getId()));
        req.setAttribute("prev", around[0]);
        req.setAttribute("next", around[1]);
        req.setAttribute("practice", s.course.practiceFor(lesson, solved, 4));
        req.setAttribute("canEdit", lesson.isEditableBy(user));
        req.setAttribute("passPercent", (int) (CourseService.PASS_RATIO * 100));
        render(req, resp, "learn/lesson");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        if (path.size() != 3) {
            throw new NotFoundException("Page");
        }
        Services s = services();
        User user = currentUser(req);
        Lesson lesson = s.course.lesson(path.get(0), path.get(1), user);
        switch (path.get(2)) {
            case "quiz" -> {
                Map<Long, Integer> answers = new HashMap<>();
                for (QuizQuestion q : lesson.getQuestions()) {
                    Long chosen = longParam(req, "q" + q.getId());
                    if (chosen != null) {
                        answers.put(q.getId(), chosen.intValue());
                    }
                }
                CourseService.QuizResult r = s.course.gradeQuiz(lesson, answers, user);
                Lesson next = s.course.neighbours(lesson)[1];
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("score", r.score());
                out.put("total", r.total());
                out.put("passed", r.passed());
                out.put("saved", r.saved());
                out.put("completed", r.completed());
                out.put("best", r.best());
                out.put("results", r.results());
                out.put("nextUrl", req.getContextPath() + (next == null ? "/learn" : next.getUrl()));
                json(resp, 200, out);
            }
            case "complete" -> {
                if (user == null) {
                    json(resp, 401, Map.of("error", "Log in to save your progress."));
                    return;
                }
                s.course.markComplete(lesson, user);
                json(resp, 200, Map.of("completed", true));
            }
            default -> throw new NotFoundException("Page");
        }
    }
}
