package com.pyjudge.course;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.pyjudge.model.QuizQuestion;

/**
 * Reads the built-in Python course from Markdown files on the classpath:
 * <pre>
 *   curriculum/NN-topic-slug/_topic.md       topic front matter + description
 *   curriculum/NN-topic-slug/NN-lesson.md    one lesson each, quiz after "&lt;!-- quiz --&gt;"
 * </pre>
 * A {@code ```pyrun} block is a runnable example; a line {@code ### stdin} inside it separates the
 * code from the input it reads. {@code ```pyrun-error} marks an example that fails on purpose.
 */
public final class CurriculumLoader {

    public static final String QUIZ_MARKER = "<!-- quiz -->";
    public static final String STDIN_MARKER = "### stdin";
    public static final Pattern PYRUN = Pattern.compile("```(pyrun(?:-error)?)\\n(.*?)```", Pattern.DOTALL);

    private CurriculumLoader() {
    }

    public record TopicSpec(String slug, int position, String title, String level, String summary, String description,
                            List<LessonSpec> lessons) { }

    public record LessonSpec(String slug, int position, String title, int minutes, String practice, String content,
                             List<QuizQuestion> quiz) { }

    public record Playground(String code, String stdin, boolean expectsError) { }

    /** Thrown when a lesson file is malformed (missing title, a question without one correct answer, ...). */
    public static class CurriculumException extends RuntimeException {
        public CurriculumException(String message) {
            super(message);
        }
    }

    public static List<TopicSpec> load() {
        URL root = CurriculumLoader.class.getResource("/curriculum");
        if (root == null) {
            throw new IllegalStateException("curriculum/ is missing from the classpath");
        }
        try {
            URI uri = root.toURI();
            if ("jar".equals(uri.getScheme())) {
                FileSystem fs;
                try {
                    fs = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    fs = FileSystems.newFileSystem(uri, Map.of());
                }
                return load(fs.getPath("/curriculum"));
            }
            return load(Paths.get(uri));
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<TopicSpec> load(Path root) throws IOException {
        List<TopicSpec> topics = new ArrayList<>();
        List<Path> folders;
        try (Stream<Path> s = Files.list(root)) {
            folders = s.filter(Files::isDirectory).filter(p -> Character.isDigit(name(p).charAt(0))).sorted().toList();
        }
        for (int i = 0; i < folders.size(); i++) {
            Path folder = folders.get(i);
            String[] topic = frontMatter(read(folder.resolve("_topic.md")), name(folder));
            Map<String, String> meta = parseMeta(topic[0]);
            List<Path> files;
            try (Stream<Path> s = Files.list(folder)) {
                files = s.filter(p -> name(p).endsWith(".md") && Character.isDigit(name(p).charAt(0))).sorted().toList();
            }
            List<LessonSpec> lessons = new ArrayList<>();
            for (int j = 0; j < files.size(); j++) {
                lessons.add(parseLesson(read(files.get(j)), slug(name(files.get(j))), j, name(folder) + "/" + name(files.get(j))));
            }
            topics.add(new TopicSpec(slug(name(folder)), i, require(meta, "title", name(folder)),
                    meta.getOrDefault("level", "Beginner"), meta.getOrDefault("summary", ""), topic[1].strip(), lessons));
        }
        return topics;
    }

    public static LessonSpec parseLesson(String text, String slug, int position, String where) {
        String[] parts = frontMatter(text, where);
        Map<String, String> meta = parseMeta(parts[0]);
        String body = parts[1];
        int quizAt = body.indexOf(QUIZ_MARKER);
        String content = quizAt < 0 ? body : body.substring(0, quizAt);
        String quiz = quizAt < 0 ? "" : body.substring(quizAt + QUIZ_MARKER.length());
        return new LessonSpec(slug, position, require(meta, "title", where),
                Integer.parseInt(meta.getOrDefault("minutes", "5")), meta.getOrDefault("practice", ""),
                content.strip(), parseQuiz(quiz, where));
    }

    /** Parses "Q: ... / - [ ] ... / - [x] ... / > explanation" blocks. */
    public static List<QuizQuestion> parseQuiz(String text, String where) {
        List<QuizQuestion> out = new ArrayList<>();
        String prompt = null;
        List<String> options = new ArrayList<>();
        Integer answer = null;
        StringBuilder explanation = new StringBuilder();
        for (String raw : (text + "\nQ: <end>").replace("\r\n", "\n").split("\n")) {
            String line = raw.stripTrailing();
            if (line.startsWith("Q: ")) {
                if (prompt != null) {
                    if (options.size() < 2 || answer == null) {
                        throw new CurriculumException(where + ": \"" + prompt + "\" needs 2+ options and exactly one [x]");
                    }
                    out.add(new QuizQuestion(prompt, options, answer, explanation.toString().strip()));
                }
                prompt = line.substring(3).strip();
                options = new ArrayList<>();
                answer = null;
                explanation = new StringBuilder();
            } else if (prompt == null || line.isBlank()) {
                continue;
            } else if (line.startsWith("- [ ] ") || line.startsWith("- [x] ")) {
                if (line.startsWith("- [x] ")) {
                    if (answer != null) {
                        throw new CurriculumException(where + ": two correct answers in \"" + prompt + "\"");
                    }
                    answer = options.size();
                }
                options.add(line.substring(6).strip());
            } else if (line.startsWith("> ")) {
                explanation.append(' ').append(line.substring(2).strip());
            } else if (options.isEmpty()) {
                prompt += "\n" + line; // multi-line prompt, e.g. a code snippet
            }
        }
        return out;
    }

    /** Every runnable example in a lesson's Markdown. */
    public static List<Playground> playgrounds(String content) {
        List<Playground> out = new ArrayList<>();
        Matcher m = PYRUN.matcher(content);
        while (m.find()) {
            out.add(splitPlayground(m.group(2), m.group(1).equals("pyrun-error")));
        }
        return out;
    }

    public static Playground splitPlayground(String block, boolean expectsError) {
        String marker = "\n" + STDIN_MARKER + "\n";
        int at = block.indexOf(marker);
        if (at < 0) {
            return new Playground(block.stripTrailing() + "\n", "", expectsError);
        }
        return new Playground(block.substring(0, at).stripTrailing() + "\n", block.substring(at + marker.length()), expectsError);
    }

    // ---------------------------------------------------------------- helpers

    private static String[] frontMatter(String text, String where) {
        String t = text.replace("\r\n", "\n");
        if (!t.startsWith("---\n")) {
            throw new CurriculumException(where + ": missing front matter");
        }
        int end = t.indexOf("\n---\n", 4);
        if (end < 0) {
            throw new CurriculumException(where + ": unterminated front matter");
        }
        return new String[]{t.substring(4, end), t.substring(end + 5)};
    }

    private static Map<String, String> parseMeta(String head) {
        Map<String, String> meta = new LinkedHashMap<>();
        for (String line : head.split("\n")) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                meta.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
            }
        }
        return meta;
    }

    private static String require(Map<String, String> meta, String key, String where) {
        String v = meta.get(key);
        if (v == null || v.isBlank()) {
            throw new CurriculumException(where + ": missing " + key);
        }
        return v;
    }

    private static String slug(String fileName) {
        return fileName.replaceFirst("\\.md$", "").replaceFirst("^\\d+-", "");
    }

    private static String name(Path p) {
        return p.getFileName().toString().replace("/", "");
    }

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
