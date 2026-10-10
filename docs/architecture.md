# Architecture

## Layer overview

```
Browser (Thymeleaf pages + chat widget)
        │
        ▼
 ┌─────────────── Tomcat 9 ───────────────┐
 │ AuthFilter ── session & role guard     │
 │ WebController  (GET pages / form POST)  │
 │ *Controller REST   (JSON API /api/**)   │
 │ GlobalExceptionHandler (400/403/404/429)│
 │        │                                │
 │        ▼                                │
 │ Service layer  (Auth/Product/Cart/      │
 │   Order/Seller/Admin/Review/Chat)       │
 │        │                                │
 │        ▼                                │
 │ Repository layer  (Spring Data JPA)     │
 │        │                                │
 │        ▼                                │
 │ JPA/Hibernate ──► H2 (HTTP/TCP) or MySQL│
 └─────────────────────────────────────────┘
```

- **Controller → Service → Repository** separation; services own transactions and rules.
- **`AuthFilter`** (javax `Filter` via Spring Boot auto-registration) runs before MVC,
  whitelisting public paths (`/`, `/login`, `/register`, `/api/v1/health`, `/api/chat`,
  static assets) and guarding role/ownership checks for the rest.
- **`GlobalExceptionHandler`** maps domain exceptions to clean HTTP statuses and JSON
  bodies; `server.error.include-stacktrace=never` hides internals.
- All DTOs are immutable `record`s; responses are plain JSON (REST) or Thymeleaf models.

## Request flows

### Browse → cart → order (buyer)
1. `GET /products` → `WebController` → `ProductService.search(...)` → list page.
2. `POST /cart/add` (logged-in only) → `CartService.add` (lazy cart creation, line
   dedupe, price snapshot from product).
3. `GET /cart` → `CartService.getCart` → totals computed server-side.
4. `POST /checkout/place` → `OrderService.placeOrder` → within a transaction:
   validate auth → load cart → **validate & decrement stock** for every line →
   build `Order` + `OrderItem` snapshots → persist → **clear cart** → return order.
5. `GET /orders` shows the buyer's order history; admin can advance status.

### Seller & admin
- Seller: POST/PUT/DELETE `/api/seller/products...` (or `/seller/...` forms) restricted
  to `ROLE_SELLER`; every product is scoped to `product.seller`.
- Admin: `GET /api/admin/orders|users|reviews`, `PATCH /api/admin/orders/{id}/status`,
  `DELETE /api/admin/reviews/{id}` — `ROLE_ADMIN` only.

## AI Shopping Assistant pipeline

```
chat.js (widget) ──POST /api/chat──► ChatController
        │
        ▼
   ChatService.answer(message, session)
        │ 1. validate: blank/length > max  → 400
        │ 2. rate limit (per-session window) → 429
        │ 3. cache lookup (key = session+msg, TTL/size) → hit? return cached
        │ 4. ChatDomainGuard — refuse off-topic keyword → domain response
        │ 5. active provider (mock | gemini) builds answer
        │    └ GeminiChatProvider: RestTemplate, system-prompt domain lock,
        │      timeouts, JSON extraction; falls back to FAQ provider on failure
        │ 6. store in cache → ChatResponse(reply, provider, cached=false)
```

- **`FAQChatProvider`** — regex intents (greeting, prices, categories, products, stock,
  ordering, delivery, returns, payment, reviews, seller, account, contact) plus a
  product-name matcher that reads `ProductRepository` live data. No network, always works.
- **`GeminiChatProvider`** — optional; uses `GEMINI_API_KEY` from environment only.
  A system prompt restricts the assistant to the RayyanMart domain; a dangerous/off-topic
  gate reinforces scope.
- **`ChatDomainGuard`** — keyword filter refusing competitive/personal-harm/off-domain
  topics so mock and Gemini both stay on-brand.
- **Rate limiting** — token-ish sliding window stored per `HttpSession`; 11th message in a
  minute returns 429 `{"error":"...slow down..."}`.
- **Cache** — bounded per-session map (`app.chat.cache.size`, TTL default 120s) avoids
  repeat work; caching is **off** for the test profile? No — on; tests verify cache hit via
  `cached=true` response flag.
- **UI safety** — widget renders replies with `textContent` (no HTML injection) and never
  receives the Gemini key.

## Security model

| Concern | Mechanism |
|---------|-----------|
| Passwords | Spring Security `BCryptPasswordEncoder` (never stored in plaintext) |
| Sessions | Server-side `HttpSession`, cookie `HttpOnly` + `SameSite=lax`, cookie-only tracking |
| Authorization | `AuthFilter` role checks + service-level ownership checks |
| CSRF | Disabled (stateless JSON APIs + session-cookie mitigations); do not re-enable without revisiting all existing tests |
| Injection | JPA parameter binding everywhere; Hibernate DDL prevents raw SQL |
| XSS | Thymeleaf/Th:text HTML-escapes all model output; chat widget uses `textContent` |
| Secrets | No keys in browser or tracked files; `GEMINI_API_KEY`, `DB_PASSWORD` env-only |
| Rate limiting | Session bucket for `/api/chat` (429), max message length |
| Error leakage | `include-stacktrace=never`, safe-path redirect validation |

## Technology decisions (rationale)

- **WAR + provided Tomcat 9** — matches the seeded environment (`javax.servlet`),
  keeps `spring-boot:run` working locally by pulling the provided starter-tomcat at build
  time inside the IDE/CLI.
- **Thymeleaf SSR** — replaces the former JS frontend; each page and form post is a single
  request/response, which keeps the capstone demo simple and testable with MockMvc.
- **Spring Boot 2.7.18 / Java 17 target** — javax-based API surface, verified to run on the
  available JDK 25 runtime (community build).
- **H2 by default, MySQL optional** — zero-config local runs/tests; prod switches via env vars.
- **Two providers for chat** — deterministic mock keeps the feature demoable *and*
  fully automated-testable with no external dependency; Gemini adds the "AI" without risk.

## Test strategy

- MockMvc integration tests (spring-boot-starter-test) against embedded H2 — 88 tests
  covering auth, products, filters, cart, orders, order-status, reviews, security, seller,
  seller-orders and chat endpoints.
- 10 focused unit tests for `ChatService` (validation, rate limit, caching, provider
  fallback, domain refusal).
- See [final-report.md](final-report.md) for the requirements matrix.