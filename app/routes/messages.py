from flask import Blueprint, abort, flash, redirect, render_template, request, url_for
from flask_login import current_user, login_required
from sqlalchemy import or_

from .. import db
from ..models import Message, Submission, User, utcnow
from ..settings import setting

bp = Blueprint("messages", __name__)
MAX_LENGTH = 4000


def can_message(sender, recipient):
    """Students talk to instructors and admins; instructors and admins can message anyone."""
    if recipient is None or recipient.id == sender.id or recipient.is_blocked:
        return False
    if not setting("messaging_enabled") and not sender.is_admin:
        return False
    if sender.is_student:
        return recipient.role in ("instructor", "admin")
    return True


def contacts():
    users = User.query.filter(User.id != current_user.id, User.is_blocked.is_(False)).order_by(User.role, User.name)
    return [u for u in users if can_message(current_user, u)]


@bp.route("/")
@login_required
def inbox():
    msgs = (Message.query.filter(or_(Message.sender_id == current_user.id, Message.recipient_id == current_user.id))
            .order_by(Message.id.desc()).all())
    conversations = {}
    for m in msgs:
        other = m.recipient if m.sender_id == current_user.id else m.sender
        conv = conversations.setdefault(other.id, {"user": other, "last": m, "unread": 0})
        if m.recipient_id == current_user.id and m.read_at is None:
            conv["unread"] += 1
    return render_template("messages/inbox.html", conversations=list(conversations.values()))


@bp.route("/new", methods=["GET", "POST"])
@login_required
def compose():
    if request.method == "POST":
        recipient = User.query.filter_by(username=request.form.get("to", "")).first()
        return _send(recipient)
    submission = None
    if sub_id := request.args.get("submission", type=int):
        submission = db.session.get(Submission, sub_id)
        if submission and not submission.viewable_by(current_user):
            submission = None
    return render_template("messages/compose.html", contacts=contacts(), to=request.args.get("to", ""),
                           submission=submission)


@bp.route("/<username>", methods=["GET", "POST"])
@login_required
def thread(username):
    other = User.query.filter_by(username=username).first_or_404()
    if request.method == "POST":
        return _send(other)

    msgs = (Message.query.filter(or_(
                (Message.sender_id == current_user.id) & (Message.recipient_id == other.id),
                (Message.sender_id == other.id) & (Message.recipient_id == current_user.id)))
            .order_by(Message.id).all())
    if not msgs and not can_message(current_user, other):
        abort(404)
    now = utcnow()
    for m in msgs:
        if m.recipient_id == current_user.id and m.read_at is None:
            m.read_at = now
    db.session.commit()
    return render_template("messages/thread.html", other=other, msgs=msgs,
                           can_reply=can_message(current_user, other))


def _send(recipient):
    body = request.form.get("body", "").strip()
    if not can_message(current_user, recipient):
        flash("You can't message that person." if recipient else "Pick someone to message.", "error")
        return redirect(request.referrer or url_for("messages.inbox"))
    if not body:
        flash("Write a message first.", "error")
        return redirect(request.referrer or url_for("messages.thread", username=recipient.username))
    sub_id = request.form.get("submission_id", type=int)
    submission = db.session.get(Submission, sub_id) if sub_id else None
    if submission and not submission.viewable_by(current_user):
        submission = None
    db.session.add(Message(sender_id=current_user.id, recipient_id=recipient.id, body=body[:MAX_LENGTH],
                           submission_id=submission.id if submission else None))
    db.session.commit()
    flash(f"Message sent to {recipient.display_name}.", "success")
    return redirect(url_for("messages.thread", username=recipient.username))
