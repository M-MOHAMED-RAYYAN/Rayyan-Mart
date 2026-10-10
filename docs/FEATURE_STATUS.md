# FEATURE_STATUS.md — RayyanMart Online Shopping System

Status legend:

- **VERIFIED** — behaviour confirmed by the automated test suite and/or direct live inspection in this project cycle.
- **PRESENT BUT UNTESTED** — code exists and is wired up, but no automated test and no manual browser verification was recorded.
- **NOT VERIFIED** — present in code, not exercised during this cycle.
- **NOT IMPLEMENTED** — intentionally absent from the codebase (documented for transparency).

Verify each claim against `src/main`; nothing below claims behaviour that is not in the code.

---

## Accounts & sessions

| Feature | Status | Evidence |
|---|---|---|
| Register (web `/register` + API `/api/auth/register`) | VERIFIED | AuthControllerIntegrationTest, live curl earlier |
| Duplicate email rejection (409) | VERIFIED | AuthControllerIntegrationTest |
| Login / logout (session cookie) | VERIFIED | AuthControllerIntegrationTest, SecurityIntegrationTest |
| `/api/auth/me` | VERIFIED | AuthControllerIntegrationTest |
| Password BCrypt hashing | VERIFIED | AuthService code; SecurityIntegrationTest |
| Registration as CUSTOMER or SELLER (API), ADMIN forbidden | VERIFIED | AuthService.resolveRole + tests |
| Admin bootstrap via `ADMIN_EMAIL`/`ADMIN_PASSWORD` | PRESENT BUT UNTESTED | AdminInitializer code; not covered by suite |
| Session timeout 30m / HTTP-only SameSite=Lax cookie | PRESENT BUT UNTESTED | `application.properties` |

## Catalogue

| Feature | Status | Evidence |
|---|---|---|
| List products (search, category, price range, availability, sort) | VERIFIED | ProductFilterIntegrationTest, ProductControllerIntegrationTest |
| Product detail | VERIFIED | ProductControllerIntegrationTest |
| Category list | VERIFIED | ProductControllerIntegrationTest (categories) |
| Seed 5 products via `data.sql` | VERIFIED | data.sql; live listing |

## Reviews & ratings

| Feature | Status | Evidence |
|---|---|---|
| Create review (logged-in, 5-star) | VERIFIED | ReviewControllerIntegrationTest |
| One review per user per product (unique) → 409 | VERIFIED | ReviewControllerIntegrationTest |
| Update own review | VERIFIED | ReviewControllerIntegrationTest |
| Delete own review | VERIFIED | ReviewControllerIntegrationTest |
| Average rating + star rendering | VERIFIED | RatingSummary code; UI (Ui.stars) |

## Cart & checkout

| Feature | Status | Evidence |
|---|---|---|
| One cart per user (created at registration) | VERIFIED | AuthService; CartControllerIntegrationTest |
| Add / update qty / remove / clear (REST + web) | VERIFIED | CartControllerIntegrationTest, CartService |
| Quantity validation (stock-aware) | VERIFIED | CartService; InsufficientStockException |
| Cart empty → 400 `EmptyCartException` | VERIFIED | CartService; Order flow tests |
| Server-recomputed order total from DB prices | VERIFIED | OrderControllerIntegrationTest |
| Place order: PENDING, stock decrement, cart clear | VERIFIED | OrderControllerIntegrationTest |
| Order history / detail (owner only) | VERIFIED | OrderControllerIntegrationTest |
| **Payment processing** | **NOT IMPLEMENTED** | No payment code, entity, table, or gateway anywhere |
| Duplicate-submit protection / idempotency on checkout | NOT IMPLEMENTED | Both `POST /checkout/place` and `POST /api/orders` have no token |
| CSRF token on checkout form | NOT IMPLEMENTED | `SecurityConfig` disables CSRF |

## Order status workflow

| Feature | Status | Evidence |
|---|---|---|
| Allowed transitions enforced (PENDING→…→DELIVERED, or CANCELLED) | VERIFIED | OrderStatusIntegrationTest |
| Terminal states guarded | VERIFIED | OrderStatusIntegrationTest |

## Seller area

| Feature | Status | Evidence |
|---|---|---|
| Dashboard (own products + orders) | VERIFIED | SellerControllerIntegrationTest |
| Product CRUD scoped to own `seller_id` | VERIFIED | SellerControllerIntegrationTest |
| Stock update | VERIFIED | SellerControllerIntegrationTest |
| Order book limited to seller's products | VERIFIED | SellerOrderViewIntegrationTest |
| Seller-only access (403 for others) | VERIFIED | SecurityIntegrationTest, SessionService |

## Administration

| Feature | Status | Evidence |
|---|---|---|
| Global dashboard stats | PRESENT BUT UNTESTED | AdminService/AdminController exist; not covered |
| All orders + status update (any order) | PRESENT BUT UNTESTED | AdminController; no dedicated admin test |
| Global product CRUD | PRESENT BUT UNTESTED | AdminController |
| User list | PRESENT BUT UNTESTED | AdminController |
| Review list + delete any review | PRESENT BUT UNTESTED | AdminController |
| Admin-only access | VERIFIED | SecurityIntegrationTest |

## Shopping assistant (chat)

| Feature | Status | Evidence |
|---|---|---|
| `POST /api/chat` FAQ (mock provider) | VERIFIED | ChatControllerIntegrationTest, ChatServiceTest (11) |
| Rate limit per session (429) | VERIFIED | ChatServiceTest |
| Max message length validation | VERIFIED | ChatControllerIntegrationTest |
| Widget visible only when logged in | VERIFIED | Live inspection (logged-in vs anonymous) |
| Gemini provider (env API key) | NOT VERIFIED | Requires external key; provider code exists, unevailable in test env |

## Infrastructure

| Feature | Status | Evidence |
|---|---|---|
| `/api/v1/health` liveness + DB probe | VERIFIED | Live inspection (status UP, db UP) |
| Standard error envelope `ApiError` + status mapping | VERIFIED | GlobalExceptionHandler; error-path tests |

## Known open items (future, require source changes)

1. Real payment or COD flow (currently absent — see PROJECT_OVERVIEW.md §4).
2. Idempotency token / CSRF guard on order placement.
3. Automated tests for the AdminController.
4. UI path to register sellers (API-only today).
5. Mail/notifications, order cancellation by customer, wishlist, paging of large catalogues — all absent today.