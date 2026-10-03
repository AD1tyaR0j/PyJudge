"""The Python course: content integrity, lessons, quizzes, progress, achievements, and authoring."""
import pytest

from app import db, models
from app.curriculum import load_curriculum, parse_lesson, playgrounds, sync_curriculum, CurriculumError
from app.judge.sandbox import run_code
from app.lessons import render_lesson
from app.models import Lesson, LessonProgress, Message, Topic, User
from app.stats import achievements, course_overview, streak

from .conftest import login, make_user, new_client, post

CURRICULUM = load_curriculum()
EXAMPLES = [(f"{t['slug']}/{l['slug']}#{i}", code, stdin, err)
            for t in CURRICULUM for l in t["lessons"]
            for i, (code, stdin, err) in enumerate(playgrounds(l["content"]), 1)]


# ---------------------------------------------------------------- content

def test_course_shape():
    assert len(CURRICULUM) >= 10
    for t in CURRICULUM:
        assert t["title"] and t["summary"] and t["lessons"], t["slug"]
        for lesson in t["lessons"]:
            assert len(lesson["quiz"]) >= 3, lesson["slug"]
            assert playgrounds(lesson["content"]), f"{lesson['slug']} has no runnable example"


@pytest.mark.parametrize("name,code,stdin,expects_error", EXAMPLES, ids=[e[0] for e in EXAMPLES])
def test_every_runnable_example_works(name, code, stdin, expects_error):
    r = run_code(code, stdin, time_limit=3, memory_limit=256)
    if expects_error:
        assert r.status == "RE", f"{name} was supposed to fail"
    else:
        assert r.status == "OK", f"{name}: {r.status}\n{r.stderr}"


def test_quiz_parser_rejects_bad_questions():
    bad = "---\ntitle: X\n---\nbody\n<!-- quiz -->\nQ: Pick one\n- [ ] a\n- [ ] b\n"
    with pytest.raises(CurriculumError):
        parse_lesson(bad)
    two = "---\ntitle: X\n---\nbody\n<!-- quiz -->\nQ: Pick one\n- [x] a\n- [x] b\n"
    with pytest.raises(CurriculumError):
        parse_lesson(two)


def test_render_turns_pyrun_into_playground_and_escapes():
    html = str(render_lesson("Intro\n\n```pyrun\nprint('<b>')\n### stdin\n5\n```\n\nAfter"))
    assert 'class="playground"' in html and "&lt;b&gt;" in html and "<b>" not in html
    assert 'class="pg-stdin"' in html and ">5" in html


# ---------------------------------------------------------------- syncing

@pytest.fixture
def course(app):
    with app.app_context():
        sync_curriculum(db, models)
        first = Lesson.query.join(Topic).order_by(Topic.position, Lesson.position).first()
        return {"topic": first.topic.slug, "lesson": first.slug, "id": first.id,
                "answers": {q.id: q.answer for q in first.questions}}


def test_sync_is_idempotent_and_keeps_custom_lessons(app, course):
    with app.app_context():
        total = Lesson.query.count()
        topic = Topic.query.first()
        custom = Lesson(topic_id=topic.id, slug="mine", title="Mine", content="x", source="custom", status="approved")
        db.session.add(custom)
        db.session.commit()
        created_topics, created, updated = sync_curriculum(db, models)
        assert (created_topics, created) == (0, 0) and updated == total
        assert Lesson.query.filter_by(slug="mine").one().content == "x"


# ---------------------------------------------------------------- pages, quizzes, progress

def test_pages_render_for_guests_and_students(app, course):
    lesson_url = f"/learn/{course['topic']}/{course['lesson']}"
    for client in (new_client(app), _student(app)):
        assert client.get("/learn/").status_code == 200
        assert client.get(f"/learn/{course['topic']}").status_code == 200
        page = client.get(lesson_url)
        assert page.status_code == 200 and b'class="playground"' in page.data and b"quiz-form" in page.data


def _student(app, name="stu"):
    make_user(app, name)
    c = new_client(app)
    login(c, name)
    return c


def _answer(client, course, correct=True):
    data = {f"q{qid}": (ans if correct else ans + 1) for qid, ans in course["answers"].items()}
    return post(client, f"/learn/{course['topic']}/{course['lesson']}/quiz", **data)


def test_passing_quiz_completes_lesson(app, course):
    c = _student(app)
    r = _answer(c, course).json
    assert r["score"] == r["total"] and r["passed"] and r["completed"] and r["saved"]
    with app.app_context():
        p = LessonProgress.query.one()
        assert p.completed_at and p.quiz_score == r["total"] and p.attempts == 1
        user = db.session.get(User, p.user_id)
        assert course_overview(user)["done"] == 1
        assert streak(user.id) == 1
        assert {b["name"]: b["earned"] for b in achievements(user)}["First Steps"]


