# Optional minimal image for the docker sandbox backend.
# Build: docker build -f docker/sandbox.Dockerfile -t pyjudge-sandbox .
# Then set DOCKER_IMAGE=pyjudge-sandbox in .env
FROM python:3.11-slim
RUN useradd --uid 65534 --no-create-home --shell /usr/sbin/nologin sandbox || true
USER 65534
WORKDIR /code
