# RENDER_DEPLOYMENT.md — RayyanMart on Render

> **Status:** this repository is ready to deploy on Render using **Docker
> (Tomcat 9 + WAR)**. The exact deploy artifact (`target/online-shopping-system.war`)
> is verified locally; the free-tier Render deploy itself must be triggered from
> your GitHub account (no Render credentials exist in this repo, and none should
> ever be committed).

---

## 1. Why Docker + Tomcat 9 (not Render's native Java runtime)

- The project is a Spring Boot 2.7.18 **WAR** using **`javax.servlet`** APIs.
  `spring-boot-starter-tomcat` is declared with scope `provided`, so the WAR
  **only runs inside an external Apache Tomcat 9.x** (Tomcat 10+ uses `jakarta.*`
  and will throw `ClassNotFoundException javax.servlet.*`).
- Render's native Java runtime expects a runnable JAR/process; Render's own
  documentation recommends **Docker for JVM-based workloads**, which also keeps
  the production runtime identical to the documented local stack and lets us bind
  Tomcat to Render's `PORT`.
- Everything renderable is server-side (Thymeleaf + one chat widget that talks to
  the same origin `/api/chat`), so there is **no frontend build step** and no CORS
  configuration needed.

### Port binding
Render requires web services to listen on the `PORT` env var (default `10000`).
For a WAR under external Tomcat this is not Spring Boot's `server.port` — it is
Tomcat's Connector port, which the stock `server.xml` hardcodes to `8080`. The
container entrypoint (`deploy/docker-entrypoint.sh`) patches the HTTP/1.1
Connector to `$PORT` before starting Tomcat.

---

## 2. What changed / was added in this repo (for Render)

| Path | Reason |
|---|---|
| `Dockerfile` | Multi-stage build: `maven:3.9-eclipse-temurin-17` runs `mvn -B clean package` (same as CI), then `tomcat:9.0-jdk17-temurin` runs the WAR as `ROOT.war`. |
| `.dockerignore` | Keeps build context small and excludes the local H2 DB files. |
| `deploy/docker-entrypoint.sh` | Binds Tomcat's Connector to `$PORT` (Render requirement). |
| `render.yaml` | Optional declarative Blueprint (env var placeholders only — no secrets). |
| `pom.xml` | Added `<finalName>online-shopping-system</finalName>` so the artifact is `online-shopping-system.war` (the CI workflow already uploads that exact name; without this the build produced `online-shopping-system-1.0.0.war` and CI upload silently broke). |
| `.gitignore` | Ignore H2 runtime files (`database/*.mv.db`, `*.lock.db`, `database/db/`) so the live local database is never committed. |

No business logic, security configuration, frontend, test, or database schema was
changed. No secrets were added anywhere.

---

## 3. Database options

Render does not provide a native managed **MySQL** service. Two supported options,
both driven entirely by environment variables:

### Option A — External MySQL 8 (recommended for production)
Provision MySQL anywhere (e.g. Aiven, Railway, Clever Cloud, PlanetScale-less
generic host) and set, in Render:

```text
DB_URL=jdbc:mysql://HOST:3306/rayyanmart?useSSL=true&requireSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=rayyanmart
DB_PASSWORD=<strong password>
DB_DRIVER=com.mysql.cj.jdbc.Driver
```

Schema is created automatically (`DB_DDL_AUTO=update`); the 5 base products are
seeded by `data.sql` (`INSERT IGNORE`, idempotent). To also populate the 41-item
catalogue, run `database/sample-products.sql` once in MySQL.

### Option B — Bundled H2 file database + persistent disk (zero external service)
No external DB needed; data persists across deploys on a Render disk.

1. In the Render service, add a **Persistent Disk** mounted at `/var/lib/rayyanmart`.
2. Set these env vars:
   ```text
   DB_URL=jdbc:h2:file:/var/lib/rayyanmart/db/rayyanmartdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
   DB_USERNAME=sa
   DB_PASSWORD=sa
   # DB_DRIVER omitted — auto-detected (org.h2.Driver)
   ```
3. `data.sql` seeds the 5 base products on first boot. (Optionally import
   `database/sample-products.sql` for the full 41-product catalogue.)

> Do **not** use in-memory H2 on Render — data is lost on every restart.

---

## 4. Deploy steps (Dashboard — recommended)

1. Push `main` to `https://github.com/M-MOHAMED-RAYYAN/Rayyan-Mart.git`.
2. Render → **New → Web Service** → connect the GitHub repo.
3. Choose **Docker** runtime; the `Dockerfile` is at the repo root (auto-detected).
4. Service settings:
   - **Name:** e.g. `rayyan-mart`
   - **Region:** closest to you
   - **Branch:** `main`
   - **Instance type:** Free (starts with `rayyan-mart.onrender.com`)
   - **Health Check Path:** `/api/v1/health` (optional but recommended)
