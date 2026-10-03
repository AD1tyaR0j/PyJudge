import re
from datetime import datetime, timezone

from flask_login import UserMixin
from werkzeug.security import check_password_hash, generate_password_hash

from . import db

ROLES = ("student", "instructor", "admin")
DIFFICULTIES = ("Easy", "Medium", "Hard")
STATUSES = ("pending", "approved", "rejected")
POINTS = {"Easy": 10, "Medium": 20, "Hard": 40}
DEFAULT_STARTER = "# Read input with input() and print the answer\n"


def utcnow():
    """Naive UTC: SQLite drops tzinfo, so everything is stored and compared naive."""
    return datetime.now(timezone.utc).replace(tzinfo=None)


def slugify(text):
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-") or "exercise"


class User(UserMixin, db.Model):
    __tablename__ = "users"

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(50), unique=True, nullable=False, index=True)
    name = db.Column(db.String(100), nullable=False, default="")
    email = db.Column(db.String(100), unique=True, nullable=False, index=True)
    bio = db.Column(db.Text, default="")
    password_hash = db.Column(db.String(255), nullable=False)
    role = db.Column(db.String(12), nullable=False, default="student")
    is_blocked = db.Column(db.Boolean, nullable=False, default=False)
    created_at = db.Column(db.DateTime, default=utcnow)
    last_seen_at = db.Column(db.DateTime)

    submissions = db.relationship("Submission", back_populates="user", cascade="all, delete-orphan",
                                  foreign_keys="Submission.user_id")

    def set_password(self, password):
        self.password_hash = generate_password_hash(password)

    def check_password(self, password):
        return check_password_hash(self.password_hash, password)

    @property
    def display_name(self):
        return self.name or self.username

    @property
    def is_admin(self):
        return self.role == "admin"

    @property
    def is_instructor(self):
        return self.role == "instructor"

    @property
    def is_student(self):
        return self.role == "student"

    @property
    def can_author(self):
        """Admins and instructors can create exercises and challenges."""
        return self.role in ("admin", "instructor")

    @property
    def is_active(self):  # Flask-Login refuses logins for inactive users
        return not self.is_blocked


class Problem(db.Model):
    """A coding exercise. Instructors submit them; admins approve or reject them."""
    __tablename__ = "problems"

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(150), nullable=False)
    slug = db.Column(db.String(150), unique=True, nullable=False, index=True)
    description = db.Column(db.Text, nullable=False, default="")
    difficulty = db.Column(db.String(10), nullable=False, default="Easy")
    time_limit = db.Column(db.Integer, nullable=False, default=2)
    memory_limit = db.Column(db.Integer, nullable=False, default=128)
    tags = db.Column(db.String(255), default="")
    starter_code = db.Column(db.Text, default=DEFAULT_STARTER)
    status = db.Column(db.String(10), nullable=False, default="pending", index=True)
    review_note = db.Column(db.Text)
    reviewed_by = db.Column(db.Integer, db.ForeignKey("users.id"))
    reviewed_at = db.Column(db.DateTime)
    created_by = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=True)
    created_at = db.Column(db.DateTime, default=utcnow)
    updated_at = db.Column(db.DateTime, default=utcnow, onupdate=utcnow)

    author = db.relationship("User", foreign_keys=[created_by])
    reviewer = db.relationship("User", foreign_keys=[reviewed_by])
    test_cases = db.relationship(
        "TestCase", back_populates="problem", cascade="all, delete-orphan", order_by="TestCase.id"
    )
    submissions = db.relationship("Submission", back_populates="problem", cascade="all, delete-orphan")

    @property
    def tag_list(self):
        return [t.strip() for t in (self.tags or "").split(",") if t.strip()]

    @property
    def sample_cases(self):
        return [tc for tc in self.test_cases if tc.is_sample]

    @property
    def is_approved(self):
        return self.status == "approved"

    def editable_by(self, user):
        return user.is_authenticated and (user.is_admin or (user.is_instructor and self.created_by == user.id))

    def visible_to(self, user):
        return self.is_approved or self.editable_by(user)

    @staticmethod
    def unique_slug(title, exclude_id=None):
        base = slugify(title)
        slug, n = base, 2
        while True:
            existing = Problem.query.filter_by(slug=slug).first()
            if not existing or existing.id == exclude_id:
                return slug
            slug, n = f"{base}-{n}", n + 1


