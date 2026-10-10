# USE_CASE_DIAGRAM.md — RayyanMart Online Shopping System

Actors and use cases that are actually implemented in the code.

```mermaid
flowchart LR
    CUS[Customer]
    SEL[Seller]
    ADM[Administrator]
    VIS[Visitor / anonymous]

    subgraph Public
        VIS -->|browse products| P1[View catalogue & search/filter/sort]
        VIS -->|view detail| P2[View product detail & reviews]
        VIS -->|read FAQ| P3[Ask shopping assistant *]
        VIS -->|register| P4[Register CUSTOMER]
    end

    subgraph Customer
        CUS --> P5[Login / logout]
        CUS --> P6[Manage cart<br>add / update / remove / clear]
        CUS --> P7[Checkout & place order]
        CUS --> P8[View order history & details]
        CUS --> P9[Write / edit / delete own review]
        CUS --> P10[Use shopping assistant *]
    end

    subgraph Seller
        SEL --> P11[Seller account]
        SEL --> P12[Manage own products CRUD]
        SEL --> P13[Update own product stock]
        SEL --> P14[View own orders / update status]
        SEL --> P15[View seller dashboard]
    end

    subgraph Administrator
        ADM --> P16[Log in as admin]
        ADM --> P17[View global dashboard]
        ADM --> P18[Manage all orders / statuses]
        ADM --> P19[Manage any product]
        ADM --> P20[List users]
        ADM --> P21[Moderate / delete any review]
    end

    SEL ---|can be| CUS
```

\* The shopping assistant REST endpoint is public; the floating widget in the UI
is rendered only for logged-in users.

## Use-case → implementation map (evidence)

| UC | Where implemented | Notes / status |
|---|---|---|
| Browse & search | `WebController /products`, `ProductController.search`, `ProductService` | VERIFIED (tests) |
| View product detail | `/products/{id}`, `ProductController.get` | VERIFIED |
| Ask shopping assistant | `ChatController` + `ChatService` + FAQ/Gemini providers | VERIFIED (tests, live) |
| Register | `/api/auth/register`, `WebController.register` | role CUSTOMER/SELLER only |
| Login/logout | `/api/auth/login` and `/api/auth/logout`, session cookie | VERIFIED |
| Manage cart | `CartController` + `CartService` | VERIFIED |
| Checkout & place order | `/checkout/place` (web), `POST /api/orders` | VERIFIED |
| Order history | `/orders`, `GET /api/orders`, `/api/orders/{id}` | VERIFIED |
| Reviews | `ReviewController` + `ReviewService` | one per user/product; VERIFIED |
| Seller manage own products/stock | `SellerController` (`/api/seller/products*`) | scoped to `seller_id`; VERIFIED |
| Seller orders/status | `SellerService` + seller order views | VERIFIED |
| Admin dashboard/orders/products/users/reviews | `AdminController` (`/api/admin/*`) | present; **no automated admin tests** |

## Security notes

- `SessionService.getCurrentSeller` / `getCurrentAdmin` throw 403 for the wrong role.
- Registration can **never** yield ADMIN (`AuthService.resolveRole`).
- Customers can only view their **own** orders and edit their **own** reviews
  (admin may delete any review).