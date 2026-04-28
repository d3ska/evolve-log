## ADDED Requirements

### Requirement: Pipeline triggers on push to main
The GitHub Actions workflow SHALL trigger automatically on every push to the `main` branch and SHALL also support manual dispatch (`workflow_dispatch`).

#### Scenario: Push to main starts the pipeline
- **WHEN** a commit is pushed to the `main` branch
- **THEN** the GitHub Actions workflow starts within 60 seconds

#### Scenario: Push to non-main branch does not trigger deploy
- **WHEN** a commit is pushed to any branch other than `main`
- **THEN** the deploy workflow does NOT run

---

### Requirement: Backend tests must pass before images are built
The pipeline SHALL run `./gradlew test` in the `test` job. The `build-and-push` job SHALL depend on `test` and SHALL NOT run if tests fail.

#### Scenario: Failing backend test blocks the pipeline
- **WHEN** a backend test fails
- **THEN** the `build-and-push` and `deploy` jobs are skipped and the pipeline is marked failed

#### Scenario: All tests pass and pipeline continues
- **WHEN** all backend tests pass
- **THEN** the `build-and-push` job starts

---

### Requirement: Docker images are pushed to GHCR with dual tags
The pipeline SHALL build the backend and frontend Docker images and push them to `ghcr.io/<owner>/<repo>` with two tags: `latest` and the full git commit SHA.

#### Scenario: Images are tagged with latest and SHA
- **WHEN** the pipeline completes the `build-and-push` job
- **THEN** `ghcr.io/<owner>/evolve-log-backend:latest` and `ghcr.io/<owner>/evolve-log-backend:<sha>` are available in GHCR

---

### Requirement: Docker layer cache is reused between runs
The pipeline SHALL use GitHub Actions cache (`type=gha`) for Docker BuildKit layer caching to reduce rebuild time on subsequent runs.

#### Scenario: Unchanged layers are not rebuilt
- **WHEN** only application source files change between two pipeline runs
- **THEN** the dependency download and base image layers are restored from cache, reducing build time

---

### Requirement: Deployment runs after successful image push via SSH
The `deploy` job SHALL depend on `build-and-push` and SHALL SSH into the Hetzner server using the `SSH_PRIVATE_KEY`, `SERVER_IP`, and `SERVER_USER` GitHub Actions secrets. It SHALL run `docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d` on the server to update the running containers.

#### Scenario: Successful build triggers SSH deploy
- **WHEN** `build-and-push` completes successfully
- **THEN** the deploy job SSHes into the server and the new images are pulled and started

#### Scenario: Missing SSH_PRIVATE_KEY causes deploy to fail with a clear error
- **WHEN** the `SSH_PRIVATE_KEY` secret is not configured in the repository
- **THEN** the deploy job fails with an SSH authentication error, not a silent no-op

#### Scenario: Server is unreachable during deploy
- **WHEN** the SSH connection to SERVER_IP times out
- **THEN** the deploy job fails with a connection error and the pipeline is marked failed

---

### Requirement: Pipeline uses Java 25 and Node 22 toolchains
The `test` job SHALL set up Java 25 (`temurin` distribution) via `actions/setup-java@v4`. The frontend lint/build job SHALL set up Node 22 via `actions/setup-node@v4`. Both SHALL enable their respective dependency caches.

#### Scenario: Java version matches production runtime
- **WHEN** the pipeline runs `./gradlew test`
- **THEN** the JDK version reported by `java -version` is 25
