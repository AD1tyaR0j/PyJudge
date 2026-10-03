from flask import Blueprint, flash, redirect, render_template, request, url_for
from flask_login import current_user, login_required

from .. import db
from .auth import MIN_PASSWORD, validate_account

bp = Blueprint("account", __name__)


@bp.route("/dashboard")
@login_required
def dashboard():
    """Send each role to its own dashboard."""
    if current_user.is_admin:
        return redirect(url_for("admin.dashboard"))
    if current_user.is_instructor:
        return redirect(url_for("instructor.dashboard"))
    return redirect(url_for("student.dashboard"))


@bp.route("/settings", methods=["GET", "POST"])
@login_required
def settings():
    """Profile management: name, email, bio, and password."""
    if request.method == "POST":
        action = request.form.get("action")
        if action == "profile":
            name = request.form.get("name", "").strip()
            email = request.form.get("email", "").strip().lower()
            bio = request.form.get("bio", "").strip()[:1000]
            errors = validate_account(None, name, email, exclude_id=current_user.id)
            if errors:
                for e in errors:
                    flash(e, "error")
            else:
                current_user.name, current_user.email, current_user.bio = name, email, bio
                db.session.commit()
                flash("Profile updated.", "success")
                return redirect(url_for("account.settings"))

        elif action == "password":
            current = request.form.get("current_password", "")
            new = request.form.get("new_password", "")
            if not current_user.check_password(current):
                flash("Your current password is incorrect.", "error")
            elif len(new) < MIN_PASSWORD:
                flash(f"New password must be at least {MIN_PASSWORD} characters.", "error")
            elif new != request.form.get("confirm_password", ""):
                flash("New passwords do not match.", "error")
            else:
                current_user.set_password(new)
                db.session.commit()
                flash("Password changed.", "success")
                return redirect(url_for("account.settings"))

    return render_template("account/settings.html")
