package com.pyjudge.model;

import java.time.LocalDateTime;

/** A direct message between two users, optionally about a submission. */
public class Message implements Identifiable {

    private long id;
    private long senderId;
    private long recipientId;
    private String body;
    private Long submissionId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private String submissionTitle;
    private String senderName;
    private String senderUsername;

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    @Override public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getSenderId() { return senderId; }
    public void setSenderId(long senderId) { this.senderId = senderId; }
    public long getRecipientId() { return recipientId; }
    public void setRecipientId(long recipientId) { this.recipientId = recipientId; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public boolean isUnread() { return readAt == null; }
    public String getSubmissionTitle() { return submissionTitle; }
    public void setSubmissionTitle(String submissionTitle) { this.submissionTitle = submissionTitle; }
}
