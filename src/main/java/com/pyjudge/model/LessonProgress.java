package com.pyjudge.model;

import java.time.LocalDateTime;

/** One student's progress on one lesson: best quiz score and when they completed it. */
public class LessonProgress {

    private long userId;
    private long lessonId;
    private LocalDateTime completedAt;
    private Integer quizScore;
    private Integer quizTotal;
    private int attempts;

    public boolean isCompleted() {
        return completedAt != null;
    }

    public boolean isPerfect() {
        return quizTotal != null && quizTotal > 0 && quizTotal.equals(quizScore);
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getLessonId() { return lessonId; }
    public void setLessonId(long lessonId) { this.lessonId = lessonId; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Integer getQuizScore() { return quizScore; }
    public void setQuizScore(Integer quizScore) { this.quizScore = quizScore; }
    public Integer getQuizTotal() { return quizTotal; }
    public void setQuizTotal(Integer quizTotal) { this.quizTotal = quizTotal; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
}
