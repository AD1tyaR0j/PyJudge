# Python-Based Online Coding Platform

> A web-based platform where users can write, run, and submit Python code in the browser, solve programming problems, and get instant automated feedback, much like a lightweight LeetCode or HackerRank.

---

## 1. Introduction

### 1.1 Overview
The **Python-Based Online Coding Platform** lets students and developers practice programming without installing anything on their computer. Users write code in a browser editor. The server runs it inside a secure, isolated sandbox, checks it against hidden test cases, and returns the verdict (Accepted, Wrong Answer, Time Limit Exceeded, and so on) within seconds.

### 1.2 Problem Statement
- Beginners struggle to install and configure Python environments.
- Teachers spend a lot of time checking programming assignments by hand.
- Many existing platforms are complex, paid, or don't fit college or classroom use.

### 1.3 Objectives
1. Give users a browser-based code editor with syntax highlighting.
2. Run user code **safely** in an isolated environment.
3. Evaluate submissions automatically against predefined test cases.
4. Track user progress, submissions, and rankings.
5. Give admins and teachers a panel to manage problems and test cases.

### 1.4 Scope
- **In scope:** Python 3 execution, problem bank, automated judging, user accounts, leaderboard, admin panel.
- **Future scope:** Multi-language support (C, C++, Java, JS), live contests, AI hints, plagiarism detection.

---

## 2. User Types & Features

The platform has three user types. Administrators oversee accounts, exercise content, and system settings. Instructors create exercises and guide students. Students complete exercises and challenges.

### 2.1 Admin
| Functionality | Input | Output |
|---|---|---|
| **User Management**: manage accounts and roles | User details (name, email, role) | Confirmation of create / update / delete |
| **Exercise Content Management**: approve or reject instructor exercises | Exercise details (title, description, difficulty) | Approval status (approved / pending / rejected) |
| **System Settings**: manage platform-wide configuration | Configuration settings | Confirmation of settings update |

**Admin Dashboard:** user management table (create / update / delete) · exercise content table (approved and pending) · system settings interface · user activity charts · content approval panel.

### 2.2 Instructor
| Functionality | Input | Output |
|---|---|---|
| **Create Coding Exercises** | Exercise details (title, description, difficulty) | Confirmation of creation (sent for admin approval) |
| **Review Student Submissions** | Submission details | Feedback and grade (0–100) |
| **Interact with Students** | Messages, feedback | Confirmation of message sent |

**Instructor Dashboard:** exercise management · student submissions table for grading · student messages · performance reports · profile management.

### 2.3 Student
| Functionality | Input | Output |
|---|---|---|
| **Complete Coding Exercises** | Exercise selection | Exercise results (verdict) and feedback |
| **Track Progress** | Progress details | Progress report (charts, difficulty breakdown, streak, rank) |
| **Interact with Instructors** | Messages, feedback | Confirmation of message sent |

**Student Dashboard:** exercise completion · graphical progress tracking · instructor messages and feedback · personal profile · active and completed challenges.

### 2.4 Shared Features
| Feature | Description |
|---|---|
| Registration / Login | Session login with hashed passwords; self sign-up creates student accounts |
| Code Editor | Monaco editor with syntax highlighting; Run with custom input; Submit for judging |
| Challenges | Time-boxed sets of exercises with standings; completed when all are solved in the window |
| Leaderboard | Rank by points (Easy 10, Medium 20, Hard 40) |

### 2.3 Verdict Types
| Verdict | Meaning |
|---|---|
| ✅ **AC**: Accepted | All test cases passed |
| ❌ **WA**: Wrong Answer | Output did not match the expected output |
| ⏱ **TLE**: Time Limit Exceeded | Execution took longer than the allowed time |
| 💾 **MLE**: Memory Limit Exceeded | Program used too much memory |
| 💥 **RE**: Runtime Error | Exception raised during execution |
| ⚠️ **CE**: Compilation/Syntax Error | Code has syntax errors |

---

## 3. Technology Stack

| Layer | Technology |
|---|---|
| **Frontend** | HTML5, CSS3 / Tailwind CSS, JavaScript (or React.js) |
| **Code Editor** | Monaco Editor (the editor behind VS Code) or CodeMirror |
| **Backend** | Python 3.11+ with **Flask** / **Django** / **FastAPI** |
| **Database** | PostgreSQL / MySQL (SQLite for development) |
| **ORM** | SQLAlchemy / Django ORM |
| **Code Execution Sandbox** | Docker containers (or `subprocess` + resource limits for a basic version) |
| **Task Queue** | Celery + Redis (asynchronous judging) |
| **Authentication** | JWT / Flask-Login / Django Auth, bcrypt password hashing |
| **Deployment** | Gunicorn + Nginx on Linux (AWS / Render / Railway) |
| **Version Control** | Git & GitHub |

