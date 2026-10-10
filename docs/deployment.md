# Deployment Guide + E2E Checklist

## Target environment

- **App server:** Apache Tomcat 9.0.x (javax.servlet) — the WAR does not embed Tomcat.
- **JVM:** Java 17+ (tested on JDK 25 community build).
- **Database:** MySQL 8 recommended for prod; H2 (embedded or external TCP) for dev/test.

## Build

```bash
cd online-shopping-system
mvn -B clean verify
```

Produces `target/online-shopping-system.war` and runs the full test suite (98 tests).
The GitHub Actions workflow (`.github/workflows/build.yml`) does the same on Java 17.

## Option A — Apache Tomcat 9 (recommended)

1. Build the WAR (above).
2. Stop Tomcat, drop the WAR into `$CATALINA_BASE/webapps/`:
   ```
   cp target/online-shopping-system.war $CATALINA_BASE/webapps/
   ```
3. Export environment variables before starting Tomcat (see `.env.example`):
   ```
   SERVER_PORT=8080
   DB_URL=jdbc:mysql://localhost:3306/rayyanmart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   DB_USERNAME=rayyanmart
   DB_PASSWORD=change-me
   DB_DRIVER=com.mysql.cj.jdbc.Driver
   CHAT_PROVIDER=mock          # or gemini
   # GEMINI_API_KEY=...        # only when CHAT_PROVIDER=gemini
   ```
4. Start/restart Tomcat, then open `http://<host>:8080/online-shopping-system/`.
   (Root-deployment the app: name the WAR `ROOT.war`.)
5. Sanity check: `GET /api/v1/health` returns `{"status":"UP",...}`.

> The app reads env vars (e.g. `DB_URL`) for overrides. `application.properties`
> already defaults to H2 so a WAR deployed with no tuning still boots for a smoke test;
> switch to MySQL for anything real.

## Option B — Docker (Tomcat 9 + MySQL)

Minimal `docker-compose.yml` pattern:

```yaml
services:
  db:
    image: mysql:8
    environment:
      MYSQL_DATABASE: rayyanmart
      MYSQL_USER: rayyanmart
      MYSQL_PASSWORD: change-me
      MYSQL_ROOT_PASSWORD: root-change-me
    volumes:
      - ./docs/schema.sql:/docker-entrypoint-initdb.d/schema.sql:ro
  app:
    image: tomcat:9-jdk17
    ports: ["8080:8080"]
    environment:
      DB_URL: jdbc:mysql://db:3306/rayyanmart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      DB_USERNAME: rayyanmart
      DB_PASSWORD: change-me
      DB_DRIVER: com.mysql.cj.jdbc.Driver
      CHAT_PROVIDER: mock
    volumes:
      - ./target/online-shopping-system.war:/usr/local/tomcat/webapps/ROOT.war:ro
    depends_on:
      - db
```

> If you pre-provision MySQL with `docs/schema.sql`, set `DB_DDL_AUTO=validate`.

## Live (public) exposure

The repo only ships a local tunnel helper (`cloudflared.exe`, git-ignored) suitable for a
temporary demo URL. There are **no remote host/credentials in this repo** — live publishing
was not performed. To demo publicly:

```bash
cloudflared tunnel --url http://localhost:8080   # gives a trycloudflare.com URL
```

Or deploy to any Tomcat host / PaaS and point DNS at it. Leave a `GEMINI_API_KEY`
unset (mock provider) unless you add one to the target host's secret store.

## E2E checklist (manual smoke test)

### Buyer journey
- [ ] `GET /` renders with nav + cart badge + chat widget.
- [ ] `/products` lists 5 seeded products; search `"wireless"` filters; category and
      price filters narrow the list; sort works.
- [ ] Product detail shows description, ₹ price, rating distribution, review form.
- [ ] Registration validates (bad email, short password, mismatch), then succeeds.
- [ ] Login works; logout redirects to login.
- [ ] Anonymous `/cart` redirects to login with `?redirect=cart`, returns after login.
- [ ] Add a product to cart → badge increments; cart shows line total/total in ₹.
- [ ] Update quantity → total recalculates; remove → line gone; clear → cart empty.
- [ ] Checkout places the order; is redirected to `/orders` with order number flash;
      **product stock decremented**; **cart cleared**.
- [ ] `/orders` shows the new order with status `PENDING`.
- [ ] Rate the purchased product (1–5 + comment); average updates; only purchasers can review.

### Seller journey
- [ ] Create a **SELLER** account; `/seller` opens dashboard.
- [ ] Create product → appears in seller list and public catalogue.
- [ ] Update product; change stock; delete product (removed from catalogue).
- [ ] Seller sees orders containing own products; cannot edit another seller's product.

### Admin journey
- [ ] Create an ADMIN (see `ADMIN_EMAIL`/`ADMIN_PASSWORD` bootstrap or promote in DB);
      `/admin` opens dashboard with stats.
- [ ] Admin can create/edit/delete any product; update any order's status
      (`PENDING → … → DELIVERED`); view users; delete any review.

### AI shopping assistant
- [ ] Chat bubble on every page; open, ask "hello" → greeting.
- [ ] "show available products" → lists catalogue (Wireless Mouse … Ceramic Mug Set).
- [ ] "how much is the Java book?" / "what is the price?" → ₹ price answer.
- [ ] "how do I place an order?" → step-by-step. "delivery", "returns", "payment" → answers.
- [ ] Product-specific: "tell me about the Leather Wallet" → description + stock.
- [ ] Off-topic: "write me poetry" → politely refused.
- [ ] Send an 500+ char message (or, in tests, 50+) → 400. Fire 10 messages in a minute →
      429 on the 11th (with default settings).

### Security spot-checks
- [ ] Not-logged-in user cannot reach `/api/seller/**`, `/api/admin/**`, own orders of others.
- [ ] Logged-in roles can't cross roles (customer → seller page shows access denied).
- [ ] Responser UI shows no stack traces (400/500 pages generic).
- [ ] Chat widget must not contain `GEMINI_API_KEY` in page source.

## Rollback

- Keep the previous `.war`; Tomcat `manager` can re-deploy. Non-DDL schema changes come
  from Hibernate `update`; back up the DB first (`mysqldump`) before upgrading.

## Troubleshooting

| Symptom | Likely cause / fix |
|---------|--------------------|
| `Could not connect to H2` on run | No external H2 server; use embedded URL: `DB_URL=jdbc:h2:mem:shopping_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE` |
| `Table 'rayyanmart' doesn't exist` | Create DB; or point `DB_URL` at a valid database |
| 429 immediately on chat | `CHAT_RATE_LIMIT_PER_MINUTE` too low for the demo; raise it |
| Gemini replies look like mock | `CHAT_PROVIDER=gemini` not set, or `GEMINI_API_KEY` blank on the server |
| WAR 404s at `/` | Deploy as `ROOT.war` or use context path `/online-shopping-system/` |