package com.pyjudge.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pyjudge.Services;
import com.pyjudge.model.User;
import com.pyjudge.service.ForbiddenException;
import com.pyjudge.service.NotFoundException;
import com.pyjudge.service.PyJudgeException;
import com.pyjudge.service.ValidationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Helpers shared by every servlet: rendering JSP views, redirects with flash messages, role checks,
 * JSON responses, and turning the application's exceptions into the right HTTP response.
 */
public abstract class BaseServlet extends HttpServlet {

    protected static final ObjectMapper JSON = new ObjectMapper();

    // ---------------------------------------------------------------- request dispatch with error handling

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp, false);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp, true);
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp, boolean post) throws ServletException, IOException {
        try {
            if (post) {
                post(req, resp, segments(req));
            } else {
                get(req, resp, segments(req));
            }
        } catch (LoginRequired e) {
            if (isApi(req)) {
                json(resp, 401, Map.of("error", "Login required"));
            } else {
                String next = req.getRequestURI() + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
                resp.sendRedirect(req.getContextPath() + "/login?next=" + java.net.URLEncoder.encode(next, "UTF-8"));
            }
        } catch (ValidationException e) {
            if (isApi(req) || wantsJson(req)) {
                json(resp, 400, Map.of("error", e.getMessage(), "errors", e.errors()));
            } else if (post) {
                e.errors().forEach(msg -> flash(req, "error", msg));
                redirectBack(req, resp);
            } else {
                resp.sendError(400, e.getMessage());
            }
        } catch (PyJudgeException e) {
            if (isApi(req) || wantsJson(req)) {
                json(resp, e.status(), Map.of("error", e.getMessage()));
            } else if (post && e instanceof ForbiddenException) {
                flash(req, "error", e.getMessage());
                redirectBack(req, resp);
            } else {
                resp.sendError(e.status(), e.getMessage());
            }
        }
    }

    /** Path segments after the servlet mapping: /admin/users/5/edit -> ["users", "5", "edit"]. */
    private static List<String> segments(HttpServletRequest req) {
        List<String> out = new ArrayList<>();
        String info = req.getPathInfo();
        if (info != null) {
            for (String part : info.split("/")) {
                if (!part.isEmpty()) {
                    out.add(part);
                }
            }
        }
        return out;
    }

    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        throw new NotFoundException("Page");
    }

    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        throw new NotFoundException("Page");
    }

    // ---------------------------------------------------------------- context

    protected Services services() {
        return (Services) getServletContext().getAttribute(Services.ATTRIBUTE);
    }

    protected static User currentUser(HttpServletRequest req) {
        return (User) req.getAttribute("currentUser");
    }

    /** Thrown when a page needs a logged-in user; turned into a redirect to /login (or a 401 for the API). */
    protected static final class LoginRequired extends RuntimeException {
        LoginRequired() {
            super(null, null, false, false);
        }
    }

    protected static User requireUser(HttpServletRequest req) {
        User u = currentUser(req);
        if (u == null) {
            throw new LoginRequired();
        }
        return u;
    }

    protected static User requireAuthor(HttpServletRequest req) {
        User u = requireUser(req);
        if (!u.canAuthor()) {
            throw new ForbiddenException();
        }
        return u;
    }

    protected static User requireAdmin(HttpServletRequest req) {
        User u = requireUser(req);
        if (!u.isAdmin()) {
            throw new ForbiddenException();
        }
        return u;
    }

    // ---------------------------------------------------------------- responses

    protected void render(HttpServletRequest req, HttpServletResponse resp, String view) throws ServletException, IOException {
        resp.setContentType("text/html; charset=UTF-8");
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    protected static void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    /** Back to the form that was submitted (same-site referrers only), or the home page. */
    protected static void redirectBack(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ref = req.getHeader("Referer");
        String host = req.getScheme() + "://" + req.getServerName();
        if (ref != null && ref.startsWith(host)) {
            resp.sendRedirect(ref);
        } else {
            redirect(req, resp, "/");
        }
    }

    @SuppressWarnings("unchecked")
    protected static void flash(HttpServletRequest req, String kind, String message) {
        List<String[]> flashes = (List<String[]>) req.getSession().getAttribute("flash");
        if (flashes == null) {
            flashes = new ArrayList<>();
            req.getSession().setAttribute("flash", flashes);
        }
        flashes.add(new String[]{kind, message});
    }

    protected static void json(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        JSON.writeValue(resp.getOutputStream(), body);
    }

    private static boolean isApi(HttpServletRequest req) {
        return req.getRequestURI().substring(req.getContextPath().length()).startsWith("/api/");
    }

    /** Fetch requests from our own pages ask for JSON explicitly. */
    private static boolean wantsJson(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    // ---------------------------------------------------------------- parameters

    protected static String param(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v;
    }

    protected static Long longParam(HttpServletRequest req, String name) {
        try {
            String v = req.getParameter(name);
            return v == null || v.isBlank() ? null : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static int intParam(HttpServletRequest req, String name, int fallback) {
        Long v = longParam(req, name);
        return v == null ? fallback : v.intValue();
    }

    protected static long id(String segment) {
        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            throw new NotFoundException("Page");
        }
    }

    static {
        JSON.registerModule(new JavaTimeModule());
        JSON.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
