# PyJudge: Python Online Coding Platform

An online platform for learning Python through interactive coding exercises and challenges. The full design document is in [docs/DESIGN.md](docs/DESIGN.md). It has three roles:

| Role | What they do |
|---|---|
| **Admin** | Manages user accounts and roles, approves or rejects exercises, and controls system settings |
| **Instructor** | Creates exercises and challenges, grades student submissions, messages students, and views performance reports |
| **Student** | Solves exercises in the browser, tracks progress, joins challenges, and messages instructors |

## Quick start

```bash
pip install -r requirements.txt
copy .env.example .env      # macOS/Linux: cp .env.example .env, then set SECRET_KEY
flask seed                  # 37 practice exercises + a sample challenge (safe to re-run)
flask run
```

Open http://localhost:5000 and sign up (new accounts are students). Then give yourself admin access:

```bash
flask set-role <your-username> admin
```

As an admin, create instructor accounts under **Admin → Users** (or run `flask set-role <username> instructor`).

Run the tests with `python -m pytest`.

## Features by role

### Admin (`/admin`)
| Spec item | Where |
|---|---|
| User management: create, update, delete; name, email, role | **Users**: table with search/role filter, a create form, edit page, block/unblock |
| Exercise content management: approved and pending tables | **Content**: every exercise, filterable by status |
| Content approval: approve/reject instructor exercises | **Approvals**: preview, statement, test count; approve or reject with a note (the instructor gets a message) |
| System settings | **Settings**: site name, announcement banner, open registration, require approval, messaging, leaderboard, judge limits |
| User activity overview (graphical) | Dashboard charts: submissions per day (accepted vs not) and active students / new sign-ups per day |

### Instructor (`/instructor`)
| Spec item | Where |
|---|---|
| Create and manage exercises | **Exercises** + the exercise studio: statement (Markdown), limits, tags, starter code, sample and hidden tests. New or edited exercises go to admin review. |
| Review and grade submissions | **Submissions**: "to grade" queue; each submission has a 0–100 grade and feedback form, with "Save & grade next" |
| Interact with students | **Messages**: threaded conversations; grading automatically messages the student |
| Performance reports | **Reports**: acceptance-rate chart, per-exercise stats (students, solvers, average grade), per-student progress |
| Profile management | **Account settings**: name, email, bio, password |

### Student (`/student`)
| Spec item | Where |
|---|---|
| Complete coding exercises | **Exercises**: VS Code-style editor, Run with custom input, Submit for instant judging |
| Progress tracking (graphical) | Dashboard: solved-over-time chart, progress by difficulty, points, rank, day streak, average grade |
| Interact with instructors | **Messages** + "Ask an instructor about this" on any submission; instructor feedback is shown on the dashboard |
| Personal profile | **Account settings** + public profile page |
| Challenge participation | Dashboard **Active / Completed** challenges; challenge pages show progress and standings |

## Rules worth knowing

- **Approval flow:** instructor exercises start as *pending* and are hidden from students. Admins approve or reject them, and a reason is required to reject. Editing an approved exercise sends it back for review. Admin-created exercises publish immediately. Turning off *Require exercise approval* in Settings publishes instructor exercises immediately.
- **Messaging:** students can message instructors and admins. Instructors and admins can message anyone.
- **Challenges:** only accepted solutions submitted *during the challenge window* count. A challenge is completed when every exercise in it has been solved.
- **Instructors** can only edit, grade, and see reports for their own exercises. **Admins** can do all of these for every exercise.

## Code execution and security

| Backend | Set with | Use for |
|---|---|---|
| `subprocess` (default) | `SANDBOX_BACKEND=subprocess` | Local development. Separate `python -I` process, throwaway directory, stripped environment, time limit, output cap. On Linux/macOS it also gets memory/CPU rlimits. |
| `docker` | `SANDBOX_BACKEND=docker` | **Any public deployment.** A new container per run: no network, read-only FS, non-root, memory/CPU/PID caps. |

> ⚠️ The `subprocess` backend is **not** a security boundary. Switch to Docker before letting untrusted users in.

## Project layout

```
app/
  models.py        User (roles), Problem (approval status), TestCase, Submission (grade/feedback),
                   Message, Challenge, Setting
  settings.py      system settings schema, defaults, validation
  stats.py         dashboard and chart aggregations
  routes/
    auth.py        sign-up / login        account.py   profile + password, /dashboard router
    admin.py       users, content, approvals, settings, activity
    instructor.py  exercises, grading, reports
    studio.py      exercise editor (instructors + admins)
    student.py     student dashboard      challenges.py  challenges
    messages.py    messaging              main.py / api.py  public pages + JSON API
  judge/           sandbox, verdicts, background workers
  templates/       Jinja2 pages, grouped by role
  static/          CSS (style + motion), editor.js, ui.js, charts.js
tests/             pytest: judge, API, and every role workflow
```

## Upgrading an existing database

This version adds tables and columns. There are no migrations yet, so delete `instance/platform.db` and run `flask seed` again. That wipes all accounts and submissions.
