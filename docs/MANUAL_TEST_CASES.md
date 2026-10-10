# MANUAL_TEST_CASES.md — RayyanMart Online Shopping System

> **These are SUGGESTED manual scripts for a human QA pass. They were NOT executed
> by an automated run in this cycle** (the automated suite results are in
> TEST_RESULTS.md). Do not run the payment-related IRRELEVANT cases — there is no
> payment feature; they are intentionally absent.

Preconditions: build the WAR and run locally (SETUP_GUIDE.md). For persistent-
data checks, prefer the H2 in-memory profile so the local DB is not affected.

Format: **ID — Feature — Steps — Expected result**.

## 1. Smoke
- M-01 — Home — Open `http://localhost:8080/` — Page renders, header, welcome,
  and listed seed products (Wireless Mouse, Java Book, T-Shirt, Wallet, Mug Set).
- M-02 — Health — `GET /api/v1/health` — `200 {"status":"UP","database":{"status":"UP"}}`.

## 2. Auth
- M-03 — Register — Go to `/register`; fill name/email/secret123 — Redirected to
  `/login`, success message. Email stored lowercased.
- M-04 — Duplicate register — Register same email again — Error "already exists" (409 via API).
- M-05 — Login/logout — Login, see name in header; logout; protected pages redirect to `/login`.
- M-06 — Wrong password — Login with bad password — "Invalid email or password".

## 3. Catalogue
- M-07 — Search — `/products?search=cotton` — Only "Cotton T-Shirt".
- M-08 — Filters — category=Electronics, minPrice/maxPrice — Correct subset only.
- M-09 — Sort — `sort=price-asc` (whatever ProductService supports) — Ordering matches.
- M-10 — Detail — Open a product — Image, ₹ price (USD × 83.00), stock, rating, review list.

## 4. Reviews
- M-11 — Create — Logged in, review product (rating 4 + comment) — Visible; average updates.
- M-12 — Duplicate — Review same product again — Rejected ("one review per product" 409).
- M-13 — Edit/delete — Update own review; delete it — Changes reflected; then 0 reviews.
- M-14 — Unauthenticated — Create review while logged out — Redirect to login / 401.

## 5. Cart & checkout
- M-15 — Add to cart — Add product qty 1 — Cart shows item, server total.
- M-16 — Update/remove/clear — Adjust qty, remove item, clear cart — Totals and list update.
- M-17 — Over-stock — Add qty > available stock — Rejected with stock message.
- M-18 — Place order — Add item, `/checkout`, "Place Order" — Redirect to `/orders`,
  order present with status PENDING; cart now empty; stock decremented.
- M-19 — Empty cart check — `/checkout` with empty cart — "Your cart is empty" (400 via API).
- M-20 — Duplicate submit (known risk) — Double-click Place Order — Observe: currently may
  create a duplicate order (documented limitation; not a test failure target).

## 6. Seller
- M-21 — Setup — Register a SELLER via `POST /api/auth/register` with `"role":"SELLER"` — Session is SELLER.
- M-22 — Dashboard — Visit `/seller` — Own products/orders shown; a CUSTOMER gets 403/redirect.
- M-23 — Create/update/delete product + stock — Use `/seller/products` — Only own products change.
- M-24 — Orders — Place a customer order containing the seller's product — Seller order list includes it; customer-only orders do not.

## 7. Admin
- M-25 — Setup — Start app with `ADMIN_EMAIL`+`ADMIN_PASSWORD` — Admin logged in at `/admin`.
- M-26 — Dashboard — View stats — Counts render without error.
- M-27 — Order status — Change order status along allowed path; try illegal jump — Legal path
  accepted; illegal jump rejected (400).
- M-28 — Review moderation — Delete any review from admin — Removed.
- M-29 — Users — `/api/admin/users` lists all registered users.

## 8. Chat widget (UX)
- M-30 — Visibility — Logged out: no `?` button; logged in: button bottom-right present on home/products/checkout.
- M-31 — Open/close — Toggle button — Panel opens above it; icon toggles; closes on ✕.
- M-32 — Payment question — Ask "do you accept card payments?" — Reply says no money is transferred / order created PENDING (accurate).
- M-33 — Rate limit — Send >10 messages/min — 429 + friendly message.
- M-34 — Blank message — Send empty — Validation error (400), not a crash.

## 9. Deployment checks (Tomcat 9)
- M-35 — Deploy WAR to Tomcat 9 with persistent H2 — App boots, seed products present after restart.
- M-36 — Verify — `/api/v1/health` UP; login + order placement works with persisted data.