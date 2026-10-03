package com.pyjudge.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pyjudge.Services;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;
import com.pyjudge.service.NotFoundException;
import com.pyjudge.service.Setting;
import com.pyjudge.service.ValidationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Admin area: dashboard, user management, content tables, approvals, and system settings.
 */
@WebServlet({"/admin", "/admin/*"})
public class AdminServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        requireAdmin(req);
        Services s = services();
        String route = path.isEmpty() ? "" : path.get(0);
        switch (route) {
            case "" -> {
                Map<String, Long> roles = s.users.countByRole();
                Map<String, Long> verdicts = s.submissions.verdictCounts(null);
                long judged = verdicts.entrySet().stream().filter(e -> !e.getKey().equals("PENDING"))
                        .mapToLong(Map.Entry::getValue).sum();
                long pendingLessons = s.courseDao.lessonCountByStatus().getOrDefault("pending", 0L);
                req.setAttribute("students", roles.getOrDefault("student", 0L));
                req.setAttribute("instructors", roles.getOrDefault("instructor", 0L));
                req.setAttribute("problemCounts", s.problems.countByStatus());
                req.setAttribute("pendingTotal", s.problems.countByStatus().getOrDefault("pending", 0L) + pendingLessons);
                req.setAttribute("submissionCount", s.submissions.count());
                req.setAttribute("acceptance", judged == 0 ? 0 : Math.round(100f * verdicts.getOrDefault("AC", 0L) / judged));
                req.setAttribute("lessonCount", s.courseDao.lessonCountByStatus().getOrDefault("approved", 0L));
                req.setAttribute("completions", s.courseDao.totalCompletions());
                req.setAttribute("activity", s.stats.activity(14));
                req.setAttribute("pending", s.problems.search(ContentStatus.PENDING, null, null, null, null));
                req.setAttribute("pendingLessons", s.courseDao.pendingLessons());
                req.setAttribute("recent", s.submissions.recent(10));
                req.setAttribute("judge", s.judge);
                render(req, resp, "admin/dashboard");
            }
            case "users" -> {
                if (path.size() == 3 && path.get(2).equals("edit")) {
                    req.setAttribute("target", s.users.findById(id(path.get(1))).orElseThrow(() -> new NotFoundException("User")));
                    req.setAttribute("roles", Role.values());
                    render(req, resp, "admin/user-edit");
                    return;
                }
                Role role = null;
                try {
                    role = param(req, "role").isEmpty() ? null : Role.fromCode(param(req, "role"));
                } catch (IllegalArgumentException ignored) {
                    // unknown filter: show everyone
                }
                req.setAttribute("users", s.users.search(role, param(req, "q")));
                req.setAttribute("roles", Role.values());
                req.setAttribute("roleFilter", role == null ? "" : role.code());
                req.setAttribute("submissionCounts", countsByUser(s));
                render(req, resp, "admin/users");
            }
            case "content" -> {
                ContentStatus status = null;
                try {
                    status = param(req, "status").isEmpty() ? null : ContentStatus.fromCode(param(req, "status"));
                } catch (IllegalArgumentException ignored) {
                    // show all
                }
                req.setAttribute("problems", s.problems.search(status, null, null, null, null));
                req.setAttribute("counts", s.problems.countByStatus());
                req.setAttribute("tests", s.problems.testCaseCounts());
                req.setAttribute("statusFilter", status == null ? "" : status.code());
                req.setAttribute("statuses", ContentStatus.values());
                render(req, resp, "admin/content");
            }
            case "approvals" -> {
                req.setAttribute("pending", s.problems.search(ContentStatus.PENDING, null, null, null, null).stream()
                        .map(p -> s.problems.findById(p.getId()).orElse(p)).toList());
                req.setAttribute("pendingLessons", s.courseDao.pendingLessons());
                render(req, resp, "admin/approvals");
            }
            case "settings" -> {
                req.setAttribute("settings", Setting.values());
                render(req, resp, "admin/settings");
            }
            default -> throw new NotFoundException("Page");
        }
    }

    private static Map<Long, Long> countsByUser(Services s) {
        Map<Long, Long> out = new HashMap<>();
        s.users.findAll().forEach(u -> out.put(u.getId(), s.submissions.countByUser(u.getId())));
        return out;
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User admin = requireAdmin(req);
        Services s = services();
        String key = String.join("/", path.stream().map(p -> p.matches("\\d+") ? "{id}" : p).toList());
        switch (key) {
            case "users" -> {
                Role role;
                try {
                    role = Role.fromCode(param(req, "role"));
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Pick a valid role.");
                }
                User u = s.auth.createUser(param(req, "username"), param(req, "name"), param(req, "email"),
                        param(req, "password"), role);
                flash(req, "success", "Created " + role.getLabel().toLowerCase() + " account \"" + u.getUsername() + "\".");
                redirect(req, resp, "/admin/users");
            }
            case "users/{id}/edit" -> {
                User target = s.users.findById(id(path.get(1))).orElseThrow(() -> new NotFoundException("User"));
                Role role;
                try {
                    role = Role.fromCode(param(req, "role"));
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Pick a valid role.");
                }
                s.auth.adminUpdate(target, param(req, "name"), param(req, "email"), role,
                        !param(req, "blocked").isEmpty(), param(req, "password"), admin);
                flash(req, "success", "Updated \"" + target.getUsername() + "\".");
                redirect(req, resp, "/admin/users");
            }
            case "users/{id}/delete" -> {
                long id = id(path.get(1));
                if (id == admin.getId()) {
                    throw new ValidationException("You can't delete your own account.");
                }
                // submissions, messages, and progress cascade; authored content is kept (author set to NULL)
                s.users.delete(id);
                flash(req, "info", "User deleted.");
                redirect(req, resp, "/admin/users");
            }
            case "exercises/{id}/review" -> {
                Problem p = s.content.reviewProblem(admin, id(path.get(1)), "approve".equals(param(req, "decision")),
                        param(req, "note"));
                flash(req, "success", "\"" + p.getTitle() + "\" " + (p.isApproved() ? "approved." : "returned for changes."));
                redirectBack(req, resp);
            }
            case "lessons/{id}/review" -> {
                var l = s.content.reviewLesson(admin, id(path.get(1)), "approve".equals(param(req, "decision")), param(req, "note"));
                flash(req, "success", "\"" + l.getTitle() + "\" " + (l.isApproved() ? "approved." : "returned for changes."));
                redirectBack(req, resp);
            }
            case "settings" -> {
                Map<String, String> form = new HashMap<>();
                for (Setting setting : Setting.values()) {
                    String v = req.getParameter(setting.key());
                    if (v != null) {
                        form.put(setting.key(), v);
                    }
                }
                s.settings.update(form);
                flash(req, "success", "Settings saved.");
                redirect(req, resp, "/admin/settings");
            }
            default -> throw new NotFoundException("Page");
        }
    }
}
