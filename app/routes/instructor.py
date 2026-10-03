from collections import defaultdict

from flask import Blueprint, abort, flash, redirect, render_template, request, url_for
from flask_login import current_user
from sqlalchemy import func

from .. import db
from ..models import Lesson, LessonProgress, Message, Problem, Submission, TestCase, Topic, User, utcnow
from ..stats import acceptance_stats
from .decorators import author_required

bp = Blueprint("instructor", __name__)


@bp.route("/lessons")
@author_required
def lessons():
    """Lessons the instructor wrote (admins see the whole course), with completion stats."""
    query = Lesson.query.join(Topic).order_by(Topic.position, Lesson.position)
    if not current_user.is_admin:
        query = query.filter(Lesson.created_by == current_user.id)
    rows = query.all()
    ids = [lesson.id for lesson in rows] or [-1]
    completions = dict(db.session.query(LessonProgress.lesson_id, func.count())
                       .filter(LessonProgress.lesson_id.in_(ids), LessonProgress.completed_at.isnot(None))
                       .group_by(LessonProgress.lesson_id).all())
    avg_quiz = {lid: (round(100 * s / t) if t else None) for lid, s, t in
                db.session.query(LessonProgress.lesson_id, func.sum(LessonProgress.quiz_score),
                                 func.sum(LessonProgress.quiz_total))
                .filter(LessonProgress.lesson_id.in_(ids)).group_by(LessonProgress.lesson_id).all()}
    return render_template("instructor/lessons.html", lessons=rows, completions=completions, avg_quiz=avg_quiz)


def my_problems_query():
    """Instructors see their own exercises; admins see every exercise."""
    q = Problem.query
    if not current_user.is_admin:
        q = q.filter_by(created_by=current_user.id)
    return q


def my_submissions_query():
    ids = [p.id for p in my_problems_query().with_entities(Problem.id)]
    return Submission.query.filter(Submission.problem_id.in_(ids or [-1]),
                                   Submission.verdict != "PENDING")


@bp.route("/")
@author_required
def dashboard():
    problems = my_problems_query().all()
    subs = my_submissions_query()
    by_status = defaultdict(int)
    for p in problems:
        by_status[p.status] += 1
    stats = {
        "exercises": len(problems),
        "approved": by_status["approved"],
        "pending": by_status["pending"],
        "rejected": by_status["rejected"],
        "submissions": subs.count(),
        "to_grade": subs.filter(Submission.grade.is_(None)).count(),
        "students": subs.with_entities(func.count(func.distinct(Submission.user_id))).scalar() or 0,
    }
    to_grade = subs.filter(Submission.grade.is_(None)).order_by(Submission.id.desc()).limit(8).all()
    inbox = (Message.query.filter_by(recipient_id=current_user.id)
             .order_by(Message.id.desc()).limit(5).all())
    rejected = [p for p in problems if p.status == "rejected"]
    return render_template("instructor/dashboard.html", stats=stats, to_grade=to_grade,
                           inbox=inbox, rejected=rejected)


@bp.route("/exercises")
@author_required
def exercises():
    problems = my_problems_query().order_by(Problem.id.desc()).all()
    ids = [p.id for p in problems]
    tests = dict(db.session.query(TestCase.problem_id, func.count())
                 .filter(TestCase.problem_id.in_(ids or [-1])).group_by(TestCase.problem_id).all())
    return render_template("instructor/exercises.html", problems=problems, tests=tests,
                           stats=acceptance_stats(ids))


@bp.route("/submissions")
@author_required
def submissions():
    show = request.args.get("show", "ungraded")
    problem_id = request.args.get("exercise", type=int)
    query = my_submissions_query()
    if show == "ungraded":
        query = query.filter(Submission.grade.is_(None))
    elif show == "graded":
        query = query.filter(Submission.grade.isnot(None))
    if problem_id:
        query = query.filter(Submission.problem_id == problem_id)
    page = request.args.get("page", 1, type=int)
    pagination = query.order_by(Submission.id.desc()).paginate(page=page, per_page=25, error_out=False)
    return render_template("instructor/submissions.html", pagination=pagination, show=show,
                           problem_id=problem_id, exercises=my_problems_query().order_by(Problem.title).all())


