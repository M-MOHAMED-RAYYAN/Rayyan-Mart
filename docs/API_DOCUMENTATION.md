# API_DOCUMENTATION.md — RayyanMart Online Shopping System

REST API reference, verified against the controllers. Base URL: `http://localhost:8080`
(change with `SERVER_PORT`). All endpoints are JSON (`application/json`).

**Authentication**: session-based. Call `POST /api/auth/login` first; the
`JSESSIONID` cookie (HTTP-only) is then sent automatically by the client.
Role/session enforcement happens in controllers via `SessionService`
(`getCurrentUser`/`getCurrentSeller`/`getCurrentAdmin`).

**Error envelope**: every non-2xx response is an `ApiError`:
```json
{ "timestamp": "2026-10-09T00:00:00.000", "status": 400,
  "error": "Bad Request", "message": "<human-readable reason>" }
```
Common HTTP codes: 400 validation/empty-cart/illegal status · 401 not logged in ·
403 wrong role · 404 not found · 405 wrong method · 409 duplicate email / duplicate
review / stock conflict / data integrity · 429 chat rate limit · 500 unexpected.

> **Order placement (`POST /api/orders`) creates a `PENDING` order. No payment
> happens.**

---

## Auth (`/api/auth`)

### `POST /api/auth/register` — register (CUSTOMER or SELLER)
Request:
```json
{ "name": "Ray", "email": "ray@example.com", "password": "secret123", "role": "CUSTOMER" }
```
`role` is optional (defaults to `CUSTOMER`); `"SELLER"` allowed; anything else → 400.
Response `201`:
```json
{ "message": "Registration successful",
  "data": { "id": 1, "name": "Ray", "email": "ray@example.com", "role": "CUSTOMER" } }
```
`409` if email already registered.

### `POST /api/auth/login` — log in
Request: `{ "email": "...", "password": "..." }` → `200 LoginResponse {user, role, message}`, sets session cookie. `401` on bad credentials.

### `POST /api/auth/logout` — invalidate session → `200 {message: "Logged out successfully"}`.

### `GET /api/auth/me` — current user → `200 UserResponse`, `401` if not logged in.

---

## Products (`/api/products`)

| Method | Path | Description | Access |
|---|---|---|---|
| GET | `/api/products?search=&category=&minPrice=&maxPrice=&available=&sort=` | Filtered list | public |
| GET | `/api/products/categories` | Distinct category names | public |
| GET | `/api/products/{id}` | Product detail | public |
| POST | `/api/products` | Create product | SELLER (created under session seller) |
| PUT | `/api/products/{id}` | Update product | SELLER/owner |
| DELETE | `/api/products/{id}` | Delete product | SELLER/owner or ADMIN |

`sort` values and filter semantics are implemented in `ProductService.search`
(sorting, category filter, price range, availability).

Create/Update body (`ProductRequest`, all validated):
```json
{ "name": "Wireless Mouse", "description": "...", "price": 24.99,
  "quantity": 100, "imageUrl": "https://...", "category": "Electronics", "available": true }
```
`imageUrl` must be empty or start with `http(s)://`.

---

## Cart (`/api/cart`) — requires login

| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/api/cart` | — | Current cart |
| POST | `/api/cart/add` | `CartRequest {productId, quantity>=1}` | Add / increment |
| PUT | `/api/cart/item/{cartItemId}` | `QuantityRequest {quantity>=1}` | Change quantity |
| DELETE | `/api/cart/item/{cartItemId}` | — | Remove item |
| DELETE | `/api/cart/clear` | — | Empty cart |

All return `CartResponse` (`{id, user, items:[CartItemResponse], totalAmount}`).
Stock/availability is validated in `CartService` (errors → 400/409/404).

---

## Orders (`/api/orders`) — requires login, owner-scoped

| Method | Path | Description |
|---|---|---|
| POST | `/api/orders` | Place order from cart → `201 OrderResponse` (`status: PENDING`) |
| GET | `/api/orders` | My orders (list) |
| GET | `/api/orders/{id}` | My order detail (`404` if not owner) |

`OrderResponse` includes order id, status, total, `createdAt`, and
`items:[OrderItemResponse]` (snapshot name/quantity/unit price).

---

## Reviews (`/api`)

| Method | Path | Description | Access |
|---|---|---|---|
| GET | `/api/products/{productId}/reviews` | Reviews + rating summary | public |
| POST | `/api/products/{productId}/reviews` | Create review | login |
| PUT | `/api/reviews/{id}` | Update **own** review | author |
| DELETE | `/api/reviews/{id}` | Delete **own** review | author |

Body (`ReviewRequest`): `{ "rating": 5, "comment": "..." }` — rating required 1–5.
Creating a second review for the same product → `409 DuplicateReviewException`.

---

## Seller (`/api/seller`) — requires SELLER role

| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/api/seller/dashboard` | — | Seller stats |
| GET | `/api/seller/products` | — | Own products |
| POST | `/api/seller/products` | `ProductRequest` | Create own product |
| PUT | `/api/seller/products/{id}` | `ProductRequest` | Update own product |
| DELETE | `/api/seller/products/{id}` | — | Delete own product |
| PATCH | `/api/seller/products/{id}/stock` | `StockUpdateRequest {quantity>=0}` | Set stock |
| GET | `/api/seller/orders` | — | Orders containing own products |

Non-seller → `403 Forbidden`.

---

## Admin (`/api/admin`) — requires ADMIN role

| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/api/admin/dashboard` | — | Global statistics |
| GET | `/api/admin/orders` | — | All orders |
| GET | `/api/admin/orders/{id}` | — | Any order detail |
| PATCH | `/api/admin/orders/{id}/status` | `OrderStatusUpdateRequest {status}` | Change status (allowed transitions) |
| GET | `/api/admin/users` | — | All users |
| GET | `/api/admin/reviews` | — | All reviews |
| DELETE | `/api/admin/reviews/{id}` | — | Delete any review |

Non-admin → `403 Forbidden`.

---

## Shopping assistant (`/api/chat`)

`POST /api/chat` — public, rate-limited per session.
Request: `{ "message": "How do I track my order?" }`
Success `200`:
```json
{ "reply": "...", "provider": "mock", "fallback": false, "timestamp": "..." }
```
`fallback:true` means the deterministic FAQ/mock path answered. Constraints:
max message length `CHAT_MAX_MESSAGE_LENGTH` (500) → 400; over
`CHAT_RATE_LIMIT_PER_MINUTE` (10/min) → 429. `provider` is `"mock"` by default,
`"gemini"` when configured.

---

## Health (`/api/v1`)

`GET /api/v1/health` — liveness + DB probe.
- `200`:
```json
{ "status": "UP", "service": "online-shopping-system",
  "database": { "status": "UP", "vendor": "H2",
                "databaseProduct": "H2 2.1.x", "url": "<datasource>" } }
```
- `503` with `status: "DOWN"` and a `database.error` when the DB is unreachable.

---

## Web (HTML) routes (for completeness)

All pages are rendered by `web/WebController` — see USER_GUIDE.md for the route
table (`/`, `/login`, `/register`, `/products`, `/products/{id}`, `/cart`,
`/checkout`, `/orders`, `/seller`, `/admin` plus form POST handlers).