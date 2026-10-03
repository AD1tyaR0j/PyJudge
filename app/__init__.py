import os
import secrets

import markdown as md
from flask import Flask, abort, jsonify, redirect, request, session, url_for
from flask_login import LoginManager
from flask_sqlalchemy import SQLAlchemy
from markupsafe import Markup

from .config import Config

db = SQLAlchemy()
login_manager = LoginManager()

VERDICT_LABELS = {
    "AC": "Accepted",
    "WA": "Wrong Answer",
    "TLE": "Time Limit Exceeded",
    "MLE": "Memory Limit Exceeded",
    "RE": "Runtime Error",
    "CE": "Syntax Error",
    "IE": "Internal Error",
    "PENDING": "Judging…",
}


def create_app(overrides=None):
    app = Flask(__name__, instance_relative_config=True)
    app.config.from_object(Config)
    if overrides:
        app.config.update(overrides)

    os.makedirs(app.instance_path, exist_ok=True)
    if not app.config.get("SQLALCHEMY_DATABASE_URI"):
        db_path = os.path.join(app.instance_path, "platform.db")
        app.config["SQLALCHEMY_DATABASE_URI"] = f"sqlite:///{db_path}"

    db.init_app(app)
    login_manager.init_app(app)
    login_manager.login_view = "auth.login"
    login_manager.login_message_category = "info"

    from .routes.account import bp as account_bp
    from .routes.admin import bp as admin_bp
    from .routes.api import bp as api_bp
    from .routes.auth import bp as auth_bp
    from .routes.challenges import bp as challenges_bp
    from .routes.instructor import bp as instructor_bp
    from .routes.main import bp as main_bp
    from .routes.messages import bp as messages_bp
    from .routes.student import bp as student_bp
    from .routes.studio import bp as studio_bp

    app.register_blueprint(main_bp)
    app.register_blueprint(auth_bp)
    app.register_blueprint(account_bp)
    app.register_blueprint(api_bp, url_prefix="/api")
    app.register_blueprint(admin_bp, url_prefix="/admin")
    app.register_blueprint(instructor_bp, url_prefix="/instructor")
    app.register_blueprint(student_bp)
    app.register_blueprint(studio_bp, url_prefix="/studio")
    app.register_blueprint(messages_bp, url_prefix="/messages")
    app.register_blueprint(challenges_bp, url_prefix="/challenges")

    _register_csrf(app)
    _register_template_helpers(app)
    _register_activity_tracking(app)

    from .cli import register_cli
    register_cli(app)

    with app.app_context():
        from . import models  # noqa: F401  (register tables)
        db.create_all()

    return app


@login_manager.user_loader
def load_user(user_id):
    from .models import User
    return db.session.get(User, int(user_id))


@login_manager.unauthorized_handler
def unauthorized():
    if request.path.startswith("/api/"):
        return jsonify(error="Login required"), 401
    return redirect(url_for("auth.login", next=request.full_path))


def csrf_token():
    if "_csrf" not in session:
        session["_csrf"] = secrets.token_hex(16)
    return session["_csrf"]


def _register_csrf(app):
    @app.before_request
    def csrf_protect():
        if request.method != "POST":
            return
        if request.path.startswith("/api/"):
            if not request.is_json:
                abort(415)
            token = request.headers.get("X-CSRF-Token")
        else:
            token = request.form.get("csrf_token")
        if not token or not secrets.compare_digest(token, session.get("_csrf", "")):
            abort(400, description="Invalid or missing CSRF token. Reload the page and try again.")


def _register_template_helpers(app):
    app.jinja_env.globals["csrf_token"] = csrf_token
    app.jinja_env.globals["VERDICT_LABELS"] = VERDICT_LABELS

    @app.template_filter("markdown")
    def render_markdown(text):
        return Markup(md.markdown(text or "", extensions=["fenced_code", "tables"]))

    @app.template_filter("dt")
    def format_datetime(value):
        return value.strftime("%Y-%m-%d %H:%M") if value else ""

    @app.context_processor
    def inject_globals():
        from flask_login import current_user
        from .models import Message
        from .settings import get_settings

        unread = 0
        if current_user.is_authenticated:
            unread = Message.query.filter_by(recipient_id=current_user.id, read_at=None).count()
        return {"site": get_settings(), "unread_messages": unread}


def _register_activity_tracking(app):
    from datetime import timedelta

    @app.before_request
    def touch_last_seen():
        from flask_login import current_user
        from .models import utcnow

        if not current_user.is_authenticated or request.path.startswith("/static/"):
            return
        now = utcnow()
        if not current_user.last_seen_at or now - current_user.last_seen_at > timedelta(minutes=5):
            current_user.last_seen_at = now
            db.session.commit()
