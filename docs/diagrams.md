# Diagrams

## Use-case diagram

```mermaid
flowchart LR
    Guest([Guest]) -->|register / login| Auth
    Guest -->|browse catalogue, search, filter, view product & reviews| Catalog
    Guest -->|ask the shopping assistant| Chat
    Customer([Customer]) -->|add to cart, checkout, place order| Orders
    Customer -->|view order history & status| Orders
    Customer -->|rate & review purchased products| Reviews
    Seller([Seller]) -->|CRUD own products| Catalog
    Seller -->|manage stock| Catalog
    Seller -->|view orders with own products| Orders
    Admin([Admin]) -->|dashboard stats| AdminDash
    Admin -->|manage any product| Catalog
    Admin -->|update order status| Orders
    Admin -->|view users| AdminDash
    Admin -->|remove reviews| Reviews

    Auth([Authentication & sessions])
    Catalog([Product catalogue])
    Chat([AI shopping assistant])
    Orders([Order management])
    Reviews([Reviews & ratings])
    AdminDash([Admin dashboard])
```

## Sequence: place an order (happy path)

```mermaid
sequenceDiagram
    participant B as Browser (customer)
    participant W as WebController
    participant O as OrderService
    participant P as Product (JPA)
    participant C as Cart (JPA)

    B->>W: POST /checkout/place
    W->>O: placeOrder(session)
    O->>O: load cart + verify user logged in
    O->>P: for each line: check quantity >= requested
    O->>P: decrement quantity (stock validation)
    O->>O: build Order + OrderItem snapshots, compute total
    O->>C: clear cart (server-side)
    O-->>W: OrderResponse (id, total)
    W-->>B: 302 redirect /orders + flash "Order #n placed"
```

## Sequence: AI assistant request

```mermaid
sequenceDiagram
    participant B as Browser (chat widget)
    participant CC as ChatController
    participant S as ChatService
    participant G as ChatDomainGuard
    participant P as ChatProvider (mock/Gemini)
    participant C as cache (per-session)

    B->>CC: POST /api/chat {"message":"show available products"}
    CC->>S: answer(message, session)
    S->>S: validate length <= max
    S->>S: rate-limit check (bucket)
    S->>C: lookup(key = sessionId+message)
    alt cache hit
        C-->>S: entry
        S-->>CC: 200 {reply, cached=true}
    else cache miss
        S->>G: isOffTopic(message)?
        G-->>S: false
        S->>P: answer(message)
        P->>P: intent match / product lookup / Gemini call
        P-->>S: reply text
        S->>C: store(key, reply)
        S-->>CC: 200 {reply, provider, cached=false}
    end
    CC-->>B: JSON reply → textContent() render
```

## Sequence: admin updates order status

```mermaid
sequenceDiagram
    participant B as Browser (admin)
    participant A as AdminController
    participant O as OrderService
    participant U as Order (JPA)

    B->>A: PATCH /api/admin/orders/{id}/status {"status":"SHIPPED"}
    A->>O: updateStatus(session, id, SHIPPED)
    O->>O: verify session.role == ADMIN
    O->>U: set status + save
    U-->>O: order
    O-->>A: OrderResponse
    A-->>B: 200 {message:"status updated"}
```

## Sequence: rate limited chat call

```mermaid
sequenceDiagram
    participant B as Browser
    participant CC as ChatController
    participant S as ChatService
    participant GL as GlobalExceptionHandler

    B->>CC: POST /api/chat (12th message this minute)
    CC->>S: answer(message, session)
    S->>S: rate-limit exceeded? → throw ChatRateLimitException
    CC->>GL: exception propagates
    GL-->>B: 429 {"error":"Too many requests. Please slow down."}
```

(These diagrams are Mermaid — render in any GitHub-flavoured Markdown viewer or
[mermaid.live](https://mermaid.live).)