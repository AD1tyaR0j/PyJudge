package com.pyjudge.web;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

import com.pyjudge.Services;
import com.pyjudge.model.User;
import com.pyjudge.util.Times;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Runs before every request:
 * <ol>
 *   <li>forces UTF-8 for request parameters and responses;</li>
 *   <li>loads the logged-in user from the session (fresh from the database, so role changes and
 *       blocks take effect immediately);</li>
 *   <li>rejects state-changing requests without a valid CSRF token;</li>
 *   <li>exposes common values (current user, site settings, unread count) to the JSP views.</li>
 * </ol>
 */
@WebFilter(urlPatterns = "/*")
public class RequestContextFilter extends HttpFilter {

    public static final String SESSION_USER = "userId";
    public static final String SESSION_CSRF = "csrf";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.startsWith("/static/")) {
            chain.doFilter(req, resp);
            return;
        }
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "same-origin");

        Services services = (Services) getServletContext().getAttribute(Services.ATTRIBUTE);
        HttpSession session = req.getSession();
        String csrf = (String) session.getAttribute(SESSION_CSRF);
        if (csrf == null) {
            byte[] bytes = new byte[16];
            RANDOM.nextBytes(bytes);
            csrf = HexFormat.of().formatHex(bytes);
            session.setAttribute(SESSION_CSRF, csrf);
        }

        User user = null;
        Object id = session.getAttribute(SESSION_USER);
        if (id instanceof Long uid) {
            user = services.users.findById(uid).orElse(null);
            if (user == null || user.isBlocked()) {
                session.removeAttribute(SESSION_USER);
                user = null;
            } else if (user.getLastSeenAt() == null
                    || Duration.between(user.getLastSeenAt(), Times.now()).toMinutes() >= 5) {
                services.users.touchLastSeen(user.getId());
            }
        }

        if ("POST".equalsIgnoreCase(req.getMethod()) && !validCsrf(req, path, csrf)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing CSRF token. Reload the page and try again.");
            return;
        }

        req.setAttribute("currentUser", user);
        req.setAttribute("csrfToken", csrf);
        req.setAttribute("site", services.settings.asMap());
        req.setAttribute("unreadMessages", services.messaging.unread(user));
        req.setAttribute("path", path);
        chain.doFilter(req, resp);
    }

    /** API calls send the token in a header (and must be JSON, which browsers can't send cross-site without CORS). */
    private static boolean validCsrf(HttpServletRequest req, String path, String expected) {
        String token;
        if (path.startsWith("/api/")) {
            String type = req.getContentType();
            if (type == null || !type.startsWith("application/json")) {
                return false;
            }
            token = req.getHeader("X-CSRF-Token");
        } else {
            token = req.getParameter("csrf_token");
        }
        return token != null && java.security.MessageDigest.isEqual(token.getBytes(), expected.getBytes());
    }
}