---

## 4. System Architecture

```
┌──────────────────┐        HTTP/JSON        ┌──────────────────────┐
│   Web Browser    │ ──────────────────────▶ │   Web Server (Flask/ │
│ (Editor + UI)    │ ◀────────────────────── │   Django / FastAPI)  │
└──────────────────┘                         └──────────┬───────────┘
                                                        │
                         ┌──────────────────────────────┼─────────────────┐
                         ▼                              ▼                 ▼
                ┌────────────────┐            ┌────────────────┐  ┌──────────────┐
                │   Database     │            │  Redis Queue   │  │  Auth / JWT  │
                │ (PostgreSQL)   │            └───────┬────────┘  └──────────────┘
                └────────────────┘                    │
                                                      ▼
                                            ┌──────────────────────┐
                                            │  Judge Worker(s)     │
                                            │  (Celery)            │
                                            └─────────┬────────────┘
                                                      ▼
                                            ┌──────────────────────┐
                                            │  Docker Sandbox      │
                                            │  - No network        │
                                            │  - CPU/mem limits    │
                                            │  - Timeout           │
                                            └──────────────────────┘
```

### 4.1 Submission Workflow
1. The user writes code and clicks **Submit**.
2. The frontend sends `POST /api/submit` with `{problem_id, code}`.
3. The backend saves the submission with status `PENDING` and pushes a job to the queue.
4. A judge worker picks up the job and runs the code in a sandbox against each test case.
5. The worker compares the output with the expected output, then records the verdict, runtime, and memory.
6. The database is updated and the frontend polls or receives the result over WebSocket.

---

## 5. Secure Code Execution (Sandboxing)

Running untrusted user code is the **most critical** part of the system. The platform must make sure that user code cannot:
- Access the server file system
- Make network calls
- Fork-bomb or exhaust CPU/RAM
- Run forever

