"""Background judging. A thread pool stands in for Celery + Redis so the app
runs with no extra services; swap `dispatch` for a Celery task to scale out."""
from concurrent.futures import ThreadPoolExecutor

from .. import db
from ..models import Submission
from .judge import judge_code

_executor = None


def sandbox_options(app, judging=False):
    return {
        "backend": app.config["SANDBOX_BACKEND"],
        "docker_image": app.config["DOCKER_IMAGE"],
        "max_output": app.config["JUDGE_MAX_OUTPUT_BYTES" if judging else "MAX_OUTPUT_BYTES"],
    }


def dispatch(app, submission_id):
    global _executor
    if app.config["JUDGE_SYNC"]:
        judge_submission(app, submission_id)
        return
    if _executor is None:
        _executor = ThreadPoolExecutor(max_workers=app.config["JUDGE_WORKERS"], thread_name_prefix="judge")
    _executor.submit(judge_submission, app, submission_id)


def judge_submission(app, submission_id):
    with app.app_context():
        try:
            sub = db.session.get(Submission, submission_id)
            if sub is None:
                return
            problem = sub.problem
            cases = [(tc.input, tc.expected_output, tc.is_sample) for tc in problem.test_cases]
            try:
                result = judge_code(sub.code, cases, problem.time_limit, problem.memory_limit,
                                    **sandbox_options(app, judging=True))
            except Exception:
                app.logger.exception("Judging failed for submission %s", submission_id)
                result = {"verdict": "IE", "passed": 0, "total": len(cases), "runtime_ms": None,
                          "error": "The judge hit an internal error. Please try again."}
            sub.verdict = result["verdict"]
            sub.passed = result["passed"]
            sub.total = result["total"]
            sub.runtime_ms = result["runtime_ms"]
            sub.error = result["error"]
            db.session.commit()
        finally:
            db.session.remove()
