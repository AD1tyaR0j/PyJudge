import os
import secrets


class Config:
    # Set SECRET_KEY in .env; the random fallback logs everyone out on restart.
    SECRET_KEY = os.environ.get("SECRET_KEY") or secrets.token_hex(32)

    # Defaults to instance/platform.db (resolved in create_app).
    SQLALCHEMY_DATABASE_URI = os.environ.get("DATABASE_URL")

    # "subprocess" runs code in a restricted local Python process (development).
    # "docker" runs each execution in a throwaway container (production).
    SANDBOX_BACKEND = os.environ.get("SANDBOX_BACKEND", "subprocess")
    DOCKER_IMAGE = os.environ.get("DOCKER_IMAGE", "python:3.11-slim")

    DEFAULT_TIME_LIMIT = 2       # seconds
    DEFAULT_MEMORY_LIMIT = 128   # MB
    MAX_CODE_LENGTH = 64 * 1024  # characters
    MAX_INPUT_LENGTH = 64 * 1024
    MAX_OUTPUT_BYTES = 64 * 1024               # output shown to the user after Run
    JUDGE_MAX_OUTPUT_BYTES = 16 * 1024 * 1024  # output compared when judging (big tests need more)

    JUDGE_WORKERS = int(os.environ.get("JUDGE_WORKERS", "4"))
    JUDGE_SYNC = False  # tests judge inline instead of in the thread pool
