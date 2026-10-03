package com.pyjudge.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.pyjudge.TestSupport;
import com.pyjudge.judge.ProcessSandbox;
import com.pyjudge.judge.RunResult;
import com.pyjudge.judge.Sandbox;

class CurriculumTest {

    private static final List<CurriculumLoader.TopicSpec> COURSE = CurriculumLoader.load();

    @Test
    void courseShape() {
        assertTrue(COURSE.size() >= 10);
        for (CurriculumLoader.TopicSpec t : COURSE) {
            assertFalse(t.lessons().isEmpty(), t.slug());
            for (CurriculumLoader.LessonSpec l : t.lessons()) {
                assertTrue(l.quiz().size() >= 3, l.slug() + " needs a 3+ question quiz");
                assertFalse(CurriculumLoader.playgrounds(l.content()).isEmpty(), l.slug() + " has no runnable example");
            }
        }
    }

    /** Every runnable example in every lesson is executed for real: no lesson ships broken code. */
    @TestFactory
    Stream<DynamicTest> everyExampleRuns() {
        Sandbox sandbox = new ProcessSandbox(TestSupport.config().pythonCommand());
        List<DynamicTest> tests = new ArrayList<>();
        for (CurriculumLoader.TopicSpec t : COURSE) {
            for (CurriculumLoader.LessonSpec l : t.lessons()) {
                List<CurriculumLoader.Playground> examples = CurriculumLoader.playgrounds(l.content());
                for (int i = 0; i < examples.size(); i++) {
                    CurriculumLoader.Playground p = examples.get(i);
                    tests.add(DynamicTest.dynamicTest(t.slug() + "/" + l.slug() + " #" + (i + 1), () -> {
                        RunResult r = sandbox.run(p.code(), p.stdin(), 3, 256, Sandbox.DISPLAY_OUTPUT_LIMIT);
                        if (p.expectsError()) {
                            assertEquals(RunResult.Status.RE, r.status(), "should fail on purpose");
                        } else {
                            assertEquals(RunResult.Status.OK, r.status(), r.stderr());
                        }
                    }));
                }
            }
        }
        return tests.stream();
    }

    @Test
    void quizParserRejectsBadQuestions() {
        assertThrows(CurriculumLoader.CurriculumException.class, () ->
                CurriculumLoader.parseLesson("---\ntitle: X\n---\nbody\n<!-- quiz -->\nQ: Pick\n- [ ] a\n- [ ] b\n", "x", 0, "t"));
        assertThrows(CurriculumLoader.CurriculumException.class, () ->
                CurriculumLoader.parseLesson("---\ntitle: X\n---\nbody\n<!-- quiz -->\nQ: Pick\n- [x] a\n- [x] b\n", "x", 0, "t"));
    }

    @Test
    void lessonRenderingEscapesHtmlAndBuildsPlaygrounds() {
        String html = MarkdownRenderer.renderLesson("Intro <script>x</script>\n\n```pyrun\nprint('<b>')\n### stdin\n5\n```\n");
        assertTrue(html.contains("class=\"playground\""));
        assertTrue(html.contains("&lt;b&gt;") && !html.contains("<b>"));
        assertTrue(!html.contains("<script>"), "raw HTML from authors is escaped");
        assertTrue(html.contains("class=\"pg-stdin\""));
    }
}
