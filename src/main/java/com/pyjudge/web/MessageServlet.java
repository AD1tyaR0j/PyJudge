package com.pyjudge.web;

import java.io.IOException;
import java.util.List;

import com.pyjudge.Services;
import com.pyjudge.model.Message;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;
import com.pyjudge.service.NotFoundException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** /messages (inbox), /messages/new (compose), /messages/{username} (conversation). */
@WebServlet({"/messages", "/messages/*"})
public class MessageServlet extends BaseServlet {

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireUser(req);
        Services s = services();
        if (path.isEmpty()) {
            req.setAttribute("conversations", s.messaging.conversations(user));
            render(req, resp, "messages/inbox");
            return;
        }
        if (path.get(0).equals("new")) {
            req.setAttribute("contacts", s.messaging.contacts(user));
            req.setAttribute("to", param(req, "to"));
            Long subId = longParam(req, "submission");
            if (subId != null) {
                s.submissions.findById(subId).filter(sub -> s.submissionService.canView(sub, user))
                        .ifPresent(sub -> req.setAttribute("submission", sub));
            }
            render(req, resp, "messages/compose");
            return;
        }
        User other = s.users.findByUsername(path.get(0)).orElseThrow(() -> new NotFoundException("User"));
        List<Message> thread = s.messaging.thread(user, other);
        boolean canReply = s.messaging.canMessage(user, other);
        if (thread.isEmpty() && !canReply) {
            throw new NotFoundException("Conversation");
        }
        req.setAttribute("other", other);
        req.setAttribute("thread", thread);
        req.setAttribute("canReply", canReply);
        render(req, resp, "messages/thread");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp, List<String> path) throws ServletException, IOException {
        User user = requireUser(req);
        Services s = services();
        String username = path.isEmpty() || path.get(0).equals("new") ? param(req, "to") : path.get(0);
        User to = s.users.findByUsername(username).orElse(null);
        Long subId = longParam(req, "submission_id");
        if (subId != null) {
            Submission sub = s.submissions.findById(subId).orElse(null);
            if (sub == null || !s.submissionService.canView(sub, user)) {
                subId = null;
            }
        }
        s.messaging.send(user, to, param(req, "body"), subId);
        flash(req, "success", "Message sent to " + to.getDisplayName() + ".");
        redirect(req, resp, "/messages/" + to.getUsername());
    }
}
