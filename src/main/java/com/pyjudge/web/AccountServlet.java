package com.pyjudge.web;

import java.io.IOException;
import java.util.List;

import com.pyjudge.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** /dashboard sends each role to its own dashboard; /settings edits the profile and password. */
@WebServlet({"/dashboard", "/settings"})
public class AccountServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireUser(req);
        if (req.getServletPath().equals("/dashboard")) {
            redirect(req, resp, user.getDashboardPath()); // polymorphic: each User subclass knows its dashboard
            return;
        }
        render(req, resp, "account/settings");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireUser(req);
        if ("password".equals(param(req, "action"))) {
            services().auth.changePassword(user, param(req, "current_password"), param(req, "new_password"),
                    param(req, "confirm_password"));
            flash(req, "success", "Password changed.");
        } else {
            services().auth.updateProfile(user, param(req, "name"), param(req, "email"), param(req, "bio"));
            flash(req, "success", "Profile updated.");
        }
        redirect(req, resp, "/settings");
    }
}
