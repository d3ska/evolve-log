## ADDED Requirements

### Requirement: Backend and frontend are deployed as separate Fly.io apps
The backend SHALL be deployed as `evolve-log-backend` and the frontend as `evolve-log-frontend`, both in the same Fly.io organization. Each app SHALL have its own `fly.toml` in its respective source directory.

#### Scenario: Backend app is reachable on its internal hostname
- **WHEN** the frontend nginx container resolves `evolve-log-backend.internal`
- **THEN** it connects to the Spring Boot backend on port 8080 over Fly.io's private network

#### Scenario: Frontend app is publicly accessible via HTTPS
- **WHEN** a browser navigates to `https://evolve-log-frontend.fly.dev`
- **THEN** the response is the React app served over TLS with a valid certificate

---

### Requirement: Managed PostgreSQL is provisioned on Fly.io
A Fly.io managed PostgreSQL cluster SHALL be created and attached to the backend app. The `DATABASE_URL` secret SHALL be set automatically by Fly.io on attach and mapped to `SPRING_DATASOURCE_URL`.

#### Scenario: Flyway migrations run on first deploy
- **WHEN** the backend app starts for the first time against a fresh database
- **THEN** Flyway applies migrations V1 through V19 and the `flyway_schema_history` table is populated

#### Scenario: Flyway is idempotent on subsequent deploys
- **WHEN** the backend app restarts against a database that already has all migrations applied
- **THEN** Flyway detects no new migrations and the app starts without error

---

### Requirement: Persistent volume is mounted for file uploads
A Fly.io persistent volume of at least 1 GB SHALL be created and mounted at `/app/uploads` in the backend container so uploaded files survive container restarts and redeployments.

#### Scenario: Uploaded file persists across redeploy
- **WHEN** a file is uploaded via the API and the backend container is redeployed
- **THEN** the file is still accessible at the same path after restart

---

### Requirement: Both apps run in the Frankfurt (fra) region
Both `evolve-log-backend` and `evolve-log-frontend` fly.toml files SHALL specify `primary_region = "fra"`.

#### Scenario: Apps are placed in the correct region
- **WHEN** `flyctl status` is run for either app
- **THEN** the machine region is reported as `fra`

---

### Requirement: All backend secrets are set via flyctl secrets
The following secrets SHALL be set on the `evolve-log-backend` app via `flyctl secrets set` before the first deploy. They SHALL NOT be committed to source control or included in the Docker image.

Required secrets: `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_FRONTEND_URL`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `WITHINGS_CLIENT_ID`, `WITHINGS_CLIENT_SECRET`, `APP_AI_ENCRYPTION_KEY`.

#### Scenario: App starts with all secrets present
- **WHEN** all required secrets are set via flyctl and the app is deployed
- **THEN** the app starts without missing-property errors and `/actuator/health` returns `{"status":"UP"}`

#### Scenario: Missing required secret causes startup failure
- **WHEN** `APP_AI_ENCRYPTION_KEY` is not set and the app starts
- **THEN** the app fails to start with a clear configuration error (not a NullPointerException at runtime)

---

### Requirement: Health checks are configured in fly.toml
Each app's `fly.toml` SHALL define an HTTP health check. Backend: `GET /actuator/health` on port 8080. Frontend: `GET /health` on port 80. Fly.io SHALL only route traffic to a machine after its health check passes.

#### Scenario: Unhealthy backend machine receives no traffic
- **WHEN** the backend fails its health check during a deploy
- **THEN** Fly.io keeps the previous machine running and the new machine does not receive traffic
