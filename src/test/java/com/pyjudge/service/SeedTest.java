package com.pyjudge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.pyjudge.Services;
import com.pyjudge.TestSupport;
import com.pyjudge.judge.Judge;
import com.pyjudge.judge.ProcessSandbox;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.Verdict;

class SeedTest {

    @Test
    void seedIsIdempotent() {
        try (Services s = TestSupport.services()) {
            SeedService.Report first = s.seed.seed();
            assertEquals(37, first.exercisesAdded());
            assertEquals(11, first.course().topicsCreated());
            assertEquals(32, first.course().lessonsCreated());
            SeedService.Report second = s.seed.seed();
            assertEquals(0, second.exercisesAdded());
            assertEquals(0, second.course().lessonsCreated());
            assertEquals(32, second.course().lessonsUpdated());
            assertEquals(37, s.problems.countApproved());
            assertEquals(1, s.challenges.count());
            assertEquals(3, s.problems.findByTitle("Edit Distance").map(p -> p.getTimeLimit()).orElse(0));
        }
    }

    /** Each exercise ships with an independent solution; it must be Accepted against the bank's tests. */
    @TestFactory
    Stream<DynamicTest> everyExerciseIsSolvable() {
        Judge judge = new Judge(new ProcessSandbox(TestSupport.config().pythonCommand()));
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode ex : SeedService.readBank()) {
            if (ex.get("solution") == null || ex.get("solution").isNull()) {
                continue;
            }
            tests.add(DynamicTest.dynamicTest(ex.get("title").asText(), () -> {
                List<TestCase> cases = new ArrayList<>();
                ex.get("tests").forEach(t -> cases.add(new TestCase(t.get("input").asText(), t.get("output").asText(),
                        t.get("sample").asBoolean())));
                Judge.Result r = judge.judge(ex.get("solution").asText(), cases, ex.get("timeLimit").asInt(2), 256);
                assertEquals(Verdict.AC, r.verdict(), r.error());
            }));
        }
        assertTrue(tests.size() >= 27);
        return tests.stream();
    }
}
