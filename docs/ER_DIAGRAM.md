# ER_DIAGRAM.md — RayyanMart Online Shopping System

Entity-relationship diagram for the actual JPA model. Rendered with Mermaid
(renders on GitHub, GitLab, and mermaid.live). Pair with DATABASE_SCHEMA.md for
column details.

```mermaid
erDiagram
    USERS ||--o| CARTS : "owns (1:1)"
    USERS ||--o{ ORDERS : "places"
    USERS ||--o{ PRODUCTS : "sells (seller)"
    USERS ||--o{ REVIEWS : "writes"
    CARTS ||--o{ CART_ITEMS : "contains"
    PRODUCTS ||--o{ CART_ITEMS : "is in"
    PRODUCTS ||--o{ ORDERS_ITEMS : "ordered as"
    ORDERS ||--o{ ORDERS_ITEMS : "has"
    PRODUCTS ||--o{ REVIEWS : "receives"
    REVIEWS ||--o| USERS : "authored by"

    USERS {
        bigint id PK
        varchar name
        varchar email UK
        varchar password
        varchar role
        timestamp created_at
    }
    PRODUCTS {
        bigint id PK
        varchar name
        varchar description
        decimal price
        integer quantity
        varchar image_url
        varchar category
        boolean available
        timestamp created_at
        bigint seller_id FK
    }
    CARTS {
        bigint id PK
        bigint user_id FK,UK
    }
    CART_ITEMS {
        bigint id PK
        bigint cart_id FK
        bigint product_id FK
        integer quantity
    }
    ORDERS {
        bigint id PK
        bigint user_id FK
        decimal total_amount
        varchar status
        timestamp created_at
    }
    ORDERS_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        varchar product_name
        integer quantity
        decimal price
    }
    REVIEWS {
        bigint id PK
        bigint product_id FK
        bigint user_id FK
        integer rating
        varchar comment
        timestamp created_at
        timestamp updated_at
    }
```

## Relationship legend

| Relationship | Rule |
|---|---|
| `USERS 1:1 CARTS` | `carts.user_id` is UNIQUE; cart created at registration |
| `USERS 1:N ORDERS` | `orders.user_id` |
| `USERS 1:N PRODUCTS` | `products.seller_id` nullable (products without a seller OK) |
| `USERS 1:N REVIEWS` | `reviews.user_id` |
| `CARTS 1:N CART_ITEMS` | `cart_items.cart_id`; unique `(cart_id, product_id)` |
| `PRODUCTS 1:N CART_ITEMS` | `cart_items.product_id` |
| `PRODUCTS 1:N ORDERS_ITEMS` | `order_items.product_id` **nullable** (snapshot design) |
| `ORDERS 1:N ORDERS_ITEMS` | `order_items.order_id`; `product_name`+`price` snapshotted |
| `PRODUCTS 1:N REVIEWS` | `reviews.product_id`; unique `(product_id, user_id)` = max 1 review/user/product |

## Business rules visible in the model

- A user has **exactly one** cart; the cart is created on registration.
- A user can review a product **once** (UNIQUE constraint backs app-level logic/409).
- Orders store **snapshot** data (item name + unit price) so history is stable
  even if products/prices change, and `order_items.product_id` tolerates deletions.
- One "status string" per order; allowed transitions live in
  `OrderService.buildTransitions` (see SEQUENCE_DIAGRAMS.md / USER_GUIDE.md).
- There is **no** payment-related table/entity in the model.