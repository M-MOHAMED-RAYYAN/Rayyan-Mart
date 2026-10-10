# USER_GUIDE.md — RayyanMart Online Shopping System

> Describes how each actor uses the features that exist in the code today.
> Pages are server-rendered (Thymeleaf); form posts and REST endpoints share the
> same business services.

## Roles

- **CUSTOMER** — registers freely, browses, reviews, carts, orders.
- **SELLER** — registers freely (API with `role: "SELLER"`), manages own products/stock and order book.
- **ADMIN** — created only via `ADMIN_EMAIL`/`ADMIN_PASSWORD` at startup; global oversight.

---

## 1. Customer guide

### Register an account
- Web: open `/register`, fill name/email/password (≥ 6 chars). Success → redirected to `/login`.
- The role defaults to `CUSTOMER` on the web form.

### Log in / out
- `/login` (email + password). Session lasts 30 minutes (HTTP-only cookie).
- Log out via the header button (`POST /logout`).

### Browse and find products
- Home page `GET /` lists available products.
- `/products` supports: keyword **search**, **category** filter, **min/max price**, **availability**, and **sorting**.
- Click a product for `/products/{id}` detail (price in ₹, rating stars, description, reviews).

### Review and rate
- Logged-in customers can post a review on the product page (`POST /products/{productId}/reviews`).
- **One review per user per product** — a second attempt fails.
- Star rating 1–5 + optional comment. You can edit your own review, and delete it (`POST /reviews/{id}/delete`). An admin may also delete it.

### Manage the cart
- Add to cart from a product page (`POST /cart/add`).
- `/cart`: see line items; **update quantity**, **remove item**, **clear cart**.
- Quantity is validated against current stock; exceeding it is rejected.

### Checkout and orders
- `/checkout` shows the order summary from your cart.
- Place order (`POST /checkout/place`): prices re-read from the database, total recomputed, stock decremented, cart cleared. The order is created as **PENDING**.
- `/orders` shows your order history with status badges; click to see details.
- Order status is updated by the seller/admin (below). **No payment is taken.**

### Use the shopping assistant (chat)
- When logged in, a circular **?** button appears bottom-right (Help & Support).
- It opens a chat panel powered by the site's FAQ (mock provider) — ask about orders, products, shipping, payments, etc. A Gemini-backed provider is available when configured.

---

## 2. Seller guide

Register as a seller via the API:

```
POST /api/auth/register
{ "name": "Ikea-ish", "email": "seller@example.com", "password": "secret123", "role": "SELLER" }
```

Seller workspace is at `/seller` (seller-only; otherwise 403/redirected).

- **Dashboard** (`GET /api/seller/dashboard`, page `/seller`): overview of own products and orders.
- **Products** (`/api/seller/products`): list own products.
  - Create (`POST /api/seller/products`, page `POST /seller/products`)
  - Update (`PUT /api/seller/products/{id}`, page `POST /seller/products/{id}/update`)
  - Delete (`DELETE /api/seller/products/{id}`, page `POST /seller/products/{id}/delete`)
  - Adjust stock (`PATCH /api/seller/products/{id}/stock`, page `POST /seller/products/{id}/stock`)
- **Orders** (`/api/seller/orders`, page fragment under `/seller`): only orders containing your products; status may be advanced along the allowed workflow.

> Sellers can only ever touch products where `products.seller_id` equals their own user id.

---

## 3. Administrator guide

The admin is created at startup from `ADMIN_EMAIL`/`ADMIN_PASSWORD` (see SETUP_GUIDE.md).
Admin workspace is `/admin` (admin-only).

- **Dashboard** (`GET /api/admin/dashboard`, page `/admin`): statistics (counts, revenue view) across the marketplace.
- **Orders** (`/api/admin/orders`): all orders; update any order's status (`PATCH /api/admin/orders/{id}/status`, page `POST /admin/orders/{id}/status`); view order detail (`/api/admin/orders/{id}`).
- **Products** (`/admin/products`): create, update, delete any product (pages under `/admin/products/...`).
- **Users** (`/api/admin/users`): list all users.
- **Reviews** (`/api/admin/reviews`): list all reviews; delete any (`DELETE /api/admin/reviews/{id}`, page `POST /admin/reviews/{id}/delete`).

### Order-status workflow (enforced)

```
PENDING ─▶ CONFIRMED ─▶ PROCESSING ─▶ SHIPPED ─▶ DELIVERED
    │
    └──────────────────────────────────────────▶ CANCELLED
```

Transitions are validated in `OrderService.buildTransitions`; invalid transitions
(and moves out of terminal states) are rejected.

---

## 4. Key pages (routes)

| URL | Purpose | Access |
|-----|---------|--------|
| `/` | Home / featured products | Public |
| `/login`, `/register` | Auth pages | Public |
| `/products`, `/products/{id}` | Catalogue and detail | Public |
| `/cart` | Cart | Customer |
| `/checkout` | Checkout summary + Place Order | Customer |
| `/orders`, `/orders/{id}` | Order history/details | Owner |
| `/seller`, `/seller/products` | Seller workspace | SELLER |
| `/admin` | Admin dashboard | ADMIN |

All `API` counterparts are documented in API_DOCUMENTATION.md.