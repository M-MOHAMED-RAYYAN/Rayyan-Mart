# PROJECT_PRESENTATION_OUTLINE.md — RayyanMart Online Shopping System

A presentation outline that only claims features that exist today. Proposed
enhancements are explicitly labelled **proposed**.

## 1. Problem statement
- A small marketplace needs a single application that lets customers browse and
  order products while allowing sellers to manage inventory and an administrator
  to oversee everything — from one codebase, with no heavy JS frontend.
- Today's system: a Spring Boot + Thymeleaf web app with a REST API on top.

## 2. Project objectives
- Server-rendered storefront + JSON API from one service layer.
- Customer accounts, browsing/filtering, reviews, per-user cart, orders with an
  enforced status workflow.
- Seller inventory management; admin oversight and moderation.
- Zero-config local run (in-memory H2) and Tomcat 9 deployment with persistent DB.
- Built-in shopping assistant for customers.

## 3. System architecture
- Layered: Web/REST controllers → services → Spring Data JPA repositories → DB.
- Session-based auth via a custom `AuthFilter` + `SessionService`
  (roles CUSTOMER / SELLER / ADMIN).
- Central error envelope `ApiError` via `GlobalExceptionHandler`.
- Diagram: SYSTEM_ARCHITECTURE.md (flowchart) + SEQUENCE_DIAGRAMS.md.

## 4. Technology stack
- Java 17 · Spring Boot 2.7.18 (Web, Data JPA, Security, Validation, Thymeleaf) ·
  H2 (runtime: in-memory or TCP server) · MySQL driver (optional) ·
  WAR for Apache Tomcat 9.0.x · Maven 3.8+ · JUnit 5 / MockMvc.

## 5. Existing features
- Accounts: register (CUSTOMER/SELLER via API; ADMIN via env bootstrap), login,
  logout, session cookie.
- Catalogue: search, category, price range, availability filters, sorting,
  product detail, category list.
- Reviews: 5-star, one per user per product, edit/delete own, admin moderation.
- Cart: add / update / remove / clear, stock-aware.
- Checkout & orders: server-computed totals, PENDING creation, stock decrement,
  order history, status workflow (PENDING → … → DELIVERED, or CANCELLED).
- Seller area: own-product CRUD, stock, own orders.
- Admin area: dashboard, all orders + status, all products, users, review moderation.
- Shopping assistant: FAQ chat (`mock` provider default, optional Gemini);
  widget visible to logged-in users.
- Health endpoint `/api/v1/health`.
- **No payment processing exists** — order creation only (stated honestly).

## 6. Database design
- 7 tables: `users`, `products`, `carts`, `cart_items`, `orders`, `order_items`,
  `reviews`; Hibernate owns the schema; `data.sql` seeds 5 products.
- ER diagram: ER_DIAGRAM.md (Mermaid) + DATABASE_SCHEMA.md (columns/FKs).
- Design notes: snapshot order items, unique cart per user, one review per
  user/product, no payment tables.

## 7. Application screenshots to capture
1. Home page with seed catalogue.
2. Product listing with filters/search/sort.
3. Product detail with reviews + ₹ price.
4. Login and register pages.
5. Cart page.
6. Checkout summary page ("Place Order").
7. Order history with status badges.
8. Seller dashboard (own products/stock/orders).
9. Admin dashboard (+ order status update).
10. Chat widget open (logged-in).

## 8. Testing evidence
- Automated suite: **99 tests, 0 failures** (`mvn -B test`, TEST_RESULTS.md)
  across auth, products, filters, cart, orders, status workflow, reviews, seller
  scoping, security, and chat.
- Live spot-check (read-only): health UP, seeded products, chat widget
  login-gated.
- Manual test scripts proposed for browser/UX coverage: MANUAL_TEST_CASES.md.

## 9. Limitations and future enhancements (**proposed**)
| Area | Current state | Proposed enhancement |
|---|---|---|
| Payments | NOT IMPLEMENTED | Real gateway or explicit COD flow |
| Order placement | No idempotency / CSRF token | Add token-based protection |
| Admin coverage | No automated tests | Add AdminController tests |
| Seller onboarding | API-only role registration | UI registration for sellers |
| Forgot password / email | Absent | Password reset flow |
| Catalogue scale | No pagination | Paged queries |
| Notifications | Absent | Order-status email |

Each is labelled **proposed** and would require source-code changes — none are
implemented in this project.

## 10. Conclusion
- A complete, tested, server-rendered marketplace with REST API, three roles,
  cart/orders/reviews, an admin and seller area, and an FAQ chat assistant.
- Runs zero-config locally and deploys cleanly to Tomcat 9 with a persistent
  database.
- Transparent about what it does **not** have (payments, admin tests) and what is
  proposed next.

## Suggested slide flow
1 title → 2 problem → 3 objectives → 4 architecture → 5 stack → 6 features →
7 DB → 8 screenshots → 9 testing → 10 limitations/proposal → 11 conclusion/Q&A.