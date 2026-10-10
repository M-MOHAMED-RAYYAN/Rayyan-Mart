# TEST_PLAN.md — RayyanMart Online Shopping System

## 1. Scope and strategy

- **Automated (already in the repo)**: JUnit 5 integration tests using
  `@SpringBootTest` + MockMvc or `TestRestTemplate` against an isolated
  in-memory H2 (`jdbc:h2:mem:shopping_db`, MySQL mode) — no project DB is touched.
- **Manual (proposed)**: browser-driven checks for the server-rendered UI and the
  admin/seller dashboards. These are **suggested** here; see MANUAL_TEST_CASES.md.
- Excluded from tests: real payments (none exist), destructive DB operations,
  deletion of real user data, and anything requiring external accounts.

## 2. Test levels

| Level | Tooling | Target |
|---|---|---|
| Unit | JUnit 5 (`ChatServiceTest`) | Chat logic, rate limit, FAQ matching |
| Integration | `@SpringBootTest` + MockMvc | Controllers/endpoints + security + DB round-trips |
| Manual (suggested) | Browser | Thymeleaf pages, dashboards, chat widget UX |

## 3. Automated test inventory (existing suite)

| Test class | Coverage |
|---|---|
| `AuthControllerIntegrationTest` | register, duplicate email → 409, login, logout, `/me`, invalid credentials |
| `ProductControllerIntegrationTest` | list/detail/categories, create/update/delete (authorization) |
| `ProductFilterIntegrationTest` | search, category, price range, availability, sort |
| `CartControllerIntegrationTest` | add, update, remove, clear, stock validation |
| `OrderControllerIntegrationTest` | place order, order history, owner-only detail, empty cart → 400 |
| `OrderStatusIntegrationTest` | allowed transitions, terminal-state guards |
| `ReviewControllerIntegrationTest` | create/update/delete, duplicate → 409, ratings summary |
| `SellerControllerIntegrationTest` | dashboard, product CRUD scoping, stock |
| `SellerOrderViewIntegrationTest` | seller sees only own-product orders |
| `SecurityIntegrationTest` | role gates (401/403), session behaviour |
| `ChatControllerIntegrationTest` | chat endpoint, validation, rate limit |
| `ChatServiceTest` (unit) | FAQ intents, payment-answer accuracy, cache, rate limit |

## 4. Test execution

```bash
mvn test          # full suite
mvn -B test       # batch/non-interactive (CI-friendly), prints BUILD SUCCESS/FAILURE
```

Why trust the suite: each integration test runs in an isolated in-memory H2 with
`data.sql` seeding, so runs are repeatable and do not touch production data.

## 5. Environments

- **Dev/CI**: Maven with JDK 17+; in-memory H2 (test profile) — 2–3 minutes.
- **QA/UAT**: same WAR on Tomcat 9 with persistent H2 TCP or MySQL; re-run the
  suite, then run the manual cases.

## 6. Suggested manual test passes (see MANUAL_TEST_CASES.md)

1. Smoke: home loads, catalogue shows 5 seed products.
2. Auth: register → login → logout; duplicate email error.
3. Catalogue: search/filter/sort; product detail.
4. Reviews: create/edit/delete; duplicate blocked; average rating updates.
5. Cart & checkout: add → update → remove → clear → place order → order history.
6. Seller flow: register SELLER via API, create product, update stock, check orders.
7. Admin flow: start with `ADMIN_EMAIL/PASSWORD`, dashboard, order status updates, review moderation.
8. PWA/UX: chat widget opens/closes only when logged in; payment question answer is accurate.
9. Health & deploy checks: `/api/v1/health`, WAR on Tomcat 9 (DEPLOYMENT_GUIDE.md).

## 7. Exit criteria

- 100% of automated tests pass (no test-file modifications to force this).
- Manual cases with clear PASS/FAIL recorded; failures triaged as defect or doc gap.
- No payments, no data destruction, no external accounts during testing.