### 5.1 Security Measures
| Threat | Mitigation |
|---|---|
| Infinite loops | Hard timeout (e.g., 2 seconds per test case) |
| Memory abuse | Memory cap (e.g., `--memory=128m` in Docker) |
| Fork bombs | `--pids-limit=64` |
| Network access | `--network=none` |
| File system damage | Read-only filesystem, non-root user, temporary working directory |
| Dangerous imports | Run in a container (don't rely only on blocking `os` or `subprocess`) |

### 5.2 Example: Running Code in Docker
```python
import subprocess, tempfile, os

def run_in_sandbox(code: str, stdin_data: str, time_limit: int = 2):
    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, "main.py")
        with open(path, "w") as f:
            f.write(code)

        cmd = [
            "docker", "run", "--rm", "-i",
            "--network=none",
            "--memory=128m", "--cpus=0.5",
            "--pids-limit=64",
            "--read-only",
            "--user", "1000:1000",
            "-v", f"{tmp}:/code:ro",
            "python:3.11-slim",
            "python3", "/code/main.py",
        ]
        try:
            result = subprocess.run(
                cmd, input=stdin_data, capture_output=True,
                text=True, timeout=time_limit + 1,
            )
            return {
                "stdout": result.stdout,
                "stderr": result.stderr,
                "exit_code": result.returncode,
            }
        except subprocess.TimeoutExpired:
            return {"verdict": "TLE"}
```

### 5.3 Example: Judging Logic
```python
def judge(code, test_cases):
    for tc in test_cases:
        res = run_in_sandbox(code, tc.input)
        if res.get("verdict") == "TLE":
            return "TLE"
        if res["exit_code"] != 0:
            return "CE" if "SyntaxError" in res["stderr"] else "RE"
        if res["stdout"].strip() != tc.expected_output.strip():
            return "WA"
    return "AC"
```

---

## 6. Database Design

### 6.1 ER Overview
```
User 1───* Submission *───1 Problem (exercise) 1───* TestCase
User 1───* Message (sender / recipient)       Submission 0..1───* Message
Challenge *───* Problem                        Setting (key/value)
```

### 6.2 Tables

**users**
| Column | Type | Notes |
|---|---|---|
| id | INT (PK) | Auto increment |
| username | VARCHAR(50) | Unique |
| name | VARCHAR(100) | Full name |
| email | VARCHAR(100) | Unique |
| bio | TEXT | Shown on profile |
| password_hash | VARCHAR(255) | Salted hash |
| role | ENUM('student','instructor','admin') | Default `student` |
| is_blocked | BOOLEAN | Blocked users can't log in |
| created_at, last_seen_at | DATETIME | Used for the activity charts |

**problems** (exercises)
| Column | Type | Notes |
|---|---|---|
| id | INT (PK) | |
| title, slug | VARCHAR(150) | Slug is URL-friendly and unique |
| description | TEXT | Markdown supported |
| difficulty | ENUM('Easy','Medium','Hard') | |
| time_limit / memory_limit | INT | Seconds / MB |
| tags, starter_code | VARCHAR / TEXT | |
| status | ENUM('pending','approved','rejected') | Only approved exercises are shown to students |
| review_note, reviewed_by, reviewed_at | TEXT / FK / DATETIME | Admin's decision |
| created_by | INT (FK → users.id) | Author (instructor or admin) |

**test_cases**
| Column | Type | Notes |
|---|---|---|
| id | INT (PK) | |
| problem_id | INT (FK → problems.id) | |
| input, expected_output | TEXT | |
| is_sample | BOOLEAN | Shown to the student if true |

**submissions**
| Column | Type | Notes |
|---|---|---|
| id | INT (PK) | |
| user_id, problem_id | INT (FK) | |
| code | TEXT | |
| verdict | VARCHAR(10) | AC / WA / TLE / MLE / RE / CE / PENDING |
| passed, total, runtime_ms | INT | Judge results |
| grade | INT | Instructor grade 0–100 |
| feedback, graded_by, graded_at | TEXT / FK / DATETIME | Instructor review |
| submitted_at | DATETIME | |

**messages**
| Column | Type | Notes |
|---|---|---|
| id | INT (PK) | |
| sender_id, recipient_id | INT (FK → users.id) | Students ↔ instructors/admins |
| body | TEXT | |
| submission_id | INT (FK, nullable) | Links a question or feedback to a submission |
| created_at, read_at | DATETIME | `read_at` drives unread badges |

**challenges** + **challenge_problems**
| Column | Type | Notes |
|---|---|---|
| id, title, description | | |
| start_at, end_at | DATETIME | Only solves inside the window count |
| created_by | INT (FK → users.id) | |
| challenge_problems | (challenge_id, problem_id) | Many-to-many |

**settings**
| Column | Type | Notes |
|---|---|---|
| key | VARCHAR(50) (PK) | site_name, announcement, registration_open, require_approval, messaging_enabled, leaderboard_enabled, default_time_limit, default_memory_limit, max_pending_submissions |
| value | TEXT | |

---

## 7. API Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user | ❌ |
| POST | `/api/auth/login` | Log in and return a token | ❌ |
| GET | `/api/problems` | List all problems | ❌ |
| GET | `/api/problems/<slug>` | Get problem details + sample tests | ❌ |
| POST | `/api/run` | Run code with custom input | ✅ |
| POST | `/api/submit` | Submit code for judging | ✅ |
| GET | `/api/submissions/<id>` | Get submission status/result | ✅ |
| GET | `/api/users/me/submissions` | User's submission history | ✅ |
| GET | `/api/leaderboard` | Top users | ❌ |
| POST | `/api/admin/problems` | Create a problem | 🔒 Admin |
| PUT | `/api/admin/problems/<id>` | Update a problem | 🔒 Admin |
| DELETE | `/api/admin/problems/<id>` | Delete a problem | 🔒 Admin |
| POST | `/api/admin/problems/<id>/testcases` | Add test cases | 🔒 Admin |

**Example request: `POST /api/submit`**
```json
{
  "problem_id": 12,
  "code": "a, b = map(int, input().split())\nprint(a + b)"
}
```

**Example response**
```json
{
  "submission_id": 845,
  "verdict": "AC",
  "passed": 10,
  "total": 10,
  "runtime_ms": 48,
  "memory_kb": 9200
}
```

---

## 8. Project Folder Structure

```
python-coding-platform/
├── app/
│   ├── __init__.py          # App factory
│   ├── config.py            # Settings (DB URL, secret key, limits)
│   ├── models.py            # User, Problem, TestCase, Submission
│   ├── routes/
│   │   ├── auth.py
│   │   ├── problems.py
│   │   ├── submissions.py
│   │   └── admin.py
│   ├── judge/
│   │   ├── sandbox.py       # Docker execution
│   │   ├── judge.py         # Compare outputs, assign verdict
│   │   └── tasks.py         # Celery tasks
│   ├── templates/           # Jinja2 HTML (if not using React)
│   └── static/
│       ├── css/
│       └── js/editor.js     # Monaco editor setup
├── migrations/
├── tests/
│   ├── test_auth.py
│   ├── test_judge.py
│   └── test_api.py
├── docker/
│   └── sandbox.Dockerfile
├── requirements.txt
├── docker-compose.yml
├── .env.example
├── run.py
└── README.md
```

---

## 9. Installation & Setup

### 9.1 Prerequisites
- Python 3.11+
- Docker & Docker Compose
- Redis
- PostgreSQL (or SQLite for local development)

### 9.2 Steps
```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/python-coding-platform.git
cd python-coding-platform

# 2. Create a virtual environment
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate

# 3. Install dependencies
pip install -r requirements.txt

# 4. Configure environment variables
cp .env.example .env            # then edit DB URL, SECRET_KEY, etc.

# 5. Pull the sandbox image
docker pull python:3.11-slim

# 6. Initialize the database
flask db upgrade

# 7. Start Redis + Celery worker
celery -A app.judge.tasks worker --loglevel=info

# 8. Run the web server
flask run
```
Open **http://localhost:5000** in your browser.

### 9.3 Sample `requirements.txt`
```
Flask==3.0.*
Flask-SQLAlchemy
Flask-Migrate
Flask-Login
Flask-JWT-Extended
bcrypt
celery
redis
psycopg2-binary
python-dotenv
gunicorn
```

---

## 10. Modules Description

| Module | Responsibility |
|---|---|
| **Authentication Module** | Registration, login, logout, password hashing, role-based access |
| **Problem Module** | Listing, filtering, and showing problem statements |
| **Editor Module** | Browser code editor, custom input box, output console |
| **Execution Module** | Sends code to the sandbox and returns stdout/stderr |
| **Judge Module** | Runs all test cases, compares output, assigns a verdict |
| **Submission Module** | Stores and displays the submission history |
| **Leaderboard Module** | Calculates rankings from accepted submissions |
| **Admin Module** | CRUD for problems, test cases, and users |

---

## 11. Testing

| Test Type | What to Test |
|---|---|
| Unit Tests | Judge logic, output comparison, verdict assignment |
| Integration Tests | API endpoints with a test database |
| Security Tests | Infinite loops, `import os; os.system(...)`, fork bombs, network calls, large memory allocation |
| Load Tests | 100+ concurrent submissions (Locust / JMeter) |
| UI Tests | Editor loading, run/submit buttons, result display |

**Sample malicious inputs that must be handled safely:**
```python
while True: pass                       # → TLE
a = [0] * (10**10)                     # → MLE
import os; os.system("rm -rf /")       # → no effect (sandboxed)
import socket; socket.create_connection(("google.com", 80))  # → blocked
```

---

## 12. Screens / UI Pages

1. **Home Page**: intro, call-to-action, featured problems
2. **Login / Register**
3. **Problem List**: table with title, difficulty, acceptance %, and solved status
4. **Problem Page**: split view with the statement on the left and the editor + console on the right
5. **Submission Result**: verdict, test cases passed, runtime, memory
6. **Submissions History**
7. **Leaderboard**
8. **User Profile / Dashboard**
9. **Admin Panel**: manage problems, test cases, and users

---

## 13. Advantages
- No local installation is needed; it works on any device with a browser.
- Instant automated feedback speeds up learning.
- Saves teachers the time spent checking assignments.
- Secure execution protects the server.
- Scales horizontally by adding more judge workers.

## 14. Limitations
- Supports only Python in the initial version.
- Docker-based sandboxing needs a Linux server with enough resources.
- Very high traffic requires queue scaling and load balancing.

## 15. Future Enhancements
- 🌐 Multi-language support (C, C++, Java, JavaScript)
- 🏆 Timed coding contests with live rankings
- 🤖 AI-powered hints and code explanations
- 🕵️ Plagiarism detection between submissions
- 💬 Discussion forum per problem
- 👥 Classroom mode for teachers (assignments, deadlines, grading)
- 📱 Mobile-responsive / PWA version
- 🔴 Real-time results using WebSockets

---

## 16. Conclusion
The Python-Based Online Coding Platform gives learners a practical, accessible way to practice programming from any browser. It combines a modern code editor, secure sandboxed execution, and automated judging. This makes coding practice faster for students and evaluation easier for teachers. The modular architecture makes it easy to add more languages, contests, and AI features later.

---

## 17. References
- Python Documentation: https://docs.python.org/3/
- Flask Documentation: https://flask.palletsprojects.com/
- Docker Documentation: https://docs.docker.com/
- Monaco Editor: https://microsoft.github.io/monaco-editor/
- Celery Documentation: https://docs.celeryq.dev/
- Judge0 (open-source code execution system, for reference): https://judge0.com/
