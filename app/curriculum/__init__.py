"""The built-in Python course, stored as Markdown so it reads well on GitHub and is easy to edit.

Layout:
    app/curriculum/NN-topic-slug/_topic.md       topic front matter + description
    app/curriculum/NN-topic-slug/NN-lesson.md    one lesson each

Lesson format:
    ---
    title: Variables and Types
    minutes: 8
    practice: basics, math          (exercise tags to suggest for practice)
    ---
    Markdown body. A ```pyrun fence becomes a runnable playground; inside it, a line
    `### stdin` separates the code from the input it should be run with.

    <!-- quiz -->
    Q: Question text (Markdown allowed)
    - [ ] wrong option
    - [x] correct option
    > Explanation shown after answering.
"""
import re
from pathlib import Path

ROOT = Path(__file__).parent
QUIZ_MARKER = "<!-- quiz -->"
STDIN_MARKER = "### stdin"
# ```pyrun runs cleanly; ```pyrun-error demonstrates an exception on purpose
PYRUN_RE = re.compile(r"```(pyrun(?:-error)?)\n(.*?)```", re.S)


class CurriculumError(ValueError):
    pass


def _front_matter(text, where):
    if not text.startswith("---\n"):
        raise CurriculumError(f"{where}: missing front matter")
    head, _, body = text[4:].partition("\n---\n")
    meta = {}
    for line in head.splitlines():
        if line.strip():
            key, _, value = line.partition(":")
            meta[key.strip()] = value.strip()
    return meta, body.strip("\n")


def parse_quiz(text, where="quiz"):
    questions, current = [], None
    for raw in text.splitlines():
        line = raw.rstrip()
        if line.startswith("Q: "):
            current = {"prompt": line[3:].strip(), "options": [], "answer": None, "explanation": ""}
            questions.append(current)
        elif current is None or not line.strip():
            continue
        elif line.startswith("- [ ] ") or line.startswith("- [x] "):
            if line.startswith("- [x] "):
                if current["answer"] is not None:
                    raise CurriculumError(f"{where}: two correct answers in “{current['prompt']}”")
                current["answer"] = len(current["options"])
            current["options"].append(line[6:].strip())
        elif line.startswith("> "):
            current["explanation"] = (current["explanation"] + " " + line[2:].strip()).strip()
        elif not current["options"]:
            current["prompt"] += "\n" + line  # multi-line prompt (e.g. a code snippet)
    for q in questions:
        if len(q["options"]) < 2 or q["answer"] is None:
            raise CurriculumError(f"{where}: “{q['prompt']}” needs 2+ options and exactly one [x]")
    return questions


def parse_lesson(text, where="lesson"):
    meta, body = _front_matter(text, where)
    content, _, quiz = body.partition(QUIZ_MARKER)
    if "title" not in meta:
        raise CurriculumError(f"{where}: missing title")
    return {
        "title": meta["title"],
        "minutes": int(meta.get("minutes", 5)),
        "practice": meta.get("practice", ""),
        "content": content.strip(),
        "quiz": parse_quiz(quiz, where) if quiz.strip() else [],
    }


def split_playground(block):
    """A ```pyrun block -> (code, stdin)."""
    code, sep, stdin = block.partition("\n" + STDIN_MARKER + "\n")
    if not sep and block.startswith(STDIN_MARKER + "\n"):
        return "", block[len(STDIN_MARKER) + 1:]
    return code.rstrip("\n") + "\n", stdin


def playgrounds(content):
    """[(code, stdin, expects_error), ...] for every runnable block in a lesson."""
    return [(*split_playground(m.group(2)), m.group(1) == "pyrun-error") for m in PYRUN_RE.finditer(content)]


def _slug(path):
    return re.sub(r"^\d+-", "", path.stem if path.is_file() else path.name)


def load_curriculum(root=ROOT):
    topics = []
    for position, folder in enumerate(sorted(p for p in root.iterdir() if p.is_dir() and p.name[0].isdigit())):
        meta, description = _front_matter((folder / "_topic.md").read_text(encoding="utf-8"), folder.name)
        lessons = []
        for lpos, file in enumerate(sorted(f for f in folder.glob("*.md") if f.name[0].isdigit())):
            lesson = parse_lesson(file.read_text(encoding="utf-8"), f"{folder.name}/{file.name}")
            lesson.update(slug=_slug(file), position=lpos)
            lessons.append(lesson)
        topics.append({
            "slug": _slug(folder), "position": position, "title": meta["title"],
            "icon": meta.get("icon", "📘"), "level": meta.get("level", "Beginner"),
            "summary": meta.get("summary", ""), "description": description, "lessons": lessons,
        })
    return topics


def sync_curriculum(db, models):
    """Create or update built-in topics and lessons. Custom (instructor) lessons are never touched.
    Returns (topics_created, lessons_created, lessons_updated)."""
    Topic, Lesson, QuizQuestion = models.Topic, models.Lesson, models.QuizQuestion
    created_topics = created = updated = 0
    for t in load_curriculum():
        topic = Topic.query.filter_by(slug=t["slug"]).first()
        if not topic:
            topic = Topic(slug=t["slug"])
            db.session.add(topic)
            created_topics += 1
        topic.title, topic.icon, topic.level = t["title"], t["icon"], t["level"]
        topic.summary, topic.description, topic.position = t["summary"], t["description"], t["position"]
        db.session.flush()

        for spec in t["lessons"]:
            lesson = Lesson.query.filter_by(topic_id=topic.id, slug=spec["slug"]).first()
            if lesson and lesson.source != "builtin":
                continue
            if not lesson:
                lesson = Lesson(topic_id=topic.id, slug=spec["slug"], source="builtin", status="approved")
                db.session.add(lesson)
                created += 1
            else:
                updated += 1
            lesson.title, lesson.content, lesson.minutes = spec["title"], spec["content"], spec["minutes"]
            lesson.practice_tags, lesson.position = spec["practice"], spec["position"]
            lesson.questions = [
                QuizQuestion(prompt=q["prompt"], options_json="[]", answer=q["answer"],
                             explanation=q["explanation"], position=i)
                for i, q in enumerate(spec["quiz"])
            ]
            for q, qspec in zip(lesson.questions, spec["quiz"]):
                q.options = qspec["options"]
    db.session.commit()
    return created_topics, created, updated
