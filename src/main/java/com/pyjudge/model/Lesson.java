package com.pyjudge.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A tutorial page. Built-in lessons come from the curriculum files; instructors can add custom ones. */
public class Lesson implements Reviewable {

    public enum Source { BUILTIN, CUSTOM }

    private long id;
    private long topicId;
    private String topicSlug;
    private String topicTitle;
    private String slug;
    private String title;
    private String content = "";
    private int minutes = 5;
    private String practiceTags = "";
    private int position;
    private Source source = Source.CUSTOM;
    private ContentStatus status = ContentStatus.PENDING;
    private String reviewNote;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private Long createdBy;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<QuizQuestion> questions = new ArrayList<>();

    public List<String> getPracticeTagList() {
        return practiceTags == null || practiceTags.isBlank() ? List.of()
                : Arrays.stream(practiceTags.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();
    }

    public boolean isBuiltin() {
        return source == Source.BUILTIN;
    }

    /** Built-in lessons are only editable by admins (and get overwritten by the next curriculum sync). */
    @Override
    public boolean isVisibleTo(User user) {
        return Reviewable.super.isVisibleTo(user);
    }

    public boolean isEditableBy(User user) {
        if (user == null) {
            return false;
        }
        return user.isAdmin() || (!isBuiltin() && user.canEdit(this));
    }

    public String getUrl() {
        return "/learn/" + topicSlug + "/" + slug;
    }

    @Override public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getTopicId() { return topicId; }
    public void setTopicId(long topicId) { this.topicId = topicId; }
    public String getTopicSlug() { return topicSlug; }
    public void setTopicSlug(String topicSlug) { this.topicSlug = topicSlug; }
    public String getTopicTitle() { return topicTitle; }
    public void setTopicTitle(String topicTitle) { this.topicTitle = topicTitle; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
    public String getPracticeTags() { return practiceTags; }
    public void setPracticeTags(String practiceTags) { this.practiceTags = practiceTags == null ? "" : practiceTags; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    @Override public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    @Override public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions; }
}
