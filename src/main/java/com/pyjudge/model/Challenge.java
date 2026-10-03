package com.pyjudge.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A time-boxed set of exercises. Only solves inside the window count toward it. */
public class Challenge implements Authored {

    public enum State { UPCOMING, ACTIVE, ENDED }

    private long id;
    private String title;
    private String description = "";
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long createdBy;
    private String authorName;
    private LocalDateTime createdAt;
    private List<Problem> problems = new ArrayList<>();

    public State stateAt(LocalDateTime now) {
        if (now.isBefore(startAt)) {
            return State.UPCOMING;
        }
        return now.isAfter(endAt) ? State.ENDED : State.ACTIVE;
    }

    public State getState() {
        return stateAt(com.pyjudge.util.Times.now());
    }

    public List<Problem> getApprovedProblems() {
        return problems.stream().filter(Problem::isApproved).toList();
    }

    @Override public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    @Override public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<Problem> getProblems() { return problems; }
    public void setProblems(List<Problem> problems) { this.problems = problems; }
}