class TestCase(db.Model):
    __tablename__ = "test_cases"
    __test__ = False  # keep pytest from collecting this model

    id = db.Column(db.Integer, primary_key=True)
    problem_id = db.Column(db.Integer, db.ForeignKey("problems.id"), nullable=False, index=True)
    input = db.Column(db.Text, nullable=False, default="")
    expected_output = db.Column(db.Text, nullable=False, default="")
    is_sample = db.Column(db.Boolean, nullable=False, default=False)

    problem = db.relationship("Problem", back_populates="test_cases")


class Submission(db.Model):
    __tablename__ = "submissions"

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False, index=True)
    problem_id = db.Column(db.Integer, db.ForeignKey("problems.id"), nullable=False, index=True)
    code = db.Column(db.Text, nullable=False)
    verdict = db.Column(db.String(10), nullable=False, default="PENDING", index=True)
    passed = db.Column(db.Integer, default=0)
    total = db.Column(db.Integer, default=0)
    runtime_ms = db.Column(db.Integer)
    memory_kb = db.Column(db.Integer)
    error = db.Column(db.Text)
    submitted_at = db.Column(db.DateTime, default=utcnow, index=True)

    # Instructor review
    grade = db.Column(db.Integer)          # 0-100
    feedback = db.Column(db.Text)
    graded_by = db.Column(db.Integer, db.ForeignKey("users.id"))
    graded_at = db.Column(db.DateTime)

    user = db.relationship("User", back_populates="submissions", foreign_keys=[user_id])
    grader = db.relationship("User", foreign_keys=[graded_by])
    problem = db.relationship("Problem", back_populates="submissions")

    @property
    def score_percent(self):
        return round(100 * (self.passed or 0) / self.total) if self.total else 0

    def viewable_by(self, user):
        if not user.is_authenticated:
            return False
        return user.id == self.user_id or self.problem.editable_by(user)

    def to_dict(self):
        return {
            "submission_id": self.id,
            "problem_id": self.problem_id,
            "verdict": self.verdict,
            "passed": self.passed,
            "total": self.total,
            "runtime_ms": self.runtime_ms,
            "memory_kb": self.memory_kb,
            "error": self.error,
            "grade": self.grade,
            "feedback": self.feedback,
            "submitted_at": self.submitted_at.isoformat() if self.submitted_at else None,
        }


class Message(db.Model):
    __tablename__ = "messages"

    id = db.Column(db.Integer, primary_key=True)
    sender_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False, index=True)
    recipient_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False, index=True)
    body = db.Column(db.Text, nullable=False)
    submission_id = db.Column(db.Integer, db.ForeignKey("submissions.id"))
    created_at = db.Column(db.DateTime, default=utcnow, index=True)
    read_at = db.Column(db.DateTime)

    sender = db.relationship("User", foreign_keys=[sender_id])
    recipient = db.relationship("User", foreign_keys=[recipient_id])
    submission = db.relationship("Submission")


challenge_problems = db.Table(
    "challenge_problems",
    db.Column("challenge_id", db.Integer, db.ForeignKey("challenges.id"), primary_key=True),
    db.Column("problem_id", db.Integer, db.ForeignKey("problems.id"), primary_key=True),
)


class Challenge(db.Model):
    """A time-boxed set of exercises. Completed when a student solves them all."""
    __tablename__ = "challenges"

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(150), nullable=False)
    description = db.Column(db.Text, default="")
    start_at = db.Column(db.DateTime, nullable=False)
    end_at = db.Column(db.DateTime, nullable=False)
    created_by = db.Column(db.Integer, db.ForeignKey("users.id"))
    created_at = db.Column(db.DateTime, default=utcnow)

    author = db.relationship("User")
    problems = db.relationship("Problem", secondary=challenge_problems, order_by="Problem.id")

    @property
    def state(self):
        now = utcnow()
        if now < self.start_at:
            return "upcoming"
        if now > self.end_at:
            return "ended"
        return "active"

    @property
    def approved_problems(self):
        return [p for p in self.problems if p.is_approved]

    def editable_by(self, user):
        return user.is_authenticated and (user.is_admin or (user.is_instructor and self.created_by == user.id))


