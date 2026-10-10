# DEPLOYMENT_GUIDE.md — RayyanMart Online Shopping System

Deployment guidance for the application's actual supported stack: a Spring Boot
2.7.18 **WAR** on **Apache Tomcat 9.0.x**, with a persistent database (H2 TCP
server or MySQL). Nothing here changes the project — it is operational guidance.

---

## 1. Target topology

```mermaid
flowchart LR
    U[Client browser] --> LB[Reverse proxy / TLS (optional)]
    LB --> T[Apache Tomcat 9.0.x]
    T --> APP[online-shopping-system-1.0.0.war]
    APP --> H2[H2 TCP server :9092<br>file or MySQL]
```

- A single WAR (Spring Boot) deploys to Tomcat 9 like any web app.
- The app connects to an external database; it owns the schema via
  `ddl-auto=update` and seeds products via `data.sql`.

## 2. Prerequisites

- JRE 17+ (JDK 25 verified in dev).
- Apache Tomcat 9.0.x (not Tomcat 10 — this WAR uses `javax.servlet`, supplied
  `provided` by Tomcat 9).
- A writable location for the H2 database files (or an existing MySQL instance).
- Build host: JDK 17+ and Maven 3.8+ only if building from source.

## 3. Build

```bash
cd C:\Rayyan Mart\online-shopping-system
mvn clean package
# -> target\online-shopping-system-1.0.0.war
```

Run the test suite first (`mvn test`) — see TEST_RESULTS.md (99/99 green).

## 4. Database persistence requirements

**Chosen default — H2 TCP server (persistent):**

1. Start H2 server (any path, e.g. `/opt/rayyanmart/db`, or a Windows directory):
   ```bash
   java -cp h2-2.1.214.jar org.h2.tools.Server \
        -tcp -tcpPort 9092 -tcpAllowOthers \
        -baseDir /opt/rayyanmart/db -webAllowOthers
   ```
2. Keep port 9092 reachable only by the app (bind where appropriate;
   `-tcpAllowOthers` + firewall rules control access).
3. The database file is created on first connection; H2 stores the default
   owner (`sa/sa` unless changed). Make the file directory writable by the
   Tomcat service account and **back it up** (stop app, copy the `.mv.db` file).

**Alternative — MySQL:** set `DB_URL=jdbc:mysql://host:3306/rayyanmart?...`,
`DB_USERNAME`, `DB_PASSWORD`; the connector is bundled. Hibernate will create
tables (`ddl-auto=update`); run `data.sql` seeding is controlled by
`DB_INIT_MODE=always`.

> Do **not** use in-memory H2 in production — all data is lost on restart.

## 5. Environment configuration

| Variable | Example | Notes |
|---|---|---|
| `SERVER_PORT` | 8080 | Tomcat's `Connector port` normally wins for deployed WARs; keep it for embedded runs |
| `DB_URL` | `jdbc:h2:tcp://localhost:9092/rayyanmartdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | Required |
| `DB_USERNAME` / `DB_PASSWORD` | `sa` / `sa` | Match the H2 owner or MySQL user |
| `DB_DRIVER` | *(auto)* | Only if HikariCP cannot auto-detect from URL |
| `DB_DDL_AUTO` | `update` | Production option: `validate` after a stable schema review |
| `DB_INIT_MODE` | `always` | Keep to seed products on first run |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@shop.local` / `<strong>` | Creates the admin once on startup |
| `CHAT_ENABLED`, `CHAT_PROVIDER`, `CHAT_MAX_MESSAGE_LENGTH`, `CHAT_RATE_LIMIT_PER_MINUTE`, `CHAT_CACHE_SIZE`, `CHAT_CACHE_TTL_SECONDS` | `true`, `mock`, ... | Chat tuning |
| `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_TIMEOUT_MS` | secret, model, 8000 | Optional Gemini provider |

