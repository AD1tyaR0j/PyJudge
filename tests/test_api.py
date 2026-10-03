from app import db
from app.models import Submission, User

from .conftest import api_post, login, make_user, post, problem_id

SUM = "a, b = map(int, input().split())\nprint(a + b)"


def test_register_creates_student(client, app):
    r = post(client, "/register", username="bob", name="Bob Smith", email="bob@example.com",
             password="password123", confirm="password123")
    assert r.status_code == 302
    with app.app_context():
        user = User.query.filter_by(username="bob").one()
        assert user.role == "student" and user.name == "Bob Smith"
        assert user.password_hash != "password123"


def test_login_rejects_bad_password(client, app):
    make_user(app)
    r = login(client, password="wrong")
    assert b"Invalid username" in r.data


def test_post_without_csrf_is_rejected(client, app):
    make_user(app)
    r = client.post("/login", data={"identifier": "alice", "password": "password123"})
    assert r.status_code == 400


def test_run_requires_login(client):
    r = api_post(client, "/api/run", {"code": "print(1)"})
    assert r.status_code == 401


def test_run_with_custom_input(client, app):
    make_user(app)
    login(client)
    r = api_post(client, "/api/run", {"code": SUM, "input": "7 8"})
    assert r.status_code == 200
    assert r.json["status"] == "OK" and r.json["stdout"].strip() == "15"


def test_submit_accepted(client, app):
    make_user(app)
    login(client)
    r = api_post(client, "/api/submit", {"problem_id": problem_id(app), "code": SUM})
    assert r.status_code == 202
    sub = client.get(f"/api/submissions/{r.json['submission_id']}").json
    assert sub["verdict"] == "AC" and sub["passed"] == 3 and sub["total"] == 3

    board = client.get("/api/leaderboard").json
    assert board[0]["username"] == "alice" and board[0]["score"] == 10


def test_submit_wrong_answer(client, app):
    make_user(app)
    login(client)
    r = api_post(client, "/api/submit", {"problem_id": problem_id(app), "code": "print(5)"})
    sub = client.get(f"/api/submissions/{r.json['submission_id']}").json
    assert sub["verdict"] == "WA" and sub["passed"] == 1


def test_cannot_view_other_students_submission(client, app):
    other = make_user(app, "mallory")
    make_user(app)
    with app.app_context():
        sub = Submission(user_id=other, problem_id=problem_id(app), code="print(1)", verdict="WA")
        db.session.add(sub)
        db.session.commit()
        sub_id = sub.id
    login(client)
    assert client.get(f"/api/submissions/{sub_id}").status_code == 404
    assert client.get(f"/submissions/{sub_id}").status_code == 403


def test_role_areas_are_protected(client, app):
    make_user(app)
    login(client)
    for path in ["/admin/", "/admin/users", "/admin/settings", "/instructor/", "/studio/exercises/new"]:
        assert client.get(path).status_code == 403, path


def test_blocked_user_cannot_log_in(client, app):
    uid = make_user(app)
    with app.app_context():
        db.session.get(User, uid).is_blocked = True
        db.session.commit()
    r = login(client)
    assert b"blocked" in r.data


def test_dashboard_redirects_by_role(app):
    from .conftest import new_client
    for role, target in [("student", "/student"), ("instructor", "/instructor/"), ("admin", "/admin/")]:
        make_user(app, role, role=role)
        c = new_client(app)
        login(c, role)
        r = c.get("/dashboard")
        assert r.status_code == 302 and r.headers["Location"].endswith(target), role


def test_pages_render_for_every_role(app):
    from .conftest import new_client
    pages = {
        "student": ["/", "/problems", "/problems/sum", "/leaderboard", "/submissions", "/u/student",
                    "/student", "/challenges/", "/messages/", "/messages/new", "/settings"],
        "instructor": ["/instructor/", "/instructor/exercises", "/instructor/submissions",
                       "/instructor/reports", "/studio/exercises/new", "/challenges/new", "/messages/"],
        "admin": ["/admin/", "/admin/users", "/admin/content", "/admin/approvals", "/admin/settings",
                  "/instructor/reports"],
    }
    for role, paths in pages.items():
        make_user(app, role, role=role)
        c = new_client(app)
        login(c, role)
        for path in paths:
            assert c.get(path).status_code == 200, (role, path)
