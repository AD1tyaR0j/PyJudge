package com.pyjudge.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pyjudge.dao.ChallengeDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.util.Times;

/** Time-boxed challenges: create, list by state, track progress, and rank participants. */
public class ChallengeService {

    private final ChallengeDao challenges;
    private final ProblemDao problems;
    private final SubmissionDao submissions;
    private final StatsService stats;

    public ChallengeService(ChallengeDao challenges, ProblemDao problems, SubmissionDao submissions, StatsService stats) {
        this.challenges = challenges;
        this.problems = problems;
        this.submissions = submissions;
        this.stats = stats;
    }

    /** A challenge plus one user's progress in it. */
    public static class ChallengeProgress {
        private final Challenge challenge;
        private final Set<Long> solved;

        ChallengeProgress(Challenge challenge, Set<Long> solved) {
            this.challenge = challenge;
            this.solved = solved;
        }

        public Challenge getChallenge() { return challenge; }
        public Set<Long> getSolvedIds() { return solved; }
        public int getSolved() { return solved.size(); }
        public int getTotal() { return challenge.getApprovedProblems().size(); }
        public int getPercent() { return getTotal() == 0 ? 0 : Math.round(100f * getSolved() / getTotal()); }
        public boolean isCompleted() { return getTotal() > 0 && getSolved() == getTotal(); }
    }

    public Challenge get(long id) {
        return challenges.findById(id).orElseThrow(() -> new NotFoundException("Challenge"));
    }

    /** Exercises the user solved inside the challenge window. */
    public ChallengeProgress progress(Challenge c, User user) {
        List<Long> ids = c.getApprovedProblems().stream().map(Problem::getId).toList();
        Set<Long> solved = user == null ? Set.of() : submissions.solvedBetween(user.getId(), ids, c.getStartAt(), c.getEndAt());
        return new ChallengeProgress(c, solved);
    }

    public Map<Challenge.State, List<ChallengeProgress>> grouped(User user) {
        Map<Challenge.State, List<ChallengeProgress>> out = new EnumMap<>(Challenge.State.class);
        for (Challenge.State s : Challenge.State.values()) {
            out.put(s, new ArrayList<>());
        }
        for (Challenge c : challenges.findAll()) {
            out.get(c.getState()).add(progress(c, user));
        }
        return out;
    }

    public List<StatsService.LeaderboardRow> standings(Challenge c) {
        List<Long> ids = c.getApprovedProblems().stream().map(Problem::getId).toList();
        return ids.isEmpty() ? List.of() : stats.leaderboard(null, ids, c.getStartAt(), c.getEndAt());
    }

    public boolean anyCompleted(User user) {
        return challenges.findAll().stream().anyMatch(c -> progress(c, user).isCompleted());
    }

    /** Validates and saves (create when {@code existing} is null). Only approved exercises can be included. */
    public Challenge save(Challenge existing, User author, String title, String description, String start, String end,
                          List<Long> problemIds) {
        if (author == null || !author.canAuthor() || (existing != null && !author.canEdit(existing))) {
            throw new ForbiddenException();
        }
        List<String> errors = new ArrayList<>();
        Challenge c = existing == null ? new Challenge() : existing;
        c.setTitle(title == null ? "" : title.trim());
        c.setDescription(description == null ? "" : description.trim());
        if (c.getTitle().isEmpty()) {
            errors.add("Title is required.");
        }
        try {
            c.setStartAt(LocalDateTime.parse(start, Times.INPUT));
            c.setEndAt(LocalDateTime.parse(end, Times.INPUT));
            if (!c.getEndAt().isAfter(c.getStartAt())) {
                errors.add("The end time must be after the start time.");
            }
        } catch (DateTimeParseException | NullPointerException e) {
            errors.add("Enter valid start and end times.");
        }
        List<Problem> chosen = problemIds.stream().map(id -> problems.findById(id).orElse(null))
                .filter(p -> p != null && p.isApproved()).toList();
        if (chosen.isEmpty()) {
            errors.add("Pick at least one approved exercise.");
        }
        ValidationException.throwIfAny(errors);
        c.setProblems(new ArrayList<>(chosen));
        if (existing == null) {
            c.setCreatedBy(author.getId());
        }
        return challenges.save(c);
    }

    public void delete(long id, User user) {
        Challenge c = get(id);
        if (user == null || !user.canEdit(c)) {
            throw new ForbiddenException();
        }
        challenges.delete(id);
    }
}
