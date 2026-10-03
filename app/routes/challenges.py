from datetime import datetime, timedelta

from flask import Blueprint, abort, flash, redirect, render_template, request, url_for
from flask_login import current_user

from .. import db
from ..models import Challenge, Problem, Submission, utcnow
from ..stats import leaderboard_rows
from .decorators import author_required

bp = Blueprint("challenges", __name__)
DT_FORMAT = "%Y-%m-%dT%H:%M"


def challenge_progress(challenge, user_id):
    """Exercises in `challenge` that `user_id` solved during its time window."""
    ids = [p.id for p in challenge.approved_problems]
    if not ids:
        return set()
    rows = (Submission.query.with_entities(Submission.problem_id)
            .filter(Submission.user_id == user_id, Submission.verdict == "AC",
                    Submission.problem_id.in_(ids),
                    Submission.submitted_at >= challenge.start_at,
                    Submission.submitted_at <= challenge.end_at)
            .distinct().all())
    return {r[0] for r in rows}


def summarize(challenges, user):
    """Attach progress for the current user to each challenge."""
    out = []
    for c in challenges:
        total = len(c.approved_problems)
        solved = len(challenge_progress(c, user.id)) if user.is_authenticated else 0
        out.append({"challenge": c, "solved": solved, "total": total,
                    "completed": total > 0 and solved == total,
                    "percent": round(100 * solved / total) if total else 0})
    return out


@bp.route("/")
def index():
    challenges = Challenge.query.order_by(Challenge.start_at.desc()).all()
    rows = summarize(challenges, current_user)
    groups = {"active": [], "upcoming": [], "ended": []}
    for r in rows:
        groups[r["challenge"].state].append(r)
    return render_template("challenges/index.html", groups=groups)


@bp.route("/<int:challenge_id>")
def detail(challenge_id):
    c = db.get_or_404(Challenge, challenge_id)
    problems = c.approved_problems
    solved = challenge_progress(c, current_user.id) if current_user.is_authenticated else set()
    standings = leaderboard_rows(problem_ids=[p.id for p in problems] or [-1], since=c.start_at, until=c.end_at)
    return render_template("challenges/detail.html", c=c, problems=problems, solved=solved,
                           standings=standings, now=utcnow())


def _apply_form(c):
    errors = []
    c.title = request.form.get("title", "").strip()
    c.description = request.form.get("description", "").strip()
    if not c.title:
        errors.append("Title is required.")
    try:
        c.start_at = datetime.strptime(request.form.get("start_at", ""), DT_FORMAT)
        c.end_at = datetime.strptime(request.form.get("end_at", ""), DT_FORMAT)
        if c.end_at <= c.start_at:
            errors.append("The end time must be after the start time.")
    except ValueError:
        errors.append("Enter valid start and end times.")
    ids = request.form.getlist("problem_ids", type=int)
    c.problems = Problem.query.filter(Problem.id.in_(ids or [-1]), Problem.status == "approved").all()
    if not c.problems:
        errors.append("Pick at least one approved exercise.")
    return errors


def _form(c, is_new):
    exercises = Problem.query.filter_by(status="approved").order_by(Problem.id).all()
    return render_template("challenges/form.html", c=c, is_new=is_new, exercises=exercises,
                           selected={p.id for p in c.problems}, fmt=DT_FORMAT)


@bp.route("/new", methods=["GET", "POST"])
@author_required
def new():
    now = utcnow().replace(minute=0, second=0, microsecond=0)
    c = Challenge(start_at=now, end_at=now + timedelta(days=7), problems=[])
    if request.method == "POST":
        errors = _apply_form(c)
        if errors:
            for e in errors:
                flash(e, "error")
            db.session.expunge(c) if c in db.session else None
        else:
            c.created_by = current_user.id
            db.session.add(c)
            db.session.commit()
            flash("Challenge created.", "success")
            return redirect(url_for("challenges.detail", challenge_id=c.id))
    return _form(c, True)


@bp.route("/<int:challenge_id>/edit", methods=["GET", "POST"])
@author_required
def edit(challenge_id):
    c = db.get_or_404(Challenge, challenge_id)
    if not c.editable_by(current_user):
        abort(403)
    if request.method == "POST":
        errors = _apply_form(c)
        if errors:
            for e in errors:
                flash(e, "error")
            db.session.rollback()
            c = db.get_or_404(Challenge, challenge_id)
        else:
            db.session.commit()
            flash("Challenge saved.", "success")
            return redirect(url_for("challenges.detail", challenge_id=c.id))
    return _form(c, False)


@bp.post("/<int:challenge_id>/delete")
@author_required
def delete(challenge_id):
    c = db.get_or_404(Challenge, challenge_id)
    if not c.editable_by(current_user):
        abort(403)
    db.session.delete(c)
    db.session.commit()
    flash(f"Deleted challenge “{c.title}”.", "info")
    return redirect(url_for("challenges.index"))
