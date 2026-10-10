# PROJECT_OVERVIEW.md — RayyanMart Online Shopping System

> Status: documentation-only. Describes the application exactly as it exists in
> the source code at `C:\Rayyan Mart\online-shopping-system`.

## 1. Project purpose

RayyanMart is an online shopping and order-management system. It lets customers
browse and search a product catalogue, read ratings and reviews, build a cart,
and place orders, while sellers manage their own products and stock, and an
administrator oversees the whole marketplace (orders, products, users, and
reviews).

A built-in **shopping assistant** (chat widget) helps logged-in visitors with
questions about the site, orders, products, and payments.

## 2. Objectives

- Provide a browsable, filterable product catalogue.
- Support customer accounts (register, login, logout).
- Allow customers to maintain a cart and place orders.
- Let sellers create and manage products, stock levels, and their own orders.
- Give an administrator a global dashboard over orders, products, users, and reviews.
- Expose a public REST API in parallel to the server-rendered web UI.
- Run zero-configuration locally (in-memory H2) and deployable to Apache Tomcat with a persistent database.

## 3. Target users

| Role | Description |
|------|-------------|
| **CUSTOMER** | Registers freely (via the register form or API). Browses products, reviews, keeps a cart, places orders, views order history. |
| **SELLER** | Registers freely with `role = SELLER` (via the API only). Manages own products, stock, and order book. |
| **ADMIN** | Bootstrap-created at startup via `ADMIN_EMAIL`/`ADMIN_PASSWORD` (see SETUP_GUIDE.md). Oversees all orders and has global moderation rights. Allows `.getCurrentAdmin`-protected endpoints. |

The `ADMIN` role **cannot** be self-registered — `AuthService.resolveRole` rejects any role
other than `CUSTOMER` or `SELLER` at registration time.

## 4. Main features (as implemented)

- **Accounts**: register, login, logout, "who am I" (`/api/auth/me`). Passwords hashed with BCrypt; session-based authentication via `AuthFilter`.
- **Catalogue**: product listing with optional search, category filter, price range, availability filter, and sorting; product detail page; category list.
- **Reviews & ratings**: logged-in users can review a product once (unique per user + product), rate 1–5 stars, edit their own review, and delete it; admins can delete any review.
- **Cart**: one cart per user; add/update/remove/clear line items; quantity validation against stock.
- **Checkout & orders**: order placement creates a `PENDING` order, server-recomputed totals, stock decrement, cart clearing; order history; order detail; order-status workflow (PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED, or CANCELLED) enforced by allowed transitions.
- **Seller area**: dashboard, own-product CRUD, stock updates, own order list.
- **Administration**: dashboard statistics, global order list + status updates, product CRUD, user list, review moderation.
- **Shopping assistant (chat)**: a floating chat widget visible to logged-in users; deterministic FAQ (mock provider) by default, optional Gemini provider; per-session rate limit.
- **Health endpoint**: `GET /api/v1/health` reports liveness and database connectivity.

> **Payment**: the system **does not process payments** in any form. Placing an order
> creates a `PENDING` order; no money is transferred and no payment/COD flow exists.
> This is intentional and is the current state of the project.

## 5. Non-functional characteristics

- **Stack**: Java 17, Spring Boot 2.7.18, Spring MVC, Spring Data JPA, Spring Security, Thymeleaf (server-side rendering), Bean Validation, H2 (runtime default), MySQL driver (available), packaged as a WAR for Apache Tomcat 9.0.x.
- **No JavaScript frontend**: the UI is rendered server-side with Thymeleaf (single `chat.js` provides widget behaviour only).
- **REST + Web duality**: the same business services back both the Thymeleaf pages and the JSON API.

## 6. Audited facts (evidence)

- 7 JPA-backed tables: `users`, `products`, `carts`, `cart_items`, `orders`, `order_items`, `reviews`.
- Seed data: 5 sample products (IDs 1–5) via `data.sql` (`INSERT IGNORE`, idempotent).
- Currency: amounts are stored in USD (`price`/`total_amount` `DECIMAL(10,2)`) and displayed in INR (× 83.00, fixed rate) by `web/Ui.java`.
- Tests: automated test suite (see TEST_RESULTS.md) — 99 tests, all passing at the time of writing.