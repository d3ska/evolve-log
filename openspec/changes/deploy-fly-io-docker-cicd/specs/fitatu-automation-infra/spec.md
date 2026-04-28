## ADDED Requirements

### Requirement: Playwright runner has a dedicated Docker image
A `evolve-log-fitatu-sync/Dockerfile` SHALL exist using `mcr.microsoft.com/playwright` as the base image (which bundles Chromium and all browser dependencies). It SHALL copy the sync script and install Node dependencies via `npm ci`.

#### Scenario: Runner image builds successfully
- **WHEN** `docker build` is run in the `evolve-log-fitatu-sync/` directory
- **THEN** the image is created without error and Playwright's bundled Chromium is available

#### Scenario: Runner container executes and exits
- **WHEN** the container is started with all required env vars
- **THEN** it runs the sync script, exits with code 0 on success, and does not stay running

---

### Requirement: Runner is schedulable as a server cron job
The Hetzner server SHALL have a cron entry for the `deploy` user that runs the sync container daily (default: 06:00 UTC) via:
```
0 6 * * * docker run --rm --network evolve-log-net --env-file /opt/evolve-log/.env.fitatu ghcr.io/<owner>/evolve-log-fitatu-sync:latest
```
No separate scheduling infrastructure is required. The container runs, exits when the script completes, and Docker removes it automatically (`--rm`).

#### Scenario: Cron triggers the runner daily
- **WHEN** the configured cron schedule fires (e.g., 06:00 UTC)
- **THEN** Docker starts the container, runs the sync script, and the container exits cleanly (code 0 on success)

#### Scenario: Runner reaches the backend via Docker network
- **WHEN** the runner container is attached to the same Docker network as the backend
- **THEN** it connects to `http://backend:8080` without going through the public internet

---

### Requirement: Fitatu credentials are stored as Fly.io secrets
`FITATU_EMAIL` and `FITATU_PASSWORD` SHALL be stored as Fly.io secrets on the `evolve-log-fitatu-sync` app. They SHALL NOT appear in source control, Dockerfiles, or fly.toml.

#### Scenario: Missing Fitatu credentials cause the script to fail fast
- **WHEN** the runner starts without `FITATU_EMAIL` or `FITATU_PASSWORD` set
- **THEN** the script exits immediately with a clear error before attempting browser login

---

### Requirement: Import API call uses an internal service token
The runner SHALL authenticate its POST to the evolve-log import endpoint using an `X-Internal-Token` HTTP header containing the value of the `EVOLVELOG_IMPORT_TOKEN` secret. It SHALL NOT use browser cookie auth or user-facing OAuth.

#### Scenario: Import call succeeds with valid token
- **WHEN** the runner POSTs a valid CSV to the import endpoint with the correct `X-Internal-Token` header
- **THEN** the endpoint accepts the request and returns HTTP 200

#### Scenario: Import call is rejected with missing or wrong token
- **WHEN** the runner POSTs without the `X-Internal-Token` header or with an incorrect value
- **THEN** the endpoint returns HTTP 401 or HTTP 403

---

### Requirement: Automation infra is documented in .env.example
`.env.example` SHALL include the four automation-related variables with comments explaining their purpose:
- `FITATU_EMAIL`
- `FITATU_PASSWORD`
- `EVOLVELOG_API_URL`
- `EVOLVELOG_IMPORT_TOKEN`

#### Scenario: Developer can identify all required secrets from .env.example
- **WHEN** a developer reads `.env.example`
- **THEN** they can identify every secret needed for both the main app and the automation runner without reading source code

---

### Requirement: Automation is independently deployable
The `evolve-log-fitatu-sync` app SHALL be deployable independently of the main backend and frontend apps. Its GitHub Actions step SHALL be gated so it only runs if the `evolve-log-fitatu-sync/` directory contains changes, or on manual dispatch.

#### Scenario: Change to sync script triggers only the sync deploy step
- **WHEN** a commit modifies only files under `evolve-log-fitatu-sync/`
- **THEN** the CI pipeline builds and deploys only the sync runner, not the backend or frontend
