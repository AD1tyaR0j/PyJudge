package com.pyjudge.course;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Markdown to HTML for lessons, exercise statements, and quiz text.
 * Raw HTML written by authors is escaped (not executed), and {@code ```pyrun} blocks become
 * interactive playgrounds.
 */
public final class MarkdownRenderer {

    private static final List<org.commonmark.Extension> EXT = List.of(TablesExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXT).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXT).escapeHtml(true).build();
    private static final String TOKEN = "PYJUDGEPLAYGROUND%dX";

    private MarkdownRenderer() {
    }

    public static String render(String markdown) {
        return RENDERER.render(PARSER.parse(markdown == null ? "" : markdown));
    }

    /** For short snippets such as quiz options: no surrounding paragraph tags. */
    public static String renderInline(String markdown) {
        String html = render(markdown).strip();
        if (html.startsWith("<p>") && html.endsWith("</p>") && html.indexOf("<p>", 1) < 0) {
            html = html.substring(3, html.length() - 4);
        }
        return html;
    }

    /** Lesson body: Markdown plus runnable playgrounds. */
    public static String renderLesson(String markdown) {
        List<CurriculumLoader.Playground> blocks = new ArrayList<>();
        Matcher m = CurriculumLoader.PYRUN.matcher(markdown == null ? "" : markdown);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            blocks.add(CurriculumLoader.splitPlayground(m.group(2), m.group(1).equals("pyrun-error")));
            m.appendReplacement(sb, Matcher.quoteReplacement("\n\n" + TOKEN.formatted(blocks.size() - 1) + "\n\n"));
        }
        m.appendTail(sb);
        String html = render(sb.toString());
        for (int i = 0; i < blocks.size(); i++) {
            html = html.replace("<p>" + TOKEN.formatted(i) + "</p>", playground(blocks.get(i)));
        }
        return html;
    }

    private static String playground(CurriculumLoader.Playground p) {
        int rows = Math.max(3, (int) p.code().lines().count());
        int inputRows = Math.max(2, (int) p.stdin().lines().count());
        return "<div class=\"playground\" data-expect-error=\"" + (p.expectsError() ? 1 : 0) + "\">"
                + "<div class=\"pg-bar\"><span class=\"pg-title\">" + (p.expectsError() ? "Example (raises an error)" : "Example")
                + "</span><span class=\"pg-actions\"><button type=\"button\" class=\"pg-reset\">Reset</button>"
                + "<button type=\"button\" class=\"pg-run\">Run</button></span></div>"
                + "<textarea class=\"pg-code\" spellcheck=\"false\" rows=\"" + rows + "\" aria-label=\"Python code\">"
                + escape(p.code()) + "</textarea>"
                + "<details class=\"pg-input\"" + (p.stdin().isEmpty() ? "" : " open") + "><summary>Input</summary>"
                + "<textarea class=\"pg-stdin\" spellcheck=\"false\" rows=\"" + inputRows + "\" aria-label=\"Program input\">"
                + escape(p.stdin()) + "</textarea></details>"
                + "<pre class=\"pg-output\" hidden></pre></div>";
    }

    public static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
