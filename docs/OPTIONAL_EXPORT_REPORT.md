# OPTIONAL_EXPORT_REPORT.md — Creating spreadsheets from existing data

> This page explains *how you* could build a separate spreadsheet/report from data
> already served by the application, **without adding a dashboard, changing code,
> or touching the database**. It adds nothing to the app itself.

There is **no built-in CSV/Excel export endpoint** in the application. Instead,
use the existing read-only REST endpoints and format the JSON yourself. All of
these are safe: they never modify data.

## 1. Available read-only data sources

| Dataset | Endpoint | Notes |
|---|---|---|
| Product catalogue | `GET /api/products` | public |
| Order history (own) | `GET /api/orders` | requires login |
| All orders | `GET /api/admin/orders` | requires ADMIN session |
| All users | `GET /api/admin/users` | requires ADMIN session |
| All reviews | `GET /api/admin/reviews` | requires ADMIN session |

## 2. Options to build the spreadsheet

### Option A — PowerShell (Windows, no extra tools)
```powershell
# 1) auth: capture the session cookie
$body = @{ email = 'admin@x'; password = 'secret123' } | ConvertTo-Json
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' `
          -Method Post -Body $body -ContentType 'application/json' -SessionVariable s

# 2) pull all orders (admin) and convert JSON to CSV
$orders = Invoke-RestMethod -Uri 'http://localhost:8080/api/admin/orders' `
           -WebSession $s
$orders | Select-Object id,status,totalAmount,createdAt | Export-Csv orders.csv -NoTypeInformation

# repeat for /api/admin/users and /api/admin/reviews as needed
```
Open `orders.csv` etc. in Excel/LibreOffice.

### Option B — Postman/Insight (GUI)
1. Log in via `POST /api/auth/login` (store the cookie).
2. Call `GET /api/admin/orders`; click **Save Response → Save as example** or use
   the built-in "Export" to collection, then paste JSON into Excel via
   Data → From JSON/Text.
3. Repeat per endpoint.

### Option C — curl + jq → CSV
```bash
curl -sc cj -H 'Content-Type: application/json' \
  -d '{"email":"admin@x","password":"secret123"}' \
  http://localhost:8080/api/auth/login >/dev/null
curl -sb cj http://localhost:8080/api/admin/orders | jq -r \
  '.[] | [.id,.status,.totalAmount,.createdAt] | @csv' > orders.csv
```

## 3. Important safeguards
- Use credentials you own (a test admin/user); never place secrets in the report.
- These endpoints are read-only — no order is created by reading them.
- Do **not** run this while expecting the in-memory H2 demo to persist; for real
  reporting use the persistent DB deployment.
- A custom "sales report" aggregation beyond what these DTOs expose would need new
  backend code — labelled **proposed**; not implemented in this task.

## 4. What NOT to do
- No new analytics dashboard in the app (would require source changes).
- No direct DB writes or `data.sql` edits to "fix" a report.
- No access to private external accounts (none are used by the app anyway).