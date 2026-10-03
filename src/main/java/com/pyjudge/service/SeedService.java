package com.pyjudge.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyjudge.course.CourseService;
import com.pyjudge.dao.ChallengeDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.TestCase;
import com.pyjudge.util.Slugs;
import com.pyjudge.util.Times;

/**
 * Loads the built-in content on startup. Safe to run repeatedly:
 * exercises are added only if missing (matched by title), and the course is synced.
 */
public class SeedService {

    public static final String EXERCISES = "/data/exercises.json.gz";

    private final ProblemDao problems;
    private final ChallengeDao challenges;
    private final CourseService course;

    public SeedService(ProblemDao problems, ChallengeDao challenges, CourseService course) {
        this.problems = problems;
        this.challenges = challenges;
        this.course = course;
    }

    public record Report(int exercisesAdded, CourseService.SyncReport course) { }

    public Report seed() {
        int added = seedExercises();
        CourseService.SyncReport sync = course.syncCurriculum();
        seedChallenge();
        return new Report(added, sync);
    }

    /** Exercise bank: statements plus generated test cases, exported from the reference solutions. */
    public static JsonNode readBank() {
        try (InputStream raw = SeedService.class.getResourceAsStream(EXERCISES)) {
            if (raw == null) {
                throw new IllegalStateException("Missing " + EXERCISES);
            }
            try (InputStream in = new GZIPInputStream(raw)) {
                return new ObjectMapper().readTree(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    int seedExercises() {
        int added = 0;
        for (JsonNode ex : readBank()) {
            String title = ex.get("title").asText();
            if (problems.findByTitle(title).isPresent()) {
                continue;
            }
            Problem p = new Problem();
            p.setTitle(title);
            p.setSlug(Slugs.unique(title, s -> problems.slugExists(s, 0)));
            p.setDescription(ex.get("description").asText());
            p.setDifficulty(Difficulty.fromLabel(ex.get("difficulty").asText()));
            p.setTags(ex.get("tags").asText());
            p.setStarterCode(ex.get("starterCode").asText());
            p.setTimeLimit(ex.get("timeLimit").asInt(2));
            p.setStatus(ContentStatus.APPROVED);
            List<TestCase> cases = new ArrayList<>();
            for (JsonNode t : ex.get("tests")) {
                cases.add(new TestCase(t.get("input").asText(), t.get("output").asText(), t.get("sample").asBoolean()));
            }
            p.setTestCases(cases);
            problems.save(p);
            added++;
        }
        return added;
    }

    void seedChallenge() {
        if (challenges.count() > 0) {
            return;
        }
        List<Problem> easy = problems.search(ContentStatus.APPROVED, Difficulty.EASY, null, null, null)
                .stream().limit(5).toList();
        if (easy.isEmpty()) {
            return;
        }
        Challenge c = new Challenge();
        c.setTitle("Python Starter Sprint");
        c.setDescription("Solve all five warm-up exercises within the challenge window to complete it.");
        c.setStartAt(Times.now().minusDays(1).withMinute(0).withSecond(0));
        c.setEndAt(c.getStartAt().plusDays(31));
        c.setProblems(new ArrayList<>(easy));
        challenges.save(c);
    }
}
