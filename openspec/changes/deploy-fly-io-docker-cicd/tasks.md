## 1. Secrets Externalization

- [ ] 1.1 Update `evolve-log/src/main/resources/application.properties` — replace all hardcoded values with `${ENV_VAR:default}` placeholders for DB URL/credentials, Google OAuth, Withings, AI encryption key, and `app.frontend-url`
- [ ] 1.2 Add `src/main/resources/application-local.properties` to `evolve-log/.gitignore`
- [ ] 1.3 Create `.env.example` at the repo root with all required env var names, placeholder values, and a comment for each

## 2. nginx Configuration

- [ ] 2.1 Create `evolve-log-ui/nginx.conf` with upstream block pointing to `backend:8080`
- [ ] 2.2 Add `/api/` proxy_pass block with `Host`, `X-Real-IP`, `X-Forwarded-Proto` headers, `proxy_buffering off`, `proxy_read_timeout 180s`, and cookie passthrough
- [ ] 2.3 Add service worker location (`/sw.js`, `/service-worker.js`) with `Cache-Control: no-store`
- [ ] 2.4 Add versioned assets location (`~* \.(js|css|woff2)$`) with `Cache-Control: public, max-age=31536000, immutable`
- [ ] 2.5 Add SPA fallback `location /` with `try_files $uri $uri/ /index.html` and `Cache-Control: no-cache, must-revalidate`
- [ ] 2.6 Add `/health` location returning `200 OK` for Fly.io health checks

## 3. Frontend Dockerfile

- [ ] 3.1 Create `evolve-log-ui/Dockerfile` — Stage 1: `node:22-alpine`, `npm ci --frozen-lockfile`, `npm run build`
- [ ] 3.2 Add Stage 2: `nginx:alpine`, copy `nginx.conf` to `/etc/nginx/conf.d/default.conf`, copy `dist/` to `/usr/share/nginx/html`
- [ ] 3.3 Add `HEALTHCHECK` pinging `http://localhost/health`, `EXPOSE 80`, `CMD ["nginx", "-g", "daemon off;"]`
- [ ] 3.4 Verify locally: `docker build -t evolve-log-ui evolve-log-ui/` builds without error

## 4. Backend Dockerfile

- [ ] 4.1 Create `evolve-log/Dockerfile` — Stage 1: `eclipse-temurin:25-jdk-alpine`, copy Gradle wrapper + build files, run `./gradlew dependencies --no-daemon` (cache layer), copy `src/`, run `./gradlew clean build -x test --no-daemon`
- [ ] 4.2 Add Stage 2: `eclipse-temurin:25-jre-alpine`, create `spring:spring` user, copy JAR as `/app/app.jar`, `chown spring:spring`
- [ ] 4.3 Add `HEALTHCHECK` calling `wget http://localhost:8080/actuator/health`, `USER spring`, `EXPOSE 8080`, `ENTRYPOINT ["java", "-jar", "app.jar"]`
- [ ] 4.4 Verify locally: `docker build -t evolve-log evolve-log/` builds without error

## 5. docker-compose.yml (Full-Stack Local Testing)

- [ ] 5.1 Add `backend` service to `evolve-log/docker-compose.yml` with `build: { context: . }`, all env vars from `.env`, `depends_on: postgres (service_healthy)`, volume `backend_uploads:/app/uploads`
- [ ] 5.2 Add `frontend` service with `build: { context: ../evolve-log-ui }`, `ports: ["80:80"]`, `depends_on: [backend]`
- [ ] 5.3 Add named volumes `postgres_data` and `backend_uploads` and a `app-network` bridge network
- [ ] 5.4 Smoke test: copy `.env.example` → `.env`, fill in values, run `docker compose up --build`, verify `curl http://localhost/health` and `curl http://localhost/api/actuator/health`
- [ ] 5.5 Create `docker-compose.prod.yml` at repo root — same services but using GHCR image references (`image: ghcr.io/<owner>/evolve-log-backend:latest`) instead of `build:` directives, frontend port `3000:80` (host nginx sits in front), `env_file: .env`

## 6. Hetzner Server Setup

