package com.pyjudge.web;

import java.io.IOException;
import java.util.List;

import com.pyjudge.Services;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.Problem;
import com.pyjudge.service.Setting;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** The landing page ("" maps exactly to the site root). */
@WebServlet("")
public class HomeServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        Services s = services();
        List<Problem> approved = s.problems.approved();
        req.setAttribute("course", s.course.overview(currentUser(req)));
        req.setAttribute("featured", approved.stream().limit(6).toList());
        req.setAttribute("exerciseCount", approved.size());
        req.setAttribute("studentCount", s.users.countByRole().getOrDefault("student", 0L));
        req.setAttribute("submissionCount", s.submissions.count());
        req.setAttribute("acceptance", s.submissions.acceptanceStats());
        req.setAttribute("leaders", s.settings.flag(Setting.LEADERBOARD_ENABLED) ? s.stats.leaderboard(5, null, null, null) : List.of());
        req.setAttribute("challenges", s.challenges.findAll().stream()
                .filter(c -> c.getState() == Challenge.State.ACTIVE).limit(2).toList());
        render(req, resp, "home");
    }
}
