from functools import wraps

from flask import abort
from flask_login import current_user, login_required


def role_required(*roles):
    """Allow only users whose role is in `roles` (admins are always allowed)."""
    def decorator(view):
        @wraps(view)
        @login_required
        def wrapper(*args, **kwargs):
            if current_user.role not in roles and not current_user.is_admin:
                abort(403)
            return view(*args, **kwargs)
        return wrapper
    return decorator


admin_required = role_required("admin")
author_required = role_required("instructor", "admin")