5. **Advanced → Add Environment Variables** (choose MySQL **or** H2/disk per §3):
   - `CHAT_PROVIDER=mock` (default; no key needed)
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER` (per §3)
   - `ADMIN_EMAIL` / `ADMIN_PASSWORD` (optional, creates an ADMIN on first boot)
   - `GEMINI_API_KEY` (optional — only used if `CHAT_PROVIDER=gemini`)
6. **Create Web Service** → wait for the build + deploy.
7. Open `https://rayyan-mart.onrender.com`.

> Alternative: commit `render.yaml` and use **New → Blueprint**, then fill the
> `sync: false` variables in the dashboard.

---

## 5. Environment variables summary

| Variable | Default | Notes |
|---|---|---|
| `PORT` | `10000` | Set by Render; entrypoint binds Tomcat to it. |
| `DB_URL` | H2 tcp (local dev) | **Required on Render** — set MySQL or H2-file URL (see §3). |
| `DB_USERNAME` / `DB_PASSWORD` | `sa` / `sa` | Match the DB user. |
| `DB_DRIVER` | auto-detect | `com.mysql.cj.jdbc.Driver` for MySQL; omit for H2. |
| `DB_DDL_AUTO` | `update` | Keep `update`; `validate` only after schema review. |
| `DB_INIT_MODE` | `always` | Required so `data.sql` seeds products. |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | empty | Bootstrap an ADMIN account on first run. |
| `CHAT_ENABLED` / `CHAT_PROVIDER` | `true` / `mock` | `mock` needs no API key. |
| `CHAT_MAX_MESSAGE_LENGTH`, `CHAT_RATE_LIMIT_PER_MINUTE`, `CHAT_CACHE_SIZE`, `CHAT_CACHE_TTL_SECONDS` | 500 / 10 / 50 / 120 | Chat tuning. |
| `GEMINI_API_KEY` / `GEMINI_MODEL` / `GEMINI_TIMEOUT_MS` | empty / gemini-2.0-flash / 8000 | Only for `CHAT_PROVIDER=gemini`. |

All of these map to existing `${VAR:default}` entries in `application.properties`
— nothing was hardcoded.

---

## 6. Local verification performed

- `mvn -B clean verify` → builds `target/online-shopping-system.war`, full test
  suite green.
- The **same WAR artifact** was booted locally against the durable H2 file DB
  (`database/rayyanmart`), using the same env overrides a Render service would
  receive; `GET /api/v1/health` returned `{"status":"UP", ...}` and the catalogue
  showed the expected products, confirming the artifact runs.
- Docker image build could **not** be executed locally (no Docker engine on this
  machine) — the `Dockerfile` follows the CI's exact `mvn -B clean package`
  command and the documented Tomcat 9 target, but this remains the one step to
  confirm in Render's build logs.

---

## 7. Verify after deploy

```bash
curl -s https://rayyan-mart.onrender.com/api/v1/health
# {"status":"UP","service":"online-shopping-system","database":{"status":"UP",...}}

curl -s https://rayyan-mart.onrender.com/api/products            # 5+ products
```

Then: register → login → add to cart → checkout → order appears under `/orders`
as `PENDING`. Confirm the chat widget loads and answers (mock provider).

---

## 8. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Deploy fails at `Upload WAR artifact` / no WAR | `mvn` produced a differently-named WAR — ensure `finalName` is `online-shopping-system` (this repo now sets it). |
| `java.lang.ClassNotFoundException: javax.servlet...` | The image must be Tomcat **9** (`javax`), never Tomcat 10/11 (`jakarta`). |
| App starts but `/api/v1/health` = 503, `database.status: DOWN` | DB unreachable/creds wrong; check `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` in the Render env. |
| 404 at `/` | WAR must run as `ROOT.war` (this Dockerfile already does; if you switched to a native deploy, deploy as `ROOT.war`). |
| Data gone every restart | Using in-memory H2; switch to external MySQL or H2-on-disk (§3). |
| Seed products missing | `DB_INIT_MODE` must be `always`; check logs — `continue-on-error=true` can mask SQL errors. |
| Free instance sleeps | Free tier sleeps after idle; Render wakes it on a new request (first load is slow as the app cold-starts). |
| Port bind error in logs | Confirm Render's `PORT` is present; the entrypoint prints the port it bound. |

---

## 9. Rollback

- Re-deploy the previous successful build from Render's **Events/Deploys** tab.
- DB (MySQL or H2-on-disk) survives deploys; back it up before schema-changing
  upgrades (`mysqldump` / copy the `.mv.db` file).

---

## 10. Commands for the operator

```bash
# Build the exact artifact Render will produce
mvn -B clean verify                    # -> target/online-shopping-system.war

# Commit + push the deployment files
git add pom.xml .gitignore .gitattributes .dockerignore Dockerfile render.yaml deploy docs/RENDER_DEPLOYMENT.md docs/DEPLOYMENT_GUIDE.md database/sample-products.sql
git commit -m "deploy: add Render (Docker/Tomcat 9) support"
git push origin main
```