@bp.post("/submissions/<int:sub_id>/grade")
@author_required
def grade(sub_id):
    sub = db.get_or_404(Submission, sub_id)
    if not sub.problem.editable_by(current_user):
        abort(403)
    try:
        grade_value = int(request.form.get("grade", ""))
    except ValueError:
        grade_value = -1
    feedback = request.form.get("feedback", "").strip()
    if not 0 <= grade_value <= 100:
        flash("Grade must be a whole number from 0 to 100.", "error")
        return redirect(url_for("main.submission", sub_id=sub.id))

    first_time = sub.grade is None
    sub.grade, sub.feedback = grade_value, feedback or None
    sub.graded_by, sub.graded_at = current_user.id, utcnow()
    body = f"Your submission #{sub.id} for “{sub.problem.title}” was {'graded' if first_time else 'regraded'}: {grade_value}/100."
    if feedback:
        body += f"\n\n{feedback}"
    db.session.add(Message(sender_id=current_user.id, recipient_id=sub.user_id, body=body, submission_id=sub.id))
    db.session.commit()
    flash(f"Graded submission #{sub.id} and sent feedback to {sub.user.display_name}.", "success")

    next_sub = my_submissions_query().filter(Submission.grade.is_(None)).order_by(Submission.id.desc()).first()
    if request.form.get("next") and next_sub:
        return redirect(url_for("main.submission", sub_id=next_sub.id))
    return redirect(url_for("main.submission", sub_id=sub.id))


@bp.route("/reports")
@author_required
def reports():
    problems = my_problems_query().filter_by(status="approved").order_by(Problem.id).all()
    ids = [p.id for p in problems]
    stats = acceptance_stats(ids)
    avg_grades = dict(db.session.query(Submission.problem_id, func.avg(Submission.grade))
                      .filter(Submission.problem_id.in_(ids or [-1]), Submission.grade.isnot(None))
                      .group_by(Submission.problem_id).all())

    exercise_rows = []
    for p in problems:
        total, accepted, students, solvers = stats.get(p.id, (0, 0, 0, 0))
        exercise_rows.append({
            "problem": p, "submissions": total, "students": students, "solvers": solvers,
            "acceptance": round(100 * accepted / total) if total else None,
            "avg_grade": round(avg_grades[p.id]) if p.id in avg_grades else None,
        })

    # Per-student performance across these exercises
    subs = (Submission.query.filter(Submission.problem_id.in_(ids or [-1]), Submission.verdict != "PENDING")
            .with_entities(Submission.user_id, Submission.problem_id, Submission.verdict,
                           Submission.grade, Submission.submitted_at).all())
    per_student = defaultdict(lambda: {"attempted": set(), "solved": set(), "grades": [], "subs": 0, "last": None})
    for uid, pid, verdict, grade_value, ts in subs:
        s = per_student[uid]
        s["subs"] += 1
        s["attempted"].add(pid)
        if verdict == "AC":
            s["solved"].add(pid)
        if grade_value is not None:
            s["grades"].append(grade_value)
        s["last"] = max(s["last"], ts) if s["last"] else ts
    users = {u.id: u for u in User.query.filter(User.id.in_(list(per_student) or [-1]))}
    student_rows = sorted(
        ({"user": users[uid], "attempted": len(s["attempted"]), "solved": len(s["solved"]),
          "submissions": s["subs"], "last": s["last"],
          "avg_grade": round(sum(s["grades"]) / len(s["grades"])) if s["grades"] else None,
          "progress": round(100 * len(s["solved"]) / len(problems)) if problems else 0}
         for uid, s in per_student.items() if uid in users),
        key=lambda r: (-r["solved"], -r["submissions"]))

    # Only attempted exercises go in the chart: "no attempts" must not read as "0% accepted"
    attempted = [r for r in exercise_rows if r["acceptance"] is not None]
    chart = {
        "labels": [r["problem"].title for r in attempted],
        "acceptance": [r["acceptance"] for r in attempted],
    }
    return render_template("instructor/reports.html", exercise_rows=exercise_rows,
                           student_rows=student_rows, chart=chart)
