## ADDED Requirements

### Requirement: Hetzner CX22 server is provisioned and hardened
A Hetzner Cloud CX22 instance SHALL be created in region `nbg1` (Nuremberg) running Ubuntu 24.04. The server SHALL have Docker CE + Compose plugin installed, a non-root `deploy` user with Docker access, and UFW firewall allowing only ports 22 (SSH), 80 (HTTP), and 443 (HTTPS).

#### Scenario: Docker is available for the deploy user
- **WHEN** `deploy` user SSH session runs `docker compose version`
- **THEN** the command exits 0 and prints the installed Compose version

#### Scenario: Firewall blocks unexpected ports
- **WHEN** an external client attempts to connect to port 8080 on the server
- **THEN** the connection is refused (UFW default deny)

---

### Requirement: Host nginx + Certbot provide HTTPS termination
nginx SHALL be installed on the host (not in a container) and configured to proxy `https://<domain>` → `http://127.0.0.1:3000`. Certbot SHALL obtain a Let's Encrypt certificate for the domain and configure auto-renewal via the systemd timer `certbot.timer`.

#### Scenario: HTTPS is served with a valid certificate
- **WHEN** a browser navigates to `https://<domain>`
- **THEN** the response has a valid TLS certificate (not self-signed, not expired) and returns HTTP 200

#### Scenario: Certbot auto-renews before expiry
- **WHEN** `certbot renew --dry-run` is executed
- **THEN** the command exits 0, confirming the renewal mechanism works

---

### Requirement: All services run via docker-compose.prod.yml
A `docker-compose.prod.yml` file SHALL define three services: `postgres` (PostgreSQL 17), `backend` (Spring Boot), and `frontend` (nginx). All services SHALL be connected via a shared `app-network` bridge network. The file SHALL be stored on the server at `/opt/evolve-log/docker-compose.prod.yml`.

#### Scenario: All three containers start healthy
- **WHEN** `docker compose -f docker-compose.prod.yml up -d` is run on the server
- **THEN** `docker compose ps` shows all three services as `running` or `healthy`

#### Scenario: Frontend container reaches backend via Docker network
- **WHEN** nginx in the frontend container proxies a request to `http://backend:8080/actuator/health`
- **THEN** it receives HTTP 200 from the Spring Boot backend

---

### Requirement: PostgreSQL data persists across container restarts
A named Docker volume `postgres_data` SHALL be declared in `docker-compose.prod.yml` and mounted at `/var/lib/postgresql/data` in the `postgres` container. Data SHALL survive `docker compose restart` and container image upgrades.

#### Scenario: Data survives container restart
- **WHEN** the `postgres` container is stopped and restarted
- **THEN** all previously written rows are accessible without re-running migrations

---

### Requirement: Backend uploads persist across redeployments
A named Docker volume `backend_uploads` SHALL be mounted at `/app/uploads` in the `backend` container. Files uploaded via the API SHALL survive container image upgrades triggered by CI/CD.

#### Scenario: Uploaded file persists across redeploy
- **WHEN** a file is uploaded via the API and `docker compose pull && docker compose up -d` is run
- **THEN** the file is still accessible at the same path after the new container starts

---

### Requirement: All backend secrets are stored in .env on the server
The file `/opt/evolve-log/.env` on the Hetzner server SHALL contain all runtime secrets. It SHALL be owned by `root:deploy` with mode `640`. It SHALL NOT be committed to source control. The `docker-compose.prod.yml` SHALL reference it via `env_file`.

Required variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_FRONTEND_URL`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `WITHINGS_CLIENT_ID`, `WITHINGS_CLIENT_SECRET`, `APP_AI_ENCRYPTION_KEY`, `SPRING_PROFILES_ACTIVE`.

#### Scenario: App starts with all secrets present
- **WHEN** all required variables are in `.env` and `docker compose up -d` is run
- **THEN** the backend starts without missing-property errors and `/actuator/health` returns `{"status":"UP"}`

---

### Requirement: Health checks are configured in docker-compose.prod.yml
Each service in `docker-compose.prod.yml` SHALL define a `healthcheck`. Backend: `wget http://localhost:8080/actuator/health`. Frontend: `wget http://localhost/health`. Postgres: `pg_isready -U $POSTGRES_USER`. The `backend` service SHALL declare `depends_on: postgres: condition: service_healthy`.

#### Scenario: Backend waits for healthy Postgres before starting
- **WHEN** all containers are started simultaneously
- **THEN** the backend container does not begin accepting connections until Postgres health check passes

---

### Requirement: Server has a cron entry for the Fitatu sync runner (deferred)
When the Fitatu sync runner is implemented, the server SHALL have a cron entry for the `deploy` user that runs daily (e.g., 06:00 UTC):
```
0 6 * * * docker run --rm --env-file /opt/evolve-log/.env.fitatu ghcr.io/<owner>/evolve-log-fitatu-sync:latest
```
Secrets for the runner SHALL be stored separately in `/opt/evolve-log/.env.fitatu` (not mixed into the main `.env`).

#### Scenario: Cron entry is listed for deploy user
- **WHEN** `crontab -l` is run as the `deploy` user on the server
- **THEN** the Fitatu sync entry appears with the correct schedule and image reference
