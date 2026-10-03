from collections import Counter

from flask import Blueprint, abort, render_template, request
from flask_login import current_user, login_required

from .. import db
from ..models import DIFFICULTIES, POINTS, Challenge, Problem, Submission, User
from ..settings import setting
from ..stats import (acceptance_stats, achievements, attempted_problem_ids, course_overview, leaderboard_rows,
                     solved_problem_ids)

bp = Blueprint("main", __name__)


@bp.route("/")
def index():
    approved = Problem.query.filter_by(status="approved")
    course = course_overview(current_user)
    stats = {
        "lessons": course["total"],
        "problems": approved.count(),
        "users": User.query.filter_by(role="student").count(),
        "submissions": Submission.query.count(),
    }
    active_challenges = [c for c in Challenge.query.order_by(Challenge.end_at).all() if c.state == "active"][:2]
    return render_template("index.html", featured=approved.order_by(Problem.id).limit(3).all(), stats=stats,
                           leaders=leaderboard_rows(limit=5) if setting("leaderboard_enabled") else [],
                           active_challenges=active_challenges, course=course)


@bp.route("/problems")
def problems():
    difficulty = request.args.get("difficulty", "")
    tag = request.args.get("tag", "").strip()
    q = request.args.get("q", "").strip()

    query = Problem.query.filter_by(status="approved")
    if difficulty in DIFFICULTIES:
        query = query.filter_by(difficulty=difficulty)
    if tag:
        query = query.filter(Problem.tags.ilike(f"%{tag}%"))
    if q:
        query = query.filter(Problem.title.ilike(f"%{q}%"))
    items = query.order_by(Problem.id).all()

    all_tags = sorted({t for p in Problem.query.filter_by(status="approved") for t in p.tag_list})
    solved = attempted = set()
    if current_user.is_authenticated:
        solved = solved_problem_ids(current_user.id)
        attempted = attempted_problem_ids(current_user.id)

    return render_template("problems.html", problems=items, stats=acceptance_stats(),
                           solved=solved, attempted=attempted, all_tags=all_tags,
                           selected_difficulty=difficulty, tag=tag, q=q, difficulties=DIFFICULTIES)


@bp.route("/problems/<slug>")
def problem(slug):
    p = Problem.query.filter_by(slug=slug).first_or_404()
    if not p.visible_to(current_user):
        abort(404)
    last = None
    if current_user.is_authenticated:
        last = (Submission.query.filter_by(user_id=current_user.id, problem_id=p.id)
                .order_by(Submission.id.desc()).first())
    return render_template("problem.html", problem=p, last=last)


@bp.route("/submissions")
@login_required
def submissions():
    page = request.args.get("page", 1, type=int)
    pagination = (Submission.query.filter_by(user_id=current_user.id)
                  .order_by(Submission.id.desc()).paginate(page=page, per_page=25, error_out=False))
    return render_template("submissions.html", pagination=pagination)


@bp.route("/submissions/<int:sub_id>")
@login_required
def submission(sub_id):
    sub = db.get_or_404(Submission, sub_id)
    if not sub.viewable_by(current_user):
        abort(403)
    return render_template("submission.html", sub=sub)


@bp.route("/leaderboard")
def leaderboard():
    if not setting("leaderboard_enabled"):
        abort(404)
    return render_template("leaderboard.html", rows=leaderboard_rows(), points=POINTS)


@bp.route("/u/<username>")
def profile(username):
    user = User.query.filter_by(username=username).first_or_404()
    solved_ids = solved_problem_ids(user.id)
    approved = Problem.query.filter_by(status="approved").all()
    solved = [p for p in approved if p.id in solved_ids]

    by_difficulty = {d: {"solved": 0, "total": 0} for d in DIFFICULTIES}
    for p in approved:
        by_difficulty.setdefault(p.difficulty, {"solved": 0, "total": 0})["total"] += 1
        if p.id in solved_ids:
            by_difficulty[p.difficulty]["solved"] += 1

    verdicts = Counter(v for (v,) in db.session.query(Submission.verdict).filter_by(user_id=user.id))
    total = sum(verdicts.values())
    rank = next((i for i, r in enumerate(leaderboard_rows(), 1) if r["user"].id == user.id), None)
    own = current_user.is_authenticated and (current_user.id == user.id or current_user.can_author)
    recent = Submission.query.filter_by(user_id=user.id).order_by(Submission.id.desc()).limit(10).all() if own else []
    authored = Problem.query.filter_by(created_by=user.id, status="approved").all() if user.can_author else []

    return render_template("profile.html", user=user, solved=solved, by_difficulty=by_difficulty,
                           verdicts=verdicts, total=total, rank=rank, recent=recent, authored=authored,
                           score=sum(POINTS.get(p.difficulty, 10) for p in solved),
                           course=course_overview(user), badges=achievements(user) if user.is_student else [])
