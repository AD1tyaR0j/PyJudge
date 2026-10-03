package com.pyjudge.judge;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.Verdict;

/**
 * Judges submissions in the background so a web request never waits for test cases to run.
 * <ul>
 *   <li>A fixed {@link ThreadPoolExecutor} of "judge-N" worker threads takes queued submissions;</li>
 *   <li>a {@link Semaphore} caps how many interactive "Run" requests execute at once;</li>
 *   <li>atomic counters let the admin dashboard show live judge statistics safely.</li>
 * </ul>
 */
public class JudgeService implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(JudgeService.class.getName());

    private final ThreadPoolExecutor pool;
    private final Semaphore runSlots;
    private final AtomicLong judged = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final Judge judge;
    private final Sandbox sandbox;
    private final SubmissionDao submissions;
    private final ProblemDao problems;
    private final boolean synchronous;

    public JudgeService(Sandbox sandbox, SubmissionDao submissions, ProblemDao problems, int workers, boolean synchronous) {
        this.sandbox = sandbox;
        this.judge = new Judge(sandbox);
        this.submissions = submissions;
        this.problems = problems;
        this.synchronous = synchronous;
        this.pool = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), namedThreads("judge-"));
        this.runSlots = new Semaphore(workers * 2, true);
    }

    private static ThreadFactory namedThreads(String prefix) {
        AtomicInteger n = new AtomicInteger(1);
        return task -> {
            Thread t = new Thread(task, prefix + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }

    /** Queues a saved submission for judging. Returns a Future that completes when its verdict is stored. */
    public Future<?> enqueue(long submissionId) {
        if (synchronous) {
            judgeNow(submissionId);
            return CompletableFuture.completedFuture(null);
        }
        return pool.submit(() -> judgeNow(submissionId));
    }

    /** Runs on a worker thread: judge, then save the verdict. Never lets an exception kill the worker. */
    void judgeNow(long submissionId) {
        Submission sub = submissions.findById(submissionId).orElse(null);
        if (sub == null) {
            return;
        }
        try {
            Problem p = problems.findById(sub.getProblemId()).orElseThrow();
            Judge.Result r = judge.judge(sub.getCode(), p.getTestCases(), p.getTimeLimit(), p.getMemoryLimit());
            sub.setVerdict(r.verdict());
            sub.setPassed(r.passed());
            sub.setTotal(r.total());
            sub.setRuntimeMs(r.runtimeMs());
            sub.setError(r.error());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Judging failed for submission " + submissionId, e);
            failures.incrementAndGet();
            sub.setVerdict(Verdict.IE);
            sub.setError("The judge hit an internal error. Please try again.");
        }
        submissions.saveResult(sub);
        judged.incrementAndGet();
    }

    /**
     * Interactive "Run" with custom input. Waits briefly for a free slot so a burst of clicks
     * can't start dozens of Python processes at once.
     */
    public RunResult runInteractive(String code, String stdin, int timeLimit, int memoryLimit) {
        boolean acquired = false;
        try {
            acquired = runSlots.tryAcquire(10, TimeUnit.SECONDS);
            if (!acquired) {
                return new RunResult(RunResult.Status.RE, "", "The server is busy. Please try again in a moment.", null, 0);
            }
            return sandbox.run(code, stdin, timeLimit, memoryLimit, Sandbox.DISPLAY_OUTPUT_LIMIT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RunResult(RunResult.Status.RE, "", "Interrupted", null, 0);
        } finally {
            if (acquired) {
                runSlots.release();
            }
        }
    }

    public Judge judge() {
        return judge;
    }

    // live statistics for the admin dashboard
    public int getWorkers() { return pool.getCorePoolSize(); }
    public int getActive() { return pool.getActiveCount(); }
    public int getQueued() { return pool.getQueue().size(); }
    public long getJudged() { return judged.get(); }
    public long getFailures() { return failures.get(); }

    /** Lets in-flight submissions finish (up to 10s) when the web app shuts down. */
    @Override
    public void close() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
