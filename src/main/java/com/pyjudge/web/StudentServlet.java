package com.pyjudge.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pyjudge.Services;
import com.pyjudge.course.CourseOverview;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.service.ChallengeService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Student dashboard: course progress, exercises, achievements, challenges, feedback, messages. */
@WebServlet("/student")
public class StudentServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        Services s = services();
        User user = requireUser(req);
        long uid = user.getId();
        List<Problem> approved = s.problems.approved();
        Set<Long> solvedIds = s.submissions.solvedProblemIds(uid);
        Set<Long> attemptedIds = s.submissions.attemptedProblemIds(uid);
        List<Problem> solved = approved.stream().filter(p -> solvedIds.contains(p.getId())).toList();
        CourseOverview course = s.course.overview(user);
        int streak = s.stats.streak(uid);

        List<Problem> upNext = new ArrayList<>(approved.stream()
                .filter(p -> attemptedIds.contains(p.getId()) && !solvedIds.contains(p.getId())).limit(3).toList());
        approved.stream().filter(p -> !attemptedIds.contains(p.getId())).limit(3 - upNext.size()).forEach(upNext::add);

        Map<Challenge.State, List<ChallengeService.ChallengeProgress>> challenges = s.challengeService.grouped(user);
        List<ChallengeService.ChallengeProgress> active = challenges.get(Challenge.State.ACTIVE).stream()
                .filter(c -> !c.isCompleted()).toList();
        List<ChallengeService.ChallengeProgress> completed = new ArrayList<>();
        challenges.values().forEach(list -> list.stream().filter(ChallengeService.ChallengeProgress::isCompleted)
                .forEach(completed::add));

        req.setAttribute("course", course);
        req.setAttribute("solvedCount", solved.size());
        req.setAttribute("exerciseCount", approved.size());
        req.setAttribute("score", s.stats.score(solved));
        req.setAttribute("rank", s.stats.rankOf(uid));
        req.setAttribute("streak", streak);
        Double avg = s.submissions.averageGradeForUser(uid);
        req.setAttribute("avgGrade", avg == null ? null : (int) Math.round(avg));
        req.setAttribute("byDifficulty", s.stats.byDifficulty(approved, solvedIds));
        req.setAttribute("timeline", s.stats.solveTimeline(uid, 30));
        req.setAttribute("upNext", upNext);
        req.setAttribute("attempted", attemptedIds);
        req.setAttribute("activeChallenges", active);
        req.setAttribute("completedChallenges", completed);
        req.setAttribute("feedback", s.submissions.gradedForUser(uid, 5));
        req.setAttribute("inbox", s.messaging.inbox(user, 4));
        req.setAttribute("senders", s.messaging.conversations(user));
        req.setAttribute("badges", s.stats.achievements(course, solved.size(),
                solved.stream().anyMatch(p -> p.getDifficulty() == Difficulty.HARD), streak, !completed.isEmpty()));
        render(req, resp, "student/dashboard");
    }
}
