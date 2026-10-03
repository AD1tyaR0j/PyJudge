package com.pyjudge.judge;

import java.util.List;

import com.pyjudge.model.TestCase;
import com.pyjudge.model.Verdict;

/** Runs a submission against every test case and decides the verdict. Stops at the first failure. */
public class Judge {

    private final Sandbox sandbox;

    public Judge(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    /** The outcome of judging one submission. */
    public record Result(Verdict verdict, int passed, int total, int runtimeMs, String error) { }

    public Result judge(String code, List<TestCase> cases, int timeLimit, int memoryLimit) {
        int total = cases.size();
        if (total == 0) {
            return new Result(Verdict.IE, 0, 0, 0, "This exercise has no test cases yet.");
        }
        int passed = 0;
        long maxTime = 0;
        for (int i = 0; i < total; i++) {
            TestCase tc = cases.get(i);
            RunResult run = sandbox.run(code, tc.getInput(), timeLimit, memoryLimit, Sandbox.JUDGE_OUTPUT_LIMIT);
            maxTime = Math.max(maxTime, run.timeMs());
            String label = (tc.isSample() ? "sample" : "hidden") + " test case " + (i + 1);

            switch (run.status()) {
                case CE:
                    return new Result(Verdict.CE, passed, total, (int) maxTime, tail(run.stderr()));
                case TLE:
                    return new Result(Verdict.TLE, passed, total, (int) maxTime,
                            "Time limit of " + timeLimit + "s exceeded on " + label + ".");
                case MLE:
                    return new Result(Verdict.MLE, passed, total, (int) maxTime,
                            "Memory limit of " + memoryLimit + " MB exceeded on " + label + ".");
                case RE:
                    return new Result(Verdict.RE, passed, total, (int) maxTime,
                            "Runtime error on " + label + ":\n" + tail(run.stderr()));
                case OK:
                default:
                    break;
            }
            if (!OutputComparator.matches(run.stdout(), tc.getExpectedOutput())) {
                String error = "Wrong answer on " + label + ".";
                if (tc.isSample()) {
                    error += "\n\nInput:\n" + tc.getInput().strip()
                            + "\n\nExpected:\n" + OutputComparator.normalize(tc.getExpectedOutput())
                            + "\n\nYour output:\n" + truncate(OutputComparator.normalize(run.stdout()), 2000);
                }
                return new Result(Verdict.WA, passed, total, (int) maxTime, error);
            }
            passed++;
        }
        return new Result(Verdict.AC, passed, total, (int) maxTime, null);
    }

    private static String tail(String s) {
        String t = s.strip();
        return t.length() > 2000 ? "…" + t.substring(t.length() - 2000) : t;
    }

    private static String truncate(String s, int n) {
        return s.length() > n ? s.substring(0, n) + "…" : s;
    }
}
