"""Admin, instructor, and student workflows from the project spec."""
from datetime import timedelta

from app import db
from app.models import Challenge, Message, Problem, Submission, TestCase, User, utcnow
from app.settings import get_settings

from .conftest import api_post, login, make_user, new_client, post, problem_id

SUM = "a, b = map(int, input().split())\nprint(a + b)"


def create_exercise(client, title="Double It"):
    return post(client, "/studio/exercises/new", title=title, difficulty="Easy", time_limit="2",
                memory_limit="128", description="Print 2n.", tags="math")


# ---------------------------------------------------------------- admin: users

def test_admin_creates_updates_and_deletes_users(client, app):
    make_user(app, "root", role="admin")
    login(client, "root")

    r = post(client, "/admin/users", username="teacher", name="Ms Teacher", email="t@example.com",
             role="instructor", password="password123")
    assert r.status_code == 302
    with app.app_context():
        uid = User.query.filter_by(username="teacher").one().id
        assert db.session.get(User, uid).role == "instructor"

    post(client, f"/admin/users/{uid}/edit", name="Dr Teacher", email="dr@example.com", role="student")
    with app.app_context():
        u = db.session.get(User, uid)
        assert (u.name, u.email, u.role) == ("Dr Teacher", "dr@example.com", "student")

    post(client, f"/admin/users/{uid}/delete")
    with app.app_context():
        assert db.session.get(User, uid) is None


def test_admin_cannot_demote_or_delete_self(client, app):
    uid = make_user(app, "root", role="admin")
    login(client, "root")
    post(client, f"/admin/users/{uid}/edit", name="Root", email="root@example.com", role="student")
    post(client, f"/admin/users/{uid}/delete")
    with app.app_context():
        assert db.session.get(User, uid).role == "admin"


def test_deleting_user_removes_their_messages(client, app):
    make_user(app, "root", role="admin")
    sid = make_user(app, "stu")
    tid = make_user(app, "teach", role="instructor")
    with app.app_context():
        db.session.add(Message(sender_id=sid, recipient_id=tid, body="hi"))
        db.session.commit()
    login(client, "root")
    post(client, f"/admin/users/{sid}/delete")
    with app.app_context():
        assert Message.query.count() == 0


# ---------------------------------------------------------------- content approval

def test_instructor_exercise_needs_approval(app):
    make_user(app, "teach", role="instructor")
    make_user(app, "root", role="admin")
    make_user(app, "stu")
    teacher, admin, student = new_client(app), new_client(app), new_client(app)
    login(teacher, "teach"), login(admin, "root"), login(student, "stu")

    create_exercise(teacher)
    with app.app_context():
        p = Problem.query.filter_by(slug="double-it").one()
        assert p.status == "pending"
        pid = p.id

    # Hidden from students, visible to its author as a preview
    assert student.get("/problems/double-it").status_code == 404
    assert b"Double It" not in student.get("/problems").data
    assert teacher.get("/problems/double-it").status_code == 200

    # Can't approve without test cases
    post(admin, f"/admin/exercises/{pid}/review", decision="approve")
    with app.app_context():
        assert db.session.get(Problem, pid).status == "pending"

    post(teacher, f"/studio/exercises/{pid}/testcases", input="4", expected_output="8", is_sample="1")
    post(admin, f"/admin/exercises/{pid}/review", decision="approve", note="Nice one")
    with app.app_context():
        assert db.session.get(Problem, pid).status == "approved"
        msg = Message.query.filter_by(recipient_id=User.query.filter_by(username="teach").one().id).one()
        assert "approved" in msg.body and "Nice one" in msg.body
    assert student.get("/problems/double-it").status_code == 200


def test_reject_requires_note_and_edit_resubmits(app):
    make_user(app, "teach", role="instructor")
    make_user(app, "root", role="admin")
    teacher, admin = new_client(app), new_client(app)
    login(teacher, "teach"), login(admin, "root")
    create_exercise(teacher)
    pid = problem_id(app, "double-it")

    post(admin, f"/admin/exercises/{pid}/review", decision="reject")
    with app.app_context():
        assert db.session.get(Problem, pid).status == "pending"
    post(admin, f"/admin/exercises/{pid}/review", decision="reject", note="Needs edge cases")
    with app.app_context():
        assert db.session.get(Problem, pid).status == "rejected"

    post(teacher, f"/studio/exercises/{pid}/edit", title="Double It", difficulty="Easy", time_limit="2",
         memory_limit="128", description="Print 2n. Handles negatives.", tags="math")
    with app.app_context():
        p = db.session.get(Problem, pid)
        assert p.status == "pending" and p.review_note is None


def test_admin_exercises_publish_immediately(client, app):
    make_user(app, "root", role="admin")
    login(client, "root")
    create_exercise(client)
    with app.app_context():
        assert Problem.query.filter_by(slug="double-it").one().status == "approved"


def test_instructor_cannot_edit_others_exercises(client, app):
    make_user(app, "teach", role="instructor")
    login(client, "teach")
    assert client.get(f"/studio/exercises/{problem_id(app)}/edit").status_code == 403


# ---------------------------------------------------------------- settings

def test_admin_updates_settings(client, app):
    make_user(app, "root", role="admin")
    login(client, "root")
    r = post(client, "/admin/settings", site_name="CodeCamp", announcement="Exams on Friday",
             default_time_limit="3", default_memory_limit="256", max_pending_submissions="5",
             registration_open="1", messaging_enabled="1")
    assert r.status_code == 302
    page = client.get("/").data
    assert b"CodeCamp" in page and b"Exams on Friday" in page
    with app.test_request_context():
        s = get_settings()
        assert s["default_time_limit"] == 3 and s["require_approval"] is False and s["leaderboard_enabled"] is False


