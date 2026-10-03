package com.pyjudge.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.pyjudge.Services;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.service.ContentService;
import com.pyjudge.service.NotFoundException;
import com.pyjudge.service.Setting;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authoring for instructors and admins:
 * <pre>
 *   /studio/exercises/new                  /studio/exercises/{id}/edit   (+ POST delete, testcases)
 *   /studio/testcases/{id}/delete
 *   /studio/lessons/new                    /studio/lessons/{id}/edit     (+ POST delete, questions)
 *   /studio/questions/{id}/delete
 * </pre>
 */
@WebServlet("/studio/*")
public class StudioServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireAuthor(req);
        Services s = services();
        if (path.size() == 2 && path.get(0).equals("exercises") && path.get(1).equals("new")) {
            Problem p = new Problem();
            p.setTimeLimit(s.settings.number(Setting.DEFAULT_TIME_LIMIT));
            p.setMemoryLimit(s.settings.number(Setting.DEFAULT_MEMORY_LIMIT));
            showExercise(req, resp, p, true);
        } else if (path.size() == 3 && path.get(0).equals("exercises") && path.get(2).equals("edit")) {
            showExercise(req, resp, s.content.editableProblem(user, id(path.get(1))), false);
        } else if (path.size() == 2 && path.get(0).equals("lessons") && path.get(1).equals("new")) {
            Lesson l = new Lesson();
            Long topic = longParam(req, "topic");
            if (topic != null) {
                l.setTopicId(topic);
            }
            showLesson(req, resp, l, true);
        } else if (path.size() == 3 && path.get(0).equals("lessons") && path.get(2).equals("edit")) {
            showLesson(req, resp, s.content.editableLesson(user, id(path.get(1))), false);
        } else {
            throw new NotFoundException("Page");
        }
    }

    private void showExercise(HttpServletRequest req, HttpServletResponse resp, Problem p, boolean isNew)
            throws ServletException, IOException {
        req.setAttribute("problem", p);
        req.setAttribute("isNew", isNew);
        req.setAttribute("difficulties", Difficulty.values());
        render(req, resp, "studio/exercise");
    }

    private void showLesson(HttpServletRequest req, HttpServletResponse resp, Lesson l, boolean isNew)
            throws ServletException, IOException {
        req.setAttribute("lesson", l);
        req.setAttribute("isNew", isNew);
        req.setAttribute("topics", services().courseDao.topics());
        render(req, resp, "studio/lesson");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireAuthor(req);
        ContentService content = services().content;
        String key = String.join("/", path.stream().map(p -> p.matches("\\d+") ? "{id}" : p).toList());
        switch (key) {
            case "exercises/new" -> {
                Problem p = content.createProblem(user, exerciseForm(req));
                flash(req, "success", p.isApproved() ? "Exercise published. Now add test cases."
                        : "Exercise created and sent for approval. Add test cases while you wait.");
                redirect(req, resp, "/studio/exercises/" + p.getId() + "/edit");
            }
            case "exercises/{id}/edit" -> {
                Problem p = content.updateProblem(user, id(path.get(1)), exerciseForm(req));
                flash(req, "success", p.isApproved() ? "Exercise saved." : "Exercise saved and sent for review.");
                redirect(req, resp, "/studio/exercises/" + p.getId() + "/edit");
            }
            case "exercises/{id}/delete" -> {
                content.deleteProblem(user, id(path.get(1)));
                flash(req, "info", "Exercise deleted.");
                redirect(req, resp, user.isAdmin() ? "/admin/content" : "/instructor/exercises");
            }
            case "exercises/{id}/testcases" -> {
                long pid = id(path.get(1));
                content.addTestCase(user, pid, param(req, "input"), param(req, "expected_output"),
                        !param(req, "is_sample").isEmpty());
                flash(req, "success", "Test case added.");
                redirect(req, resp, "/studio/exercises/" + pid + "/edit#tests");
            }
            case "testcases/{id}/delete" -> {
                long pid = content.deleteTestCase(user, id(path.get(1)));
                flash(req, "info", "Test case deleted.");
                redirect(req, resp, "/studio/exercises/" + pid + "/edit#tests");
            }
            case "lessons/new" -> {
                Lesson l = content.createLesson(user, lessonForm(req));
                flash(req, "success", l.isApproved() ? "Lesson published. Add a few quiz questions."
                        : "Lesson created and sent for approval. Add quiz questions while you wait.");
                redirect(req, resp, "/studio/lessons/" + l.getId() + "/edit");
            }
            case "lessons/{id}/edit" -> {
                Lesson l = content.updateLesson(user, id(path.get(1)), lessonForm(req));
                flash(req, "success", l.isApproved() ? "Lesson saved." : "Lesson saved and sent for review.");
                redirect(req, resp, "/studio/lessons/" + l.getId() + "/edit");
            }
            case "lessons/{id}/delete" -> {
                content.deleteLesson(user, id(path.get(1)));
                flash(req, "info", "Lesson deleted.");
                redirect(req, resp, "/instructor/lessons");
            }
            case "lessons/{id}/questions" -> {
                long lid = id(path.get(1));
                String[] options = req.getParameterValues("option");
                content.addQuestion(user, lid, param(req, "prompt"), options == null ? List.of() : Arrays.asList(options),
                        longParam(req, "answer") == null ? null : longParam(req, "answer").intValue(), param(req, "explanation"));
                flash(req, "success", "Question added.");
                redirect(req, resp, "/studio/lessons/" + lid + "/edit#quiz");
            }
            case "questions/{id}/delete" -> {
                long lid = content.deleteQuestion(user, id(path.get(1)));
                flash(req, "info", "Question deleted.");
                redirect(req, resp, "/studio/lessons/" + lid + "/edit#quiz");
            }
            default -> throw new NotFoundException("Page");
        }
    }

    private static ContentService.ProblemForm exerciseForm(HttpServletRequest req) {
        return new ContentService.ProblemForm(param(req, "title"), param(req, "description"), param(req, "difficulty"),
                param(req, "time_limit"), param(req, "memory_limit"), param(req, "tags"), param(req, "starter_code"));
    }

    private static ContentService.LessonForm lessonForm(HttpServletRequest req) {
        return new ContentService.LessonForm(param(req, "title"), param(req, "topic_id"), param(req, "minutes"),
                param(req, "practice_tags"), param(req, "content"));
    }
}
