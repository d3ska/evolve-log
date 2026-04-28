# Deployment Setup Checklist

Everything you need to buy, configure, or provide before `git push` triggers a working deployment.

---

## Phase 1: Buy / Provision

### 1.1 Hetzner Cloud Server
1. Go to [console.hetzner.cloud](https://console.hetzner.cloud) and sign up / log in
2. Create a new project (e.g., `evolve-log`)
3. Click **Add Server**:
   - **Location**: `nbg1` (Nuremberg, EU)
   - **Image**: Ubuntu 24.04
   - **Type**: **CX22** (shared vCPU, 4 GB RAM — ~4 EUR/month)
   - **SSH keys**: Add your public key during creation (see Phase 2 below)
4. Note the server's **public IPv4 address** after creation

### 1.2 Domain Name
- Buy a domain from any registrar (Cloudflare, Namecheap, Porkbun, etc.)
- You'll point it at the Hetzner server IP in Phase 3

---

## Phase 2: One-time Local Preparations

### 2.1 SSH Key Pair for Deployment
Generate a dedicated key pair (do NOT reuse your personal key):
```bash
ssh-keygen -t ed25519 -C "evolve-log-deploy" -f ~/.ssh/evolve_log_deploy
```
- **Public key** (`~/.ssh/evolve_log_deploy.pub`) — paste into Hetzner when creating the server
- **Private key** (`~/.ssh/evolve_log_deploy`) — you'll add to GitHub Secrets in Phase 4

### 2.2 AI Encryption Key
Generate a random 256-bit key:
```bash
openssl rand -base64 32
```
Save the output — you'll need it for the server `.env` and for local testing.

### 2.3 Push Both Repos to GitHub
If not already done:
```bash
# Backend
cd ~/IdeaProjects/evolve-log
git init && git add . && git commit -m "chore: initial commit"
gh repo create evolve-log --private --source=. --push

# Frontend
cd ~/IdeaProjects/evolve-log-ui
git init && git add . && git commit -m "chore: initial commit"
gh repo create evolve-log-ui --private --source=. --push
```
> If the frontend repo is **private**, uncomment the `token: ${{ secrets.REPO_PAT }}` line in
> `.github/workflows/deploy.yml` and add a `REPO_PAT` GitHub secret (Personal Access Token with `repo` scope).

---

## Phase 3: DNS Setup

1. Log in to your domain registrar / DNS provider
2. Add an **A record**:
   - **Name**: `@` (or a subdomain like `app`)
   - **Value**: your Hetzner server IPv4
   - **TTL**: 300
3. Wait for propagation: `dig +short yourdomain.com`

---

## Phase 4: GitHub Secrets

Go to your backend repo on GitHub -> **Settings -> Secrets and variables -> Actions -> New repository secret**:

| Secret name | Value |
|---|---|
| `SSH_PRIVATE_KEY` | Full contents of `~/.ssh/evolve_log_deploy` (private key file) |
| `SERVER_IP` | Hetzner server public IPv4 |
| `SERVER_USER` | `deploy` (created in Phase 5) |

---

## Phase 5: Server Setup (run once via SSH)

SSH in as root: `ssh root@<SERVER_IP>`

### 5.1 Create deploy user
```bash
useradd -m -s /bin/bash deploy
usermod -aG sudo deploy
mkdir -p /home/deploy/.ssh
cp ~/.ssh/authorized_keys /home/deploy/.ssh/
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys
```

### 5.2 Install Docker
```bash
curl -fsSL https://get.docker.com | sh
usermod -aG docker deploy
```

### 5.3 Configure firewall
```bash
ufw allow 22
ufw allow 80
ufw allow 443
ufw enable
```

### 5.4 Install host nginx + Certbot
```bash
apt update && apt install -y nginx certbot python3-certbot-nginx
```

### 5.5 Configure HTTPS
```bash
certbot --nginx -d yourdomain.com
# Follow prompts; choose "Redirect HTTP to HTTPS"
certbot renew --dry-run   # verify auto-renewal works
```

### 5.6 Configure host nginx to proxy to Docker frontend
Create `/etc/nginx/sites-available/evolve-log`:
```nginx
server {
    server_name yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Certbot will add SSL config below this line automatically
}
```
Then:
```bash
ln -s /etc/nginx/sites-available/evolve-log /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
```

### 5.7 Create app directory
```bash
mkdir -p /opt/evolve-log
chown deploy:deploy /opt/evolve-log
```

### 5.8 Copy docker-compose.prod.yml to server
From your local machine:
```bash
scp evolve-log/docker-compose.prod.yml deploy@<SERVER_IP>:/opt/evolve-log/
```

### 5.9 Create server .env file
As `deploy` user on the server:
```bash
cat > /opt/evolve-log/.env << 'EOF'
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/evolvelog
SPRING_DATASOURCE_USERNAME=evolvelog
SPRING_DATASOURCE_PASSWORD=<strong-random-password>
POSTGRES_PASSWORD=<same-as-SPRING_DATASOURCE_PASSWORD>
SPRING_PROFILES_ACTIVE=prod
APP_FRONTEND_URL=https://yourdomain.com
GOOGLE_CLIENT_ID=<from-google-console>
GOOGLE_CLIENT_SECRET=<from-google-console>
WITHINGS_CLIENT_ID=<from-withings-portal>
WITHINGS_CLIENT_SECRET=<from-withings-portal>
APP_AI_ENCRYPTION_KEY=<output-from-phase-2.2>
GITHUB_REPOSITORY_OWNER=<your-github-username>
EOF

chmod 640 /opt/evolve-log/.env
chown root:deploy /opt/evolve-log/.env
```

### 5.10 First start (can do before or after first CI/CD run)
```bash
cd /opt/evolve-log
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
```

---

## Phase 6: OAuth Redirect URIs

### 6.1 Google Cloud Console
1. Go to [console.cloud.google.com](https://console.cloud.google.com) -> APIs & Services -> Credentials
2. Open your OAuth 2.0 Client ID -> Edit
3. Add to **Authorized redirect URIs**:
   ```
   https://yourdomain.com/login/oauth2/code/google
   ```

### 6.2 Withings Developer Portal
1. Go to your app at [developer.withings.com](https://developer.withings.com)
2. Update the redirect/callback URI to:
   ```
   https://yourdomain.com/auth/withings/callback
   ```

---

## Phase 7: First Automated Deploy

Push to `main` in the backend repo:
```bash
git push origin main
```
Watch the Actions tab: **Backend Tests -> Build & Push Docker Images -> Deploy to Hetzner**

After deploy:
```bash
curl https://yourdomain.com/health
# Expected: OK
curl https://yourdomain.com/api/actuator/health
# Expected: {"status":"UP"}
```

---

## Phase 8: Local Docker Build Verification (optional but recommended first)

With Docker Desktop running:
```bash
# Frontend
cd ~/IdeaProjects/evolve-log-ui
docker build -t evolve-log-ui .

# Backend
cd ~/IdeaProjects/evolve-log
docker build -t evolve-log .
```

Full stack smoke test:
```bash
cd ~/IdeaProjects/evolve-log
cp .env.example .env
# Edit .env with real values (at minimum: DB passwords, AI key, OAuth credentials)
docker compose up --build
curl http://localhost/health
curl http://localhost/api/actuator/health
```

---

## Cost Summary

| Item | Cost |
|---|---|
| Hetzner CX22 server | ~4 EUR/month |
| Domain name | ~10-15 EUR/year |
| GitHub Actions (free tier) | 0 EUR |
| GHCR image storage | 0 EUR (500 MB free; small images stay well under) |
| **Total** | **~5 EUR/month + domain** |
