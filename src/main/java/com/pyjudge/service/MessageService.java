package com.pyjudge.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pyjudge.dao.MessageDao;
import com.pyjudge.dao.UserDao;
import com.pyjudge.model.Message;
import com.pyjudge.model.User;

/** Direct messages between students and staff. Who may message whom is decided by the User subclasses. */
public class MessageService {

    public static final int MAX_LENGTH = 4000;

    private final MessageDao messages;
    private final UserDao users;
    private final SettingsService settings;

    public MessageService(MessageDao messages, UserDao users, SettingsService settings) {
        this.messages = messages;
        this.users = users;
        this.settings = settings;
    }

    public boolean canMessage(User from, User to) {
        if (!settings.flag(Setting.MESSAGING_ENABLED) && !from.isAdmin()) {
            return false;
        }
        return from.canMessage(to); // polymorphic: Student restricts this to staff
    }

    /** One row per person the user has talked to, newest first. */
    public static class Conversation {
        private final User other;
        private final Message last;
        private int unread;

        Conversation(User other, Message last) {
            this.other = other;
            this.last = last;
        }

        public User getOther() { return other; }
        public Message getLast() { return last; }
        public int getUnread() { return unread; }
    }

    public List<Conversation> conversations(User user) {
        Map<Long, Conversation> byOther = new LinkedHashMap<>();
        for (Message m : messages.involving(user.getId())) {
            long otherId = m.getSenderId() == user.getId() ? m.getRecipientId() : m.getSenderId();
            Conversation c = byOther.get(otherId);
            if (c == null) {
                User other = users.findById(otherId).orElse(null);
                if (other == null) {
                    continue;
                }
                c = new Conversation(other, m);
                byOther.put(otherId, c);
            }
            if (m.getRecipientId() == user.getId() && m.isUnread()) {
                c.unread++;
            }
        }
        return new ArrayList<>(byOther.values());
    }

    /** Messages between two users; marks the viewer's incoming ones as read. */
    public List<Message> thread(User viewer, User other) {
        messages.markRead(viewer.getId(), other.getId());
        return messages.thread(viewer.getId(), other.getId());
    }

    public Message send(User from, User to, String body, Long submissionId) {
        if (!canMessage(from, to)) {
            throw new ForbiddenException(to == null ? "Pick someone to message." : "You can't message that person.");
        }
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw new ValidationException("Write a message first.");
        }
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH);
        }
        return messages.send(from.getId(), to.getId(), text, submissionId);
    }

    public List<User> contacts(User user) {
        return users.findAll().stream().filter(u -> canMessage(user, u)).toList();
    }

    public long unread(User user) {
        return user == null ? 0 : messages.unreadCount(user.getId());
    }

    public List<Message> inbox(User user, int limit) {
        return messages.inbox(user.getId(), limit);
    }
}
