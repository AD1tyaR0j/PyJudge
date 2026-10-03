package com.pyjudge.web;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.pyjudge.Services;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.service.NotFoundException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Exercise list (/problems) and the solving workspace (/problems/{slug}). */
@WebServlet({"/problems", "/problems/*"})
public class ExerciseServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        Services s = services();
        User user = currentUser(req);
        if (path.isEmpty()) {
            Difficulty difficulty = null;
            try {
                difficulty = param(req, "difficulty").isEmpty() ? null : Difficulty.fromLabel(param(req, "difficulty"));
            } catch (IllegalArgumentException ignored) {
                // unknown filter value: show everything
            }
            req.setAttribute("problems", s.problems.search(ContentStatus.APPROVED, difficulty, param(req, "tag"),
                    param(req, "q"), null));
            req.setAttribute("tags", s.problems.approvedTags());
            req.setAttribute("difficulties", Difficulty.values());
            req.setAttribute("acceptance", s.submissions.acceptanceStats());
            req.setAttribute("solved", user == null ? Set.of() : s.submissions.solvedProblemIds(user.getId()));
            req.setAttribute("attempted", user == null ? Set.of() : s.submissions.attemptedProblemIds(user.getId()));
            render(req, resp, "problems/list");
            return;
        }
        Problem p = s.problems.findBySlug(path.get(0)).orElseThrow(() -> new NotFoundException("Exercise"));
        if (!p.isVisibleTo(user)) {
            throw new NotFoundException("Exercise");
        }
        req.setAttribute("problem", p);
        req.setAttribute("canEdit", user != null && user.canEdit(p));
        req.setAttribute("last", user == null ? null : s.submissions.latestFor(user.getId(), p.getId()).orElse(null));
        render(req, resp, "problems/workspace");
    }
}
