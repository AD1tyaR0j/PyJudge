package com.pyjudge.model;

import java.util.ArrayList;
import java.util.List;

/** A multiple-choice question at the end of a lesson. {@code answer} is the index of the correct option. */
public class QuizQuestion implements Identifiable {

    private long id;
    private long lessonId;
    private String prompt;
    private List<String> options = new ArrayList<>();
    private int answer;
    private String explanation = "";
    private int position;

    public QuizQuestion() {
    }

    public QuizQuestion(String prompt, List<String> options, int answer, String explanation) {
        this.prompt = prompt;
        this.options = new ArrayList<>(options);
        this.answer = answer;
        this.explanation = explanation == null ? "" : explanation;
    }

    public boolean isCorrect(Integer chosen) {
        return chosen != null && chosen == answer;
    }

    @Override public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getLessonId() { return lessonId; }
    public void setLessonId(long lessonId) { this.lessonId = lessonId; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public int getAnswer() { return answer; }
    public void setAnswer(int answer) { this.answer = answer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
