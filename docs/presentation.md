# Presentation & Live Demo Script

Suggested length: ~10 minutes. Slides first, then live demo.

---

## 1. Intro (1 min)

> "This capstone delivers **RayyanMart**, a full e-commerce web application — buyers,
> sellers and admins on one platform — plus an **AI shopping assistant** built into every
> page. It's a Spring Boot monolith rendered server-side with Thymeleaf, packaged as a WAR
> and deployable to Apache Tomcat 9."

## 2. Problem & goals (1 min)

- Retailers need Browse → Cart → Order → Manage products → Support, all with role control.
- Capstone goals: functional shopping flow, three actor roles, clean 3-layer architecture,
  an AI chatbot for support, and solid automated tests.

## 3. Architecture (2 min)

Reference `docs/architecture.md`.

- **Layers:** Controllers (REST + `WebController`) → Services → Spring Data JPA → DB (H2/MySQL).
- **Roles:** `CUSTOMER`, `SELLER`, `ADMIN`, enforced by `AuthFilter` + service checks.
- **Frontend:** Thymeleaf server-side rendering — each page/form is one request/response.
- **Chat pipeline:** `ChatController → ChatService`, validation → per-session rate limit →
  cache → domain guard → provider (`mock` FAQ or Gemini) → JSON back to widget.

## 4. Key implementation highlights (2 min)

- Order placement = transaction, stock validated & decremented, cart cleared.
- Chat: two providers, zero config (mock) vs optional Gemini; no API key in the browser;
  HTTP 429 past 10 msgs/min; XSS-safe (textContent).
- Security: bcrypt, http-only + SameSite cookies, safe redirects, no stack-trace leakage.
- Testing: **98 automated tests**, all green (`mvn -B clean verify`).

## 5. Live demo (4 min)

Start with `mvn -B spring-boot:run`, open `http://localhost:8080`.

1. **Buyer path (2 min)**
   - Home page → chat widget bottom-right (talk about it now).
   - Products: search "wireless", apply category filter, open product, see ₹ price + reviews.
   - Register a customer, log in, add product to cart, update quantity, checkout,
     note the order number, show `/orders` PENDING status.
2. **Chat assistant (1 min)** — ask:
   - "hello", "show available products", "how much is the Java book?",
     "how do I place an order?", "write me poetry" (refused politely).
3. **Seller path (45 s)** — register/login as seller, create a product, update its stock.
4. **Admin path (45 s)** — sign in as admin (`ADMIN_EMAIL`/`ADMIN_PASSWORD` bootstrap),
   show dashboard stats, move an order to SHIPPED, delete an offending review.

## 6. Quality & results (30 s)

- `mvn -B clean verify`: **BUILD SUCCESS, 98 tests, 0 failures.**
- DB spread: cart/order/review ownership, stock, roles, chat 400/429 — all covered.

## 7. Demo command cheat-sheet

```bash
# local run (H2 embedded, 5 seeded products)
mvn -B spring-boot:run --args="" # or: DB_URL=jdbc:h2:mem:...;MODE=MySQL mvn -B spring-boot:run

# admin bootstrap (local) — create/promote an ADMIN account on startup
ADMIN_EMAIL=admin@rayyanmart.com ADMIN_PASSWORD=ChangeMe123! mvn -B spring-boot:run

# full build + tests → target/online-shopping-system.war
mvn -B clean verify

# optional Gemini chat
set CHAT_PROVIDER=gemini
set GEMINI_API_KEY=<server-side secret>
mvn -B spring-boot:run
```

## 8. Q&A notes

- Why no SPA? Demonstrates clean server-rendered MVC and keeps the demo/test surface small.
- Why mock chat default? Feature must work with no external key and be fully testable.
- How is stock protected? Validated inside the order transaction; tests pin the behaviour.
- What's left? Production hosting (no credentials at hand) — everything else is scripted.