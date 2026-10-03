from flask import Blueprint, current_app, jsonify, request
from flask_login import current_user, login_required

from .. import db
from ..judge.judge import check_syntax
from ..judge.sandbox import run_code
from ..judge.tasks import dispatch, sandbox_options
from ..models import Problem, Submission
from ..settings import setting
from ..stats import leaderboard_rows

bp = Blueprint("api", __name__)


def _error(message, status=400):
    return jsonify(error=message), status


def _validate_code(code):
    if not isinstance(code, str) or not code.strip():
        return "Code cannot be empty."
    if len(code) > current_app.config["MAX_CODE_LENGTH"]:
        return "Code is too long."
    return None


@bp.get("/problems")
def list_problems():
    return jsonify([
        {"id": p.id, "title": p.title, "slug": p.slug, "difficulty": p.difficulty, "tags": p.tag_list}
        for p in Problem.query.filter_by(status="approved").order_by(Problem.id)
    ])


@bp.get("/problems/<slug>")
def get_problem(slug):
    p = Problem.query.filter_by(slug=slug).first()
    if not p or not p.visible_to(current_user):
        return _error("Problem not found.", 404)
    return jsonify({
        "id": p.id, "title": p.title, "slug": p.slug, "difficulty": p.difficulty,
        "description": p.description, "tags": p.tag_list,
        "time_limit": p.time_limit, "memory_limit": p.memory_limit,
        "samples": [{"input": tc.input, "output": tc.expected_output} for tc in p.sample_cases],
    })


@bp.post("/run")
@login_required
def run():
    data = request.get_json(silent=True) or {}
    code = data.get("code")
    stdin = data.get("input") or ""
    if err := _validate_code(code):
        return _error(err)
    if not isinstance(stdin, str) or len(stdin) > current_app.config["MAX_INPUT_LENGTH"]:
        return _error("Input is too long.")

    time_limit = setting("default_time_limit")
    memory_limit = setting("default_memory_limit")
    problem = db.session.get(Problem, data.get("problem_id") or 0)
    if problem and problem.visible_to(current_user):
        time_limit, memory_limit = problem.time_limit, problem.memory_limit

    if syntax_error := check_syntax(code):
        return jsonify(status="CE", stdout="", stderr=syntax_error, time_ms=0)

    result = run_code(code, stdin, time_limit, memory_limit, **sandbox_options(current_app))
    return jsonify(status=result.status, stdout=result.stdout, stderr=result.stderr,
                   time_ms=result.time_ms, exit_code=result.exit_code)


@bp.post("/submit")
@login_required
def submit():
    data = request.get_json(silent=True) or {}
    code = data.get("code")
    if err := _validate_code(code):
        return _error(err)
    problem = db.session.get(Problem, data.get("problem_id") or 0)
    if not problem or not problem.visible_to(current_user):
        return _error("Exercise not found.", 404)

    pending = Submission.query.filter_by(user_id=current_user.id, verdict="PENDING").count()
    if pending >= setting("max_pending_submissions"):
        return _error("You already have submissions being judged. Please wait.", 429)

    sub = Submission(user_id=current_user.id, problem_id=problem.id, code=code,
                     verdict="PENDING", total=len(problem.test_cases))
    db.session.add(sub)
    db.session.commit()

    dispatch(current_app._get_current_object(), sub.id)
    db.session.refresh(sub)
    return jsonify(sub.to_dict()), 202


@bp.get("/submissions/<int:sub_id>")
@login_required
def get_submission(sub_id):
    sub = db.session.get(Submission, sub_id)
    if not sub or not sub.viewable_by(current_user):
        return _error("Submission not found.", 404)
    return jsonify(sub.to_dict())


@bp.get("/users/me/submissions")
@login_required
def my_submissions():
    subs = (Submission.query.filter_by(user_id=current_user.id)
            .order_by(Submission.id.desc()).limit(100).all())
    return jsonify([s.to_dict() for s in subs])


@bp.get("/leaderboard")
def leaderboard():
    if not setting("leaderboard_enabled"):
        return _error("The leaderboard is disabled.", 404)
    return jsonify([
        {"rank": i, "username": r["user"].username, "solved": r["solved"], "score": r["score"]}
        for i, r in enumerate(leaderboard_rows(limit=100), start=1)
    ])
