package com.pyjudge.model;

import java.util.ArrayList;
import java.util.List;

/** A chapter of the Python course, e.g. "Strings". */
public class Topic implements Identifiable {

    private long id;
    private String slug;
    private String title;
    private String level = "Beginner";
    private String summary = "";
    private String description = "";
    private int position;
    private List<Lesson> lessons = new ArrayList<>();

    public List<Lesson> getApprovedLessons() {
        return lessons.stream().filter(Lesson::isApproved).toList();
    }

    /** "01", "02", ... used as the topic's marker in the course map. */
    public String getNumber() {
        return String.format("%02d", position + 1);
    }

    @Override public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public List<Lesson> getLessons() { return lessons; }
    public void setLessons(List<Lesson> lessons) { this.lessons = lessons; }
}
