package com.pyjudge.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.pyjudge.course.CourseOverview;
import com.pyjudge.dao.CourseDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.dao.UserDao;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.util.Times;

/** Leaderboards, charts, streaks, achievements, and instructor reports. Mostly Collections/Streams over DAO rows. */
public class StatsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d");

    private final SubmissionDao submissions;
    private final UserDao users;
    private final CourseDao course;

    public StatsService(SubmissionDao submissions, UserDao users, CourseDao course) {
        this.submissions = submissions;
        this.users = users;
        this.course = course;
    }

    // ---------------------------------------------------------------- leaderboard

    public static class LeaderboardRow {
        private final User user;
        private final int solved;
        private final int score;
        private int rank;

        LeaderboardRow(User user, int solved, int score) {
            this.user = user;
            this.solved = solved;
            this.score = score;
        }

        public User getUser() { return user; }
        public int getSolved() { return solved; }
        public int getScore() { return score; }
        public int getRank() { return rank; }
    }

    /** Ranked by points (Easy 10, Medium 20, Hard 40), then solves, then username. Null filters mean "all". */
    public List<LeaderboardRow> leaderboard(Integer limit, Collection<Long> problemIds, LocalDateTime from, LocalDateTime to) {
        Map<Long, int[]> totals = new HashMap<>(); // user -> {solved, score}
        for (long[] row : submissions.solvePairs(problemIds, from, to)) {
            int[] t = totals.computeIfAbsent(row[0], k -> new int[2]);
            t[0]++;
            t[1] += (int) row[2];
        }
        Map<Long, User> byId = users.findByIds(new ArrayList<>(totals.keySet())).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<LeaderboardRow> rows = new ArrayList<>();
        totals.forEach((uid, t) -> {
            if (byId.containsKey(uid)) {
                rows.add(new LeaderboardRow(byId.get(uid), t[0], t[1]));
            }
        });
        rows.sort((a, b) -> a.score != b.score ? b.score - a.score
                : a.solved != b.solved ? b.solved - a.solved
                : a.user.getUsername().compareToIgnoreCase(b.user.getUsername()));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).rank = i + 1;
        }
        return limit == null ? rows : rows.subList(0, Math.min(limit, rows.size()));
    }

    public Integer rankOf(long userId) {
        return leaderboard(null, null, null, null).stream().filter(r -> r.user.getId() == userId)
                .map(LeaderboardRow::getRank).findFirst().orElse(null);
    }

    public int score(Collection<Problem> solved) {
        return solved.stream().mapToInt(p -> p.getDifficulty().getPoints()).sum();
    }

    // ---------------------------------------------------------------- charts

    public record Series(List<String> labels, Map<String, List<Integer>> data) { }

    private static List<LocalDate> lastDays(int days) {
        LocalDate today = Times.now().toLocalDate();
        List<LocalDate> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            out.add(today.minusDays(i));
        }
        return out;
    }

    /** Admin activity chart: submissions (accepted vs not), active students, and sign-ups per day. */
    public Series activity(int days) {
        List<LocalDate> labels = lastDays(days);
        LocalDateTime since = labels.get(0).atStartOfDay();
        Map<LocalDate, Integer> accepted = new HashMap<>();
        Map<LocalDate, Integer> other = new HashMap<>();
        Map<LocalDate, Set<Long>> active = new HashMap<>();
        for (Object[] row : submissions.activitySince(since)) {
            LocalDate d = ((LocalDateTime) row[0]).toLocalDate();
            (("AC".equals(row[1])) ? accepted : other).merge(d, 1, Integer::sum);
            active.computeIfAbsent(d, k -> new HashSet<>()).add((Long) row[2]);
        }
        Map<LocalDate, Integer> signups = new HashMap<>();
        users.signupTimesSince(since).forEach(t -> signups.merge(t.toLocalDate(), 1, Integer::sum));

        Map<String, List<Integer>> data = new LinkedHashMap<>();
        data.put("accepted", labels.stream().map(d -> accepted.getOrDefault(d, 0)).toList());
        data.put("other", labels.stream().map(d -> other.getOrDefault(d, 0)).toList());
        data.put("active", labels.stream().map(d -> active.getOrDefault(d, Set.of()).size()).toList());
        data.put("signups", labels.stream().map(d -> signups.getOrDefault(d, 0)).toList());
        return new Series(labels.stream().map(DAY::format).toList(), data);
    }

    /** Student chart: cumulative distinct exercises solved per day. */
    public Series solveTimeline(long userId, int days) {
        List<LocalDate> labels = lastDays(days);
        Collection<LocalDateTime> firsts = submissions.firstSolveTimes(userId).values();
        List<Integer> cumulative = labels.stream()
                .map(d -> (int) firsts.stream().filter(t -> !t.toLocalDate().isAfter(d)).count()).toList();
        return new Series(labels.stream().map(DAY::format).toList(), Map.of("solved", cumulative));
    }

    /** Consecutive days (ending today or yesterday) with a submission or a completed lesson. */
    public int streak(long userId) {
        Set<LocalDate> days = new HashSet<>();
        submissions.submissionTimes(userId).forEach(t -> days.add(t.toLocalDate()));
        course.completionTimes(userId).forEach(t -> days.add(t.toLocalDate()));
        LocalDate cursor = Times.now().toLocalDate();
        if (!days.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int count = 0;
        while (days.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    /** solved/total per difficulty, for progress bars. */
    public Map<String, int[]> byDifficulty(List<Problem> approved, Set<Long> solvedIds) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Difficulty d : Difficulty.values()) {
            out.put(d.getLabel(), new int[2]);
        }
        for (Problem p : approved) {
            int[] v = out.get(p.getDifficulty().getLabel());
            v[1]++;
            if (solvedIds.contains(p.getId())) {
                v[0]++;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- achievements

    public static class Achievement {
        private final String code;
        private final String name;
        private final String description;
        private final boolean earned;
        private final String progress;

        Achievement(String code, String name, String description, boolean earned, String progress) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.earned = earned;
            this.progress = progress;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isEarned() { return earned; }
        public String getProgress() { return progress; }
    }

    /** Badges computed from activity; nothing extra is stored. */
    public List<Achievement> achievements(CourseOverview course, int solved, boolean solvedHard, int streak,
                                          boolean challengeDone) {
        int done = course.getDone();
        long perfect = course.getPerfectQuizzes();
        long topics = course.getTopicsComplete();
        return List.of(
                new Achievement("L1", "First steps", "Complete your first lesson", done >= 1, Math.min(done, 1) + "/1"),
                new Achievement("L10", "Bookworm", "Complete 10 lessons", done >= 10, Math.min(done, 10) + "/10"),
                new Achievement("T1", "Topic master", "Finish every lesson in a topic", topics >= 1, topics + " topics"),
                new Achievement("ALL", "Graduate", "Complete the whole course",
                        course.getTotal() > 0 && done == course.getTotal(), done + "/" + course.getTotal()),
                new Achievement("Q5", "Quiz whiz", "Get 5 perfect quiz scores", perfect >= 5, Math.min(perfect, 5) + "/5"),
                new Achievement("S10", "Problem solver", "Solve 10 exercises", solved >= 10, Math.min(solved, 10) + "/10"),
                new Achievement("H1", "Hard hitter", "Solve a Hard exercise", solvedHard, solvedHard ? "1/1" : "0/1"),
                new Achievement("D7", "Consistent", "Keep a 7-day streak", streak >= 7, Math.min(streak, 7) + "/7 days"),
                new Achievement("C1", "Challenger", "Complete a challenge", challengeDone, challengeDone ? "done" : "0/1"));
    }

    // ---------------------------------------------------------------- instructor reports

    public static class ExerciseReport {
        private final Problem problem;
        private final long submissions;
        private final long students;
        private final long solvers;
        private final Integer acceptance;
        private final Integer averageGrade;

        ExerciseReport(Problem problem, long[] stats, Double avgGrade) {
            this.problem = problem;
            this.submissions = stats == null ? 0 : stats[0];
            this.acceptance = stats == null || stats[0] == 0 ? null : Math.round(100f * stats[1] / stats[0]);
            this.students = stats == null ? 0 : stats[2];
            this.solvers = stats == null ? 0 : stats[3];
            this.averageGrade = avgGrade == null ? null : (int) Math.round(avgGrade);
        }

        public Problem getProblem() { return problem; }
        public long getSubmissions() { return submissions; }
        public long getStudents() { return students; }
        public long getSolvers() { return solvers; }
        public Integer getAcceptance() { return acceptance; }
        public Integer getAverageGrade() { return averageGrade; }
    }

    public static class StudentReport {
        private final User user;
        private final int attempted;
        private final int solved;
        private final int submissions;
        private final Integer averageGrade;
        private final LocalDateTime lastActive;
        private final int progress;
        private final long lessonsDone;

        StudentReport(User user, int attempted, int solved, int submissions, Integer averageGrade,
                      LocalDateTime lastActive, int progress, long lessonsDone) {
            this.user = user;
            this.attempted = attempted;
            this.solved = solved;
            this.submissions = submissions;
            this.averageGrade = averageGrade;
            this.lastActive = lastActive;
            this.progress = progress;
            this.lessonsDone = lessonsDone;
        }

        public User getUser() { return user; }
        public int getAttempted() { return attempted; }
        public int getSolved() { return solved; }
        public int getSubmissions() { return submissions; }
        public Integer getAverageGrade() { return averageGrade; }
        public LocalDateTime getLastActive() { return lastActive; }
        public int getProgress() { return progress; }
        public long getLessonsDone() { return lessonsDone; }
    }

    public List<ExerciseReport> exerciseReports(List<Problem> problems) {
        Map<Long, long[]> stats = submissions.acceptanceStats();
        Map<Long, Double> grades = submissions.averageGrades();
        return problems.stream().map(p -> new ExerciseReport(p, stats.get(p.getId()), grades.get(p.getId()))).toList();
    }

    public List<StudentReport> studentReports(List<Problem> problems) {
        List<Long> ids = problems.stream().map(Problem::getId).toList();
        Map<Long, Set<Long>> attempted = new HashMap<>();
        Map<Long, Set<Long>> solved = new HashMap<>();
        Map<Long, List<Integer>> grades = new HashMap<>();
        Map<Long, Integer> counts = new HashMap<>();
        Map<Long, LocalDateTime> last = new HashMap<>();
        for (Object[] r : submissions.reportRows(ids)) {
            long uid = (Long) r[0];
            long pid = (Long) r[1];
            attempted.computeIfAbsent(uid, k -> new HashSet<>()).add(pid);
            if ("AC".equals(r[2])) {
                solved.computeIfAbsent(uid, k -> new HashSet<>()).add(pid);
            }
            if (r[3] != null) {
                grades.computeIfAbsent(uid, k -> new ArrayList<>()).add((Integer) r[3]);
            }
            counts.merge(uid, 1, Integer::sum);
            last.merge(uid, (LocalDateTime) r[4], (a, b) -> a.isAfter(b) ? a : b);
        }
        Map<Long, Long> lessons = course.completionsByUser();
        Map<Long, User> byId = users.findByIds(new ArrayList<>(attempted.keySet())).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        TreeMap<String, StudentReport> sorted = new TreeMap<>();
        for (Long uid : attempted.keySet()) {
            User u = byId.get(uid);
            if (u == null) {
                continue;
            }
            int s = solved.getOrDefault(uid, Set.of()).size();
            List<Integer> g = grades.getOrDefault(uid, List.of());
            Integer avg = g.isEmpty() ? null : (int) Math.round(g.stream().mapToInt(Integer::intValue).average().orElse(0));
            StudentReport report = new StudentReport(u, attempted.get(uid).size(), s, counts.get(uid), avg, last.get(uid),
                    problems.isEmpty() ? 0 : Math.round(100f * s / problems.size()), lessons.getOrDefault(uid, 0L));
            // sort by solved desc, then submissions desc, then name
            sorted.put(String.format("%06d-%06d-%s", 999_999 - s, 999_999 - counts.get(uid), u.getUsername()), report);
        }
        return new ArrayList<>(sorted.values());
    }
}
