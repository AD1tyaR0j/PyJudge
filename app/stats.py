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
    """Consecutive days (ending today or yesterday) with a submission or a completed lesson."""
    from .models import LessonProgress
    days = {ts.date() for (ts,) in db.session.query(Submission.submitted_at).filter_by(user_id=user_id)}
    days |= {ts.date() for (ts,) in db.session.query(LessonProgress.completed_at)
             .filter(LessonProgress.user_id == user_id, LessonProgress.completed_at.isnot(None))}
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


# ---------------------------------------------------------------- learning

def progress_map(user_id):
    """{lesson_id: LessonProgress} for one user."""
    from .models import LessonProgress
    return {p.lesson_id: p for p in LessonProgress.query.filter_by(user_id=user_id)}


def course_overview(user):
    """Topics with approved lessons, per-topic progress, and where to continue."""
    from .models import Topic
    progress = progress_map(user.id) if user.is_authenticated else {}
    topics, all_lessons = [], []
    for t in Topic.query.order_by(Topic.position).all():
        lessons = t.approved_lessons
        if not lessons:
            continue
        done = sum(1 for lesson in lessons if lesson.id in progress and progress[lesson.id].completed_at)
        started = any(lesson.id in progress for lesson in lessons)
        topics.append({"topic": t, "lessons": lessons, "done": done, "total": len(lessons),
                       "percent": round(100 * done / len(lessons)),
                       "state": "done" if done == len(lessons) else "active" if started or done else "new",
                       "minutes": sum(lesson.minutes for lesson in lessons)})
        all_lessons.extend(lessons)
    total = len(all_lessons)
    done = sum(1 for lesson in all_lessons if lesson.id in progress and progress[lesson.id].completed_at)
    next_lesson = next((lesson for lesson in all_lessons
                        if not (lesson.id in progress and progress[lesson.id].completed_at)), None)
    scored = [p for p in progress.values() if p.quiz_total]
    quiz_avg = round(100 * sum(p.quiz_score for p in scored) / sum(p.quiz_total for p in scored)) if scored else None
    return {"topics": topics, "total": total, "done": done, "percent": round(100 * done / total) if total else 0,
            "next": next_lesson, "progress": progress, "quiz_avg": quiz_avg,
            "perfect_quizzes": sum(1 for p in scored if p.perfect)}


def achievements(user):
    """Badges computed from activity (nothing extra is stored)."""
    from .models import Challenge
    course = course_overview(user)
    solved_ids = solved_problem_ids(user.id)
    hard_solved = Problem.query.filter(Problem.id.in_(solved_ids or [-1]), Problem.difficulty == "Hard").count()
    topics_done = sum(1 for t in course["topics"] if t["state"] == "done")
    days = streak(user.id)

    def challenge_done():
        from .routes.challenges import challenge_progress
        for c in Challenge.query.all():
            ids = c.approved_problems
            if ids and len(challenge_progress(c, user.id)) == len(ids):
                return True
        return False

    defs = [
        ("👣", "First Steps", "Complete your first lesson", course["done"] >= 1, f"{min(course['done'], 1)}/1"),
        ("📚", "Bookworm", "Complete 10 lessons", course["done"] >= 10, f"{min(course['done'], 10)}/10"),
        ("🧩", "Topic Master", "Finish every lesson in a topic", topics_done >= 1, f"{topics_done} topics"),
        ("🎓", "Graduate", "Complete the whole Python course",
         course["total"] > 0 and course["done"] == course["total"], f"{course['done']}/{course['total']}"),
        ("🧠", "Quiz Whiz", "Get 5 perfect quiz scores", course["perfect_quizzes"] >= 5, f"{min(course['perfect_quizzes'], 5)}/5"),
        ("✅", "Problem Solver", "Solve 10 exercises", len(solved_ids) >= 10, f"{min(len(solved_ids), 10)}/10"),
        ("🏔️", "Hard Hitter", "Solve a Hard exercise", hard_solved >= 1, f"{min(hard_solved, 1)}/1"),
        ("🔥", "On Fire", "Keep a 7-day learning streak", days >= 7, f"{min(days, 7)}/7 days"),
        ("🏁", "Challenger", "Complete a challenge", None, ""),
    ]
    out = []
    for icon, name, desc, earned, progress in defs:
        if earned is None:
            earned = challenge_done()
            progress = "done" if earned else "0/1"
        out.append({"icon": icon, "name": name, "desc": desc, "earned": bool(earned), "progress": progress})
    return out
