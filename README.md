# RayyanMart — Online Shopping & Order Management System

A server-side rendered e-commerce platform (Spring Boot + Thymeleaf) with a built-in
AI shopping assistant. Buyers browse a product catalogue, manage a cart and place orders;
sellers manage their products and stock; administrators oversee users, orders and reviews.

The application is packaged as a **WAR** and runs on **Apache Tomcat 9.0.x**
(javax.servlet-based). It runs everywhere targets Java 17+, including a plain
community JDK 25 runtime.

---

## Features

- **Public catalogue** — search, category / price filter, availability and sortable listing.
- **Authentication & roles** — `CUSTOMER`, `SELLER`, `ADMIN` with bcrypt-hashed passwords
  and session-based filtering (`AuthFilter`).
- **Shopping cart** — add / update / remove / clear, server-side totals.
- **Orders** — place an order (validates stock), per-user order history, status flow
  `PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED`, admin status updates.
- **Seller dashboard** — CRUD on own products, stock updates, order overview.
- **Admin dashboard** — global stats, manage any product / order / user / review.
- **Reviews & ratings** — verified purchasers can rate and review; average + distribution
  shown on product pages.
- **AI Shopping Assistant (chatbot)** — a chat widget on every page. Out of the box it uses a
  deterministic, product-aware FAQ provider; it can be switched to **Google Gemini** with an env var.
    - Per-session rate limiting (default 10 req/min), message length cap, caching,
      prompt domain restriction and out-of-scope refusal.
- **Health endpoint** — `GET /api/v1/health` verifies the application and database.

## Tech stack

| Layer      | Technology |
|------------|------------|
| Language   | Java 17 (bytecode target), runs on JDK 17–25 |
| Framework  | Spring Boot 2.7.18 |
| Rendering  | Thymeleaf (server-side, no client-side framework) |
| Persistence| Spring Data JPA / Hibernate 5, H2 (dev/test) or MySQL (prod) |
| Security   | Spring Security (bcrypt + `AuthFilter`), `SameSite=lax`, http-only cookies |
| Build      | Maven, WAR packaging for Apache Tomcat 9.0.x |
| Tests      | JUnit 5 (Spring Boot Test / MockMvc) + a few unit tests around embedded H2 |

## Project layout

```
src/main/java/com/shopping/system/
  controller/   REST endpoints (auth, products, cart, orders, reviews, seller, admin, chat, health)
  web/          WebController (Thymeleaf pages + form posts) and Ui view helpers
  service/      business logic (auth, product, cart, order, seller, admin, chat, ...)
  repository/   Spring Data JPA repositories
  model/        JPA entities (User, Role, Product, Cart, CartItem, Order, OrderItem, Review, ...)
  filter/       AuthFilter (session + role guard), ChatRateLimit metadata
  exception/    GlobalExceptionHandler, ChatRateLimitException
  dto/          request / response records

src/main/resources/
  templates/    Thymeleaf pages (index, products, product, cart, checkout, orders, seller, admin, login, register)
  fragments.html  shared layout + chat widget fragment
  static/       js/chat.js, css/style.css
  application.properties
  data.sql      seed products (idempotent)

docs/           architecture, diagrams, database script, API reference, deployment guide, reports
```

## Quick start (local, zero-config)

Requirements: **JDK 17+** and **Maven 3.8+** (no database setup — H2 is bundled).

```bash
mvn -B clean verify     # builds the WAR and runs the full test suite (98 tests)
mvn -B spring-boot:run  # starts https://localhost:8080
```

Open **http://localhost:8080** — the shop is seeded with 5 products. Register an account,
or log in after registration. To get an `ADMIN` or `SELLER` account locally, restart with:

```bash
mvn -B spring-boot:run \
  -Dspring-boot.run.arguments="--admin.email=admin@rayyanmart.com --admin.password=ChangeMe123!"
```

then assign the role in the database, or register a Seller directly from the Register page.

> Tip: the app binds to port `8080` by default per `SERVER_PORT`. See `.env.example`
> for every configurable setting.

## Running on a real database (MySQL)

```bash
set DB_URL=jdbc:mysql://localhost:3306/rayyanmart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
set DB_USERNAME=root
set DB_PASSWORD=secret
set DB_DRIVER=com.mysql.cj.jdbc.Driver
mvn -B spring-boot:run
```

Production (WAR → Tomcat 9): copy `target/online-shopping-system.war` to
`$CATALINA_BASE/webapps/`. Set the same `DB_*` env vars in the service environment.
Full instructions in [docs/deployment.md](docs/deployment.md).

## The AI shopping assistant

- Post `{ "message": "..." }` to **`POST /api/chat`** or click the chat bubble on any page.
- Default provider is `mock` (deterministic FAQ + live product data). No key required.
- Enable Gemini: set `GEMINI_API_KEY` (server-side only — never sent to the browser)
  and `CHAT_PROVIDER=gemini`. See `.env.example`.
- Responds to greetings, prices, categories, stock, order/delivery/returns/payment/how-to,
  seller onboarding and product-specific questions; refuses off-topic questions.
- Hardened: rate limited per session (429 on overload), max message length, cached answers,
  HTML-escaped output in the widget.

## Test suite

```bash
mvn -B test        # runs all tests against embedded H2
mvn -B clean verify  # full build + tests, produces target/online-shopping-system.war
```

**98 tests, all green** across 12 classes: auth, cart, orders, order status, product &
filters, reviews, security, seller, seller orders, chat (controller + service unit).

## Documentation

| Doc | What it covers |
|-----|----------------|
| [docs/architecture.md](docs/architecture.md) | Layers, request flow, chat pipeline, security |
| [docs/database.md](docs/database.md) | ER diagram + full `schema.sql` |
| [docs/diagrams.md](docs/diagrams.md) | Use-case and sequence diagrams (Mermaid) |
| [docs/api.md](docs/api.md) | REST API reference |
| [docs/deployment.md](docs/deployment.md) | WAR/Tomcat & MySQL/docker guide + E2E checklist |
| [docs/final-report.md](docs/final-report.md) | Final capstone report & requirements matrix |
| [docs/presentation.md](docs/presentation.md) | Presentation / live-demo script |

## License

Educational capstone project. See [CHANGELOG.md](CHANGELOG.md) for revision history.