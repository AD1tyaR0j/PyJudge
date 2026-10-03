package com.pyjudge.judge;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Runs each program as a separate {@code python -I} process in its own temporary directory:
 * <ul>
 *   <li>stripped environment (no secrets leak into user code);</li>
 *   <li>wall-clock time limit: the process and everything it spawned are killed when it expires;</li>
 *   <li>stdout/stderr go to files and are read back with a size cap;</li>
 *   <li>on Linux/macOS, {@code ulimit -v} caps memory.</li>
 * </ul>
 * This is suitable for development and classroom use. A public deployment should use a container sandbox.
 */
public class ProcessSandbox implements Sandbox {

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private final String python;

    public ProcessSandbox(String pythonCommand) {
        this.python = pythonCommand;
    }

    @Override
    public RunResult run(String code, String stdin, int timeLimitSeconds, int memoryLimitMb, int maxOutputBytes) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("judge_");
            Path source = dir.resolve("main.py");
            Path in = dir.resolve("input.txt");
            Path out = dir.resolve("stdout.txt");
            Path err = dir.resolve("stderr.txt");
            Files.writeString(source, code, StandardCharsets.UTF_8);
            Files.writeString(in, stdin == null ? "" : stdin, StandardCharsets.UTF_8);

            ProcessBuilder pb = new ProcessBuilder(command(memoryLimitMb))
                    .directory(dir.toFile())
                    .redirectInput(in.toFile())
                    .redirectOutput(out.toFile())
                    .redirectError(err.toFile());
            cleanEnvironment(pb.environment());

            long start = System.nanoTime();
            Process process = pb.start();
            boolean finished = process.waitFor(timeLimitSeconds, TimeUnit.SECONDS);
            if (!finished) {
                // kill grandchildren too (e.g. code that starts subprocesses)
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000;

            String stdout = readCapped(out, maxOutputBytes);
            String stderr = readCapped(err, maxOutputBytes).replace(dir.toString() + java.io.File.separator, "")
                    .replace(dir.toString(), "");
            Integer exit = finished ? process.exitValue() : null;
            return new RunResult(classify(finished, exit, stderr), stdout, stderr, exit, elapsed);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start Python (" + python + "). Is it installed?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RunResult(RunResult.Status.RE, "", "Interrupted", null, 0);
        } finally {
            deleteQuietly(dir);
        }
    }

    private List<String> command(int memoryLimitMb) {
        List<String> cmd = new ArrayList<>();
        if (WINDOWS) {
            // -X utf8: isolated mode ignores PYTHONIOENCODING; Windows would otherwise encode output as cp1252
            cmd.addAll(List.of(python, "-I", "-X", "utf8", "main.py"));
        } else {
            long kb = (long) memoryLimitMb * 1024 + 64 * 1024; // headroom for the interpreter itself
            cmd.addAll(List.of("/bin/sh", "-c", "ulimit -v " + kb + "; exec " + python + " -I -X utf8 main.py"));
        }
        return cmd;
    }

    private static void cleanEnvironment(Map<String, String> env) {
        Map<String, String> keep = new java.util.HashMap<>();
        for (String key : List.of("PATH", "SYSTEMROOT", "SystemRoot", "TEMP", "TMP", "HOME")) {
            if (env.containsKey(key)) {
                keep.put(key, env.get(key));
            }
        }
        env.clear();
        env.putAll(keep);
        env.put("PYTHONIOENCODING", "utf-8");
        env.put("PYTHONDONTWRITEBYTECODE", "1");
    }

    static RunResult.Status classify(boolean finished, Integer exit, String stderr) {
        if (!finished) {
            return RunResult.Status.TLE;
        }
        if (stderr.contains("MemoryError")) {
            return RunResult.Status.MLE;
        }
        if (exit != null && exit != 0) {
            String last = lastLine(stderr);
            if (last.startsWith("SyntaxError") || last.startsWith("IndentationError") || last.startsWith("TabError")) {
                return RunResult.Status.CE;
            }
            return RunResult.Status.RE;
        }
        return RunResult.Status.OK;
    }

    private static String lastLine(String text) {
        String[] lines = text.strip().split("\\R");
        return lines.length == 0 ? "" : lines[lines.length - 1].trim();
    }

    private static String readCapped(Path file, int limit) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] data = in.readNBytes(limit + 1);
            boolean truncated = data.length > limit;
            String text = new String(data, 0, Math.min(data.length, limit), StandardCharsets.UTF_8);
            return truncated ? text + "\n…[output truncated]" : text;
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // a leftover temp folder is harmless
        }
    }
}
