# Changelog

All notable changes to RayyanMart are documented here. The project follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) loosely, and uses
[Semantic Versioning](https://semver.org/).

## [1.0.0] — Finalization (2026-10-09)

### Added
- **AI shopping assistant (chatbot)**:
  - `FAQChatProvider` — deterministic, product-aware Q&A provider that works with no API key.
  - `GeminiChatProvider` — optional Google Gemini integration behind the `GEMINI_API_KEY` env var.
  - `ChatController` (`POST /api/chat`), `ChatService`, `ChatProperties`, `ChatDomainGuard`.
  - Per-session rate limiting (default 10 req/min → HTTP 429), max message length (default 500),
    per-session answer cache with TTL, provider fallback.
  - Chat widget embedded on all 10 pages (`chatWidget` fragment + `js/chat.js`), XSS-safe output.
- **Security hardening**:
  - `SameSite=lax` + `HttpOnly` session cookie, cookie tracking-mode.
  - `server.error.include-stacktrace=never` (no stack traces leaked in error responses).
  - URL-safe redirect validation in `WebController`.
  - DB driver / credentials fully env-overridable (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`).
- **Operational config**: optional admin bootstrap via `ADMIN_EMAIL`/`ADMIN_PASSWORD`,
  `SERVER_PORT`, `CHAT_*` knobs — see `.env.example`.
- **Test coverage**: `ChatControllerIntegrationTest` (7) + `ChatServiceTest` (10 unit scope, 9 test methods)
  → suite grows from 81 to **98 tests**.

### Fixed
- Product-list FAQ intent now wins over the broader *stock/availability* intent, so
  "show available products" lists the catalogue instead of returning a stock count.

### Changed
- `com.shopping.system.web.WebController` renders every page with Thymeleaf
  (server-side rendering; no client-side JS framework).
- `AuthFilter` permits the chat endpoint as public (`PUBLIC_CHAT`).

## [0.9.0] — Thymeleaf server-side refactor (pre-2026-10-09)
- Replaced the JavaScript/SPA frontend with Thymeleaf templates rendered server-side.
- Introduced `WebController` (GET pages + form POSTs) and `Ui` view helpers (₹ formatting, stars).
- Moved to WAR packaging: embedded Tomcat is `provided`, deploy on Apache Tomcat 9.0.x.
- Downgraded to Spring Boot 2.7.18 (javax.servlet), Java 17 target.

## [0.8.0] — Baseline
- Original REST API + JS frontend on Spring Boot 3.5.16 (superseded by 0.9.0, kept in git history).

> Dates for the 0.x entries are approximate; the working tree is the source of truth.

[1.0.0]: https://github.com/rayyan-mart/online-shopping-system/releases/tag/v1.0.0