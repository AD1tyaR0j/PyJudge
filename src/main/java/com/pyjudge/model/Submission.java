package com.pyjudge.model;

import java.time.LocalDateTime;

/** A student's attempt at an exercise, the judge's verdict, and the instructor's grade. */
public class Submission implements Identifiable {

    private long id;
    private long userId;
    private long problemId;
    private String code;
    private Verdict verdict = Verdict.PENDING;
    private int passed;
    private int total;
    private Integer runtimeMs;
    private String error;
    private Integer grade;
    private String feedback;
    private Long gradedBy;
    private LocalDateTime gradedAt;
    private LocalDateTime submittedAt;

    // joined, read-only display fields
    private String username;
    private String userDisplayName;
    private String problemTitle;
    private String problemSlug;
    private Difficulty problemDifficulty;
    private Long problemAuthorId;
    private String graderName;

    public int getScorePercent() {
        return total == 0 ? 0 : Math.round(100f * passed / total);
    }

    public boolean isGraded() {
        return grade != null;
    }

    @Override public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getProblemId() { return problemId; }
    public void setProblemId(long problemId) { this.problemId = problemId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Verdict getVerdict() { return verdict; }
    public void setVerdict(Verdict verdict) { this.verdict = verdict; }
    public int getPassed() { return passed; }
    public void setPassed(int passed) { this.passed = passed; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public Integer getRuntimeMs() { return runtimeMs; }
    public void setRuntimeMs(Integer runtimeMs) { this.runtimeMs = runtimeMs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Long getGradedBy() { return gradedBy; }
    public void setGradedBy(Long gradedBy) { this.gradedBy = gradedBy; }
    public LocalDateTime getGradedAt() { return gradedAt; }
    public void setGradedAt(LocalDateTime gradedAt) { this.gradedAt = gradedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }
    public String getProblemTitle() { return problemTitle; }
    public void setProblemTitle(String problemTitle) { this.problemTitle = problemTitle; }
    public String getProblemSlug() { return problemSlug; }
    public void setProblemSlug(String problemSlug) { this.problemSlug = problemSlug; }
    public Difficulty getProblemDifficulty() { return problemDifficulty; }
    public void setProblemDifficulty(Difficulty problemDifficulty) { this.problemDifficulty = problemDifficulty; }
    public Long getProblemAuthorId() { return problemAuthorId; }
    public void setProblemAuthorId(Long problemAuthorId) { this.problemAuthorId = problemAuthorId; }
    public String getGraderName() { return graderName; }
    public void setGraderName(String graderName) { this.graderName = graderName; }
}
