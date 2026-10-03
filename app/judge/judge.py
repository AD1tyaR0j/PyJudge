import traceback

from .sandbox import run_code

JUDGE_OUTPUT_LIMIT = 16 * 1024 * 1024  # large tests legitimately print megabytes


def check_syntax(code):
    """Return a SyntaxError message, or None if the code compiles. Never executes the code."""
    try:
        compile(code, "main.py", "exec")
        return None
    except SyntaxError as e:
        return "".join(traceback.format_exception_only(type(e), e)).strip()
    except Exception as e:  # e.g. ValueError for null bytes, RecursionError for deep nesting
        return f"{type(e).__name__}: {e}"


def normalize(text):
    lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n")
    lines = [line.rstrip() for line in lines]
    while lines and not lines[-1]:
        lines.pop()
    return "\n".join(lines)


def outputs_match(actual, expected):
    return normalize(actual) == normalize(expected)


def judge_code(code, cases, time_limit=2, memory_limit=128, **sandbox_opts):
    """Judge `code` against cases [(input, expected_output, is_sample), ...].

    Stops at the first failing case, like most online judges.
    """
    sandbox_opts.setdefault("max_output", JUDGE_OUTPUT_LIMIT)
    total = len(cases)
    result = {"verdict": "AC", "passed": 0, "total": total, "runtime_ms": 0, "error": None}

    if total == 0:
        result.update(verdict="IE", error="This problem has no test cases yet.")
        return result

    syntax_error = check_syntax(code)
    if syntax_error:
        result.update(verdict="CE", error=syntax_error)
        return result

    for i, (stdin, expected, is_sample) in enumerate(cases, start=1):
        run = run_code(code, stdin, time_limit, memory_limit, **sandbox_opts)
        result["runtime_ms"] = max(result["runtime_ms"], run.time_ms)
        label = f"sample test case {i}" if is_sample else f"hidden test case {i}"

        if run.status != "OK":
            detail = {
                "TLE": f"Time limit of {time_limit}s exceeded on {label}.",
                "MLE": f"Memory limit of {memory_limit} MB exceeded on {label}.",
                "RE": f"Runtime error on {label}:\n{run.stderr.strip()[-2000:]}",
            }[run.status]
            result.update(verdict=run.status, error=detail)
            return result

        if not outputs_match(run.stdout, expected):
            error = f"Wrong answer on {label}."
            if is_sample:
                error += (f"\n\nInput:\n{stdin.strip()}\n\nExpected:\n{normalize(expected)}"
                          f"\n\nYour output:\n{normalize(run.stdout)[:2000]}")
            result.update(verdict="WA", error=error)
            return result

        result["passed"] += 1

    return result
