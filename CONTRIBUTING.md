# Contributing to RayyanMart

Thanks for helping with the capstone project. Keep changes small, backward-compatible
and covered by tests.

## Ground rules

- **Never rewrite or delete working features.** Prefer the smallest safe patch.
- **Never weaken existing tests** to make them pass. If a test exposes a real bug, fix the
  production code.
- **Never claim something works without running it.** Every change must build and pass tests.
- The **working tree is the source of truth** (it is an intentional, uncommitted Thymeleaf
  refactor on top of an older committed baseline). Do not "restore" deleted files from git
  history unless you know what you are doing.
- Do not commit secrets — never put `GEMINI_API_KEY`, `DB_PASSWORD` etc. in tracked files.
  Use environment variables (see `.env.example`).
- The chat endpoint and widget must never expose API keys to the browser.

## Environment

- JDK 17+ (JDK 25 community build is fine), Maven 3.8+, no database needed (embedded H2).
- Build/verify: `mvn -B clean verify`
- Run locally: `mvn -B spring-boot:run` → http://localhost:8080

## Before you submit

1. Run `mvn -B clean verify` and confirm **BUILD SUCCESS** with the existing suite green
   (98 tests at the time of writing).
2. Add or update tests for any behaviour change. Chat logic → `ChatServiceTest`
   (unit) + `ChatControllerIntegrationTest` (MockMvc). New suites can reuse
   `BaseIntegrationTest` and the test `data.sql`.
3. Keep controller endpoints thin; business logic lives in `service/`.
4. Update `CHANGELOG.md` and this repo's `docs/` if user-facing behaviour changed.

## Code style

- Existing style: compact records for DTOs, constructor injection, small private helpers.
- Match the surrounding formatting; no comments unless they add real value.
- Never introduce code that logs or exposes secrets.

## Commit convention

- Concise messages such as `Add per-session chat rate limiting` or
  `Fix product-list intent precedence in FAQ provider`.
- Commit only what you are asked to commit; don't commit half-finished work.

## Testing the chat assistant

- Default `CHAT_PROVIDER=mock` — deterministic, no key needed.
- For Gemini: set `GEMINI_API_KEY` and `CHAT_PROVIDER=gemini` in the environment only.
- Rate limit: 10 messages/min per session (configurable via `CHAT_RATE_LIMIT_PER_MINUTE`).