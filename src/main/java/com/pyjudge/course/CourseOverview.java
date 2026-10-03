package com.pyjudge.course;

import java.util.List;
import java.util.Map;

import com.pyjudge.model.Lesson;
import com.pyjudge.model.LessonProgress;
import com.pyjudge.model.Topic;

/** A user's view of the whole course: per-topic progress and where to continue. */
public class CourseOverview {

    /** One topic with the viewer's progress through it. */
    public static class TopicProgress {
        private final Topic topic;
        private final List<Lesson> lessons;
        private final int done;

        public TopicProgress(Topic topic, List<Lesson> lessons, int done) {
            this.topic = topic;
            this.lessons = lessons;
            this.done = done;
        }

        public Topic getTopic() { return topic; }
        public List<Lesson> getLessons() { return lessons; }
        public int getDone() { return done; }
        public int getTotal() { return lessons.size(); }
        public int getPercent() { return lessons.isEmpty() ? 0 : Math.round(100f * done / lessons.size()); }
        public int getMinutes() { return lessons.stream().mapToInt(Lesson::getMinutes).sum(); }
        public boolean isComplete() { return done == lessons.size() && done > 0; }

        /** "done", "active" (started), or "new". */
        public String getState() {
            return isComplete() ? "done" : done > 0 ? "active" : "new";
        }
    }

    private final List<TopicProgress> topics;
    private final Map<Long, LessonProgress> progress;
    private final Lesson next;

    public CourseOverview(List<TopicProgress> topics, Map<Long, LessonProgress> progress, Lesson next) {
        this.topics = topics;
        this.progress = progress;
        this.next = next;
    }

    public List<TopicProgress> getTopics() { return topics; }
    public Map<Long, LessonProgress> getProgress() { return progress; }
    public Lesson getNext() { return next; }
    public int getTotal() { return topics.stream().mapToInt(TopicProgress::getTotal).sum(); }
    public int getDone() { return topics.stream().mapToInt(TopicProgress::getDone).sum(); }
    public int getPercent() { return getTotal() == 0 ? 0 : Math.round(100f * getDone() / getTotal()); }
    public long getTopicsComplete() { return topics.stream().filter(TopicProgress::isComplete).count(); }

    public long getPerfectQuizzes() {
        return progress.values().stream().filter(LessonProgress::isPerfect).count();
    }

    /** Percentage of quiz points earned across all attempted quizzes, or null if none taken. */
    public Integer getQuizAverage() {
        int scored = 0;
        int possible = 0;
        for (LessonProgress p : progress.values()) {
            if (p.getQuizTotal() != null && p.getQuizTotal() > 0) {
                scored += p.getQuizScore();
                possible += p.getQuizTotal();
            }
        }
        return possible == 0 ? null : Math.round(100f * scored / possible);
    }

    public boolean isCompleted(long lessonId) {
        LessonProgress p = progress.get(lessonId);
        return p != null && p.isCompleted();
    }
}
