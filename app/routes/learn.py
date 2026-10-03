from flask import Blueprint, abort, jsonify, render_template, request, url_for
from flask_login import current_user

from .. import db
from ..lessons import practice_problems, render_inline, render_lesson
from ..models import Lesson, LessonProgress, Topic, utcnow
from ..stats import course_overview, solved_problem_ids

bp = Blueprint("learn", __name__)
PASS_RATIO = 0.5  # quiz score needed to complete a lesson


def _get_lesson(topic_slug, lesson_slug):
    topic = Topic.query.filter_by(slug=topic_slug).first_or_404()
    lesson = Lesson.query.filter_by(topic_id=topic.id, slug=lesson_slug).first_or_404()
    if not lesson.visible_to(current_user):
        abort(404)
    return topic, lesson


def _neighbours(lesson):
    """Previous/next approved lessons across the whole course."""
    ordered = [l for t in Topic.query.order_by(Topic.position) for l in t.approved_lessons]
    if lesson not in ordered:
        return None, None
    i = ordered.index(lesson)
    return (ordered[i - 1] if i > 0 else None), (ordered[i + 1] if i + 1 < len(ordered) else None)


def lesson_url(lesson):
    return url_for("learn.lesson", topic_slug=lesson.topic.slug, lesson_slug=lesson.slug)


def _record(lesson, score=None, total=None, complete=False):
    """Save progress for the logged-in user. Returns the LessonProgress row (or None for guests)."""
    if not current_user.is_authenticated:
        return None
    p = LessonProgress.query.filter_by(user_id=current_user.id, lesson_id=lesson.id).first()
    if not p:
        p = LessonProgress(user_id=current_user.id, lesson_id=lesson.id, attempts=0)
        db.session.add(p)
    if total is not None:
        p.attempts = (p.attempts or 0) + 1
        if p.quiz_score is None or score > p.quiz_score:
            p.quiz_score, p.quiz_total = score, total
    if complete and not p.completed_at:
        p.completed_at = utcnow()
    db.session.commit()
    return p


@bp.route("/")
def index():
    return render_template("learn/index.html", course=course_overview(current_user))


@bp.route("/<topic_slug>")
def topic(topic_slug):
    t = Topic.query.filter_by(slug=topic_slug).first_or_404()
    course = course_overview(current_user)
    info = next((x for x in course["topics"] if x["topic"].id == t.id), None)
    if info is None:
        abort(404)
    solved = solved_problem_ids(current_user.id) if current_user.is_authenticated else set()
    practice, seen = [], set()
    for lesson in info["lessons"]:
        for p in practice_problems(lesson, solved, limit=3):
            if p.id not in seen:
                seen.add(p.id)
                practice.append(p)
    position = [x["topic"].id for x in course["topics"]].index(t.id)
    return render_template("learn/topic.html", info=info, course=course, practice=practice[:6],
                           solved=solved, number=position + 1)


@bp.route("/<topic_slug>/<lesson_slug>")
def lesson(topic_slug, lesson_slug):
    t, lesson = _get_lesson(topic_slug, lesson_slug)
    course = course_overview(current_user)
    prev_lesson, next_lesson = _neighbours(lesson)
    solved = solved_problem_ids(current_user.id) if current_user.is_authenticated else set()
    questions = [{"id": q.id, "prompt": render_lesson(q.prompt),
                  "options": [render_inline(o) for o in q.options]} for q in lesson.questions]
    return render_template(
        "learn/lesson.html", topic=t, lesson=lesson, body=render_lesson(lesson.content),
        questions=questions, progress=course["progress"].get(lesson.id),
        siblings=t.approved_lessons if lesson.is_approved else [lesson], course=course,
        prev_lesson=prev_lesson, next_lesson=next_lesson,
        practice=practice_problems(lesson, solved), solved=solved, pass_ratio=PASS_RATIO)


@bp.post("/<topic_slug>/<lesson_slug>/quiz")
def quiz(topic_slug, lesson_slug):
    _, lesson = _get_lesson(topic_slug, lesson_slug)
    if not lesson.questions:
        return jsonify(error="This lesson has no quiz."), 400
    results, score = [], 0
    for q in lesson.questions:
        chosen = request.form.get(f"q{q.id}", type=int)
        correct = chosen == q.answer
        score += correct
        results.append({"id": q.id, "chosen": chosen, "answer": q.answer, "correct": correct,
                        "explanation": str(render_inline(q.explanation)) if q.explanation else ""})
    total = len(lesson.questions)
    passed = score >= total * PASS_RATIO
    progress = _record(lesson, score, total, complete=passed)
    _, next_lesson = _neighbours(lesson)
    return jsonify(score=score, total=total, passed=passed, results=results,
                   saved=progress is not None,
                   completed=bool(progress and progress.completed_at),
                   best=progress.quiz_score if progress else None,
                   next_url=lesson_url(next_lesson) if next_lesson else url_for("learn.index"))


@bp.post("/<topic_slug>/<lesson_slug>/complete")
def complete(topic_slug, lesson_slug):
    """For lessons without a quiz: mark as read."""
    _, lesson = _get_lesson(topic_slug, lesson_slug)
    if not current_user.is_authenticated:
        return jsonify(error="Log in to save your progress."), 401
    if lesson.questions:
        return jsonify(error="Pass the quiz to complete this lesson."), 400
    _record(lesson, complete=True)
    return jsonify(completed=True)
