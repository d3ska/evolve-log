## ADDED Requirements

### Requirement: All runtime secrets are injected via environment variables
`application.properties` SHALL use `${ENV_VAR:default}` syntax for every value that differs between environments. The defaults SHALL be safe for local development (e.g., localhost DB URL, empty OAuth keys). No secret value SHALL be present in `application.properties` or any file tracked by git.

#### Scenario: Application starts locally without application-local.properties
- **WHEN** the application is started with `SPRING_PROFILES_ACTIVE=local` and a populated `application-local.properties`
- **THEN** the application connects to the local database and all integrations resolve correctly

#### Scenario: Application starts in production with environment variables only
- **WHEN** the application is started in a Docker container with all required env vars set and no `application-local.properties`
- **THEN** the application starts successfully and connects to the production database

---

### Requirement: application-local.properties is excluded from version control
The file `src/main/resources/application-local.properties` SHALL be listed in `.gitignore`. No commit to the repository SHALL contain this file.

#### Scenario: git status does not track the secrets file
- **WHEN** `git status` is run after modifying `application-local.properties`
- **THEN** the file does not appear in staged or unstaged changes

---

### Requirement: Required environment variables are documented
An `.env.example` file SHALL exist at the repository root listing every environment variable required by the backend, with placeholder values and a comment describing each.

#### Scenario: Developer can set up local environment from .env.example
- **WHEN** a developer copies `.env.example` to `.env` and fills in real values
- **THEN** `docker compose up` starts the full stack successfully

---

### Requirement: Spring property mapping covers all secret categories
The following properties SHALL be sourced from environment variables:

| Property | Env var |
|---|---|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` |
| `app.frontend-url` | `APP_FRONTEND_URL` |
| `withings.client-id` | `WITHINGS_CLIENT_ID` |
| `withings.client-secret` | `WITHINGS_CLIENT_SECRET` |
| `spring.security.oauth2.client.registration.google.client-id` | `GOOGLE_CLIENT_ID` |
| `spring.security.oauth2.client.registration.google.client-secret` | `GOOGLE_CLIENT_SECRET` |
| `app.ai.encryption.key` | `APP_AI_ENCRYPTION_KEY` |

#### Scenario: Database URL env var overrides default
- **WHEN** `SPRING_DATASOURCE_URL=jdbc:postgresql://prod-host:5432/evolvelog` is set
- **THEN** the application connects to `prod-host` instead of `localhost`

#### Scenario: Missing optional integration key does not crash startup
- **WHEN** `WITHINGS_CLIENT_ID` is not set
- **THEN** the application starts successfully; Withings OAuth flows return a configuration error only when invoked
