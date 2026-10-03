"""Exercise authoring, shared by instructors (their own exercises) and admins (all)."""
from flask import Blueprint, abort, flash, redirect, render_template, request, url_for
from flask_login import current_user

from .. import db
from ..models import DEFAULT_STARTER, DIFFICULTIES, Lesson, Problem, QuizQuestion, TestCase, Topic, slugify, utcnow
from ..settings import setting
from .decorators import author_required

bp = Blueprint("studio", __name__)


def _exercise_list_url():
    return url_for("admin.content") if current_user.is_admin else url_for("instructor.exercises")


def _get_editable(problem_id):
    problem = db.get_or_404(Problem, problem_id)
    if not problem.editable_by(current_user):
        abort(403)
    return problem


def _apply_form(problem):
    errors = []
    title = request.form.get("title", "").strip()
    difficulty = request.form.get("difficulty", "Easy")
    try:
        time_limit = int(request.form.get("time_limit", 2))
        memory_limit = int(request.form.get("memory_limit", 128))
    except ValueError:
        time_limit, memory_limit = 0, 0
    if not title:
        errors.append("Title is required.")
    if not request.form.get("description", "").strip():
        errors.append("Description is required.")
    if difficulty not in DIFFICULTIES:
        errors.append("Pick a valid difficulty.")
    if not 1 <= time_limit <= 10:
        errors.append("Time limit must be 1–10 seconds.")
    if not 16 <= memory_limit <= 1024:
        errors.append("Memory limit must be 16–1024 MB.")

    problem.title = title
    problem.description = request.form.get("description", "")
    problem.difficulty = difficulty
    problem.time_limit = time_limit
    problem.memory_limit = memory_limit
    problem.tags = ", ".join(t.strip() for t in request.form.get("tags", "").split(",") if t.strip())
    problem.starter_code = request.form.get("starter_code", "") or DEFAULT_STARTER
    return errors


def _needs_review():
    return current_user.is_instructor and setting("require_approval")


@bp.route("/exercises/new", methods=["GET", "POST"])
@author_required
def new_exercise():
    problem = Problem(time_limit=setting("default_time_limit"), memory_limit=setting("default_memory_limit"),
                      starter_code=DEFAULT_STARTER, difficulty="Easy")
    if request.method == "POST":
        errors = _apply_form(problem)
        if errors:
            for e in errors:
                flash(e, "error")
        else:
            problem.slug = Problem.unique_slug(problem.title)
            problem.created_by = current_user.id
            if _needs_review():
                problem.status = "pending"
                message = "Exercise created and sent to an admin for approval. Add test cases while you wait."
            else:
                problem.status, problem.reviewed_by, problem.reviewed_at = "approved", current_user.id, utcnow()
                message = "Exercise created and published. Now add some test cases."
            db.session.add(problem)
            db.session.commit()
            flash(message, "success")
            return redirect(url_for("studio.edit_exercise", problem_id=problem.id))
    return render_template("studio/exercise_form.html", problem=problem, difficulties=DIFFICULTIES, is_new=True)


@bp.route("/exercises/<int:problem_id>/edit", methods=["GET", "POST"])
@author_required
def edit_exercise(problem_id):
    problem = _get_editable(problem_id)
    if request.method == "POST":
        errors = _apply_form(problem)
        if errors:
            for e in errors:
                flash(e, "error")
            db.session.rollback()
            problem = _get_editable(problem_id)
        else:
            problem.slug = Problem.unique_slug(problem.title, exclude_id=problem.id)
            message = "Exercise saved."
            if _needs_review() and problem.status != "pending":
                problem.status, problem.review_note = "pending", None
                message = "Exercise saved and sent back to an admin for review."
            db.session.commit()
            flash(message, "success")
            return redirect(url_for("studio.edit_exercise", problem_id=problem.id))
    return render_template("studio/exercise_form.html", problem=problem, difficulties=DIFFICULTIES, is_new=False)


@bp.post("/exercises/<int:problem_id>/delete")
@author_required
def delete_exercise(problem_id):
    problem = _get_editable(problem_id)
    db.session.delete(problem)
    db.session.commit()
    flash(f"Deleted “{problem.title}”.", "info")
    return redirect(_exercise_list_url())


@bp.post("/exercises/<int:problem_id>/testcases")
@author_required
def add_testcase(problem_id):
    problem = _get_editable(problem_id)
    expected = request.form.get("expected_output", "")
    if not expected.strip():
        flash("Expected output is required.", "error")
    else:
        db.session.add(TestCase(problem_id=problem.id, input=request.form.get("input", ""),
                                expected_output=expected, is_sample=bool(request.form.get("is_sample"))))
        db.session.commit()
        flash("Test case added.", "success")
    return redirect(url_for("studio.edit_exercise", problem_id=problem.id) + "#tests")


@bp.post("/testcases/<int:tc_id>/delete")
@author_required
def delete_testcase(tc_id):
    tc = db.get_or_404(TestCase, tc_id)
    problem = _get_editable(tc.problem_id)
    db.session.delete(tc)
    db.session.commit()
    flash("Test case deleted.", "info")
    return redirect(url_for("studio.edit_exercise", problem_id=problem.id) + "#tests")


