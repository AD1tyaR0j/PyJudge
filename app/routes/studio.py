"""Exercise authoring, shared by instructors (their own exercises) and admins (all)."""
from flask import Blueprint, abort, flash, redirect, render_template, request, url_for
from flask_login import current_user

from .. import db
from ..models import DEFAULT_STARTER, DIFFICULTIES, Problem, TestCase, utcnow
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
