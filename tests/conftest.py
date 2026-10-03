import pytest

from app import create_app, db
from app.models import Problem, TestCase, User


@pytest.fixture
def app(tmp_path):
    app = create_app({
        "TESTING": True,
        "SECRET_KEY": "test",
        "SQLALCHEMY_DATABASE_URI": f"sqlite:///{tmp_path / 'test.db'}",
        "JUDGE_SYNC": True,
    })
    with app.app_context():
        problem = Problem(title="Sum", slug="sum", difficulty="Easy", description="Add two numbers.", status="approved")
        problem.test_cases = [
            TestCase(input="2 3", expected_output="5", is_sample=True),
            TestCase(input="10 -4", expected_output="6"),
            TestCase(input="0 0", expected_output="0"),
        ]
        db.session.add(problem)
        db.session.commit()
    yield app
    with app.app_context():
        db.session.remove()
        db.engine.dispose()


@pytest.fixture
def client(app):
    return new_client(app)


def new_client(app):
    client = app.test_client()
    with client.session_transaction() as sess:
        sess["_csrf"] = "token"
    return client


def make_user(app, username="alice", password="password123", role="student"):
    with app.app_context():
        user = User(username=username, name=username.title(), email=f"{username}@example.com", role=role)
        user.set_password(password)
        db.session.add(user)
        db.session.commit()
        return user.id


def login(client, username="alice", password="password123"):
    return client.post("/login", data={"identifier": username, "password": password, "csrf_token": "token"})


def post(client, path, **data):
    return client.post(path, data={"csrf_token": "token", **data})


def api_post(client, path, payload):
    return client.post(path, json=payload, headers={"X-CSRF-Token": "token"})


def problem_id(app, slug="sum"):
    with app.app_context():
        return Problem.query.filter_by(slug=slug).one().id
