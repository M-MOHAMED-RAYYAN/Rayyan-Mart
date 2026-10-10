# REST API Reference

Base path: `/api`. All JSON. Errors use the `ApiResponse` shape
`{"success":false,"message":"...","data":null}`; exceptions map to standard status codes
via `GlobalExceptionHandler` (including a custom `429` for chat rate limiting).

## Health

| Method | Path | Access |
|--------|------|--------|
| GET | `/api/v1/health` | Public |

Returns app name, status and a DB connectivity check.

## Auth

| Method | Path | Access | Body |
|--------|------|--------|------|
| POST | `/api/auth/register` | Public | `{name,email,password,role?}` |
| POST | `/api/auth/login` | Public | `{email,password}` |
| POST | `/api/auth/logout` | Any session | — |
| GET | `/api/auth/me` | Any session | — |

`login` returns a `LoginResponse` (user info + role). Roles: `CUSTOMER`, `SELLER`, `ADMIN`.

## Products

| Method | Path | Access |
|--------|------|--------|
| GET | `/api/products` | Public — `?search=&category=&minPrice=&maxPrice=&available=&sort=` |
| GET | `/api/products/categories` | Public |
| GET | `/api/products/{id}` | Public |
| POST | `/api/products` | SELLER, ADMIN |
| PUT | `/api/products/{id}` | Owner-seller or ADMIN |
| DELETE | `/api/products/{id}` | Owner-seller or ADMIN |

`ProductResponse` includes `id, name, description, price, quantity, unitPrice, imageUrl,
category, available, sellerId, sellerName, avgRating, ratingDistribution, createdAt`.

## Cart

| Method | Path | Access |
|--------|------|--------|
| POST | `/api/cart/add` | Logged in — `{productId,quantity}` |
| PUT | `/api/cart/item/{id}` | Logged in — `{quantity}` |
| DELETE | `/api/cart/item/{id}` | Logged in |
| DELETE | `/api/cart/clear` | Logged in |

`GET /api/cart` is served by the `CartController`? No — cart retrieval is used internally
by `CartService`; the web flow fetches it via `WebController` (`GET /cart`).

## Orders

| Method | Path | Access |
|--------|------|--------|
| GET | `/api/orders/{id}` | Owner or ADMIN |

Order placement and history are form flows via `WebController` (`POST /checkout/place`,
`GET /orders`). Admin updates: `PATCH /api/admin/orders/{id}/status`.

## Reviews

| Method | Path | Access |
|--------|------|--------|
| GET | `/api/products/{productId}/reviews` | Public |
| POST | `/api/products/{productId}/reviews` | Purchaser of the product |
| PUT | `/api/reviews/{id}` | Review owner |
| DELETE | `/api/reviews/{id}` | Review owner |

## Seller

| Method | Path | Access |
|--------|------|--------|
| GET | `/api/seller/dashboard` | SELLER |
| GET | `/api/seller/products` | SELLER |
| POST | `/api/seller/products` | SELLER — `ProductRequest` |
| PUT | `/api/seller/products/{id}` | Owner SELLER |
| DELETE | `/api/seller/products/{id}` | Owner SELLER |
| PATCH | `/api/seller/products/{id}/stock` | Owner SELLER — `{quantity}` |
| GET | `/api/seller/orders` | SELLER |

## Admin

| Method | Path | Access |
|--------|------|--------|
| GET | `/api/admin/dashboard` | ADMIN |
| GET | `/api/admin/orders` | ADMIN |
| GET | `/api/admin/orders/{id}` | ADMIN |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN — `{status}` |
| GET | `/api/admin/users` | ADMIN |
| GET | `/api/admin/reviews` | ADMIN |
| DELETE | `/api/admin/reviews/{id}` | ADMIN |

## Chat

| Method | Path | Access | Body |
|--------|------|--------|------|
| POST | `/api/chat` | Public (rate limited) | `{"message":"show available products"}` |

Response: `{"success":true,"data":{"reply":"...","provider":"mock","cached":false,"answeredAt":...}}`.

- `400` — missing / blank or over `app.chat.max-message-length` (default 500) message.
- `429` — more than `app.chat.rate-limit.per-minute` (default 10) messages per session/minute.

## Page routes (Thymeleaf)

| Route | Page |
|-------|------|
| `/` | Home |
| `/login`, `/register` | Auth forms |
| `/products`, `/products/{id}` | Catalogue + product detail |
| `/cart`, `/checkout` | Cart & checkout |
| `/orders` | Order history |
| `/seller`, `/admin` | Dashboards (role-gated) |

All form POSTs (`/login`, `/register`, `/cart/*`, `/checkout/place`, `/seller/*`,
`/admin/*`) follow PRG (post-redirect-get) with flash messages.