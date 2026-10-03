-- PyJudge schema (SQLite dialect; types map directly to MySQL/PostgreSQL).
-- Timestamps are stored as UTC text 'yyyy-MM-dd HH:mm:ss'.
-- Deleting a user/problem/lesson cascades to rows that only make sense with it;
-- "who did this" columns (created_by, graded_by, ...) are set to NULL instead.

CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    name          TEXT    NOT NULL,
    email         TEXT    NOT NULL UNIQUE,
    bio           TEXT    NOT NULL DEFAULT '',
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL DEFAULT 'student' CHECK (role IN ('student', 'instructor', 'admin')),
    blocked       INTEGER NOT NULL DEFAULT 0,
    created_at    TEXT    NOT NULL,
    last_seen_at  TEXT
);

CREATE TABLE IF NOT EXISTS problems (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    title        TEXT    NOT NULL,
    slug         TEXT    NOT NULL UNIQUE,
    description  TEXT    NOT NULL DEFAULT '',
    difficulty   TEXT    NOT NULL DEFAULT 'Easy' CHECK (difficulty IN ('Easy', 'Medium', 'Hard')),
    time_limit   INTEGER NOT NULL DEFAULT 2,
    memory_limit INTEGER NOT NULL DEFAULT 128,
    tags         TEXT    NOT NULL DEFAULT '',
    starter_code TEXT    NOT NULL DEFAULT '',
    status       TEXT    NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    review_note  TEXT,
    reviewed_by  INTEGER REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at  TEXT,
    created_by   INTEGER REFERENCES users (id) ON DELETE SET NULL,
    created_at   TEXT    NOT NULL,
    updated_at   TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_problems_status ON problems (status);

CREATE TABLE IF NOT EXISTS test_cases (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    problem_id      INTEGER NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    input           TEXT    NOT NULL DEFAULT '',
    expected_output TEXT    NOT NULL DEFAULT '',
    is_sample       INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_test_cases_problem ON test_cases (problem_id);

CREATE TABLE IF NOT EXISTS submissions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    problem_id   INTEGER NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    code         TEXT    NOT NULL,
    verdict      TEXT    NOT NULL DEFAULT 'PENDING',
    passed       INTEGER NOT NULL DEFAULT 0,
    total        INTEGER NOT NULL DEFAULT 0,
    runtime_ms   INTEGER,
    error        TEXT,
    grade        INTEGER CHECK (grade IS NULL OR grade BETWEEN 0 AND 100),
    feedback     TEXT,
    graded_by    INTEGER REFERENCES users (id) ON DELETE SET NULL,
    graded_at    TEXT,
    submitted_at TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_submissions_user ON submissions (user_id);
CREATE INDEX IF NOT EXISTS idx_submissions_problem ON submissions (problem_id);
CREATE INDEX IF NOT EXISTS idx_submissions_verdict ON submissions (verdict);

CREATE TABLE IF NOT EXISTS messages (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    sender_id     INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id  INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body          TEXT    NOT NULL,
    submission_id INTEGER REFERENCES submissions (id) ON DELETE SET NULL,
    created_at    TEXT    NOT NULL,
    read_at       TEXT
);
CREATE INDEX IF NOT EXISTS idx_messages_recipient ON messages (recipient_id, read_at);

CREATE TABLE IF NOT EXISTS challenges (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT    NOT NULL,
    description TEXT    NOT NULL DEFAULT '',
    start_at    TEXT    NOT NULL,
    end_at      TEXT    NOT NULL,
    created_by  INTEGER REFERENCES users (id) ON DELETE SET NULL,
    created_at  TEXT    NOT NULL,
    CHECK (end_at > start_at)
);

CREATE TABLE IF NOT EXISTS challenge_problems (
    challenge_id INTEGER NOT NULL REFERENCES challenges (id) ON DELETE CASCADE,
    problem_id   INTEGER NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    PRIMARY KEY (challenge_id, problem_id)
);

CREATE TABLE IF NOT EXISTS topics (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    slug        TEXT    NOT NULL UNIQUE,
    title       TEXT    NOT NULL,
    level       TEXT    NOT NULL DEFAULT 'Beginner',
    summary     TEXT    NOT NULL DEFAULT '',
    description TEXT    NOT NULL DEFAULT '',
    position    INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS lessons (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    topic_id      INTEGER NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    slug          TEXT    NOT NULL,
    title         TEXT    NOT NULL,
    content       TEXT    NOT NULL DEFAULT '',
    minutes       INTEGER NOT NULL DEFAULT 5,
    practice_tags TEXT    NOT NULL DEFAULT '',
    position      INTEGER NOT NULL DEFAULT 0,
    source        TEXT    NOT NULL DEFAULT 'custom' CHECK (source IN ('builtin', 'custom')),
    status        TEXT    NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    review_note   TEXT,
    reviewed_by   INTEGER REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at   TEXT,
    created_by    INTEGER REFERENCES users (id) ON DELETE SET NULL,
    created_at    TEXT    NOT NULL,
    updated_at    TEXT    NOT NULL,
    UNIQUE (topic_id, slug)
);

CREATE TABLE IF NOT EXISTS quiz_questions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    lesson_id    INTEGER NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    prompt       TEXT    NOT NULL,
    options_json TEXT    NOT NULL,
    answer       INTEGER NOT NULL,
    explanation  TEXT    NOT NULL DEFAULT '',
    position     INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lesson_id    INTEGER NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    completed_at TEXT,
    quiz_score   INTEGER,
    quiz_total   INTEGER,
    attempts     INTEGER NOT NULL DEFAULT 0,
    updated_at   TEXT    NOT NULL,
    UNIQUE (user_id, lesson_id)
);

CREATE TABLE IF NOT EXISTS settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