def test_invalid_settings_are_rejected(client, app):
    make_user(app, "root", role="admin")
    login(client, "root")
    post(client, "/admin/settings", site_name="X", default_time_limit="99", default_memory_limit="128",
         max_pending_submissions="3")
    with app.test_request_context():
        assert get_settings()["default_time_limit"] == 2


def test_closed_registration(client, app):
    make_user(app, "root", role="admin")
    login(client, "root")
    post(client, "/admin/settings", site_name="PyJudge", default_time_limit="2", default_memory_limit="128",
         max_pending_submissions="3")  # registration_open unchecked
    anon = new_client(app)
    post(anon, "/register", username="newbie", name="New", email="n@example.com",
         password="password123", confirm="password123")
    with app.app_context():
        assert User.query.filter_by(username="newbie").first() is None


# ---------------------------------------------------------------- grading & feedback

def test_instructor_grades_submission_and_student_is_notified(app):
    tid = make_user(app, "teach", role="instructor")
    make_user(app, "stu")
    with app.app_context():
        db.session.get(Problem, problem_id(app)).created_by = tid
        db.session.commit()
    student, teacher = new_client(app), new_client(app)
    login(student, "stu"), login(teacher, "teach")

    sub_id = api_post(student, "/api/submit", {"problem_id": problem_id(app), "code": SUM}).json["submission_id"]
    assert teacher.get(f"/submissions/{sub_id}").status_code == 200
    assert b"Grade this submission" in teacher.get(f"/submissions/{sub_id}").data

    post(teacher, f"/instructor/submissions/{sub_id}/grade", grade="92", feedback="Clean solution!")
    with app.app_context():
        sub = db.session.get(Submission, sub_id)
        assert sub.grade == 92 and sub.feedback == "Clean solution!"
        assert Message.query.filter_by(submission_id=sub_id).one().recipient.username == "stu"
    assert b"Clean solution!" in student.get("/student").data

    post(teacher, f"/instructor/submissions/{sub_id}/grade", grade="150")
    with app.app_context():
        assert db.session.get(Submission, sub_id).grade == 92


def test_instructor_cannot_grade_others_exercises(app):
    make_user(app, "teach", role="instructor")
    make_user(app, "stu")
    student, teacher = new_client(app), new_client(app)
    login(student, "stu"), login(teacher, "teach")
    sub_id = api_post(student, "/api/submit", {"problem_id": problem_id(app), "code": SUM}).json["submission_id"]
    assert post(teacher, f"/instructor/submissions/{sub_id}/grade", grade="50").status_code == 403


# ---------------------------------------------------------------- messaging

def test_student_and_instructor_message_each_other(app):
    make_user(app, "teach", role="instructor")
    make_user(app, "stu")
    student, teacher = new_client(app), new_client(app)
    login(student, "stu"), login(teacher, "teach")

    post(student, "/messages/new", to="teach", body="How do I read two numbers?")
    assert b"How do I read two numbers?" in teacher.get("/messages/stu").data
    post(teacher, "/messages/stu", body="Use input().split()")
    with app.app_context():
        assert Message.query.count() == 2
        assert Message.query.filter_by(body="How do I read two numbers?").one().read_at is not None


def test_students_cannot_message_students(app):
    make_user(app, "stu")
    make_user(app, "other")
    c = new_client(app)
    login(c, "stu")
    post(c, "/messages/new", to="other", body="psst")
    with app.app_context():
        assert Message.query.count() == 0


# ---------------------------------------------------------------- challenges & progress

def test_challenge_progress_and_completion(app):
    make_user(app, "teach", role="instructor")
    make_user(app, "stu")
    teacher, student = new_client(app), new_client(app)
    login(teacher, "teach"), login(student, "stu")

    now = utcnow()
    fmt = "%Y-%m-%dT%H:%M"
    r = post(teacher, "/challenges/new", title="Sprint", description="Go!",
             start_at=(now - timedelta(hours=1)).strftime(fmt), end_at=(now + timedelta(days=1)).strftime(fmt),
             problem_ids=str(problem_id(app)))
    assert r.status_code == 302
    with app.app_context():
        cid = Challenge.query.one().id

    page = student.get("/student").data.decode()
    assert "Sprint" in page and "0 / 1 solved" in page

    api_post(student, "/api/submit", {"problem_id": problem_id(app), "code": SUM})
    page = student.get("/student").data.decode()
    assert "🏅 Sprint" in page  # moved to completed
    assert "Challenge complete" in student.get(f"/challenges/{cid}").data.decode()


def test_challenge_rejects_bad_dates(client, app):
    make_user(app, "teach", role="instructor")
    login(client, "teach")
    post(client, "/challenges/new", title="Backwards", start_at="2030-01-02T00:00", end_at="2030-01-01T00:00",
         problem_ids=str(problem_id(app)))
    with app.app_context():
        assert Challenge.query.count() == 0


def test_profile_settings_and_password_change(client, app):
    make_user(app)
    login(client)
    post(client, "/settings", action="profile", name="Alice Liddell", email="alice@new.com", bio="Learning Python")
    post(client, "/settings", action="password", current_password="password123",
         new_password="newpassword1", confirm_password="newpassword1")
    with app.app_context():
        u = User.query.filter_by(username="alice").one()
        assert u.name == "Alice Liddell" and u.email == "alice@new.com" and u.check_password("newpassword1")


def test_wrong_current_password_is_rejected(client, app):
    make_user(app)
    login(client)
    post(client, "/settings", action="password", current_password="nope",
         new_password="newpassword1", confirm_password="newpassword1")
    with app.app_context():
        assert User.query.filter_by(username="alice").one().check_password("password123")
