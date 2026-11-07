# LineBook backend

One Spring Boot service — **Java 21 · Spring Boot 4.0 · Spring Data JPA · MySQL 8 · Flyway** — with every LineBook API in
it. It re-implements, behind a REST API, the billing and collection logic that the LineBook front end currently keeps in
the browser. **No front-end code was changed.**

```
front end ─►  linebook-backend :9000  ─►  MySQL (linebook_db)
               ├─ /api/customers, /api/packages      customer module
               ├─ /api/agents                         agent module
               ├─ /api/bills                          billing module
               ├─ /api/collections                    collection module
               └─ /api/agents/overview                dashboard module (reads the others)
```

## API documentation (Swagger)

Start the backend and open **`http://localhost:9000/swagger-ui.html`** — every endpoint is listed by module, with request
and response shapes, examples, and a **Try it out** button. The raw OpenAPI document is at `/v3/api-docs` (import it into
Postman, or generate a typed front-end client from it).

Set `SWAGGER_ENABLED=false` to switch both off (recommended on a public production deployment).

## Modules

It is one deployable, organised as modules (packages) that call each other directly — no HTTP between them.

| Package (`com.linebook.…`) | Owns | Uses |
|---|---|---|
| `agent` | agents (id, name, phone, area, active) | — |
| `customer` | customers, package catalogue, connection status | agent |
| `billing` | bills, bill status, payments applied to bills | customer |
| `collection` | collections, receipts, daily totals | billing, agent |
| `dashboard` | the Agents page overview (no tables) | all of the above |
| `common` | clock, CORS, request id, one error handler | — |

Dependencies only point one way (agent ← customer ← billing ← collection ← dashboard), so a module can be split back out
into its own service later if you ever need to.

How the modules work together:

- **A billable customer always has this month's bill.** Adding a customer, activating one, or switching recurring on
  publishes an event; the billing module creates the bill **in the same transaction**. If the bill fails, the customer
  change is rolled back.
- **Recording a collection is one transaction:** it locks the bill (two simultaneous collections cannot overpay), applies
  the payment, takes the next receipt number and saves the collection. Any failure saves nothing.
- Customers must be assigned to an **existing agent**, and a collection must name an existing agent (400 / 422 otherwise).

## API

