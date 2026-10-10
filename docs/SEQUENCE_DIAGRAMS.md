# SEQUENCE_DIAGRAMS.md — RayyanMart Online Shopping System

Sequence diagrams for the real request flows. Mermaid renders on GitHub/GitLab.

---

## 1. Registration (CUSTOMER)

```mermaid
sequenceDiagram
    actor V as Visitor
    participant B as Browser
    participant A as AuthController
    participant S as AuthService
    participant U as UserRepository
    participant C as CartRepository

    V->>B: submit /register form
    B->>A: POST /api/auth/register
    A->>S: register(RegisterRequest)
    S->>U: existsByEmail(email)
    alt email already used
        U-->>S: true
        S-->>A: DuplicateEmailException
        A-->>B: 409 {error}
    else new email
        S->>S: encode password (BCrypt) + resolveRole ("CUSTOMER")
        S->>U: save(user)
        S->>C: save(new Cart(user))
        S-->>A: UserResponse
        A-->>B: 201 {message, data}
    end
    B-->>V: redirect/login or inline success
```

---

## 2. Login (session)

```mermaid
sequenceDiagram
    actor U as User
    participant B as Browser
    participant A as AuthController
    participant S as AuthService
    participant R as UserRepository
    participant SSE as SessionService
    participant SF as AuthFilter

    U->>B: submit /login form
    B->>A: POST /api/auth/login
    A->>S: login(LoginRequest, session)
    S->>R: findByEmail(email)
    alt user missing or wrong password
        R-->>S: empty / match=false
        S-->>A: UnauthorizedException
        A-->>B: 401 "Invalid email or password"
    else ok
        S->>SSE: login(session, user) -> set userId, role
        S-->>A: LoginResponse(role, message)
        A-->>B: 200 + Set-Cookie JSESSIONID
    end
    B-->>U: render home / redirect
    Note over B,SF: subsequent requests -> AuthFilter reads session -> security context
```

---

## 3. Browse & filter products

```mermaid
sequenceDiagram
    actor V as Visitor
    participant B as Browser
    participant W as WebController
    participant P as ProductService
    participant R as ProductRepository

    V->>B: open /products?search=&category=...&sort=...
    B->>W: GET /products
    W->>P: search(params)
    P->>R: findWithFilters(...)
    R-->>P: List<Product>
    P-->>W: ProductResponse list
    W-->>B: Thymeleaf render products.html
    B-->>V: catalogue page
```

---

## 4. Add product to cart

```mermaid
sequenceDiagram
    actor C as Customer
    participant B as Browser
    participant W as WebController
    participant K as CartService
    participant R as Repositories (Cart/Product)

    C->>B: click "Add to cart" on product
    B->>W: POST /cart/add {productId, quantity}
    W->>K: add(session, request)
    K->>K: get/create cart for session user
    K->>R: load product, verify available & stock
    alt product unavailable or stock insufficient
        R-->>K: refused -> error (redirect with message / 4xx)
    else ok
        K->>K: upsert CartItem (unique cart+product)
        K->>R: save item
        K-->>W: updated cart summary
        W-->>B: redirect back to product/cart
    end
```

---

## 5. Checkout & place order

```mermaid
sequenceDiagram
    actor C as Customer
    participant B as Browser
    participant W as WebController
    participant O as OrderService
    participant R as Repositories (Order/Product/Cart)

    C->>B: click "Proceed to Checkout"
    B->>W: GET /checkout
    W->>O: load cart summary (total)
    W-->>B: checkout.html (order summary)

    C->>B: click "Place Order"
    B->>W: POST /checkout/place
    W->>O: placeOrder(session)   (single @Transactional)
    O->>O: get user + cart
    alt cart is empty
        O-->>W: EmptyCartException
        W-->>B: 400 "Your cart is empty"
    else items present
        loop each CartItem
            O->>R: re-read Product price & stock (DB)
            alt insufficient stock / unavailable
                O-->>W: InsufficientStockException / not-found
            else ok
                O->>O: build OrderItem total (server-computed)
            end
        end
        O->>O: order.status = PENDING
        O->>R: save order (+ items)
        O->>R: decrement product.quantity (out-of-stock -> available=false)
        O->>R: clear cart items
        O-->>W: created Order
        W-->>B: 302 -> /orders + flash "Order #N placed"
    end
```

> No payment step exists anywhere in this flow — see PROJECT_OVERVIEW.md.

---

## 6. Order status update (seller/admin)

```mermaid
sequenceDiagram
    actor OP as Seller/Admin
    participant B as Browser
    participant SA as SellerController/AdminController
    participant OS as OrderService
    participant R as OrderRepository

    OP->>B: submit new status
    B->>SA: POST .../status {status}
    SA->>OS: updateStatus(order, seller, nextStatus)
    OS->>OS: check allowed transition (buildTransitions)
    alt invalid transition or terminal state
        OS-->>SA: InvalidOrderStatusException
        SA-->>B: 400 "Cannot ..."
    else valid
        OS->>R: save(newStatus)
        OS-->>SA: updated order
        SA-->>B: render/refresh order list
    end
```

---

## 7. Review product

```mermaid
sequenceDiagram
    actor C as Customer
    participant B as Browser
    participant RV as ReviewController
    participant RS as ReviewService
    participant R as ReviewRepository

    C->>B: submit rating + comment on /products/{id}
    B->>RV: POST /products/{productId}/reviews
    RV->>RS: create(user, productId, request)
    alt user already reviewed this product
        RS-->>RV: DuplicateReviewException
        RV-->>B: 409 conflict
    else ok
        RS->>R: save(review)
        RS-->>RV: ReviewResponse (+ new avg rating)
        RV-->>B: redirect to product page / JSON
    end
```

---

## 8. Shopping assistant (chat)

```mermaid
sequenceDiagram
    actor U as Logged-in user
    participant B as Browser
    participant CC as ChatController
    participant CS as ChatService
    participant P as ChatProvider (FAQ / Gemini)

    U->>B: open chat widget, type message
    B->>CC: POST /api/chat {message}
    CC->>CS: chat(sessionId, message)
    CS->>CS: validate length + rate limit
    alt over limit
        CS-->>CC: ChatRateLimitException
        CC-->>B: 429
    else ok
        CS->>P: answer(question)
        P-->>CS: reply (FAQ cache or Gemini call)
        CS-->>CC: ChatResponse {reply, provider}
        CC-->>B: 200
        B-->>U: render reply bubble
    end
```

---

## 9. Health probe

```mermaid
sequenceDiagram
    participant O as Operator/CI
    participant H as HealthController
    participant DS as DataSource (HikariCP)
    participant DB as Database

    O->>H: GET /api/v1/health
    H->>DS: getConnection()
    DS->>DB: "SELECT 1"
    alt DB reachable
        DB-->>DS: 1
        H-->>O: 200 {status: UP, database: {status: UP}}
    else unreachable
        DB-->>DS: error
        H-->>O: 503 {status: DOWN, database: {status: DOWN, error}}
    end
```