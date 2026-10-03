package com.pyjudge.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.pyjudge.Services;
import com.pyjudge.judge.RunResult;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;
import com.pyjudge.service.NotFoundException;
import com.pyjudge.service.Setting;
import com.pyjudge.service.ValidationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JSON API used by the editor:
 * <pre>
 *   POST /api/run                    {code, input, problemId?}  -> {status, stdout, stderr, timeMs}
 *   POST /api/submit                 {problemId, code}          -> submission (verdict PENDING)
 *   GET  /api/submissions/{id}                                  -> submission (poll until judged)
 *   GET  /api/problems, /api/problems/{slug}, /api/leaderboard
 * </pre>
 */
@WebServlet("/api/*")
public class ApiServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        Services s = services();
        String route = path.isEmpty() ? "" : path.get(0);
        switch (route) {
            case "submissions" -> {
                User user = requireUser(req);
                if (path.size() < 2) {
                    throw new NotFoundException("Submission");
                }
                Submission sub;
                try {
                    sub = s.submissionService.viewable(id(path.get(1)), user);
                } catch (com.pyjudge.service.ForbiddenException e) {
                    throw new NotFoundException("Submission");
                }
                json(resp, 200, submissionJson(sub));
            }
            case "problems" -> {
                if (path.size() == 1) {
                    json(resp, 200, s.problems.approved().stream().map(p -> Map.of("id", p.getId(), "title", p.getTitle(),
                            "slug", p.getSlug(), "difficulty", p.getDifficulty().getLabel(), "tags", p.getTagList())).toList());
                    return;
                }
                Problem p = s.problems.findBySlug(path.get(1)).filter(x -> x.isVisibleTo(currentUser(req)))
                        .orElseThrow(() -> new NotFoundException("Exercise"));
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("id", p.getId());
                out.put("title", p.getTitle());
                out.put("difficulty", p.getDifficulty().getLabel());
                out.put("description", p.getDescription());
                out.put("timeLimit", p.getTimeLimit());
                out.put("samples", p.getSampleCases().stream().map(tc -> Map.of("input", tc.getInput(),
                        "output", tc.getExpectedOutput())).toList());
                json(resp, 200, out);
            }
            case "leaderboard" -> {
                if (!s.settings.flag(Setting.LEADERBOARD_ENABLED)) {
                    throw new NotFoundException("Leaderboard");
                }
                json(resp, 200, s.stats.leaderboard(100, null, null, null).stream().map(r -> Map.of("rank", r.getRank(),
                        "username", r.getUser().getUsername(), "solved", r.getSolved(), "score", r.getScore())).toList());
            }
            default -> throw new NotFoundException("Endpoint");
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireUser(req);
        JsonNode body;
        try {
            body = JSON.readTree(req.getInputStream());
        } catch (IOException e) {
            throw new ValidationException("Request body must be JSON.");
        }
        if (body == null) {
            throw new ValidationException("Request body must be JSON.");
        }
        String route = path.isEmpty() ? "" : path.get(0);
        switch (route) {
            case "run" -> {
                Long problemId = body.hasNonNull("problemId") ? body.get("problemId").asLong() : null;
                RunResult r = services().submissionService.run(text(body, "code"), text(body, "input"), problemId, user);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("status", r.status().name());
                out.put("stdout", r.stdout());
                out.put("stderr", r.stderr());
                out.put("timeMs", r.timeMs());
                json(resp, 200, out);
            }
            case "submit" -> {
                if (!body.hasNonNull("problemId")) {
                    throw new NotFoundException("Exercise");
                }
                Submission sub = services().submissionService.submit(body.get("problemId").asLong(), text(body, "code"), user);
                json(resp, 202, submissionJson(sub));
            }
            default -> throw new NotFoundException("Endpoint");
        }
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    static Map<String, Object> submissionJson(Submission s) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", s.getId());
        out.put("problemId", s.getProblemId());
        out.put("verdict", s.getVerdict().name());
        out.put("verdictLabel", s.getVerdict().getLabel());
        out.put("passed", s.getPassed());
        out.put("total", s.getTotal());
        out.put("runtimeMs", s.getRuntimeMs());
        out.put("error", s.getError());
        out.put("grade", s.getGrade());
        out.put("feedback", s.getFeedback());
        out.put("submittedAt", s.getSubmittedAt() == null ? null : s.getSubmittedAt().toString());
        return out;
    }
}
