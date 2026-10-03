package com.pyjudge.web;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.pyjudge.Services;
import com.pyjudge.course.CourseOverview;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.service.NotFoundException;
import com.pyjudge.service.Setting;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Public pages about people: the leaderboard and user profiles. */
@WebServlet({"/leaderboard", "/u/*"})
public class CommunityServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        Services s = services();
        if (req.getServletPath().equals("/leaderboard")) {
            if (!s.settings.flag(Setting.LEADERBOARD_ENABLED)) {
                throw new NotFoundException("Leaderboard");
            }
            req.setAttribute("rows", s.stats.leaderboard(null, null, null, null));
            req.setAttribute("difficulties", Difficulty.values());
            render(req, resp, "leaderboard");
            return;
        }
        if (path.isEmpty()) {
            throw new NotFoundException("User");
        }
        User user = s.users.findByUsername(path.get(0)).orElseThrow(() -> new NotFoundException("User"));
        User viewer = currentUser(req);
        Set<Long> solvedIds = s.submissions.solvedProblemIds(user.getId());
        List<Problem> approved = s.problems.approved();
        List<Problem> solved = approved.stream().filter(p -> solvedIds.contains(p.getId())).toList();
        CourseOverview course = s.course.overview(user);
        boolean own = viewer != null && (viewer.getId() == user.getId() || viewer.canAuthor());

        req.setAttribute("profile", user);
        req.setAttribute("solved", solved);
        req.setAttribute("score", s.stats.score(solved));
        req.setAttribute("rank", s.stats.rankOf(user.getId()));
        req.setAttribute("verdicts", s.submissions.verdictCounts(user.getId()));
        req.setAttribute("byDifficulty", s.stats.byDifficulty(approved, solvedIds));
        req.setAttribute("course", course);
        req.setAttribute("recent", own ? s.submissions.byUser(user.getId(), 10, 0) : List.of());
        req.setAttribute("authored", user.canAuthor()
                ? s.problems.search(com.pyjudge.model.ContentStatus.APPROVED, null, null, null, user.getId()) : List.of());
        if (user.isStudent()) {
            req.setAttribute("badges", s.stats.achievements(course, solved.size(),
                    solved.stream().anyMatch(p -> p.getDifficulty() == Difficulty.HARD), s.stats.streak(user.getId()),
                    s.challengeService.anyCompleted(user)));
        }
        render(req, resp, "profile");
    }
}
