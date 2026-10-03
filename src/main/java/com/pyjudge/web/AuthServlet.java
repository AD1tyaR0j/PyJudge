package com.pyjudge.web;

import java.io.IOException;
import java.util.List;

import com.pyjudge.model.User;
import com.pyjudge.service.PyJudgeException;
import com.pyjudge.service.Setting;
import com.pyjudge.service.ValidationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Sign-up, login, and logout. Uses the HTTP session to remember who is logged in. */
@WebServlet({"/login", "/register", "/logout"})
public class AuthServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        String route = req.getServletPath();
        if (currentUser(req) != null && !route.equals("/logout")) {
            redirect(req, resp, currentUser(req).getDashboardPath());
            return;
        }
        switch (route) {
            case "/login" -> render(req, resp, "auth/login");
            case "/register" -> {
                if (!services().settings.flag(Setting.REGISTRATION_OPEN)) {
                    flash(req, "info", "Sign-ups are closed right now. Ask an administrator for an account.");
                    redirect(req, resp, "/login");
                    return;
                }
                render(req, resp, "auth/register");
            }
            default -> redirect(req, resp, "/");
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        switch (req.getServletPath()) {
            case "/login" -> login(req, resp);
            case "/register" -> register(req, resp);
            case "/logout" -> {
                req.getSession().invalidate();
                flash(req, "info", "You have been logged out.");
                redirect(req, resp, "/");
            }
            default -> redirect(req, resp, "/");
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            User user = services().auth.authenticate(param(req, "identifier"), param(req, "password"));
            startSession(req, user);
            String next = param(req, "next");
            redirect(req, resp, next.startsWith("/") && !next.startsWith("//") ? next : user.getDashboardPath());
        } catch (PyJudgeException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("identifier", param(req, "identifier"));
            resp.setStatus(e.status() == 403 ? 403 : 400);
            render(req, resp, "auth/login");
        }
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            User user = services().auth.register(param(req, "username"), param(req, "name"), param(req, "email"),
                    param(req, "password"), param(req, "confirm"));
            startSession(req, user);
            flash(req, "success", "Welcome, " + user.getDisplayName() + ". Start with the first lesson or pick an exercise.");
            redirect(req, resp, user.getDashboardPath());
        } catch (ValidationException e) {
            req.setAttribute("errors", e.errors());
            resp.setStatus(400);
            render(req, resp, "auth/register");
        }
    }

    /** New session id on login prevents session fixation. */
    private static void startSession(HttpServletRequest req, User user) {
        HttpSession session = req.getSession();
        req.changeSessionId();
        session.setAttribute(RequestContextFilter.SESSION_USER, user.getId());
        if (param(req, "remember").isEmpty()) {
            session.setMaxInactiveInterval(12 * 60 * 60);
        } else {
            session.setMaxInactiveInterval(30 * 24 * 60 * 60);
        }
    }
}
