"""Run untrusted Python code with time, memory, and output limits.

Two backends:
  * "subprocess" - a separate `python -I` process in a throwaway directory with
    a stripped environment. On Linux/macOS it also gets rlimits (memory, CPU,
    file size). Fine for local development; NOT a security boundary for a
    public deployment.
  * "docker" - each run gets a fresh container with no network, a read-only
    filesystem, a non-root user, and memory, CPU, and PID caps.
"""
import os
import subprocess
import sys
import tempfile
import time
import uuid
from dataclasses import dataclass

IS_POSIX = os.name == "posix"
DOCKER_STARTUP_GRACE = 3  # seconds added for container start-up


@dataclass
class RunResult:
    status: str        # OK | TLE | MLE | RE
    stdout: str
    stderr: str
    exit_code: int | None
    time_ms: int


def run_code(code, stdin_data="", time_limit=2, memory_limit=128,
             backend="subprocess", docker_image="python:3.11-slim", max_output=64 * 1024):
    with tempfile.TemporaryDirectory(prefix="judge_", ignore_cleanup_errors=True) as tmp:
        _write(os.path.join(tmp, "main.py"), code)
        in_path = os.path.join(tmp, "input.txt")
        out_path = os.path.join(tmp, "stdout.txt")
        err_path = os.path.join(tmp, "stderr.txt")
        _write(in_path, stdin_data or "")

        container = None
        if backend == "docker":
            container = f"judge_{uuid.uuid4().hex[:12]}"
            cmd = _docker_cmd(tmp, container, docker_image, memory_limit)
            wait_limit = time_limit + DOCKER_STARTUP_GRACE
            popen_kwargs = {}
        else:
            cmd = [sys.executable, "-I", "main.py"]
            wait_limit = time_limit
            popen_kwargs = {"env": _clean_env()}
            if IS_POSIX:
                popen_kwargs["preexec_fn"] = _posix_limits(time_limit, memory_limit)
                popen_kwargs["start_new_session"] = True

        timed_out = False
        with open(in_path, "rb") as fin, open(out_path, "wb") as fout, open(err_path, "wb") as ferr:
            start = time.perf_counter()
            proc = subprocess.Popen(cmd, cwd=tmp, stdin=fin, stdout=fout, stderr=ferr, **popen_kwargs)
            try:
                exit_code = proc.wait(timeout=wait_limit)
            except subprocess.TimeoutExpired:
                timed_out = True
                if container:
                    subprocess.run(["docker", "kill", container], capture_output=True)
                proc.kill()
                exit_code = proc.wait()
            elapsed_ms = int((time.perf_counter() - start) * 1000)

        stdout = _read(out_path, max_output)
        stderr = _read(err_path, max_output).replace(tmp + os.sep, "").replace(tmp, "")

    status = _classify(timed_out, exit_code, stderr, backend)
    return RunResult(status, stdout, stderr, None if timed_out else exit_code, elapsed_ms)


def _classify(timed_out, exit_code, stderr, backend):
    if timed_out:
        return "TLE"
    if "MemoryError" in stderr or (backend == "docker" and exit_code == 137):
        return "MLE"
    if IS_POSIX and exit_code in (-24, 152):  # SIGXCPU: CPU rlimit hit
        return "TLE"
    if exit_code != 0:
        return "RE"
    return "OK"


def _docker_cmd(tmp, name, image, memory_limit):
    return [
        "docker", "run", "--rm", "-i", "--name", name,
        "--network=none",
        f"--memory={memory_limit}m", f"--memory-swap={memory_limit}m",
        "--cpus=0.5", "--pids-limit=64",
        "--read-only", "--tmpfs", "/tmp:size=16m",
        "--cap-drop=ALL", "--security-opt=no-new-privileges",
        "--user", "65534:65534",
        "-v", f"{tmp}:/code:ro", "-w", "/code",
        image, "python3", "-I", "main.py",
    ]


def _clean_env():
    env = {"PYTHONIOENCODING": "utf-8", "PYTHONDONTWRITEBYTECODE": "1"}
    for key in ("PATH", "SYSTEMROOT", "TEMP", "TMP"):  # Python on Windows needs SYSTEMROOT
        if key in os.environ:
            env[key] = os.environ[key]
    return env


def _posix_limits(time_limit, memory_limit):
    def apply():
        import resource
        mem = memory_limit * 1024 * 1024
        resource.setrlimit(resource.RLIMIT_AS, (mem, mem))
        resource.setrlimit(resource.RLIMIT_CPU, (time_limit + 1, time_limit + 1))
        resource.setrlimit(resource.RLIMIT_FSIZE, (32 * 1024 * 1024,) * 2)
        resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
    return apply


def _write(path, text):
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)


def _read(path, limit):
    with open(path, "rb") as f:
        data = f.read(limit + 1)
    text = data[:limit].decode("utf-8", errors="replace")
    if len(data) > limit:
        text += "\n…[output truncated]"
    return text
