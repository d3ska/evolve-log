## Why

evolve-log runs only locally and cannot be accessed from a phone or other devices. Deploying it to a cloud host with HTTPS will enable PWA installation on mobile and access from anywhere, for the developer and ~5 close users, at minimal cost.

## What Changes

- Add `evolve-log/Dockerfile` — multi-stage Gradle build (Java 25) → JRE Alpine runtime
- Add `evolve-log-ui/Dockerfile` — Vite build (Node 22) → nginx Alpine serve
- Add `evolve-log-ui/nginx.conf` — reverse-proxy `/api/*` to backend, SPA fallback, service-worker caching rules
- Modify `evolve-log/src/main/resources/application.properties` — replace all hardcoded values (DB URL, OAuth secrets, AI key, frontend URL) with `${ENV_VAR:default}` placeholders
- Modify `evolve-log/docker-compose.yml` — add `backend` and `frontend` services for full-stack local smoke testing
- Add `.github/workflows/deploy.yml` — CI/CD: test → build Docker images → push GHCR → deploy to Fly.io
- Add `docker-compose.prod.yml` at repo root — production Compose file for Hetzner server (backend, frontend, postgres services with Docker volumes and networking)
- Add `.env.example` — template for local secret values (never committed)
- Add `application-local.properties` to `.gitignore` — prevent accidental secret commit

## Capabilities

### New Capabilities

- `container-build`: Dockerfiles and nginx config that package backend and frontend into production-ready images, with reverse-proxy for the mandatory `/api` routing and correct PWA service-worker caching
- `secrets-externalization`: All runtime secrets (DB credentials, OAuth client IDs/secrets, AI encryption key, frontend URL) sourced from environment variables; application.properties retains safe local defaults only
- `cicd-pipeline`: GitHub Actions workflow that runs tests, builds and pushes Docker images to GHCR (tagged `latest` + git SHA), then deploys to Fly.io on every push to `main`
- `hetzner-deployment`: Hetzner CX22 server setup (Docker + nginx + Certbot), `docker-compose.prod.yml` for all three services, persistent Docker volume for file uploads, SSH-based deploy from GitHub Actions
- `fitatu-automation-infra`: Infrastructure scaffolding to support a future scheduled automation that logs into Fitatu via Playwright (or n8n), exports a nutrition CSV, and POSTs it to the evolve-log import endpoint; includes secret storage, a Dockerfile for the runner, and a Fly.io scheduled Machine definition

### Modified Capabilities

_(none — no existing spec-level requirements change)_

## Impact

- **Backend**: `application.properties` (secrets externalized), new `Dockerfile`
- **Frontend**: new `Dockerfile`, new `nginx.conf`
- **Infrastructure**: `docker-compose.yml` (extended for local smoke test), new `docker-compose.prod.yml` (Hetzner server), `.github/workflows/deploy.yml` (new), `.gitignore` (updated)
- **Automation**: new `evolve-log-fitatu-sync/Dockerfile` (Playwright + Node runner), server cron job definition (no fly.toml needed), Fitatu credentials added to secrets strategy
- **Dependencies**: none added to build files; `eclipse-temurin:25`, `node:22`, `nginx:alpine`, `mcr.microsoft.com/playwright:v1.49.0-noble` Docker base images required at build time
- **Secrets**: 7 values move from `application-local.properties` → `/opt/evolve-log/.env` on server + GitHub Actions secrets (`SSH_PRIVATE_KEY`, `SERVER_IP`); Fitatu credentials (`FITATU_EMAIL`, `FITATU_PASSWORD`) and evolve-log service token (`EVOLVELOG_IMPORT_TOKEN`) stored separately in `/opt/evolve-log/.env.fitatu`
- **Cost**: ~€4/month (Hetzner CX22) flat; automation runner costs 0 extra (server already running)
