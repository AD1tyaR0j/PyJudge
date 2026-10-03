"""System-wide settings, stored as key/value rows and edited by admins."""
from flask import g

from . import db
from .models import Setting

# key: (type, default, label, help text, (min, max) for ints)
SCHEMA = {
    "site_name": (str, "PyJudge", "Site name", "Shown in the header and page titles.", None),
    "announcement": (str, "", "Announcement banner", "Shown at the top of every page. Leave empty to hide.", None),
    "registration_open": (bool, True, "Open registration", "Let new students sign up on their own.", None),
    "require_approval": (bool, True, "Require exercise approval",
                         "Instructor exercises must be approved by an admin before students can see them.", None),
    "messaging_enabled": (bool, True, "Enable messaging", "Let students and instructors message each other.", None),
    "leaderboard_enabled": (bool, True, "Show leaderboard", "Show the public leaderboard page.", None),
    "default_time_limit": (int, 2, "Default time limit (s)", "Used for new exercises and the Run button.", (1, 10)),
    "default_memory_limit": (int, 128, "Default memory limit (MB)", "Used for new exercises and the Run button.", (16, 1024)),
    "max_pending_submissions": (int, 3, "Max submissions in the queue per student",
                                "Stops one student from flooding the judge.", (1, 20)),
}


def _decode(kind, raw):
    if kind is bool:
        return raw == "1"
    if kind is int:
        return int(raw)
    return raw


def get_settings():
    """All settings as a typed dict, cached for the current request."""
    if "settings" not in g:
        stored = {s.key: s.value for s in Setting.query.all()}
        values = {}
        for key, (kind, default, *_rest) in SCHEMA.items():
            try:
                values[key] = _decode(kind, stored[key]) if key in stored else default
            except (ValueError, TypeError):
                values[key] = default
        g.settings = values
    return g.settings


def setting(key):
    return get_settings()[key]


def update_settings(form):
    """Validate and save settings from a submitted form. Returns a list of errors."""
    errors, new_values = [], {}
    for key, (kind, _default, label, _help, bounds) in SCHEMA.items():
        if kind is bool:
            new_values[key] = "1" if form.get(key) else "0"
        elif kind is int:
            try:
                value = int(form.get(key, ""))
            except ValueError:
                errors.append(f"{label} must be a whole number.")
                continue
            lo, hi = bounds
            if not lo <= value <= hi:
                errors.append(f"{label} must be between {lo} and {hi}.")
                continue
            new_values[key] = str(value)
        else:
            value = form.get(key, "").strip()
            if key == "site_name" and not value:
                errors.append("Site name can't be empty.")
                continue
            new_values[key] = value[:500]

    if errors:
        return errors
    for key, value in new_values.items():
        row = db.session.get(Setting, key)
        if row:
            row.value = value
        else:
            db.session.add(Setting(key=key, value=value))
    db.session.commit()
    g.pop("settings", None)
    return []
