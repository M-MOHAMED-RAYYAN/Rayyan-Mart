# TEST_RESULTS.md — RayyanMart Online Shopping System

## Automated suite — results

Command run on this machine (JDK 25, Maven 3.9.x):

```bash
mvn -B test
```

Isolated in-memory H2 (test profile, MySQL mode); `data.sql` seeds from the test
classpath. **No project database, no external account, no payment was touched.**

### Summary

```
Tests run: 99, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Per class

| Test class | Tests | Failures | Errors |
|---|---|---|---|
| AuthControllerIntegrationTest | 8 | 0 | 0 |
| CartControllerIntegrationTest | 7 | 0 | 0 |
| ChatControllerIntegrationTest | 7 | 0 | 0 |
| ChatServiceTest (unit) | 11 | 0 | 0 |
| OrderControllerIntegrationTest | 9 | 0 | 0 |
| OrderStatusIntegrationTest | 7 | 0 | 0 |
| ProductControllerIntegrationTest | 5 | 0 | 0 |
| ProductFilterIntegrationTest | 9 | 0 | 0 |
| ReviewControllerIntegrationTest | 10 | 0 | 0 |
| SecurityIntegrationTest | 7 | 0 | 0 |
| SellerControllerIntegrationTest | 16 | 0 | 0 |
| SellerOrderViewIntegrationTest | 3 | 0 | 0 |
| **Total** | **99** | **0** | **0** |

> No test file was created or modified for this documentation task.

## What the passing suite covers

- Auth: register, duplicate email (409), login success/failure, logout, `/me`.
- Products: listing, detail, categories, filters (search/category/price/availability), sorting.
- Cart: add, update, remove, clear, stock-aware quantity validation.
- Orders: placement (PENDING), history, owner-only detail, empty-cart rejection (400).
- Order status: allowed transitions enforced; terminal states guarded.
- Reviews: create/update/delete, duplicate-per-product (409), rating aggregation.
- Seller: dashboard, own-product CRUD + stock, seller-only order view.
- Security: 401/403 gating; session behaviour.
- Chat: endpoint behaviour, message validation, rate limit, FAQ accuracy (incl. the
  payment question answering that **no money is transferred**).

## NOT covered by the automated suite

| Area | Why |
|---|---|
| AdminController endpoints | No dedicated admin integration tests exist in the repo |
| Browser UX (Thymeleaf pages, chat widget) | Automated suite is HTTP/JSON; see MANUAL_TEST_CASES.md (suggested) |
| Gemini chat provider | Requires an external API key/account |
| Deployment on Tomcat 9 with persistent DB | Requires a deployed environment |
| Payments | Feature does not exist in the codebase |

## Live inspection performed earlier in this cycle (informational)

A locally running instance was spot-checked (read-only, HTTP level):
`GET /api/v1/health` → `200`, database `UP`; `GET /api/products` returned the 5
seed products; chat widget rendered only for logged-in users. These are manual
observations, not part of the automated suite.