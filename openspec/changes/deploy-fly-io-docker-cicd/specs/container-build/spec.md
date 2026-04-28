## ADDED Requirements

### Requirement: Backend produces a runnable Docker image
The backend SHALL be packaged as a multi-stage Docker image using `eclipse-temurin:25-jdk-alpine` to build the Gradle fat JAR and `eclipse-temurin:25-jre-alpine` as the runtime base. The runtime image SHALL run the application as an unprivileged `spring` user and expose port 8080.

#### Scenario: Backend image builds successfully
- **WHEN** `docker build` is run in the `evolve-log/` directory
- **THEN** the image is created without error and the JAR is present at `/app/app.jar`

#### Scenario: Backend container starts and becomes healthy
- **WHEN** the backend container is started with required environment variables
- **THEN** the health check at `http://localhost:8080/actuator/health` returns HTTP 200 within 30 seconds

#### Scenario: No secrets baked into the image
- **WHEN** the Docker image is inspected (`docker inspect` or layer scan)
- **THEN** no OAuth credentials, DB passwords, or encryption keys are present in any image layer

---

### Requirement: Frontend produces a runnable Docker image
The frontend SHALL be packaged as a multi-stage Docker image using `node:22-alpine` to build the Vite production bundle and `nginx:alpine` as the runtime base. The runtime image SHALL serve static files from `/usr/share/nginx/html` and listen on port 80.

#### Scenario: Frontend image builds successfully
- **WHEN** `docker build` is run in the `evolve-log-ui/` directory
- **THEN** the image is created without error and `index.html` is present in `/usr/share/nginx/html`

#### Scenario: Frontend container serves the SPA
- **WHEN** a GET request is made to `http://localhost/`
- **THEN** the response is HTTP 200 with the React `index.html`

#### Scenario: Unknown routes fall back to index.html
- **WHEN** a GET request is made to a non-existent path such as `/dashboard/stats`
- **THEN** nginx returns HTTP 200 with `index.html` (SPA routing fallback)

---

### Requirement: nginx proxies /api to the backend
The nginx configuration SHALL forward all requests matching `/api/*` to the backend service using `proxy_pass`. The proxy SHALL preserve the original `Host`, `X-Real-IP`, and `X-Forwarded-Proto` headers and SHALL NOT buffer request or response bodies (to support Server-Sent Events and large uploads).

#### Scenario: API request is proxied correctly
- **WHEN** a GET request is made to `http://localhost/api/actuator/health`
- **THEN** nginx forwards it to `http://backend:8080/api/actuator/health` and returns the backend's response

#### Scenario: Cookies are preserved through the proxy
- **WHEN** the backend sets a `Set-Cookie` header on a login response
- **THEN** the cookie is forwarded to the browser unchanged by nginx

#### Scenario: Large file uploads pass through without timeout
- **WHEN** a POST request with a 100 MB body is sent to `/api/upload`
- **THEN** nginx forwards the full body within the 180-second proxy timeout without returning a 504

---

### Requirement: Service worker is never served from cache
The nginx configuration SHALL set `Cache-Control: no-store` on the service worker file so browsers always fetch the latest version.

#### Scenario: Service worker response has no-store header
- **WHEN** a GET request is made to `/sw.js` or `/service-worker.js`
- **THEN** the response includes `Cache-Control: no-store`

---

### Requirement: Versioned static assets are cached long-term
Vite produces content-hashed filenames for JS and CSS bundles. nginx SHALL serve these with `Cache-Control: public, max-age=31536000, immutable`.

#### Scenario: Hashed JS asset is cached immutably
- **WHEN** a GET request is made to `/assets/index-Ab3Cd5Ef.js`
- **THEN** the response includes `Cache-Control: public, max-age=31536000, immutable`
