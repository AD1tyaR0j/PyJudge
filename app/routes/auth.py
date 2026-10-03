import re

from flask import Blueprint, flash, redirect, render_template, request, url_for
from flask_login import current_user, login_required, login_user, logout_user

from .. import db
from ..models import User
from ..settings import setting

bp = Blueprint("auth", __name__)

USERNAME_RE = re.compile(r"^[A-Za-z0-9_]{3,30}$")
EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")
MIN_PASSWORD = 8


def validate_account(username, name, email, password=None, confirm=None, exclude_id=None):
    """Shared by sign-up, profile editing, and admin user management."""
    errors = []
    if username is not None and not USERNAME_RE.match(username):
        errors.append("Username must be 3–30 characters: letters, numbers, or underscores.")
    if not name or len(name) > 100:
        errors.append("Enter a name (up to 100 characters).")
    if not EMAIL_RE.match(email):
        errors.append("Enter a valid email address.")
    if password is not None:
        if len(password) < MIN_PASSWORD:
            errors.append(f"Password must be at least {MIN_PASSWORD} characters.")
        if confirm is not None and password != confirm:
            errors.append("Passwords do not match.")
    if username is not None:
        taken = User.query.filter_by(username=username).first()
        if taken and taken.id != exclude_id:
            errors.append("That username is taken.")
    taken = User.query.filter_by(email=email).first()
    if taken and taken.id != exclude_id:
        errors.append("An account with that email already exists.")
    return errors


def _safe_next(target):
    if target and target.startswith("/") and not target.startswith("//"):
        return target
    return url_for("account.dashboard")


@bp.route("/register", methods=["GET", "POST"])
def register():
    if current_user.is_authenticated:
        return redirect(url_for("account.dashboard"))
    if not setting("registration_open"):
        flash("Sign-ups are closed right now. Ask an administrator for an account.", "info")
        return redirect(url_for("auth.login"))

    form = {"username": "", "name": "", "email": ""}
    if request.method == "POST":
        form = {k: request.form.get(k, "").strip() for k in form}
        form["email"] = form["email"].lower()
        password = request.form.get("password", "")
        errors = validate_account(form["username"], form["name"], form["email"],
                                  password, request.form.get("confirm", ""))
        if errors:
            for e in errors:
                flash(e, "error")
        else:
            user = User(username=form["username"], name=form["name"], email=form["email"], role="student")
            user.set_password(password)
            db.session.add(user)
            db.session.commit()
            login_user(user)
            flash(f"Welcome, {user.display_name}! Pick an exercise to get started.", "success")
            return redirect(url_for("account.dashboard"))

    return render_template("register.html", form=form)


@bp.route("/login", methods=["GET", "POST"])
def login():
    if current_user.is_authenticated:
        return redirect(url_for("account.dashboard"))

    identifier = ""
    if request.method == "POST":
        identifier = request.form.get("identifier", "").strip()
        password = request.form.get("password", "")
        user = User.query.filter(
            (User.username == identifier) | (User.email == identifier.lower())
        ).first()
        if user and user.check_password(password):
            if user.is_blocked:
                flash("This account has been blocked. Contact an administrator.", "error")
            else:
                login_user(user, remember=bool(request.form.get("remember")))
                return redirect(_safe_next(request.args.get("next")))
        else:
            flash("Invalid username/email or password.", "error")

    return render_template("login.html", identifier=identifier)


@bp.route("/logout", methods=["POST"])
@login_required
def logout():
    logout_user()
    flash("You have been logged out.", "info")
    return redirect(url_for("main.index"))
