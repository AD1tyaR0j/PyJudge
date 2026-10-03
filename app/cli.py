import click

from . import db
from .models import ROLES, User


def register_cli(app):
    @app.cli.command("seed")
    def seed():
        """Add exercises, the Python course, and a sample challenge (safe to re-run)."""
        from . import models
        from .curriculum import sync_curriculum
        from .seed import seed_problems
        click.echo(f"Added {seed_problems()} exercise(s).")
        topics, created, updated = sync_curriculum(db, models)
        click.echo(f"Course: {topics} new topic(s), {created} new lesson(s), {updated} lesson(s) refreshed.")

    @app.cli.command("set-role")
    @click.argument("username")
    @click.argument("role", type=click.Choice(ROLES))
    def set_role(username, role):
        """Change a user's role: student, instructor, or admin."""
        user = User.query.filter_by(username=username).first()
        if not user:
            raise click.ClickException(f"No user named {username!r}. Register on the site first.")
        user.role = role
        db.session.commit()
        click.echo(f"{username} is now a {role}.")

    @app.cli.command("make-admin")
    @click.argument("username")
    @click.pass_context
    def make_admin(ctx, username):
        """Shortcut for: flask set-role USERNAME admin"""
        ctx.invoke(set_role, username=username, role="admin")
