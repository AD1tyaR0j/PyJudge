from app.judge.judge import check_syntax, judge_code, outputs_match
from app.judge.sandbox import run_code

SUM = "a, b = map(int, input().split())\nprint(a + b)"
CASES = [("2 3", "5", True), ("10 -4", "6", False)]


def test_outputs_match_ignores_trailing_whitespace():
    assert outputs_match("5  \r\n\n", "5")
    assert outputs_match("1\n2\n", "1\n2")
    assert not outputs_match("1 2", "1  2")


def test_check_syntax():
    assert check_syntax("print(1)") is None
    assert "SyntaxError" in check_syntax("print(")


def test_accepted():
    r = judge_code(SUM, CASES)
    assert r["verdict"] == "AC" and r["passed"] == 2


def test_wrong_answer_shows_details_only_for_samples():
    r = judge_code("print(5)", CASES)
    assert r["verdict"] == "WA" and r["passed"] == 1
    assert "hidden test case 2" in r["error"] and "Expected" not in r["error"]

    r = judge_code("print(0)", CASES)
    assert "Expected" in r["error"]


def test_syntax_error():
    assert judge_code("def f(:", CASES)["verdict"] == "CE"


def test_runtime_error():
    r = judge_code("1/0", CASES)
    assert r["verdict"] == "RE" and "ZeroDivisionError" in r["error"]


def test_no_test_cases():
    assert judge_code(SUM, [])["verdict"] == "IE"


# ---------- malicious inputs from the spec ----------

def test_infinite_loop_is_tle():
    assert run_code("while True: pass", time_limit=1).status == "TLE"


def test_huge_allocation_is_mle():
    assert run_code("a = [0] * (10**10)").status == "MLE"


def test_runs_in_throwaway_directory(tmp_path):
    out = run_code("import os; print(os.getcwd())").stdout.strip()
    assert "judge_" in out


def test_environment_is_stripped(monkeypatch):
    monkeypatch.setenv("SECRET_KEY", "super-secret")
    out = run_code("import os; print(os.environ.get('SECRET_KEY'))").stdout.strip()
    assert out == "None"


def test_output_is_truncated():
    r = run_code("print('x' * 10**6)", max_output=1000)
    assert r.stdout.endswith("[output truncated]") and len(r.stdout) < 1100


def test_stdin_is_passed():
    assert run_code(SUM, "40 2").stdout.strip() == "42"


def test_judging_accepts_large_correct_output():
    # Run output is capped at 64 KB for display, but judging must compare the whole answer
    n = 50_000
    expected = "\n".join(map(str, range(n)))  # ~290 KB
    r = judge_code(f"print('\\n'.join(map(str, range({n}))))", [("", expected, False)])
    assert r["verdict"] == "AC", r["error"]


def test_unicode_input_and_output():
    # Windows defaults to cp1252 for pipes; the sandbox must use UTF-8 both ways
    r = run_code("s = input()\nprint(s[::-1], '✔ ♪ café')", "héllo 世界")
    assert r.status == "OK", r.stderr
    assert r.stdout.strip() == "界世 olléh ✔ ♪ café"
