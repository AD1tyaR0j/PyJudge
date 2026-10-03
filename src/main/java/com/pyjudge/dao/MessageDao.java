package com.pyjudge.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.pyjudge.db.Database;
import com.pyjudge.model.Message;
import com.pyjudge.util.Times;

public class MessageDao extends JdbcDao implements Repository<Message> {

    private static final String SELECT = "SELECT m.*, p.title AS submission_title, su.name AS sender_name,"
            + " su.username AS sender_username FROM messages m JOIN users su ON su.id = m.sender_id"
            + " LEFT JOIN submissions s ON s.id = m.submission_id LEFT JOIN problems p ON p.id = s.problem_id";

    public MessageDao(Database db) {
        super(db);
    }

    static Message map(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setSenderId(rs.getLong("sender_id"));
        m.setRecipientId(rs.getLong("recipient_id"));
        m.setBody(rs.getString("body"));
        m.setSubmissionId(getLongOrNull(rs, "submission_id"));
        m.setCreatedAt(getTime(rs, "created_at"));
        m.setReadAt(getTime(rs, "read_at"));
        m.setSubmissionTitle(rs.getString("submission_title"));
        String name = rs.getString("sender_name");
        m.setSenderUsername(rs.getString("sender_username"));
        m.setSenderName(name == null || name.isBlank() ? m.getSenderUsername() : name);
        return m;
    }

    @Override
    public Optional<Message> findById(long id) {
        return queryOne(SELECT + " WHERE m.id = ?", MessageDao::map, id);
    }

    @Override
    public List<Message> findAll() {
        return query(SELECT + " ORDER BY m.id", MessageDao::map);
    }

    @Override
    public Message save(Message m) {
        if (m.getId() == 0) {
            m.setCreatedAt(Times.now());
            m.setId(insert("INSERT INTO messages (sender_id, recipient_id, body, submission_id, created_at)"
                    + " VALUES (?, ?, ?, ?, ?)", m.getSenderId(), m.getRecipientId(), m.getBody(), m.getSubmissionId(),
                    m.getCreatedAt()));
        } else {
            update("UPDATE messages SET read_at = ? WHERE id = ?", m.getReadAt(), m.getId());
        }
        return m;
    }

    public Message send(long from, long to, String body, Long submissionId) {
        Message m = new Message();
        m.setSenderId(from);
        m.setRecipientId(to);
        m.setBody(body);
        m.setSubmissionId(submissionId);
        return save(m);
    }

    @Override
    public boolean delete(long id) {
        return update("DELETE FROM messages WHERE id = ?", id) == 1;
    }

    /** Every message the user sent or received, newest first (grouped into conversations by the service). */
    public List<Message> involving(long userId) {
        return query(SELECT + " WHERE m.sender_id = ? OR m.recipient_id = ? ORDER BY m.id DESC", MessageDao::map,
                userId, userId);
    }

    public List<Message> thread(long a, long b) {
        return query(SELECT + " WHERE (m.sender_id = ? AND m.recipient_id = ?) OR (m.sender_id = ? AND m.recipient_id = ?)"
                + " ORDER BY m.id", MessageDao::map, a, b, b, a);
    }

    public int markRead(long recipientId, long senderId) {
        return update("UPDATE messages SET read_at = ? WHERE recipient_id = ? AND sender_id = ? AND read_at IS NULL",
                Times.now(), recipientId, senderId);
    }

    public long unreadCount(long userId) {
        return queryLong("SELECT COUNT(*) FROM messages WHERE recipient_id = ? AND read_at IS NULL", userId);
    }

    public List<Message> inbox(long userId, int limit) {
        return query(SELECT + " WHERE m.recipient_id = ? ORDER BY m.id DESC LIMIT ?", MessageDao::map, userId, limit);
    }

    public Optional<Message> latestAbout(long submissionId) {
        return queryOne(SELECT + " WHERE m.submission_id = ? ORDER BY m.id DESC LIMIT 1", MessageDao::map, submissionId);
    }
}
