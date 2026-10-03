from flask import Blueprint, abort, flash, redirect, render_template, request, url_for
from flask_login import current_user
from sqlalchemy import func, or_

from .. import db
from ..models import ROLES, STATUSES, Challenge, Message, Problem, Submission, TestCase, User, utcnow
from ..settings import SCHEMA, get_settings, update_settings
from ..stats import activity_overview
from .auth import validate_account
from .decorators import admin_required

bp = Blueprint("admin", __name__)


# ---------------------------------------------------------------- dashboard

@bp.route("/")
@admin_required
def dashboard():
    judged = Submission.query.filter(Submission.verdict != "PENDING").count()
    accepted = Submission.query.filter_by(verdict="AC").count()
    roles = dict(db.session.query(User.role, func.count()).group_by(User.role).all())
    stats = {
        "students": roles.get("student", 0),
        "instructors": roles.get("instructor", 0),
        "approved": Problem.query.filter_by(status="approved").count(),
        "pending": Problem.query.filter_by(status="pending").count(),
        "submissions": Submission.query.count(),
        "acceptance": round(100 * accepted / judged) if judged else 0,
    }
    pending = Problem.query.filter_by(status="pending").order_by(Problem.updated_at).limit(5).all()
    recent = Submission.query.order_by(Submission.id.desc()).limit(10).all()
    return render_template("admin/dashboard.html", stats=stats, activity=activity_overview(14),
                           pending=pending, recent=recent)


# ---------------------------------------------------------------- users

@bp.route("/users", methods=["GET", "POST"])
@admin_required
def users():
    form = {"username": "", "name": "", "email": "", "role": "student"}
    if request.method == "POST":
        form = {k: request.form.get(k, "").strip() for k in form}
        form["email"] = form["email"].lower()
        password = request.form.get("password", "")
        errors = validate_account(form["username"], form["name"], form["email"], password)
        if form["role"] not in ROLES:
            errors.append("Pick a valid role.")
        if errors:
            for e in errors:
                flash(e, "error")
        else:
            user = User(username=form["username"], name=form["name"], email=form["email"], role=form["role"])
            user.set_password(password)
            db.session.add(user)
            db.session.commit()
            flash(f"Created {user.role} account “{user.username}”.", "success")
            return redirect(url_for("admin.users"))

    role = request.args.get("role", "")
    q = request.args.get("q", "").strip()
    query = User.query
    if role in ROLES:
        query = query.filter_by(role=role)
    if q:
        query = query.filter(or_(User.username.ilike(f"%{q}%"), User.name.ilike(f"%{q}%"), User.email.ilike(f"%{q}%")))
    counts = dict(db.session.query(Submission.user_id, func.count()).group_by(Submission.user_id).all())
    return render_template("admin/users.html", users=query.order_by(User.id).all(), counts=counts,
                           form=form, roles=ROLES, role=role, q=q)


@bp.route("/users/<int:user_id>/edit", methods=["GET", "POST"])
@admin_required
def edit_user(user_id):
    user = db.get_or_404(User, user_id)
    if request.method == "POST":
        name = request.form.get("name", "").strip()
        email = request.form.get("email", "").strip().lower()
        role = request.form.get("role", user.role)
        password = request.form.get("password", "")
        errors = validate_account(None, name, email, password or None, exclude_id=user.id)
        if role not in ROLES:
            errors.append("Pick a valid role.")
        if user.id == current_user.id and (role != "admin" or request.form.get("is_blocked")):
            errors.append("You can't remove your own admin access or block yourself.")
        if errors:
            for e in errors:
                flash(e, "error")
        else:
            user.name, user.email, user.role = name, email, role
            user.is_blocked = bool(request.form.get("is_blocked"))
            if password:
                user.set_password(password)
            db.session.commit()
            flash(f"Updated “{user.username}”.", "success")
            return redirect(url_for("admin.users"))
    return render_template("admin/user_form.html", user=user, roles=ROLES)


@bp.post("/users/<int:user_id>/delete")
@admin_required
def delete_user(user_id):
    user = db.get_or_404(User, user_id)
    if user.id == current_user.id:
        flash("You can't delete your own account.", "error")
        return redirect(url_for("admin.users"))
    # Detach or remove rows that point at this user, then delete them.
    Message.query.filter(or_(Message.sender_id == user.id, Message.recipient_id == user.id)).delete()
    Problem.query.filter_by(created_by=user.id).update({"created_by": None})
    Problem.query.filter_by(reviewed_by=user.id).update({"reviewed_by": None})
    Submission.query.filter_by(graded_by=user.id).update({"graded_by": None})
    Challenge.query.filter_by(created_by=user.id).update({"created_by": None})
    db.session.delete(user)
    db.session.commit()
    flash(f"Deleted “{user.username}” and their submissions.", "info")
    return redirect(url_for("admin.users"))


# ---------------------------------------------------------------- exercise content

@bp.route("/content")
@admin_required
def content():
    status = request.args.get("status", "")
    query = Problem.query
    if status in STATUSES:
        query = query.filter_by(status=status)
    counts = dict(db.session.query(Problem.status, func.count()).group_by(Problem.status).all())
    tests = dict(db.session.query(TestCase.problem_id, func.count()).group_by(TestCase.problem_id).all())
    return render_template("admin/content.html", problems=query.order_by(Problem.id).all(),
                           counts=counts, tests=tests, status=status, statuses=STATUSES)


@bp.route("/approvals")
@admin_required
def approvals():
    pending = Problem.query.filter_by(status="pending").order_by(Problem.updated_at).all()
    return render_template("admin/approvals.html", pending=pending)


@bp.post("/exercises/<int:problem_id>/review")
@admin_required
def review(problem_id):
    problem = db.get_or_404(Problem, problem_id)
    decision = request.form.get("decision")
    note = request.form.get("note", "").strip()
    if decision not in ("approve", "reject"):
        abort(400)
    if decision == "reject" and not note:
        flash("Add a note so the instructor knows what to change.", "error")
        return redirect(request.referrer or url_for("admin.approvals"))
    if decision == "approve" and not problem.test_cases:
        flash("This exercise has no test cases yet, so it can't be judged. Ask the instructor to add some.", "error")
        return redirect(request.referrer or url_for("admin.approvals"))

    problem.status = "approved" if decision == "approve" else "rejected"
    problem.review_note = note or None
    problem.reviewed_by = current_user.id
    problem.reviewed_at = utcnow()

    if problem.created_by and problem.created_by != current_user.id:
        verb = "approved and published" if decision == "approve" else "returned for changes"
        body = f"Your exercise “{problem.title}” was {verb}."
        if note:
            body += f"\n\nReviewer note: {note}"
        db.session.add(Message(sender_id=current_user.id, recipient_id=problem.created_by, body=body))
    db.session.commit()
    flash(f"“{problem.title}” {'approved' if decision == 'approve' else 'rejected'}.", "success")
    return redirect(request.referrer or url_for("admin.approvals"))


# ---------------------------------------------------------------- settings

@bp.route("/settings", methods=["GET", "POST"])
@admin_required
def settings():
    if request.method == "POST":
        errors = update_settings(request.form)
        if errors:
            for e in errors:
                flash(e, "error")
        else:
            flash("Settings saved.", "success")
            return redirect(url_for("admin.settings"))
    return render_template("admin/settings.html", schema=SCHEMA, values=get_settings())