Platform examples (Tomcat, Linux):
```bash
# tomcat/bin/setenv.sh
export DB_URL='jdbc:h2:tcp://localhost:9092/rayyanmartdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
export DB_USERNAME=sa
export DB_PASSWORD=sa
export CHAT_PROVIDER=mock
```
Windows (Tomcat service): set the variables in the service's environment or via
`setenv.bat`; or pass them in `catalina.bat`'s Java options.

## 6. Safe handling of credentials and API keys

- `GEMINI_API_KEY` is read **only from the environment** — never put it in
  `application.properties`, git, logs, or WAR contents.
- Use a dedicated DB user with least privilege; avoid `sa` + empty password in
  production.
- H2 web console: it is enabled only on the **H2 server** process
  (`-webAllowOthers` flag on the server). Do not expose the H2 web console or
  TCP ports publicly; restrict via firewall/VPC and `-tcpAllowOthers` only to
  the app host.
- Admin account: set `ADMIN_EMAIL`/`ADMIN_PASSWORD` to a strong password; remove
  them from the environment after the admin is created if you prefer (the account
  persists in the DB).

## 7. Deploy WAR to Tomcat 9

1. Stop Tomcat.
2. Copy `target\online-shopping-system-1.0.0.war` into `<tomcat>/webapps/`
   (optionally rename to `ROOT.war` to serve at `/`).
3. Ensure env vars are set in the Tomcat process (see §5).
4. Start Tomcat. Observe `<tomcat>/logs/catalina.<date>.log` for
   `Spring Boot :: 2.7.18` startup and Tomcat deployment messages.
5. Allow a few seconds; then verify (§8).

## 8. Local testing before deployment

- Run the automated suite: `mvn test` (99 tests — TEST_RESULTS.md).
- Boot with the **persistent** profile locally first:
  `DB_URL=jdbc:h2:mem:...` for a quick smoke, then the real `tcp://` URL to test
  persistence (seed products survive a restart).
- Test the health/liveness probe and one full purchase flow per
  MANUAL_TEST_CASES.md (M-01…M-36); confirm cart→order→history works against the
  persistent DB.

## 9. Verify after deployment

```bash
curl -s http://localhost:8080/api/v1/health
# -> {"status":"UP","service":"online-shopping-system","database":{"status":"UP","vendor":"H2",...}}
curl -s http://localhost:8080/api/products | jq length     # expect 5 seed products
```
Then: register → login → add to cart → order → check `/orders` — order appears
as PENDING and survives an app restart (persistent DB). Confirm the chat widget
renders for logged-in users and answers accurately.

## 10. Common deployment failures and troubleshooting

| Symptom | Cause / fix |
|---|---|
| `javax.servlet.ServletException` / `ClassNotFoundException javax.servlet.*` | Wrong container — must be Tomcat 9 (not 10/11, which use `jakarta.*`) |
| App starts but `/api/v1/health` returns 503, `database.status: DOWN` | H2 server not running / DB URL wrong / credentials mismatch → see TROUBLESHOOTING.md §1–2 |
| Port conflict | `server.tomcat`/connector in Tomcat; change Tomcat port, not app code |
| Admin login rejected | `ADMIN_EMAIL/PASSWORD` unset at first boot, or already-created admin; re-check environment |
| Identifier/locale issues with `DATABASE_TO_LOWER=TRUE` | Keep the fixed H2 URL; don't mix case modes after tables exist |
| Data loss | In-memory H2 in production — switch to TCP file DB (see §4) |
| Slow startup / OOM | Increase Tomcat `JAVA_OPTS` heap (e.g., `-Xmx1g`) |
| Seed products missing | `DB_INIT_MODE` not `always`, or `continue-on-error=true` masked a SQL error — check logs |

## 11. Post-deploy hygiene

- Take a database backup after first successful boot.
- Keep `data.sql` idempotent behaviour (`INSERT IGNORE`) — reseeding is safe.
- Monitor logs for 500s (they use `server.error.include-stacktrace=never`, so use
  `catalina.log`/app logs for detail).
- Do **not** enable H2 web console remotely; it has no built-in auth on the
  server process itself.