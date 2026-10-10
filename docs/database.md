# Database design

## Overview

RayyanMart uses **Hibernate-managed schema** (`spring.jpa.hibernate.ddl-auto=update`).
The same schema supports H2 (dev/test) and MySQL (production) thanks to Hibernate's DDL.
`docs/schema.sql` documents the resulting relational model and can be used to set up a
database manually (e.g. with Hibernate `ddl-auto=none/validate`).

Stored product/order prices are in **USD**; the store front converts to **Indian rupees
(₹)** at the fixed 83.00 rate via the `Ui` view helper and the chat provider. This is a
deliberate display-only conversion.

## ER diagram

```mermaid
erDiagram
    USERS ||--o{ ORDERS : places
    USERS ||--|| CARTS : owns
    CARTS ||--o{ CART_ITEMS : contains
    PRODUCTS ||--o{ CART_ITEMS : "added as"
    USERS ||--o{ PRODUCTS : sells
    ORDERS ||--o{ ORDER_ITEMS : has
    PRODUCTS o|--o{ ORDER_ITEMS : "snapshotted as"
    USERS ||--o{ REVIEWS : writes
    PRODUCTS ||--o{ REVIEWS : receives

    USERS {
        bigint id PK
        varchar name "not null"
        varchar email "not null, unique"
        varchar password "bcrypt hash"
        varchar role "CUSTOMER | SELLER | ADMIN"
        timestamp created_at
    }
    CARTS {
        bigint id PK
        bigint user_id FK "not null, unique"
    }
    CART_ITEMS {
        bigint id PK
        bigint cart_id FK "not null"
        bigint product_id FK "not null"
        int quantity
        "unique(cart_id, product_id)"
    }
    PRODUCTS {
        bigint id PK
        varchar name
        varchar description "max 2000"
        decimal price "precision 10, scale 2 (USD)"
        int quantity
        varchar image_url "max 1000"
        varchar category "max 100"
        boolean available
        timestamp created_at
        bigint seller_id FK "nullable"
    }
    ORDERS {
        bigint id PK
        bigint user_id FK
        decimal total_amount "precision 10, scale 2"
        varchar status "PENDING..DELIVERED"
        timestamp created_at
    }
    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK "nullable (product may be deleted)"
        varchar product_name "snapshot"
        int quantity
        decimal price "snapshot at purchase"
    }
    REVIEWS {
        bigint id PK
        bigint product_id FK
        bigint user_id FK
        int rating "1..5"
        varchar comment "max 2000"
        timestamp created_at
        timestamp updated_at
        "unique(product_id, user_id)"
    }
```

## Notes

- **One cart per user** (`user_id` unique); carts and their items are cascade-deleted with
  the user.
- **Order items snapshot** product name and price so historical orders survive product
  deletion or price changes. `product_id` is nullable and only used for lookups.
- **One review per (user, product)** — enforced by unique constraint + service logic.
  Only purchasers may review; owners may edit/delete their own; admins may remove any.
- **Stock validation** happens in `OrderService.placeOrder` inside a transaction using
  pessimistic/optimistic checks against `product.quantity`; placing an order decrements
  product stock.
- Prices use `DECIMAL(10,2)`, never float/double. Money display conversion is done at the
  view layer only.

See full DDL in [schema.sql](schema.sql) and relational rules in
[architecture.md](architecture.md).