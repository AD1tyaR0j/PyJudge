"""Rendering lessons and picking practice exercises for them."""
import re

import markdown as md
from markupsafe import Markup, escape

from .curriculum import PYRUN_RE, split_playground
from .models import Problem

_TOKEN = "PYJUDGEPLAYGROUND{}X"


def _playground_html(code, stdin, expects_error):
    rows = max(3, code.count("\n") + 1)
    return (
        f'<div class="playground" data-expect-error="{1 if expects_error else 0}">'
        f'<div class="pg-bar"><span class="pg-title">▶ Try it{" (this one fails on purpose)" if expects_error else ""}</span>'
        f'<span class="pg-actions"><button type="button" class="pg-reset">Reset</button>'
        f'<button type="button" class="pg-run">Run</button></span></div>'
        f'<textarea class="pg-code" spellcheck="false" rows="{rows}" aria-label="Python code">{escape(code)}</textarea>'
        f'<details class="pg-input"{" open" if stdin else ""}><summary>Input (stdin)</summary>'
        f'<textarea class="pg-stdin" spellcheck="false" rows="{max(2, stdin.count(chr(10)) + 1)}" aria-label="Program input">{escape(stdin)}</textarea></details>'
        f'<pre class="pg-output" hidden></pre></div>'
    )


def render_lesson(content):
    """Markdown -> HTML, with ```pyrun blocks turned into runnable playgrounds."""
    blocks = []

    def stash(match):
        blocks.append((*split_playground(match.group(2)), match.group(1) == "pyrun-error"))
        return "\n\n" + _TOKEN.format(len(blocks) - 1) + "\n\n"

    text = PYRUN_RE.sub(stash, content or "")
    html = md.markdown(text, extensions=["fenced_code", "tables"])
    for i, block in enumerate(blocks):
        html = html.replace(f"<p>{_TOKEN.format(i)}</p>", _playground_html(*block))
    return Markup(html)


def render_inline(text):
    """Markdown for short snippets (quiz options): no wrapping <p>."""
    html = md.markdown(text or "", extensions=["fenced_code"])
    return Markup(re.sub(r"^<p>(.*)</p>$", r"\1", html, flags=re.S))


def practice_problems(lesson, solved_ids=frozenset(), limit=4):
    """Approved exercises sharing a tag with the lesson, unsolved and easier ones first."""
    tags = set(lesson.practice_tag_list)
    if not tags:
        return []
    order = {"Easy": 0, "Medium": 1, "Hard": 2}
    matches = [p for p in Problem.query.filter_by(status="approved").all() if tags & set(p.tag_list)]
    matches.sort(key=lambda p: (p.id in solved_ids, order.get(p.difficulty, 3), p.id))
    return matches[:limit]
