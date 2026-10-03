package com.pyjudge.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A coding exercise: statement, limits, and the test cases a submission is judged against. */
public class Problem implements Reviewable {

    public static final String DEFAULT_STARTER = "# Read input with input() and print the answer\n";

    private long id;
    private String title;
    private String slug;
    private String description = "";
    private Difficulty difficulty = Difficulty.EASY;
    private int timeLimit = 2;
    private int memoryLimit = 128;
    private String tags = "";
    private String starterCode = DEFAULT_STARTER;
    private ContentStatus status = ContentStatus.PENDING;
    private String reviewNote;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private Long createdBy;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TestCase> testCases = new ArrayList<>();

    public List<String> getTagList() {
        return tags == null || tags.isBlank() ? List.of()
                : Arrays.stream(tags.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();
    }

    public List<TestCase> getSampleCases() {
        return testCases.stream().filter(TestCase::isSample).toList();
    }

    public int getHiddenCount() {
        return testCases.size() - getSampleCases().size();
    }

    @Override public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    public int getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(int memoryLimit) { this.memoryLimit = memoryLimit; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags == null ? "" : tags; }
    public String getStarterCode() { return starterCode; }
    public void setStarterCode(String starterCode) { this.starterCode = starterCode; }
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
    public List<TestCase> getTestCases() { return testCases; }
    public void setTestCases(List<TestCase> testCases) { this.testCases = testCases; }
}