def test_failing_quiz_does_not_complete_but_best_score_is_kept(app, course):
    c = _student(app)
    r = _answer(c, course, correct=False).json
    assert r["score"] == 0 and not r["passed"] and not r["completed"]
    assert all("explanation" in item for item in r["results"])
    _answer(c, course, correct=True)
    _answer(c, course, correct=False)
    with app.app_context():
        p = LessonProgress.query.one()
        assert p.completed_at and p.quiz_score == p.quiz_total and p.attempts == 3


def test_guest_quiz_is_graded_but_not_saved(app, course):
    r = _answer(new_client(app), course).json
    assert r["passed"] and r["saved"] is False
    with app.app_context():
        assert LessonProgress.query.count() == 0


def test_mark_complete_for_lessons_without_quiz(app, course):
    with app.app_context():
        topic = Topic.query.first()
        db.session.add(Lesson(topic_id=topic.id, slug="reading", title="Reading only", content="Text",
                              source="custom", status="approved"))
        db.session.commit()
    c = _student(app)
    assert post(c, f"/learn/{course['topic']}/reading/complete").json["completed"]
    assert post(c, f"/learn/{course['topic']}/{course['lesson']}/complete").status_code == 400  # has a quiz


# ---------------------------------------------------------------- authoring and approval

def _new_lesson(client, topic_id, title="Walrus Operator"):
    return post(client, "/studio/lessons/new", title=title, topic_id=str(topic_id), minutes="6",
                practice_tags="basics", content="Use `:=`\n\n```pyrun\nif (n := 3) > 2:\n    print(n)\n```")


def test_instructor_lesson_needs_approval(app, course):
    make_user(app, "teach", role="instructor")
    make_user(app, "root", role="admin")
    teacher, admin, student = new_client(app), new_client(app), _student(app)
    login(teacher, "teach")
    login(admin, "root")
    with app.app_context():
        topic = Topic.query.filter_by(slug=course["topic"]).one()
        topic_id, topic_slug = topic.id, topic.slug

    _new_lesson(teacher, topic_id)
    with app.app_context():
        lesson = Lesson.query.filter_by(slug="walrus-operator").one()
        assert lesson.status == "pending" and lesson.source == "custom"
        lid = lesson.id
    url = f"/learn/{topic_slug}/walrus-operator"
    assert student.get(url).status_code == 404
    assert teacher.get(url).status_code == 200

    post(teacher, f"/studio/lessons/{lid}/questions", prompt="What does := do?",
         option=["assigns inside an expression", "compares", "", ""], answer="0", explanation="Walrus!")
    with app.app_context():
        assert len(db.session.get(Lesson, lid).questions) == 1

    post(admin, f"/admin/lessons/{lid}/review", decision="reject")  # no note: refused
    with app.app_context():
        assert db.session.get(Lesson, lid).status == "pending"
    post(admin, f"/admin/lessons/{lid}/review", decision="approve")
    with app.app_context():
        assert db.session.get(Lesson, lid).status == "approved"
        assert "approved" in Message.query.order_by(Message.id.desc()).first().body
    assert student.get(url).status_code == 200


def test_instructors_cannot_edit_builtin_lessons(app, course):
    make_user(app, "teach", role="instructor")
    c = new_client(app)
    login(c, "teach")
    assert c.get(f"/studio/lessons/{course['id']}/edit").status_code == 403


def test_question_validation(app, course):
    make_user(app, "root", role="admin")
    c = new_client(app)
    login(c, "root")
    before = len(course["answers"])
    post(c, f"/studio/lessons/{course['id']}/questions", prompt="Q?", option=["only one", "", "", ""], answer="0")
    post(c, f"/studio/lessons/{course['id']}/questions", prompt="Q?", option=["a", "b", "", ""], answer="3")
    with app.app_context():
        assert len(db.session.get(Lesson, course["id"]).questions) == before


def test_deleting_user_removes_lesson_progress(app, course):
    make_user(app, "root", role="admin")
    c = _student(app)
    _answer(c, course)
    admin = new_client(app)
    login(admin, "root")
    with app.app_context():
        sid = User.query.filter_by(username="stu").one().id
    post(admin, f"/admin/users/{sid}/delete")
    with app.app_context():
        assert LessonProgress.query.count() == 0


def test_staff_pages_render(app, course):
    make_user(app, "teach", role="instructor")
    make_user(app, "root", role="admin")
    for name, paths in {"teach": ["/instructor/lessons", "/studio/lessons/new", "/instructor/"],
                        "root": ["/instructor/lessons", f"/studio/lessons/{course['id']}/edit", "/admin/approvals",
                                 "/admin/"]}.items():
        c = new_client(app)
        login(c, name)
        for path in paths:
            assert c.get(path).status_code == 200, (name, path)


def test_student_dashboard_and_profile_show_learning(app, course):
    c = _student(app)
    _answer(c, course)
    dash = c.get("/student").data.decode()
    assert "Achievements" in dash and "First Steps" in dash and "Python course" in dash
    assert "First Steps" in c.get("/u/stu").data.decode()
    assert "Lessons" in c.get("/").data.decode()
