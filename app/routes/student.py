from flask import Blueprint, render_template
from flask_login import current_user, login_required

from ..models import DIFFICULTIES, POINTS, Challenge, Message, Problem, Submission
from ..stats import attempted_problem_ids, leaderboard_rows, solve_timeline, solved_problem_ids, streak
from .challenges import summarize

bp = Blueprint("student", __name__)


@bp.route("/student")
@login_required
def dashboard():
    uid = current_user.id
    approved = Problem.query.filter_by(status="approved").order_by(Problem.id).all()
    solved_ids = solved_problem_ids(uid)
    attempted_ids = attempted_problem_ids(uid)
    solved = [p for p in approved if p.id in solved_ids]

    by_difficulty = {d: {"solved": 0, "total": 0} for d in DIFFICULTIES}
    for p in approved:
        by_difficulty[p.difficulty]["total"] += 1
        if p.id in solved_ids:
            by_difficulty[p.difficulty]["solved"] += 1

    in_progress = [p for p in approved if p.id in attempted_ids and p.id not in solved_ids]
    up_next = in_progress[:3] + [p for p in approved if p.id not in attempted_ids][: max(0, 3 - len(in_progress))]

    challenges = summarize([c for c in Challenge.query.order_by(Challenge.end_at).all() if c.state != "upcoming"],
                           current_user)
    active = [r for r in challenges if r["challenge"].state == "active" and not r["completed"]]
    completed = [r for r in challenges if r["completed"]]

    feedback = (Submission.query.filter(Submission.user_id == uid, Submission.grade.isnot(None))
                .order_by(Submission.graded_at.desc()).limit(5).all())
    graded = Submission.query.filter(Submission.user_id == uid, Submission.grade.isnot(None)).all()
    messages = Message.query.filter_by(recipient_id=uid).order_by(Message.id.desc()).limit(4).all()
    rank = next((i for i, r in enumerate(leaderboard_rows(), 1) if r["user"].id == uid), None)

    stats = {
        "solved": len(solved),
        "total": len(approved),
        "score": sum(POINTS.get(p.difficulty, 10) for p in solved),
        "streak": streak(uid),
        "rank": rank,
        "avg_grade": round(sum(s.grade for s in graded) / len(graded)) if graded else None,
        "submissions": Submission.query.filter_by(user_id=uid).count(),
    }
    return render_template("student/dashboard.html", stats=stats, by_difficulty=by_difficulty,
                           timeline=solve_timeline(uid, 30), up_next=up_next, in_progress_ids={p.id for p in in_progress},
                           active=active, completed=completed, feedback=feedback, messages=messages)
