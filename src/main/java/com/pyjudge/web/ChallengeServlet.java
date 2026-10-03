package com.pyjudge.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.pyjudge.Services;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.User;
import com.pyjudge.service.ForbiddenException;
import com.pyjudge.service.NotFoundException;
import com.pyjudge.util.Times;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** /challenges, /challenges/new, /challenges/{id}, /challenges/{id}/edit, POST /challenges/{id}/delete */
@WebServlet({"/challenges", "/challenges/*"})
public class ChallengeServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        Services s = services();
        User user = currentUser(req);
        if (path.isEmpty()) {
            req.setAttribute("groups", s.challengeService.grouped(user));
            render(req, resp, "challenges/index");
            return;
        }
        if (path.get(0).equals("new")) {
            requireAuthor(req);
            Challenge c = new Challenge();
            c.setStartAt(Times.now().withMinute(0).withSecond(0));
            c.setEndAt(c.getStartAt().plusDays(7));
            showForm(req, resp, c, true);
            return;
        }
        Challenge c = s.challengeService.get(id(path.get(0)));
        if (path.size() == 2 && path.get(1).equals("edit")) {
            User author = requireAuthor(req);
            if (!author.canEdit(c)) {
                throw new ForbiddenException();
            }
            showForm(req, resp, c, false);
            return;
        }
        req.setAttribute("c", c);
        req.setAttribute("progress", s.challengeService.progress(c, user));
        req.setAttribute("standings", s.challengeService.standings(c));
        req.setAttribute("canEdit", user != null && user.canEdit(c));
        render(req, resp, "challenges/detail");
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Challenge c, boolean isNew)
            throws ServletException, IOException {
        req.setAttribute("c", c);
        req.setAttribute("isNew", isNew);
        req.setAttribute("exercises", services().problems.approved());
        req.setAttribute("selected", c.getProblems().stream().map(p -> p.getId()).toList());
        render(req, resp, "challenges/form");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireAuthor(req);
        Services s = services();
        String[] ids = req.getParameterValues("problem_ids");
        List<Long> problemIds = ids == null ? List.of() : Arrays.stream(ids).map(BaseServlet::id).toList();
        if (path.size() == 1 && path.get(0).equals("new")) {
            Challenge c = s.challengeService.save(null, user, param(req, "title"), param(req, "description"),
                    param(req, "start_at"), param(req, "end_at"), problemIds);
            flash(req, "success", "Challenge created.");
            redirect(req, resp, "/challenges/" + c.getId());
        } else if (path.size() == 2 && path.get(1).equals("edit")) {
            Challenge c = s.challengeService.save(s.challengeService.get(id(path.get(0))), user, param(req, "title"),
                    param(req, "description"), param(req, "start_at"), param(req, "end_at"), problemIds);
            flash(req, "success", "Challenge saved.");
            redirect(req, resp, "/challenges/" + c.getId());
        } else if (path.size() == 2 && path.get(1).equals("delete")) {
            s.challengeService.delete(id(path.get(0)), user);
            flash(req, "info", "Challenge deleted.");
            redirect(req, resp, "/challenges");
        } else {
            throw new NotFoundException("Page");
        }
    }
}
