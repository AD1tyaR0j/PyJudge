package com.pyjudge.web;

import java.io.IOException;
import java.util.List;

import com.pyjudge.Services;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** A student's submission history and the detail page (where instructors also grade). */
@WebServlet({"/submissions", "/submissions/*"})
public class SubmissionServlet extends BaseServlet {

    private static final int PAGE = 25;

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        Services s = services();
        User user = requireUser(req);
        if (path.isEmpty()) {
            int page = Math.max(1, intParam(req, "page", 1));
            long total = s.submissions.countByUser(user.getId());
            req.setAttribute("submissions", s.submissions.byUser(user.getId(), PAGE, (page - 1) * PAGE));
            req.setAttribute("page", page);
            req.setAttribute("pages", Math.max(1, (total + PAGE - 1) / PAGE));
            render(req, resp, "submissions/list");
            return;
        }
        Submission sub = s.submissionService.viewable(id(path.get(0)), user);
        req.setAttribute("sub", sub);
        req.setAttribute("isOwner", sub.getUserId() == user.getId());
        req.setAttribute("canGrade", s.submissionService.canGrade(sub, user));
        render(req, resp, "submissions/detail");
    }

    /** POST /submissions/{id}/grade */
    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireAuthor(req);
        if (path.size() != 2 || !path.get(1).equals("grade")) {
            throw new com.pyjudge.service.NotFoundException("Page");
        }
        long id = id(path.get(0));
        services().submissionService.grade(id, param(req, "grade"), param(req, "feedback"), user);
        flash(req, "success", "Grade saved and the student was notified.");
        if (!param(req, "next").isEmpty()) {
            List<Long> ids = services().submissionService.reviewableProblemIds(user);
            List<Submission> queue = services().submissions.forProblems(ids, false, null, 1, 0);
            if (!queue.isEmpty()) {
                redirect(req, resp, "/submissions/" + queue.get(0).getId());
                return;
            }
        }
        redirect(req, resp, "/submissions/" + id);
    }
}