| Method & path | Purpose |
|---|---|
| **Customers** | |
| `GET /api/customers?ids=&q=&area=&status=&agentId=` | Customers in creation order (Customers page filters, and one agent's customers for reports); `all` or blank = no filter |
| `GET /api/customers/{id}` | One customer |
| `POST /api/customers` | Add customer → 201 |
| `PATCH /api/customers/{id}/status` `{"status":"active"}` | Change connection status |
| `PATCH /api/customers/{id}/recurring` `{"recurring":true}` | Toggle the monthly bill |
| `GET /api/customers/counts` | `total, active, pending, suspended, disconnected, recurringActive` |
| `GET /api/customers/areas` | Distinct areas |
| `GET /api/customers/agent-counts?status=` | Connections per agent (default active) |
| `GET /api/packages` | Package catalogue |
| **Agents** | |
| `GET /api/agents?includeInactive=` | Agent dropdowns (active agents by default) |
| `GET /api/agents/{id}` | One agent (inactive ones too) |
| `POST /api/agents` / `PUT /api/agents/{id}` | Add / edit an agent (`name`, `phone`, `area`) |
| `PATCH /api/agents/{id}/active` `{"active":false}` | Deactivate / reactivate |
| `GET /api/agents/overview?cycle=&date=` | The Agents page: accounts, collected today, cycle book, % collected |
| **Bills** | |
| `GET /api/bills?cycle=&status=&agentId=&q=` | Bills page (cycle defaults to current) |
| `GET /api/bills/cycles` | Cycles that have bills, newest first |
| `GET /api/bills/summary?cycle=` | Billed, collected, pending, status counts |
| `GET /api/bills/summary/by-agent?cycle=` | The same, per agent |
| `GET /api/bills/collectable?q=` | Bill picker for "Record collection" |
| `GET /api/bills/by-customer/{customerId}` | A customer's bills across all cycles |
| `GET /api/bills/{id}` | One bill. Every bill carries its whole customer (address, VC no., package …), enough to draw the WhatsApp bill image |
| `POST /api/bills/generate?cycle=` | "Run monthly billing" → `{"created":3,"cycle":"2026-10"}` |
| **Settings** | |
| `GET /api/info` | `{operatorName, timeZone}` — the business name shown in the sidebar, receipts, shared bills and reports |
| **Collections** | |
| `POST /api/collections` | Record a collection → 201 (200 if that `collectionId` was already recorded) |
| `GET /api/collections?from=&to=&cycle=&agentId=&billId=&customerId=` | Collections between two dates (default today), newest first. With `cycle=yyyy-MM`: that cycle's collections (paid a bill of the cycle, or collected during its month), `from`/`to` optional — the Reports page |
| `GET /api/collections/summary?date=` | Count, total, per-agent split for a day (default today) |
| `GET /api/collections/daily?days=14` | Collected per day for the last N days (the chart) |
| `GET /api/collections/{id}` · `/{id}/receipt` | One collection · what the receipt prints |

JSON uses lowercase values (`active`, `paid`, `overdue`, `cash`, `upi` …). Errors are RFC 9457 problem responses:
400 bad input · 404 unknown id · 409 simultaneous update, retry · 422 collection not allowed (amount, settled or
disconnected bill, unknown agent).

Add-customer body (only `name`, `phone`, `area`, `agentId` are required; `billingDay` 1–28 is optional, default 1):

```json
{ "name": "Ravi Kumar", "phone": "9848011223", "address": "12-3, Lane 4", "area": "Ameerpet",
  "packageId": "pkg-family", "agentId": "ag-01", "status": "active", "recurring": true,
  "generateBill": true, "billingDay": 10 }
```

Record-collection body (only `billId` and `amount` are required). Send your own `collectionId` (for example a UUID) so a
retry can never record the payment twice:

```json
{ "billId": "bl-2026-10-cu-01", "amount": 299, "method": "upi", "agentId": "ag-01",
  "notes": "paid at door", "collectionId": "7f3a9c21-…" }
```

## Where each front-end action goes

| Front-end screen / action | Call |
|---|---|
| Today: billed, received this cycle, pending, status counts | `GET /api/bills/summary` |
| Today: collected today, receipts, per-agent today | `GET /api/collections/summary` |
| Today: Today's receipts · Last 14 days chart | `GET /api/collections` · `GET /api/collections/daily` |
| Today: "N recurring connections · M pending install" | `GET /api/customers/counts` |
| Agents page · agent dropdowns | `GET /api/agents/overview` · `GET /api/agents` |
| Run monthly billing | `POST /api/bills/generate` |
| Bills page, cycle dropdown | `GET /api/bills`, `GET /api/bills/cycles` |
| Record collection: find a bill · save | `GET /api/bills/collectable?q=` · `POST /api/collections` |
| Receipt dialog | `GET /api/collections/{id}/receipt` |
| Customers: list, filters, counts | `GET /api/customers`, `/areas`, `/counts` |
| Customer detail: bills | `GET /api/bills/by-customer/{customerId}` |
| Add customer · status · recurring | `POST /api/customers` · `PATCH …/status` · `PATCH …/recurring` |
| Package dropdown | `GET /api/packages` |
| Add agent | `POST /api/agents` `{"name","phone","area"}` |
| Share bill on WhatsApp (bill image) | the bill row already has everything (`GET /api/bills`, `GET /api/bills/{id}`); package name from `GET /api/packages`, agent name from `GET /api/agents` |
| Reports: summary / pending / agents | `GET /api/bills?cycle=&agentId=` (+ `GET /api/bills/cycles`, `GET /api/agents`) |
| Reports: receipts (cash / UPI split) | `GET /api/collections?cycle=&agentId=&from=&to=` |
| Reports: customer accounts | `GET /api/customers?agentId=` |
| Business name | `GET /api/info` |

## Rules copied from the front end

Customers (`lib/store.ts`): account numbers `SCN-1001, SCN-1002 …`; default STB/VC numbers; phone keeps ≤10 digits; the
monthly rate is the package's amount; recurring only for an active connection; setting a status turns recurring on for
`active` and off otherwise.
Billing (`lib/billing.ts`): billable = active + recurring; bill id `bl-{cycle}-{customerId}`, number `BL-{yyyyMM}-{seq}`;
generated on the 1st at 08:15, due on day `min(28, max(1, billingDay))`; status is derived (paid at ≥ amount − 0.001,
overdue once past the due date even if partly paid); "run monthly billing" is idempotent.
Collections: whole-rupee amounts, `> 0` and `≤ pending`; cash by default; the customer's agent by default; receipt numbers
`RC-yyyyMMdd-0001`; only pending, **current-cycle** bills of customers that are not disconnected can be collected (set
`CURRENT_CYCLE_ONLY=false` to accept older bills).

## Run

Requirements: JDK 21, Maven 3.9+, MySQL 8 (or Docker).

```bash
# with Docker (MySQL + the backend)
docker compose up --build                                  # http://localhost:9000
SPRING_PROFILES_ACTIVE=demo docker compose up --build      # also load the 36 demo customers + this month's bills

# or locally against your own MySQL
export DB_URL=jdbc:mysql://localhost:3306/linebook_db?createDatabaseIfNotExist=true
export DB_USERNAME=linebook DB_PASSWORD=linebook
mvn spring-boot:run                                        # add -Dspring-boot.run.profiles=demo for demo data
mvn test                                                   # unit tests (no database needed)

./smoke-test.sh                                            # end-to-end check against the running backend
```

Flyway creates the schema and the reference data (5 packages, 5 agents) on startup. Health: `GET /actuator/health`.
The demo profile uses `billingDay = 10` for its customers (like the front end's demo bills) and loads only into an empty
database.

## Deploy on Vercel

The backend runs on Vercel as a container (`Dockerfile.vercel` is included); the database must be a separate managed
MySQL, because Vercel does not host databases.

1. **Database:** create a managed MySQL 8 (Aiven, Railway, AWS RDS, Azure Database for MySQL …), create an empty database
   `linebook_db` and a user that may create tables. It must accept connections over the internet with TLS from any address
   (Vercel containers have no fixed outgoing IP).
2. **Vercel project:** import this folder as its own project (Root Directory = this folder). Vercel detects `Dockerfile.vercel`.
3. **Environment variables** (Project Settings → Environment Variables):

   | Variable | Value |
   |---|---|
   | `PORT` | `8080` |
   | `DB_URL` | `jdbc:mysql://HOST:3306/linebook_db?sslMode=REQUIRED` |
   | `DB_USERNAME` / `DB_PASSWORD` | the database user |
   | `CORS_ALLOWED_ORIGINS` | your UI's address, e.g. `https://linebook-ui.vercel.app` |
   | `DB_POOL_SIZE` | `5` (optional) |
   | `SPRING_PROFILES_ACTIVE` | `demo` only for the very first deploy if you want demo data |

4. Deploy (`vercel deploy --prod` or push to the connected branch). Flyway creates the tables on first start.
5. Check `https://YOUR-BACKEND.vercel.app/actuator/health`, then run `BASE_URL=https://YOUR-BACKEND.vercel.app ./smoke-test.sh`.

Notes: the first request after a quiet period starts the JVM (several seconds); the container's disk is not kept between
runs; keep the database connection pool small (`DB_POOL_SIZE`).

## Configuration

| Env var | Default |
|---|---|
| `PORT` / `SERVER_PORT` | `9000` (Vercel sets `PORT`) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:mysql://localhost:3306/linebook_db…` / `linebook` / `linebook` |
| `DB_POOL_SIZE` / `DB_MAX_LIFETIME_MS` | `5` / `300000` |
| `LINEBOOK_ZONE` | `Asia/Kolkata` (today, billing cycle, receipt dates) |
| `OPERATOR_NAME` | `Surya Cable Network` (printed on receipts) |
| `DEFAULT_AGENT_ID` | `ag-01` |
| `CURRENT_CYCLE_ONLY` | `true` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8080,http://localhost:5173` |
| `SWAGGER_ENABLED` | `true` (Swagger UI at `/swagger-ui.html`, OpenAPI at `/v3/api-docs`) |

## Changes from the separate-services version

- One application and one database; **no gateway and no HTTP between modules**. API paths are unchanged.
- The two service-to-service endpoints (`POST /api/bills/ensure`, `POST /api/bills/{id}/payments`) are gone; they are now
  plain method calls. `agentName` is no longer sent when collecting: the receipt takes the name from the agent.
- Foreign keys now tie the tables together, so unknown customers, bills or agents cannot be saved.
- Receipt numbers come from a per-day counter row taken inside the transaction, so they are gap-free and race-free.

## Database migrations

Flyway applies new migrations on startup and never edits old ones. `V3__collection_bill_cycle.sql` adds
`collections.bill_cycle` (filled in from each collection's bill) so reports can list a cycle's collections. It runs
automatically the next time the backend starts, on an existing database too.

## Not included

- **Sign-in.** The front end has none, so every request is accepted. Add authentication before exposing this beyond your
  network.
- **Connecting the front end.** It still reads and writes its in-browser store; pointing it at `http://localhost:9000` is a
  separate change in the front end.

## Status

Written to match Spring Boot 4.0's starters (Spring Boot 4.0.5 and springdoc-openapi 3.0.3 for Swagger) and package layout. In the environment where it was generated (no Maven
repository access) the pure business rules (billing, customer, collection and agent rules) were compiled and run in a JVM
(42 checks, all passing), and the whole source tree was compiled with only the missing Spring/JPA/test libraries failing —
every reference between the modules resolves. The Spring, JPA and MySQL wiring itself has not been run, so run `mvn test`
and then `./smoke-test.sh` first.
