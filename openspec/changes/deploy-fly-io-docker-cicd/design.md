## Context

evolve-log consists of three components: a Spring Boot 4.0.5 backend (Java 25, Gradle, port 8080), a React 19 + Vite frontend (PWA via vite-plugin-pwa), and PostgreSQL 17. Currently all three run locally only.

The frontend API client uses a **relative `/api` base URL** with `withCredentials: true` (cookie-based auth). This means the frontend and backend must be served from the **same origin** in production — a plain static CDN + separate backend domain will not work. nginx acting as a reverse proxy is the only viable pattern without changing the frontend source code.

PWA service workers require HTTPS, which must be provisioned on the host (Let's Encrypt via Certbot).

All secrets (Google OAuth, Withings, AI encryption key) are currently hardcoded in `application-local.properties`. This file must never reach the container image or source control.

---

## Goals / Non-Goals

**Goals:**
- Accessible from any device via HTTPS (PWA on mobile, web browser on desktop)
- Single-command local full-stack smoke test via `docker compose up`
- Automated deploy on every `git push` to `main` via GitHub Actions
- All secrets injected at runtime via environment variables — zero secrets in source or Docker images
- Cost ≤ $5/month for ~5 users

**Non-Goals:**
- High-availability / multi-region setup
- Zero-downtime blue/green deployments
- Kubernetes or orchestration beyond docker-compose
- CDN for static assets (nginx serving is sufficient for 5 users)
- Object storage migration (local filesystem + persistent volume is sufficient)

---

## Decisions

### 1. Platform: Hetzner CX22 over Fly.io / Render / Railway

| | Hetzner CX22 | Fly.io free | Render free | Railway |
|---|---|---|---|---|
| Price | €4/month | €0 (but sleeps) | €0 (sleeps after 15 min) | $5 credit only |
| RAM | 4 GB | 256 MB | 512 MB | varies |
| Always-on | Yes | No — auto-stops when idle | No | No |
| PostgreSQL | Self-hosted in Docker | Managed free 1 GB | $7/month | Pay-per-use |
| HTTPS | Certbot (Let's Encrypt) | Built-in | Built-in | Built-in |
| Region | nbg1 (Nuremberg, ~500 km from Poland) | fra (Frankfurt) | Frankfurt | US-based |

Key reason for Hetzner: **no auto-sleep**. Fly.io free-tier machines stop when idle — unacceptable for use mid-training where a PWA request must not wait 10–30 s for a cold start. Hetzner CX22 runs continuously for a flat €4/month. With 4 GB RAM and 2 vCPU it comfortably hosts Spring Boot + nginx + PostgreSQL simultaneously.

**Region**: `nbg1` (Nuremberg) — closest Hetzner datacenter to Poland, ~500 km.

### 2. Single server with docker-compose, not multiple cloud apps

All three services run as Docker containers on one Hetzner server, orchestrated by `docker-compose.prod.yml`:
- `postgres` — PostgreSQL 17 container, data on Docker volume
- `backend` — Spring Boot on internal port 8080
- `frontend` — nginx on host port 3000 (HTTP); host nginx terminates HTTPS and proxies to it

nginx in the frontend container proxies `/api/*` to the `backend` container via Docker's internal network (`http://backend:8080`). The host nginx (installed on the Hetzner server) handles TLS termination and forwards HTTPS → `http://127.0.0.1:3000`.

Alternative considered: separate Fly.io apps (backend + frontend). Rejected — free tier auto-stops; requires flyctl CLI complexity; networking via internal `.internal` hostnames; managed Postgres adds vendor lock-in.

Alternative considered: single Docker image with supervisord. Rejected — violates single-process-per-container; harder to update independently.

### 3. Docker base images: eclipse-temurin:25

Java 25 is available as `eclipse-temurin:25-jdk-alpine` (build) and `eclipse-temurin:25-jre-alpine` (runtime). Alpine variants keep the runtime image to ~150MB.

Multi-stage build:
- **Stage 1 (builder)**: `eclipse-temurin:25-jdk-alpine` — runs `./gradlew clean build -x test --no-daemon`, produces fat JAR
- **Stage 2 (runtime)**: `eclipse-temurin:25-jre-alpine` — unprivileged `spring` user, copies JAR only

Frontend multi-stage:
- **Stage 1 (builder)**: `node:22-alpine` — `npm ci --frozen-lockfile && npm run build`
- **Stage 2 (runtime)**: `nginx:alpine` — copies `dist/` and `nginx.conf`

### 4. Secrets via environment variables only

Spring Boot's relaxed binding maps `SPRING_DATASOURCE_URL` → `spring.datasource.url` automatically. All secrets are removed from `application.properties` and replaced with `${VAR:default}` placeholders where the default is safe for local dev (localhost DB URL, empty strings for OAuth keys).

`application-local.properties` is added to `.gitignore`. Developers set secrets locally via that file; CI/CD injects them via Fly.io secrets (`flyctl secrets set`) and the one GitHub Actions secret (`FLY_API_TOKEN`).

**No secrets are baked into Docker images.** The `Dockerfile` does not copy `application-local.properties`.

### 5. Container registry: GHCR (GitHub Container Registry)

Images are pushed to `ghcr.io/<owner>/evolve-log-backend` and `ghcr.io/<owner>/evolve-log-frontend`. GHCR is free for public repos and requires only `GITHUB_TOKEN` (auto-provided by Actions). Fly.io can pull from GHCR without additional credentials.

Alternative considered: Docker Hub. Rejected — rate limits on free tier can block CI.

### 6. nginx caching strategy for PWA correctness

The service worker file (`/service-worker.js`, `/sw.js`) must never be served from cache — a stale service worker prevents app updates. Vite hash-names all JS/CSS assets, so those can be cached for 1 year. `index.html` must always be fresh (no-cache).

```
/service-worker.js  → Cache-Control: no-store
/index.html         → Cache-Control: no-cache, must-revalidate
/assets/*.js|css    → Cache-Control: public, max-age=31536000, immutable
/api/*              → no caching (proxied to backend)
```

### 7. Persistent volume for uploads

File uploads are stored at `/app/uploads` inside the backend container. A named Docker volume (`backend_uploads`) backed by the Hetzner server's 40 GB local disk is mounted there. Without it, files are lost on every `docker compose up`. The volume survives container restarts and redeployments as long as the server exists.

Alternative considered: migrate to S3-compatible object storage (Hetzner Object Storage, ~€0.02/GB/month). Deferred — unnecessary complexity for 5 users with low upload volume.

---

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Single server is a single point of failure | Acceptable for 5 personal users; Hetzner SLA 99.9% (~8.7 h downtime/year) |
| `eclipse-temurin:25-alpine` image availability | Verified in Docker Hub; if unavailable fall back to `eclipse-temurin:25-jdk` (debian, ~250MB larger) |
| Flyway migrations run on every startup | Idempotent by design; V1–V19 already written correctly |
| `application-local.properties` accidentally committed | `.gitignore` entry prevents it; CI build does not copy it |
| Backend hostname `backend` only resolves inside Docker network | nginx `upstream backend { server backend:8080; }` uses Docker Compose service name; works by default |
| PWA update cycle: users may run stale app version | Service worker uses NetworkFirst strategy (already configured in vite-plugin-pwa) |
| Server disk fills up with old Docker images | Run `docker system prune -f` periodically; add as weekly cron on server |
| SSH key compromise gives full server access | Use a dedicated deploy key with no passphrase, stored only in GitHub Actions secret; restrict key to `git pull` / `docker compose` commands if needed |

---

## Migration Plan

1. **Local validation** — `docker compose up --build` with `.env` file; verify `/health` and `/api/actuator/health`
2. **Hetzner server setup** — create CX22 in nbg1, SSH in, install Docker + Compose plugin, install nginx + Certbot, configure firewall (ports 22, 80, 443)
3. **HTTPS setup** — point DNS A record to server IP, run `certbot --nginx -d <domain>`, verify auto-renewal
4. **Docker Compose on server** — create `/opt/evolve-log/docker-compose.prod.yml` and `.env` on server
5. **Set GitHub secrets** — `SSH_PRIVATE_KEY`, `SERVER_IP`, `SERVER_USER` in repository settings
6. **First deploy** — push to `main`; GitHub Actions runs test → build → push GHCR → SSH into server → `docker compose pull && docker compose up -d`
7. **Validate** — HTTPS, OAuth redirect URIs updated in Google Cloud Console and Withings portal to use `https://<domain>`
8. **Rollback** — on server: `docker compose up -d --no-deps --pull never backend` with a specific image tag if needed; DB rollback not needed (Flyway migrations are additive)

---

### 8. Fitatu automation: Playwright runner on Fly.io scheduled Machine

The planned Fitatu CSV sync needs to run on a schedule (e.g., daily at 06:00) and perform these steps:
1. Launch a Chromium browser via Playwright
2. Log into `fitatu.com` with stored credentials
3. Navigate to the export page and download the CSV
4. POST the CSV file to `http://backend:8080/api/nutrition/fitatu/import` using an internal service token (same Docker network as the backend container)

**Runner approach: server cron job running a Playwright Docker container**

Since the Hetzner server is always on, a simple `cron` job can run the sync container daily. The cron entry executes `docker run --rm --env-file /opt/evolve-log/.env.fitatu ghcr.io/<owner>/evolve-log-fitatu-sync`. The container runs the script, exits, and billing is just the always-on server that already hosts everything else. No separate scheduling infrastructure is needed.

n8n considered: powerful workflow UI, but requires a persistent container, storage for state, and a subdomain. Overkill for a single daily script. Deferred — can be adopted later if more automations are needed.

**Authentication for the import API call**

The evolve-log API currently uses cookie-based session auth (`withCredentials: true`), which is browser-oriented and unsuitable for service-to-service calls. Two options:
1. Add a static `EVOLVELOG_IMPORT_TOKEN` header check to the import endpoint — simple, no OAuth complexity
2. Reuse session by logging into evolve-log via Playwright before calling the import endpoint

Recommendation: option 1 — add a lightweight `X-Internal-Token` check on the import endpoint, validated against a secret env var. The runner container passes this header; no browser session needed for the API call itself.

**Directory structure**
```
evolve-log-fitatu-sync/
  Dockerfile          ← mcr.microsoft.com/playwright:v1.49.0-noble base
  package.json        ← playwright, node-fetch dependencies
  sync.js             ← main script (login → export → POST)
```

No `fly.toml` — scheduling is handled by a server cron entry on the Hetzner host. The container is built and pushed to GHCR by GitHub Actions, then pulled and run by cron.

**Secrets needed (stored in `/opt/evolve-log/.env.fitatu` on server):**
- `FITATU_EMAIL` — Fitatu account email
- `FITATU_PASSWORD` — Fitatu account password
- `EVOLVELOG_API_URL` — `http://backend:8080` (Docker network hostname; use this when running in the same Docker network, or `http://127.0.0.1:8080` if running outside)
- `EVOLVELOG_IMPORT_TOKEN` — shared secret for the import endpoint

The backend needs one small change: validate `X-Internal-Token` header on `POST /api/nutrition/fitatu/import` (or expose a separate internal-only route). This is a backend implementation task, not infrastructure — tracked here only as a dependency to note.

---

## Open Questions

- Custom domain vs bare server IP? → Strongly recommend a domain (even a cheap `.dev` or `.pl`); Certbot requires a domain for Let's Encrypt. Without a domain, HTTPS is not possible.
- Where to store `docker-compose.prod.yml` on the server? → `/opt/evolve-log/` — owned by `deploy` user; GitHub Actions SSHes in as that user.
- Should the GitHub Actions workflow skip deploy on test failure or always deploy? → Skip deploy if tests fail (default `needs:` dependency handles this).
- Automatic DB backups? → Not in scope for initial deploy; Hetzner daily snapshots (~€0.01/GB/month) can be enabled from the control panel as a safety net.
