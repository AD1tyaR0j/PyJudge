package com.pyjudge.judge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.pyjudge.TestSupport;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.Verdict;

class JudgeTest {

    private static final String SUM = "a, b = map(int, input().split())\nprint(a + b)";
    private static final List<TestCase> CASES = List.of(new TestCase("2 3", "5", true), new TestCase("10 -4", "6", false));

    private final ProcessSandbox sandbox = new ProcessSandbox(TestSupport.config().pythonCommand());
    private final Judge judge = new Judge(sandbox);

    @Test
    void accepted() {
        Judge.Result r = judge.judge(SUM, CASES, 2, 128);
        assertEquals(Verdict.AC, r.verdict());
        assertEquals(2, r.passed());
    }

    @Test
    void wrongAnswerShowsDetailsOnlyForSamples() {
        Judge.Result hidden = judge.judge("print(5)", CASES, 2, 128);
        assertEquals(Verdict.WA, hidden.verdict());
        assertTrue(hidden.error().contains("hidden test case 2") && !hidden.error().contains("Expected"));
        assertTrue(judge.judge("print(0)", CASES, 2, 128).error().contains("Expected"));
    }

    @Test
    void syntaxRuntimeTimeAndMemoryErrors() {
        assertEquals(Verdict.CE, judge.judge("def f(:", CASES, 2, 128).verdict());
        Judge.Result re = judge.judge("1/0", CASES, 2, 128);
        assertEquals(Verdict.RE, re.verdict());
        assertTrue(re.error().contains("ZeroDivisionError"));
        assertEquals(Verdict.TLE, judge.judge("while True: pass", CASES, 1, 128).verdict());
        assertEquals(Verdict.MLE, judge.judge("a = [0] * (10**10)", CASES, 2, 128).verdict());
    }

    @Test
    void noTestCasesIsAnInternalError() {
        assertEquals(Verdict.IE, judge.judge(SUM, List.of(), 2, 128).verdict());
    }

    @Test
    void outputComparisonIgnoresTrailingWhitespace() {
        assertTrue(OutputComparator.matches("5  \r\n\n", "5"));
        assertTrue(OutputComparator.matches("1\n2\n", "1\n2"));
        assertTrue(!OutputComparator.matches("1 2", "1  2"));
    }

    @Test
    void unicodeInAndOut() {
        RunResult r = sandbox.run("s = input()\nprint(s[::-1], '✔ café')", "héllo 世界", 2, 128, Sandbox.DISPLAY_OUTPUT_LIMIT);
        assertEquals(RunResult.Status.OK, r.status(), r.stderr());
        assertEquals("界世 olléh ✔ café", r.stdout().strip());
    }

    @Test
    void environmentIsStripped() {
        RunResult r = sandbox.run("import os; print(os.environ.get('JAVA_HOME'))", "", 2, 128, 1000);
        assertEquals("None", r.stdout().strip());
    }

    @Test
    void displayOutputIsCappedButJudgingSeesEverything() {
        RunResult shown = sandbox.run("print('x' * 10**6)", "", 2, 128, 1000);
        assertTrue(shown.stdout().endsWith("[output truncated]") && shown.stdout().length() < 1100);
        String expected = String.join("\n", java.util.stream.IntStream.range(0, 50_000).mapToObj(String::valueOf).toList());
        Judge.Result big = judge.judge("print('\\n'.join(map(str, range(50000))))", List.of(new TestCase("", expected, false)), 2, 128);
        assertEquals(Verdict.AC, big.verdict(), big.error());
    }
}
