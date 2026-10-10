# Final Capstone Report — RayyanMart

**Project:** Online Shopping & Order Management System with AI shopping assistant
**Build:** `mvn -B clean verify` → **BUILD SUCCESS**, WAR produced at
`target/online-shopping-system.war`
**Date:** 2026-10-09

## 1. What was delivered

- A complete e-commerce platform rendered server-side with **Thymeleaf**
  (Spring Boot 2.7.18, Java 17 target, runs on JDK 25, WAR for Tomcat 9).
- Full role model — `CUSTOMER`, `SELLER`, `ADMIN` — with session/role authorization.
- Product catalogue (search/filter/sort), cart, checkout with stock validation, order
  history with status workflow, seller dashboard, admin dashboard, reviews & ratings.
- **New: AI shopping assistant** — chat widget on every page and a public `POST /api/chat`
  endpoint, with a deterministic product-aware FAQ provider out of the box and an optional
  Google Gemini provider behind a server-only API key.
- **New: security & robustness hardening** — SameSite/http-only cookies, safe redirects,
  no stack-trace leakage, env-overridable DB credentials, per-session chat rate limiting
  (HTTP 429), message-length caps, bounded caching, XSS-safe widget rendering.
- **New: full project documentation** — architecture, ER/database, diagrams, REST API
  reference, deployment guide + E2E checklist, presentation script, .env.example, CI
  workflow, changelog.

## 2. Test results (latest run)

`mvn -B clean verify` — **98 tests, 0 failures, 0 errors, 0 skipped.**

| Test class | Type | Tests |
|------------|------|------:|
| `AuthControllerIntegrationTest` | REST + UI over H2 | 8 |
| `CartControllerIntegrationTest` | REST + UI over H2 | 7 |
| `ChatControllerIntegrationTest` | Chat endpoint over H2 | 7 |
| `ChatServiceTest` | Unit (cache, rate limit, domain, fallback) | 10 |
| `OrderControllerIntegrationTest` | Order placement, stock, cart clear | 9 |
| `OrderStatusIntegrationTest` | Status workflow | 7 |
| `ProductControllerIntegrationTest` | CRUD, ownership, validation | 5 |
| `ProductFilterIntegrationTest` | Search/filter/sort | 9 |
| `ReviewControllerIntegrationTest` | Post/purchase-only/update/delete | 10 |
| `SecurityIntegrationTest` | Role/route protection | 7 |
| `SellerControllerIntegrationTest` | Seller CRUD + scoping | 16 |
| `SellerOrderViewIntegrationTest` | Seller order view | 3 |
| **Total** | | **98** |

Coverage highlights exercised: unauthenticated guards, cross-role denial, review owner
versus outsider, stock decrement on order, cart clearing, chat 400/429 paths, cached
versus fresh replies, provider fallback, off-domain refusal.

## 3. Requirements matrix

| # | Requirement | Status | Evidence |
|---|-------------|--------|----------|
| 1 | Browse & search products | VERIFIED | `/products`, `/api/products` tests |
| 2 | Product detail with reviews | VERIFIED | `ProductControllerIntegrationTest`, page flow |
| 3 | Registration / login / logout | VERIFIED | `AuthControllerIntegrationTest` (8) |
| 4 | Role-based access (CUSTOMER/SELLER/ADMIN) | VERIFIED | `SecurityIntegrationTest` (7), `SellerControllerIntegrationTest` |
| 5 | Shopping cart (add/update/remove/clear) | VERIFIED | `CartControllerIntegrationTest` (7) |
| 6 | Checkout & stock validation | VERIFIED | `OrderControllerIntegrationTest.stockDecreasesAfterSuccessfulOrder` |
| 7 | Order history + status workflow | VERIFIED | `OrderStatusIntegrationTest` (7) |
| 8 | Seller product & stock management | VERIFIED | `SellerControllerIntegrationTest` (16) |
| 9 | Admin dashboard (stats, users, orders, reviews) | VERIFIED | `AdminController` routes + UI manual E2E |
| 10 | Reviews & ratings by verified purchasers | VERIFIED | `ReviewControllerIntegrationTest` (10) |
| 11 | **AI shopping assistant** | VERIFIED | `ChatControllerIntegrationTest` (7) + `ChatServiceTest` (10); widget on all 10 pages |
| 12 | Security hardening & rate limiting | VERIFIED | 429/400 tests, security tests; http-only/SameSite cookies |
| 13 | Documentation & deployment deliverables | VERIFIED | `docs/` (architecture, database, diagrams, API, deployment, report) + CI workflow |
| 14 | Live production deployment | **PENDING** | No remote host/credentials available; WAR + guide + `cloudflared` tunnel helper ready |

## 4. Notable technical decisions

1. **Preserved the intentional working tree.** The committed baseline is an older
   Spring Boot 3.5.16/JS frontend; the working tree holds the Thymeleaf/WAR/Javax refactor.
   All new work was layered onto that tree without rebuilding or deleting features.
2. **WAR + Tomcat 9** (javax) with `spring-boot-starter-tomcat` scoped `provided`, per the
   seeded environment; verified `spring-boot:run` still works locally.
3. **Chat works with zero external dependencies** (mock FAQ provider), while Gemini is
   opt-in and never exposes its key to the client.
4. **H2 for dev/test, MySQL for prod**, switched purely by env vars (`DB_URL`,
   `DB_DRIVER`, credentials). Schema stays Hibernate-managed (`ddl-auto=update`).
5. Fixed-rate USD→₹ display conversion decisions kept at the view layer.

## 5. Known limitations

- CSRF protection is **disabled** for the REST API (existing design; mitigated with
  SameSite=http-only cookies and PRG forms). Re-enabling requires reworking existing tests.
- Live hosting credentials are not available in this environment — deployment remains
  PENDING and fully scripted.
- Chat rate limit and cache are per-JVM in-memory (fine for one node; horizontal scale
  would need a shared store).
- Gemini answers depend on the external API key/service availability; the FAQ provider
  guarantees behaviour regardless.

## 6. How to reproduce

```bash
cd online-shopping-system
mvn -B clean verify     # 98 tests + WAR
mvn -B spring-boot:run  # http://localhost:8080 (seeded with 5 products)
```

See `docs/deployment.md` for MySQL/Tomcat production steps and the full E2E checklist.