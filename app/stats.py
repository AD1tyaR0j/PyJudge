"""Aggregations behind the dashboards and charts."""
from collections import Counter
from datetime import timedelta

from sqlalchemy import case, func

from . import db
from .models import POINTS, Problem, Submission, User, utcnow


def day_labels(days):
    today = utcnow().date()
    return [today - timedelta(days=days - 1 - i) for i in range(days)]


def daily_counts(timestamps, days):
    labels = day_labels(days)
    counts = Counter(ts.date() for ts in timestamps if ts)
    return [counts.get(d, 0) for d in labels]


def activity_overview(days=14):
    """Per-day submissions (accepted vs not), sign-ups, and active students for the admin chart."""
    since = utcnow() - timedelta(days=days)
    subs = db.session.query(Submission.submitted_at, Submission.verdict, Submission.user_id) \
        .filter(Submission.submitted_at >= since).all()
    labels = day_labels(days)
    accepted = daily_counts([s for s, v, _ in subs if v == "AC"], days)
    other = daily_counts([s for s, v, _ in subs if v != "AC"], days)
    active = Counter()
    seen = set()
    for ts, _v, uid in subs:
        if (ts.date(), uid) not in seen:
            seen.add((ts.date(), uid))
            active[ts.date()] += 1
    signups = daily_counts([u for (u,) in db.session.query(User.created_at).filter(User.created_at >= since)], days)
    return {
        "labels": [d.strftime("%b %d") for d in labels],
        "accepted": accepted,
        "other": other,
        "active": [active.get(d, 0) for d in labels],
        "signups": signups,
    }


def solved_problem_ids(user_id):
    rows = (db.session.query(Submission.problem_id)
            .filter_by(user_id=user_id, verdict="AC").distinct().all())
    return {r[0] for r in rows}


def attempted_problem_ids(user_id):
    rows = db.session.query(Submission.problem_id).filter_by(user_id=user_id).distinct().all()
    return {r[0] for r in rows}


def acceptance_stats(problem_ids=None):
    """{problem_id: (judged_submissions, accepted, distinct_students, distinct_solvers)}"""
    q = (db.session.query(
            Submission.problem_id,
            func.count(Submission.id),
            func.sum(case((Submission.verdict == "AC", 1), else_=0)),
            func.count(func.distinct(Submission.user_id)))
         .filter(Submission.verdict != "PENDING"))
    if problem_ids is not None:
        q = q.filter(Submission.problem_id.in_(problem_ids))
    rows = q.group_by(Submission.problem_id).all()
    solvers = dict(
        db.session.query(Submission.problem_id, func.count(func.distinct(Submission.user_id)))
        .filter(Submission.verdict == "AC").group_by(Submission.problem_id).all()
    )
    return {pid: (total, accepted or 0, students, solvers.get(pid, 0)) for pid, total, accepted, students in rows}


def solve_timeline(user_id, days=30):
    """Cumulative count of distinct problems solved, per day, for the last `days` days."""
    first_ac = dict(
        db.session.query(Submission.problem_id, func.min(Submission.submitted_at))
        .filter_by(user_id=user_id, verdict="AC").group_by(Submission.problem_id).all()
    )
    labels = day_labels(days)
    cumulative = []
    for d in labels:
        cumulative.append(sum(1 for ts in first_ac.values() if ts.date() <= d))
    return {"labels": [d.strftime("%b %d") for d in labels], "solved": cumulative}


def streak(user_id):
    """Consecutive days (ending today or yesterday) with at least one submission."""
    days = {ts.date() for (ts,) in db.session.query(Submission.submitted_at).filter_by(user_id=user_id)}
    today = utcnow().date()
    cursor = today if today in days else today - timedelta(days=1)
    count = 0
    while cursor in days:
        count += 1
        cursor -= timedelta(days=1)
    return count


def leaderboard_rows(limit=None, problem_ids=None, since=None, until=None):
    q = (db.session.query(Submission.user_id, Problem.difficulty)
         .join(Problem, Submission.problem_id == Problem.id)
         .filter(Submission.verdict == "AC", Problem.status == "approved"))
    if problem_ids is not None:
        q = q.filter(Submission.problem_id.in_(problem_ids))
    if since:
        q = q.filter(Submission.submitted_at >= since)
    if until:
        q = q.filter(Submission.submitted_at <= until)
    pairs = q.group_by(Submission.user_id, Submission.problem_id, Problem.difficulty).all()

    solved, score = Counter(), Counter()
    for user_id, difficulty in pairs:
        solved[user_id] += 1
        score[user_id] += POINTS.get(difficulty, 10)
    if not score:
        return []
    users = {u.id: u for u in User.query.filter(User.id.in_(score.keys()), User.is_blocked.is_(False))}
    rows = [{"user": users[uid], "solved": solved[uid], "score": score[uid]} for uid in score if uid in users]
    rows.sort(key=lambda r: (-r["score"], -r["solved"], r["user"].username.lower()))
    return rows[:limit] if limit else rows
