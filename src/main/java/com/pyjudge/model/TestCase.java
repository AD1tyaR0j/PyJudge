package com.pyjudge.model;

/** One input/expected-output pair. Sample cases are shown on the exercise page; the rest are hidden. */
public class TestCase implements Identifiable {

    private long id;
    private long problemId;
    private String input = "";
    private String expectedOutput = "";
    private boolean sample;

    public TestCase() {
    }

    public TestCase(String input, String expectedOutput, boolean sample) {
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.sample = sample;
    }

    @Override public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getProblemId() { return problemId; }
    public void setProblemId(long problemId) { this.problemId = problemId; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }
}