# ---------------------------------------------------------------- lessons (tutorials)

def _get_editable_lesson(lesson_id):
    lesson = db.get_or_404(Lesson, lesson_id)
    if not lesson.editable_by(current_user):
        abort(403)
    return lesson


def _apply_lesson_form(lesson):
    errors = []
    lesson.title = request.form.get("title", "").strip()
    lesson.content = request.form.get("content", "")
    lesson.practice_tags = ", ".join(t.strip() for t in request.form.get("practice_tags", "").split(",") if t.strip())
    topic = db.session.get(Topic, request.form.get("topic_id", type=int) or 0)
    try:
        lesson.minutes = int(request.form.get("minutes", 5))
    except ValueError:
        lesson.minutes = 0
    if not lesson.title:
        errors.append("Title is required.")
    if not lesson.content.strip():
        errors.append("Lesson content is required.")
    if not topic:
        errors.append("Pick a topic.")
    else:
        lesson.topic_id = topic.id
    if not 1 <= lesson.minutes <= 120:
        errors.append("Reading time must be 1–120 minutes.")
    return errors


def _lesson_form(lesson, is_new):
    return render_template("studio/lesson_form.html", lesson=lesson, is_new=is_new,
                           topics=Topic.query.order_by(Topic.position).all())


@bp.route("/lessons/new", methods=["GET", "POST"])
@author_required
def new_lesson():
    lesson = Lesson(minutes=5, content="", source="custom", topic_id=request.args.get("topic", type=int))
    if request.method == "POST":
        errors = _apply_lesson_form(lesson)
        if errors:
            for e in errors:
                flash(e, "error")
        else:
            base = slug = slugify(lesson.title)
            n = 2
            while Lesson.query.filter_by(topic_id=lesson.topic_id, slug=slug).first():
                slug, n = f"{base}-{n}", n + 1
            lesson.slug = slug
            lesson.created_by = current_user.id
            lesson.position = 1000 + Lesson.query.filter_by(topic_id=lesson.topic_id).count()
            if _needs_review():
                lesson.status = "pending"
                message = "Lesson created and sent to an admin for approval. Add quiz questions while you wait."
            else:
                lesson.status, lesson.reviewed_by, lesson.reviewed_at = "approved", current_user.id, utcnow()
                message = "Lesson published. Add a few quiz questions to check understanding."
            db.session.add(lesson)
            db.session.commit()
            flash(message, "success")
            return redirect(url_for("studio.edit_lesson", lesson_id=lesson.id))
    return _lesson_form(lesson, True)


@bp.route("/lessons/<int:lesson_id>/edit", methods=["GET", "POST"])
@author_required
def edit_lesson(lesson_id):
    lesson = _get_editable_lesson(lesson_id)
    if request.method == "POST":
        errors = _apply_lesson_form(lesson)
        if errors:
            for e in errors:
                flash(e, "error")
            db.session.rollback()
            lesson = _get_editable_lesson(lesson_id)
        else:
            message = "Lesson saved."
            if _needs_review() and lesson.status != "pending":
                lesson.status, lesson.review_note = "pending", None
                message = "Lesson saved and sent back to an admin for review."
            db.session.commit()
            flash(message, "success")
            return redirect(url_for("studio.edit_lesson", lesson_id=lesson.id))
    return _lesson_form(lesson, False)


@bp.post("/lessons/<int:lesson_id>/delete")
@author_required
def delete_lesson(lesson_id):
    lesson = _get_editable_lesson(lesson_id)
    if lesson.source == "builtin":
        flash("Built-in course lessons live in app/curriculum and can't be deleted here.", "error")
        return redirect(url_for("studio.edit_lesson", lesson_id=lesson.id))
    db.session.delete(lesson)
    db.session.commit()
    flash(f"Deleted lesson “{lesson.title}”.", "info")
    return redirect(url_for("instructor.lessons"))


@bp.post("/lessons/<int:lesson_id>/questions")
@author_required
def add_question(lesson_id):
    lesson = _get_editable_lesson(lesson_id)
    prompt = request.form.get("prompt", "").strip()
    options = [o.strip() for o in request.form.getlist("option")]
    answer = request.form.get("answer", type=int)
    filled = [i for i, o in enumerate(options) if o]
    if not prompt or len(filled) < 2:
        flash("A question needs a prompt and at least two options.", "error")
    elif answer is None or answer not in filled:
        flash("Mark which (non-empty) option is correct.", "error")
    else:
        q = QuizQuestion(lesson_id=lesson.id, prompt=prompt, answer=filled.index(answer),
                         explanation=request.form.get("explanation", "").strip(), position=len(lesson.questions))
        q.options = [options[i] for i in filled]
        db.session.add(q)
        db.session.commit()
        flash("Question added.", "success")
    return redirect(url_for("studio.edit_lesson", lesson_id=lesson.id) + "#quiz")


@bp.post("/questions/<int:question_id>/delete")
@author_required
def delete_question(question_id):
    q = db.get_or_404(QuizQuestion, question_id)
    lesson = _get_editable_lesson(q.lesson_id)
    db.session.delete(q)
    db.session.commit()
    flash("Question deleted.", "info")
    return redirect(url_for("studio.edit_lesson", lesson_id=lesson.id) + "#quiz")