class Topic(db.Model):
    """A chapter of the Python course, e.g. "Strings" or "Functions"."""
    __tablename__ = "topics"

    id = db.Column(db.Integer, primary_key=True)
    slug = db.Column(db.String(80), unique=True, nullable=False, index=True)
    title = db.Column(db.String(120), nullable=False)
    icon = db.Column(db.String(16), default="📘")
    level = db.Column(db.String(20), default="Beginner")
    summary = db.Column(db.String(300), default="")
    description = db.Column(db.Text, default="")
    position = db.Column(db.Integer, nullable=False, default=0)

    lessons = db.relationship("Lesson", back_populates="topic", cascade="all, delete-orphan",
                              order_by="Lesson.position")

    @property
    def approved_lessons(self):
        return [lesson for lesson in self.lessons if lesson.is_approved]


class Lesson(db.Model):
    """A tutorial page. Built-in lessons come from app/curriculum; instructors can add more (admin-approved)."""
    __tablename__ = "lessons"
    __table_args__ = (db.UniqueConstraint("topic_id", "slug"),)

    id = db.Column(db.Integer, primary_key=True)
    topic_id = db.Column(db.Integer, db.ForeignKey("topics.id"), nullable=False, index=True)
    slug = db.Column(db.String(120), nullable=False)
    title = db.Column(db.String(150), nullable=False)
    content = db.Column(db.Text, nullable=False, default="")
    minutes = db.Column(db.Integer, nullable=False, default=5)
    practice_tags = db.Column(db.String(255), default="")
    position = db.Column(db.Integer, nullable=False, default=0)
    source = db.Column(db.String(10), nullable=False, default="custom")  # "builtin" or "custom"
    status = db.Column(db.String(10), nullable=False, default="pending", index=True)
    review_note = db.Column(db.Text)
    reviewed_by = db.Column(db.Integer, db.ForeignKey("users.id"))
    reviewed_at = db.Column(db.DateTime)
    created_by = db.Column(db.Integer, db.ForeignKey("users.id"))
    created_at = db.Column(db.DateTime, default=utcnow)
    updated_at = db.Column(db.DateTime, default=utcnow, onupdate=utcnow)

    topic = db.relationship("Topic", back_populates="lessons")
    author = db.relationship("User", foreign_keys=[created_by])
    reviewer = db.relationship("User", foreign_keys=[reviewed_by])
    questions = db.relationship("QuizQuestion", back_populates="lesson", cascade="all, delete-orphan",
                                order_by="QuizQuestion.position")
    progress = db.relationship("LessonProgress", back_populates="lesson", cascade="all, delete-orphan")

    @property
    def is_approved(self):
        return self.status == "approved"

    @property
    def practice_tag_list(self):
        return [t.strip() for t in (self.practice_tags or "").split(",") if t.strip()]

    def editable_by(self, user):
        if not user.is_authenticated:
            return False
        if user.is_admin:
            return True
        return user.is_instructor and self.source == "custom" and self.created_by == user.id

    def visible_to(self, user):
        return self.is_approved or self.editable_by(user)


class QuizQuestion(db.Model):
    __tablename__ = "quiz_questions"

    id = db.Column(db.Integer, primary_key=True)
    lesson_id = db.Column(db.Integer, db.ForeignKey("lessons.id"), nullable=False, index=True)
    prompt = db.Column(db.Text, nullable=False)
    options_json = db.Column(db.Text, nullable=False, default="[]")
    answer = db.Column(db.Integer, nullable=False, default=0)  # index into options
    explanation = db.Column(db.Text, default="")
    position = db.Column(db.Integer, nullable=False, default=0)

    lesson = db.relationship("Lesson", back_populates="questions")

    @property
    def options(self):
        import json
        return json.loads(self.options_json or "[]")

    @options.setter
    def options(self, values):
        import json
        self.options_json = json.dumps(list(values))


class LessonProgress(db.Model):
    __tablename__ = "lesson_progress"
    __table_args__ = (db.UniqueConstraint("user_id", "lesson_id"),)

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False, index=True)
    lesson_id = db.Column(db.Integer, db.ForeignKey("lessons.id"), nullable=False, index=True)
    completed_at = db.Column(db.DateTime)
    quiz_score = db.Column(db.Integer)   # best score so far
    quiz_total = db.Column(db.Integer)
    attempts = db.Column(db.Integer, nullable=False, default=0)
    updated_at = db.Column(db.DateTime, default=utcnow, onupdate=utcnow)

    lesson = db.relationship("Lesson", back_populates="progress")

    @property
    def perfect(self):
        return bool(self.quiz_total) and self.quiz_score == self.quiz_total


class Setting(db.Model):
    __tablename__ = "settings"

    key = db.Column(db.String(50), primary_key=True)
    value = db.Column(db.Text, nullable=False, default="")
