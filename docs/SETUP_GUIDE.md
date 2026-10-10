# SETUP_GUIDE.md — RayyanMart Online Shopping System

> Instructions are based on the actual project (`pom.xml`, `application.properties`,
> seed SQL, and helpers in `web/Ui.java`). Nothing here changes the project.

## 1. Prerequisites

- **JDK 17** or newer (verified running on JDK 25 during development).
- **Apache Maven 3.8+** (verified with Maven 3.9.x).
- **Apache Tomcat 9.0.x** — only required when deploying the WAR or using the
  default H2 TCP-server database mode. For local development the WAR can run as
  an executable JAR with the built-in Tomcat 9 (`mvn spring-boot:run`).
- No external services required for the default configuration (the chat uses the
  built-in `mock` provider, which needs no API key).

## 2. Build the application

```bash
cd C:\Rayyan Mart\online-shopping-system
mvn clean package        # produces target\online-shopping-system-1.0.0.war
```

Or run the full test suite (see TEST_PLAN.md):

```bash
mvn test
```

Build properties (from `pom.xml`):

- `java.version = 17`
- packaging = `war` (Spring Boot 2.7.18, embedded Tomcat is `provided`, i.e. supplied by Tomcat 9 at runtime)
- artifactId `online-shopping-system`, version `1.0.0`

## 3. Database setup

The application does **not** create the database; it connects to one.
Two supported modes:

### 3a. Local development — in-memory H2 (zero config, data resets on restart)

Set these environment variables before launching:

```
DB_URL=jdbc:h2:mem:shopping_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
DB_USERNAME=sa
DB_PASSWORD=sa
```

No database server is needed. Tables are created automatically by Hibernate
(`spring.jpa.hibernate.ddl-auto=update`) and the 5 seed products are inserted by
`data.sql` (`INSERT IGNORE`, idempotent). **All data is lost when the app stops.**

### 3b. Persistent database — H2 TCP server (as configured by default)

The shipped default (`application.properties`) expects an **externally started H2
TCP server** on port `9092`:

```bash
java -cp h2-2.1.214.jar org.h2.tools.Server ^
     -tcp -tcpPort 9092 -tcpAllowOthers ^
     -baseDir /opt/rayyanmart/db -webAllowOthers
```

The application then connects with:

```
DB_URL=jdbc:h2:tcp://localhost:9092/rayyanmartdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

The database file is created automatically on first connect. The file's owner
credentials come from `DB_USERNAME`/`DB_PASSWORD` (the H2 server owner).

### 3c. MySQL (alternative)

A MySQL JDBC driver is included (`mysql-connector-j`, runtime scope). To use it,
set `DB_DRIVER` and a MySQL `DB_URL`; the driver class is auto-detected by
HikariCP from the URL when `DB_DRIVER` is not set. Hibernate still owns the schema
(`ddl-auto=update`).

> `MODE=MySQL` in the H2 URL keeps H2 behaviour (case handling, SQL dialect) close
> to MySQL, which is why `DATABASE_TO_LOWER=TRUE` is set.

## 4. Configuration (environment variables)

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | `8080` | Web port |
| `DB_URL` | `jdbc:h2:tcp://localhost:9092/rayyanmartdb;...` | JDBC URL |
| `DB_USERNAME` | `sa` | DB user |
| `DB_PASSWORD` | `sa` | DB password |
| `DB_DRIVER` | `org.h2.Driver` | JDBC driver (auto-detected if unset) |
| `DB_DDL_AUTO` | `update` | Hibernate DDL mode |
| `DB_INIT_MODE` | `always` | Run `data.sql` seed on startup (`continue-on-error=true`) |
| `ADMIN_EMAIL` | *(empty)* | If set, creates an ADMIN account on startup (ignored if empty or if the email exists) |
| `ADMIN_PASSWORD` | *(empty)* | Password for that admin account |
| `CHAT_ENABLED` | `true` | Enable the shopping-assistant endpoint/widget |
| `CHAT_PROVIDER` | `mock` | `mock` (deterministic FAQ) or `gemini` |
| `CHAT_MAX_MESSAGE_LENGTH` | `500` | Max chat message length |
| `CHAT_RATE_LIMIT_PER_MINUTE` | `10` | Chat messages per session per minute |
| `CHAT_CACHE_SIZE` | `50` | Chat reply cache size |
| `CHAT_CACHE_TTL_SECONDS` | `120` | Chat reply cache TTL |
| `GEMINI_API_KEY` | *(empty)* | Gemini API key — read **only** from this env var, never from a file |
| `GEMINI_MODEL` | `gemini-2.0-flash` | Gemini model id |
| `GEMINI_TIMEOUT_MS` | `8000` | Gemini HTTP timeout |

## 5. Run locally

Option A — run from source (embedded Tomcat):

```bash
mvn spring-boot:run
```

Option B — run the built WAR as an executable jar (still embedded Tomcat 9):

```bash
java -jar target\online-shopping-system-1.0.0.war
```

Then open `http://localhost:8080/`.

## 6. First login / admin account

1. Register a customer at `/register` (or `POST /api/auth/register`). Passwords must be ≥ 6 characters.
2. To get an administrator, set `ADMIN_EMAIL` and `ADMIN_PASSWORD` when starting the app. The admin is created once on startup.
3. Sellers: no UI registration path exists — the API accepts an optional `role` field, e.g. `{"name":"...","email":"...","password":"...","role":"SELLER"}`.

Seed catalogue: 5 products (Wireless Mouse, Java Programming Book, Cotton T-Shirt,
Leather Wallet, Ceramic Mug Set).

## 7. Health check

After startup, verify liveness and database connectivity:

```bash
curl http://localhost:8080/api/v1/health
```

Expected: `{"status":"UP","service":"online-shopping-system","database":{"status":"UP","vendor":"H2",...}}`.
A 503 with `status: DOWN` means the database cannot be reached (see TROUBLESHOOTING.md).

## 8. Notes

- Data is stored **only** in the database. With in-memory H2, re-register a test
  user after every restart (tables and seed products are recreated, but users are not).
- Session: HTTP-only cookie, SameSite=Lax, 30-minute timeout.
- No secrets are stored in the repository; the Gemini key is environment-only.