# DATABASE_SCHEMA.md — RayyanMart Online Shopping System

This document reflects the real data model in `src/main/java/.../model/*` as
managed by JPA/Hibernate. The schema is **owned by Hibernate**
(`spring.jpa.hibernate.ddl-auto=update`) — there is no `schema.sql`. Tables are
created on startup; `data.sql` then seeds products.

> Naming: the app runs with `DATABASE_TO_LOWER=TRUE` (and MySQL compatibility),
> and Spring Boot's default naming strategy converts camelCase fields to
> `snake_case` columns (e.g. `imageUrl` → `image_url`).

## Entities → tables

| Entity | Table | Notes |
|---|---|---|
| `User` | `users` | unique `email` |
| `Product` | `products` | optional `seller_id` |
| `Cart` | `carts` | one per user |
| `CartItem` | `cart_items` | unique `(cart_id, product_id)` |
| `Order` | `orders` | status enum stored as STRING |
| `OrderItem` | `order_items` | snapshot `product_name`, `price` |
| `Review` | `reviews` | unique `(product_id, user_id)` |

---

## Table definitions

### `users`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR(255) | NOT NULL |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE |
| `password` | VARCHAR(255) | NOT NULL (BCrypt hash) |
| `role` | VARCHAR(20) | NOT NULL — `CUSTOMER`/`SELLER`/`ADMIN` |
| `created_at` | TIMESTAMP | NOT NULL |

Other: `orders` (1–N), `products` via `seller_id` (1–N), one `cart`, many `reviews`.

### `products`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR(255) | NOT NULL |
| `description` | VARCHAR(2000) | nullable |
| `price` | DECIMAL(10,2) | NOT NULL (USD) |
| `quantity` | INTEGER | NOT NULL (stock) |
| `image_url` | VARCHAR(1000) | nullable |
| `category` | VARCHAR(100) | nullable (free text, no category table) |
| `available` | BOOLEAN | NOT NULL, default true |
| `created_at` | TIMESTAMP | NOT NULL |
| `seller_id` | BIGINT | null-able FK → `users.id` |

### `carts`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `user_id` | BIGINT | NOT NULL, UNIQUE FK → `users.id` (1:1) |

### `cart_items`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `cart_id` | BIGINT | NOT NULL FK → `carts.id` |
| `product_id` | BIGINT | NOT NULL FK → `products.id` |
| `quantity` | INTEGER | NOT NULL |
| *(composite)* | | UNIQUE `(cart_id, product_id)` |

### `orders`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `user_id` | BIGINT | NOT NULL FK → `users.id` |
| `total_amount` | DECIMAL(10,2) | NOT NULL (USD) |
| `status` | VARCHAR(20) | NOT NULL — `PENDING`,`CONFIRMED`,`PROCESSING`,`SHIPPED`,`DELIVERED`,`CANCELLED` |
| `created_at` | TIMESTAMP | NOT NULL |

### `order_items`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `order_id` | BIGINT | NOT NULL FK → `orders.id` |
| `product_id` | BIGINT | **nullable** FK → `products.id` (kept null-safe snapshot) |
| `product_name` | VARCHAR(255) | NOT NULL (historic snapshot) |
| `quantity` | INTEGER | NOT NULL |
| `price` | DECIMAL(10,2) | NOT NULL (unit price at order time) |

### `reviews`
| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `product_id` | BIGINT | NOT NULL FK → `products.id` |
| `user_id` | BIGINT | NOT NULL FK → `users.id` |
| `rating` | INTEGER | NOT NULL (1–5, app-validated) |
| `comment` | VARCHAR(2000) | nullable |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |
| *(composite)* | | UNIQUE `(product_id, user_id)` |

---

## Relationships / foreign keys (summary)

| From | To | Cardinality | FK column |
|---|---|---|---|
| `users` | `carts` | 1 → 1 | `carts.user_id` (unique) |
| `users` | `orders` | 1 → N | `orders.user_id` |
| `users` | `products` (as seller) | 1 → N | `products.seller_id` |
| `users` | `reviews` | 1 → N | `reviews.user_id` |
| `carts` | `cart_items` | 1 → N | `cart_items.cart_id` |
| `products` | `cart_items` | 1 → N | `cart_items.product_id` |
| `products` | `order_items` | 1 → N | `order_items.product_id` (nullable) |
| `orders` | `order_items` | 1 → N | `order_items.order_id` |
| `products` | `reviews` | 1 → N | `reviews.product_id` |
| `users` | `orders` (owner) | 1 → N | `orders.user_id` |

Notes:
- **No payment tables** exist (no payments feature).
- **No category table** — `products.category` is free text (the catalogue lists distinct categories).
- Cascades: `users→carts`, `users→orders` (`ALL` + orphan-removal in the entity);
  `carts→cart_items`, `orders→order_items` (`ALL` + orphan-removal). `products→*` have **no** cascade.
- `order_items.product_id` is nullable on purpose so order rows survive even if a product is later deleted.