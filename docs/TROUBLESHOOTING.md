# TROUBLESHOOTING.md — RayyanMart Online Shopping System

Common issues based on the actual configuration (`application.properties`,
`data.sql`, test config) and observed startup behaviour.

---

## 1. Application will not start

### "Failed to connect" to `jdbc:h2:tcp://localhost:9092/...`
The default configuration expects an **H2 TCP server already running** on port 9092.

- Start the H2 server first (see SETUP_GUIDE.md §3b), **or**
- switch to in-memory H2 for local work:
  ```
  DB_URL=jdbc:h2:mem:shopping_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
  DB_USERNAME=sa
  DB_PASSWORD=sa
  ```
- Port conflict: ensure nothing else binds 9092 (`netstat -ano | findstr 9092`).

### Port 8080 already in use
Set a different port: `SERVER_PORT=9090`. Check current usage with
`netstat -ano | findstr 8080` and inspect the PID.

### "Unable to resolve DRIVER" / wrong DB_URL
`DB_DRIVER` defaults to `org.h2.Driver`. If you point `DB_URL` at MySQL and do not
set `DB_DRIVER`, HikariCP auto-detects it from the URL. If auto-detection is
skipped, set `DB_DRIVER=com.mysql.cj.jdbc.Driver`.

### Java/Maven version errors
- Compiles at `java.version=17`; Maven 3.8+ required (Maven 3.9 verified).
- A `ClassNotFoundException` for `javax.servlet.*` at runtime on plain Tomcat
  means the WAR is deployed on the wrong container — embedded Tomcat 9 is
  `provided`, so **Tomcat 9.0.x is required** (Tomcat 10 uses `jakarta.*`).

---

## 2. Database problems

### Empty product list at startup
Seed runs through `spring.sql.init` with `continue-on-error=true`. If `data.sql`
fails silently, tables exist but no products. Check the startup log for SQL
errors. Verify with `/api/v1/health` then `GET /api/products` (expect the 5 seed
products).

### Data missing after restart (in-memory H2)
Expected: `mem:` databases are wiped on shutdown. Use the persistent TCP mode
(§3b) for data that must survive restarts.

### "Unique constraint violated" for products ID 1–5
`data.sql` uses `INSERT IGNORE`, so a pre-existing table with the same IDs is safe.
If the products table was created without `insert ignore`-style semantics on a
non-MySQL dialect, temporary duplicates can occur; restart cleanly with a fresh DB.

### Credentials / ownership issues (file-based H2)
The H2 file owner account is taken from the **H2 server** process at first connect.
Changing `DB_USERNAME`/`DB_PASSWORD` afterwards can cause auth failures — keep the
owner credentials stable once a database file exists.

---

## 3. Login / authentication issues

### "Invalid email or password"
- Email matching is lowercased and trimmed in `AuthService`. Try registering again.
- Passwords are BCrypt-hashed; there is no plaintext recovery.

### Registering an existing email → 409 Conflict
`DuplicateEmailException` maps to HTTP 409. Use a different email.

### Login appears to work but pages still redirect to `/login`
Session uses an HTTP-only cookie with a 30-minute timeout. If you are resetting
`userId`/`role` session attributes elsewhere or the server restarted, log in again.
In-memory databases also lose users — re-register after a restart.

### "Seller access required" / "Admin access required" (403)
- Sellers go to `/seller`, admins to `/admin`. A `CUSTOMER` gets 403.
- Sellers and admins are **not** interchangeable; role checks are strict
  (`SessionService.getCurrentSeller` / `getCurrentAdmin`).

### No admin account exists
The admin is created on startup **only** when `ADMIN_EMAIL` + `ADMIN_PASSWORD`
are set. If both are blank, no admin exists. Set them and restart.

---

## 4. Checkout / order issues

### "Your cart is empty" when placing an order
`EmptyCartException` → 400. The cart must contain at least one line item.

### "Insufficient stock" (409)
Quantity at order time exceeds `products.quantity`. Reduce the quantity in the
cart — stock is validated again at checkout (not just at add-to-cart).

### Double order created after double-clicking "Place Order"
Known duplicate-submission risk: `POST /checkout/place` is not idempotent and there
is no CSRF token. A duplicated request can create two orders. See FEATURE_STATUS.md.

### Cannot change/delete an order
Orders are immutable after creation; only the status changes, through the allowed
workflow (admin/seller). Customers cannot cancel.

---

## 5. Shopping assistant (chat) issues

### Chat button is not visible
The widget only renders for **logged-in** users (`navUser != null`). Log in first
(home, products, checkout pages all include it).

### "Too many requests" (429)
Default rate limit is 10 messages per session per minute
(`CHAT_RATE_LIMIT_PER_MINUTE`). Wait, or raise the limit.

### Chat replies are generic or empty
Default provider is `mock` (FAQ). For real model answers set
`CHAT_PROVIDER=gemini` **and** export `GEMINI_API_KEY` (read from the environment
only) *before* starting the app. Timeout is 8 s (`GEMINI_TIMEOUT_MS`). Chat is
subject to `CHAT_MAX_MESSAGE_LENGTH` (default 500) — longer messages are rejected.

---

## 6. Deployment-time failures (Tomcat)

See DEPLOYMENT_GUIDE.md §"Common deployment failures".

---

## Fast diagnostic checklist

1. `GET /api/v1/health` → 200 with `database.status: UP`?  (if DOWN → DB issue, see §1/§2)
2. `GET /api/products` → lists JSON?  (if network error → app not fully started)
3. Log in, then `/cart` → accessible?  (if redirected → session/auth issue, §3)
4. Logged-in, chat widget on `/products` → visible?  (if not → §5)