package com.pyjudge;

import com.pyjudge.config.AppConfig;
import com.pyjudge.course.CourseService;
import com.pyjudge.dao.ChallengeDao;
import com.pyjudge.dao.CourseDao;
import com.pyjudge.dao.MessageDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.SettingsDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.dao.UserDao;
import com.pyjudge.db.Database;
import com.pyjudge.judge.JudgeService;
import com.pyjudge.judge.ProcessSandbox;
import com.pyjudge.service.AuthService;
import com.pyjudge.service.ChallengeService;
import com.pyjudge.service.ContentService;
import com.pyjudge.service.MessageService;
import com.pyjudge.service.PasswordHasher;
import com.pyjudge.service.SeedService;
import com.pyjudge.service.SettingsService;
import com.pyjudge.service.StatsService;
import com.pyjudge.service.SubmissionService;

/**
 * Wires the application together (manual dependency injection): one Database, the DAOs on top of it,
 * and the services on top of those. Created once when the web app starts and closed when it stops.
 */
public final class Services implements AutoCloseable {

    public static final String ATTRIBUTE = "pyjudge.services";

    public final Database db;
    public final UserDao users;
    public final ProblemDao problems;
    public final SubmissionDao submissions;
    public final MessageDao messages;
    public final ChallengeDao challenges;
    public final CourseDao courseDao;

    public final SettingsService settings;
    public final AuthService auth;
    public final JudgeService judge;
    public final SubmissionService submissionService;
    public final ContentService content;
    public final CourseService course;
    public final StatsService stats;
    public final MessageService messaging;
    public final ChallengeService challengeService;
    public final SeedService seed;

    public Services(AppConfig config) {
        db = new Database(config.databaseUrl());
        db.runScript("/db/schema.sql");

        users = new UserDao(db);
        problems = new ProblemDao(db);
        submissions = new SubmissionDao(db);
        messages = new MessageDao(db);
        challenges = new ChallengeDao(db);
        courseDao = new CourseDao(db);

        settings = new SettingsService(new SettingsDao(db));
        auth = new AuthService(users, new PasswordHasher(config.passwordIterations()), settings);
        judge = new JudgeService(new ProcessSandbox(config.pythonCommand()), submissions, problems,
                config.judgeWorkers(), config.judgeSync());
        submissionService = new SubmissionService(db, submissions, problems, messages, judge, settings);
        content = new ContentService(db, problems, courseDao, messages, settings);
        course = new CourseService(courseDao, problems);
        stats = new StatsService(submissions, users, courseDao);
        messaging = new MessageService(messages, users, settings);
        challengeService = new ChallengeService(challenges, problems, submissions, stats);
        seed = new SeedService(problems, challenges, course);
    }

    @Override
    public void close() {
        judge.close();
        db.close();
    }
}