- [ ] 6.1 Create Hetzner Cloud CX22 server — region `nbg1`, Ubuntu 24.04, add SSH public key during creation
- [ ] 6.2 SSH in as root; create `deploy` user: `useradd -m -s /bin/bash deploy && usermod -aG docker deploy`; copy SSH authorized_keys to `/home/deploy/.ssh/`
- [ ] 6.3 Install Docker CE + Compose plugin: `curl -fsSL https://get.docker.com | sh`
- [ ] 6.4 Configure UFW firewall: `ufw allow 22 && ufw allow 80 && ufw allow 443 && ufw enable`
- [ ] 6.5 Install host nginx + Certbot: `apt install -y nginx certbot python3-certbot-nginx`
- [ ] 6.6 Point DNS A record for `<domain>` to the server's public IP; run `certbot --nginx -d <domain>` to obtain Let's Encrypt certificate; verify auto-renewal: `certbot renew --dry-run`
- [ ] 6.7 Create host nginx site config at `/etc/nginx/sites-available/evolve-log` proxying `https://<domain>` → `http://127.0.0.1:3000`; enable it with symlink in `sites-enabled/`
- [ ] 6.8 Create `/opt/evolve-log/` directory owned by `deploy`; copy `docker-compose.prod.yml` there
- [ ] 6.9 Create `/opt/evolve-log/.env` on server with all runtime secrets: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_FRONTEND_URL`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `WITHINGS_CLIENT_ID`, `WITHINGS_CLIENT_SECRET`, `APP_AI_ENCRYPTION_KEY`, `SPRING_PROFILES_ACTIVE=prod`; set permissions `chmod 640 .env && chown root:deploy .env`
- [ ] 6.10 Start all services: `docker compose -f docker-compose.prod.yml up -d`; verify with `docker compose ps`

## 7. GitHub Actions CI/CD Workflow

- [ ] 7.1 Create `.github/workflows/deploy.yml` with trigger `on: push: branches: [main]` and `workflow_dispatch`
- [ ] 7.2 Add `test` job: `actions/setup-java@v4` (Java 25, temurin, Gradle cache), run `./gradlew test --no-daemon` in `evolve-log/`
- [ ] 7.3 Add `build-and-push` job (`needs: test`): `docker/setup-buildx-action@v3`, `docker/login-action@v3` to GHCR with `GITHUB_TOKEN`, build+push backend image with `docker/build-push-action@v5` using `cache-from/to: type=gha`, tags `latest` + `${{ github.sha }}`
- [ ] 7.4 Add frontend build+push step in same job: same pattern, context `./evolve-log-ui`
- [ ] 7.5 Add `deploy` job (`needs: build-and-push`): use `appleboy/ssh-action@v1` (or `webfactory/ssh-agent` + run step) to SSH into `SERVER_IP` as `SERVER_USER`, run `docker compose -f /opt/evolve-log/docker-compose.prod.yml pull && docker compose -f /opt/evolve-log/docker-compose.prod.yml up -d`
- [ ] 7.6 Add GitHub Actions secrets to repository settings: `SSH_PRIVATE_KEY` (deploy private key), `SERVER_IP` (Hetzner server public IP), `SERVER_USER` (e.g., `deploy`); also add server's host fingerprint to `known_hosts` or use `StrictHostKeyChecking=no` in the SSH step

## 8. Fitatu Automation Infrastructure (DEFERRED — future task)

> Not implemented now. Architecture and requirements are captured in `design.md` (Decision §8) and `specs/fitatu-automation-infra/spec.md`. Implement when the automation details are finalised.

- [ ] 8.1 Add automation secret names to `.env.example` as commented-out placeholders (`FITATU_EMAIL`, `FITATU_PASSWORD`, `EVOLVELOG_API_URL`, `EVOLVELOG_IMPORT_TOKEN`) so future implementors know what will be needed

## 9. Post-Deploy Validation

- [ ] 9.1 Confirm HTTPS works: navigate to `https://<domain>` in browser; verify padlock and valid Let's Encrypt certificate
- [ ] 9.2 Confirm PWA installs on mobile: open `https://<domain>` on phone, verify browser shows "Add to Home Screen" prompt
- [ ] 9.3 Confirm API routing: `curl https://<domain>/api/actuator/health` returns `{"status":"UP"}`
- [ ] 9.4 Update Google OAuth redirect URI in Google Cloud Console to `https://<domain>/login/oauth2/code/google`
- [ ] 9.5 Update Withings redirect URI in Withings developer portal to `https://<domain>/auth/withings/callback`
- [ ] 9.6 Verify Flyway migrations applied: `ssh deploy@<server-ip> "docker exec evolve-log-backend psql \$SPRING_DATASOURCE_URL -c \"SELECT version, description FROM flyway_schema_history ORDER BY installed_rank;\""`
- [ ] 9.7 Upload a test file and redeploy (push to main); confirm file persists after redeploy (Docker volume persistence validation)
- [ ] 9.8 Check backend startup logs for errors: `ssh deploy@<server-ip> "docker compose -f /opt/evolve-log/docker-compose.prod.yml logs backend | grep ERROR"`; expect no output
- [ ] 9.9 Trigger sync runner manually (when sync.js is implemented): `ssh deploy@<server-ip> "docker run --rm --network evolve-log-net --env-file /opt/evolve-log/.env.fitatu ghcr.io/<owner>/evolve-log-fitatu-sync:latest"`; confirm it starts, logs env-var validation output, and exits cleanly
