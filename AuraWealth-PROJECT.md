# AuraWealth — Project Handoff File

> One copy of this file is shared between Claude chats (desktop and mobile).
> **Start of every session:** read this file. **End of every session:** update "Current status", "Next micro-task", and "Session log".

---

## 1. What it is
A full-stack personal wealth & subscriptions dashboard. It links mock bank accounts (Plaid sandbox), shows net worth and spending by category, and keeps a ledger of recurring subscriptions. Design story: a moody, high-end fintech UI (matte charcoal, warm taupe accents, editorial typography).

**Purpose:** Résumé/portfolio project for Product Engineer and Software Engineer roles. It needs a live link, a clean README, and real tests.

---

## 2. Final scope

### v1 MVP (this is the whole goal right now)
| Area | What's in |
|---|---|
| **Backend** | Java 17, Spring Boot 4.1, Maven |
| **Database** | PostgreSQL, migrations with **Flyway** |
| **Entities** | User, Account, Transaction, Subscription |
| **Auth** | Spring Security + JWT (register / login) |
| **Integration** | Plaid **sandbox**: link accounts, sync balances and transactions |
| **Core logic** | Net worth total; spending by category (last 30 days); recurring-charge detection that fills the Subscriptions list |
| **Frontend** | React + TypeScript (Vite), Tailwind CSS, TanStack Query, Recharts |
| **Pages** | Login, Dashboard (net worth + category chart), Accounts, Subscriptions ledger |
| **Design** | Quick Figma mockups of the 3 main pages before building the UI |
| **Testing** | JUnit 5 + Mockito for services; Spring Boot Test for controllers |
| **Local setup** | Docker Compose (Postgres + backend) |
| **CI** | GitHub Actions: build + run tests on every PR |
| **Deploy** | Railway or Render (no AWS for v1) — live clickable link |
| **README** | Live link, build badge, architecture diagram, Figma screenshot, one-command setup |

### v2 (only after v1 is live — do not start early)
1. LLM "Spending Insights" page: monthly summary of spending habits in a dry, witty voice
2. Stripe webhooks (test mode) to update subscriptions automatically
3. Redis cache for the net worth query
4. Picocli Java CLI: `sync` and `networth` commands

### Out of scope
- AWS ECS/EC2/RDS deployment
- Real (non-sandbox) bank data
- Mobile app
- Astrology framing anywhere in the repo or README

---

## 3. Decisions log
- Flyway, not Liquibase (simpler)
- Railway/Render, not AWS (avoid server/ops work)
- v1 = 4 entities, 4 pages; everything else waits for v2
- Name "AuraWealth" is a working title; can change before launch
- Spring Boot 4.1 instead of 3.x (2026-10-24): start.spring.io no longer offers 3.x and its open-source support has ended. Still Java 17.
- Plaid access tokens encrypted at rest (2026-10-25): AES-256-GCM via a JPA `AttributeConverter`, key from `TOKEN_ENCRYPTION_KEY`. Small extra cost, real protection against DB dumps, reads well in the portfolio. Rotating the key makes stored tokens unreadable (fine for sandbox; users would just re-link).

---

## 4. Milestones (v1)
- [x] **M1 — Skeleton:** repo, Spring Boot app runs, Postgres in Docker Compose, first Flyway migration
- [x] **M2 — Auth:** User entity, register/login endpoints, JWT working, tests
- [x] **M3 — Plaid:** sandbox link flow, Accounts + Transactions saved, tests
- [ ] **M4 — Logic:** net worth, category breakdown, subscription detection endpoints, tests
- [ ] **M5 — Design:** Figma mockups for Dashboard, Accounts, Subscriptions
- [ ] **M6 — Frontend:** Vite app, login, 3 pages wired to the API
- [ ] **M7 — Ship:** GitHub Actions CI, deploy to Railway/Render, README done

---

## 5. Current status
M1 in progress. Spring Boot 4.1.2 skeleton (Java 17, Maven; Web MVC, Data JPA, PostgreSQL, Flyway, Validation) generated and pushed to https://github.com/mermmin/aurawealth (first commit on `main`). `.gitignore` (incl. `.env` secrets) and `CLAUDE.md` added. Session 2 complete: `docker-compose.yml` runs Postgres 17 locally (db/user/password `aurawealth`, overridable via env vars), and `application.properties` reads `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` with local defaults. App starts against Postgres, Flyway creates its history table, Tomcat serves on 8080. `ddl-auto=validate` (Flyway owns the schema). Session 3 complete: first migration `V1__create_users_table.sql` (`users`: identity `id`, unique `email`, `password_hash`, `created_at`/`updated_at` timestamptz) applied by Flyway; table verified in Postgres. **M1 done.** M2 in progress. Session 4 complete: `User` entity + `UserRepository` (`findByEmail`, `existsByEmail`) in `com.aurawealth.user`; emails normalized (trim + lowercase) in the constructor. `UserRepositoryTests` (`@DataJpaTest` against real Postgres, rolls back) covers save/find and duplicate-email rejection. `./mvnw test`: 3 tests green. Session 5 complete: added `spring-boot-starter-security` (+ `-security-test`); `SecurityConfig` provides a BCrypt `PasswordEncoder` bean; `UserService.register(email, password)` normalizes the email, rejects duplicates with `EmailAlreadyRegisteredException` (also translates the unique-constraint race), and stores only the hash. `UserServiceTests` (JUnit 5 + Mockito, no DB) cover success, duplicate, and race. `./mvnw test`: 6 tests green. Session 6 complete: added `spring-boot-starter-security-oauth2-resource-server` (Spring's Nimbus JWT support, no third-party JWT lib). `JwtService.issue(user)` signs HS256 tokens (iss `aurawealth`, sub = user id, `email` claim, 1h TTL from `aurawealth.jwt.ttl`). Secret from `JWT_SECRET` (`aurawealth.jwt.secret`, dev default in `application.properties`; app refuses secrets < 32 bytes). Decoder validates signature, expiry, and issuer. `JwtServiceTests`: 4 tests.
Session 7 complete, **M2 done**: stateless `SecurityFilterChain` (CSRF off, no sessions; `/api/auth/**` and `/error` public, everything else needs a Bearer JWT). Endpoints: `POST /api/auth/register` (201, `{accessToken, tokenType, expiresAt}`; validates email + password 8–72 chars), `POST /api/auth/login` (200; same 401 for unknown email or wrong password), `GET /api/me` (`{id, email}` from the token). `ApiExceptionHandler` returns RFC 9457 problem JSON (409 duplicate, 401 bad credentials, 400 validation). `AuthControllerTests` (`@SpringBootTest` + MockMvc, real Postgres, rolls back): 6 tests. `./mvnw test`: 19 tests green; also smoke-tested all endpoints with curl against the running app.
**M3 done.** Session 8 complete: `plaid-java` 46.0.0 (47.0.0 was 5 days old, skipped for now). Local secrets live in `.env` (gitignored; template in `.env.example`), loaded via `spring.config.import=optional:file:.env[.properties]`; real env vars take precedence. `PlaidConfig` builds a sandbox-only `PlaidApi` from `PLAID_CLIENT_ID` / `PLAID_SECRET`. `PlaidService.createLinkToken(userId)` (product: transactions, country: US) and `POST /api/plaid/link-token` (authenticated). Plaid failures → 502 problem JSON, details logged only. Tests: `PlaidServiceTests` (3, mocked Plaid) + `PlaidControllerTests` (3, `@MockitoBean`, `jwt()` post-processor). `./mvnw test`: 25 tests green. Live check against Plaid sandbox returned a real `link-sandbox-…` token. Session 9 complete: `V2__create_plaid_items_and_accounts.sql` (`plaid_items` + `accounts`, FKs with `ON DELETE CASCADE`, indexes on FK columns, balances `NUMERIC(19,4)` nullable). `EncryptedStringConverter` (`com.aurawealth.security`, AES-256-GCM, random IV per write, stored as base64(iv‖ciphertext)) encrypts `PlaidItem.accessToken`; key from `aurawealth.crypto.token-key` / `TOKEN_ENCRYPTION_KEY` (dev default in properties, documented in `.env.example`). Entities: `PlaidItem` + `PlaidItemRepository` (`com.aurawealth.plaid`), `Account` + `AccountRepository` (`com.aurawealth.account`, `updateBalances(...)` for syncs). Tests: `EncryptedStringConverterTests` (6), `PlaidItemRepositoryTests` (2, checks the raw DB column is not the plain token), `AccountRepositoryTests` (2). `@DataJpaTest`s need `@Import(EncryptedStringConverter.class)`. `./mvnw test`: 35 tests green; Flyway at v2. Session 10 complete: `PlaidService.linkItem(userId, publicToken)` calls `/item/public_token/exchange` then `/accounts/balance/get` (institution name comes on the response's `item`, no extra lookup), then saves the item and upserts accounts in one transaction via `TransactionOperations` (Plaid calls stay outside the transaction). `POST /api/plaid/exchange` (`{publicToken}` → 201 + accounts) and `GET /api/accounts` (`AccountService` / `AccountController`, `AccountResponse` DTO; repository uses `@EntityGraph` to fetch the item since open-in-view is off). Tests: `PlaidServiceTests` now 6, `PlaidControllerTests` 6, new `AccountControllerTests` 2. `./mvnw test`: 43 tests green. Live sandbox check: First Platypus Bank linked, 14 accounts returned with exact balances, DB column holds ciphertext only. Session 11 complete: `V3__create_transactions.sql` adds `transactions` (amount `NUMERIC(19,4)` with Plaid's sign: positive = money out; date column is `transaction_date` to avoid the SQL type name; index on `(user_id, transaction_date)`) and nullable `plaid_items.transactions_cursor`. `Transaction` entity + `TransactionRepository` in `com.aurawealth.transaction` (`findByPlaidTransactionId`, bulk `deleteByPlaidTransactionIds` for sync's removed list; `update(...)` applies sync's modified list). `PlaidItem.updateTransactionsCursor(...)`. Tests: `TransactionRepositoryTests` (5, incl. DB cascade on account delete) + cursor test in `PlaidItemRepositoryTests`. `./mvnw test`: 49 tests green; Flyway at v3. Session 12 complete: `PlaidService.sync(userId)` + `POST /api/plaid/sync` → `SyncResponse{accounts, added, modified, removed}`. Per item: refresh balances, page `/transactions/sync` (count 500) from the saved cursor collecting added/modified/removed, then one transaction upserts accounts + transactions (added and modified are both upserts, so re-syncs never duplicate), bulk-deletes removed, and saves the cursor last. On `TRANSACTIONS_SYNC_MUTATION_DURING_PAGINATION` it restarts from the start cursor (max 3 attempts); `PlaidException` now carries Plaid's `error_code` (parsed with the SDK's Gson). Tests: `PlaidServiceTests` 10 (multi-page, mutation retry, give-up keeps old cursor, no items), `PlaidControllerTests` 8. `./mvnw test`: 55 tests green. Live sandbox: first sync saved 48 First Platypus transactions (9 categories, Jul–Oct), second sync added 0, cursor stored. M4 in progress. Session 13 complete: `GET /api/dashboard/net-worth` → `{assets, liabilities, netWorth, currency}` (`DashboardService` / `DashboardController` in `com.aurawealth.dashboard`). Sums run in SQL: `AccountRepository.sumCurrentBalancesByType` (JPQL constructor expression into `BalanceByType`); `credit` + `loan` are liabilities, every other type an asset, null sums count as 0; currency fixed to USD (US-only sandbox). Tests: `DashboardServiceTests` (3), `DashboardControllerTests` (2), repository query test (scoped to one user). Fixed `TransactionRepositoryTests` using `findAll()`, which broke once live sandbox data was in the dev DB; repository tests must only assert on their own rows. `./mvnw test`: 61 green. Live: sandbox user's net worth −77,164.1495 (assets 86,541.7405, liabilities 163,705.89), matches SQL. Session 14 complete: `GET /api/dashboard/spending` → `{from, to, total, currency, categories:[{category, amount}]}`, biggest first. Window = today and the 29 days before it, from an injectable `Clock` bean (declared in `AurawealthApplication`). `TransactionRepository.sumSpendingByCategory` groups in SQL, excludes `TRANSFER_IN/OUT`, `LOAN_PAYMENTS`, `INCOME`, and **nets refunds against purchases in the same category** (`HAVING SUM > 0` drops fully refunded categories); null category → `UNCATEGORIZED`. Decided during the live check: counting only positive amounts showed a fully refunded $500 flight as travel spending. Tests: repository query (window edges, refunds, exclusions, other users), service (window from fixed clock, uncategorized, empty), controller. `./mvnw test`: 66 green. Live: sandbox user spent $1,695.96 over 5 categories; ~$8,950 of transfers/card payments correctly excluded.

## 6. Next micro-task (one session, ~20 min)
**Session 15:** Subscriptions schema. `V4__create_subscriptions.sql`: `subscriptions` (id, `user_id` FK, `merchant_name`, `amount` NUMERIC(19,4) (latest charge), `frequency` (`WEEKLY`/`MONTHLY`/`ANNUAL`), `last_charged_on`, `next_expected_on`, `category`, `active`, timestamps; unique `(user_id, merchant_name)`). `Subscription` entity + repository + `@DataJpaTest`.
**Done when:** Flyway applies V4, app starts with `ddl-auto=validate`, `./mvnw test` green.
**Session 16:** Recurring-charge detection. Pure-Java `RecurringChargeDetector` (unit-tested without Spring): group a user's money-out transactions by merchant (fallback: name), need ≥3 charges with steady gaps (~7, ~30, or ~365 days, ± tolerance) and similar amounts (±15%); upsert `subscriptions` and mark ones not seen for 2× their interval inactive. Run it at the end of `PlaidService.sync`, plus `GET /api/subscriptions`. Then M4 is done.
Option to consider in Session 16: Plaid also has `/transactions/recurring/get`, which does this detection server-side. Building our own is the better portfolio story; could compare results in the live check.

## 7. Blockers / open questions
- Setup note: Docker Desktop must be open and running (whale icon in the menu bar) before `docker compose up`, or you'll get a "cannot connect to Docker daemon" error.
- Setup note: the `docker` CLI lives in `~/.docker/bin`; that's now on PATH via `~/.zshrc`. Open a new terminal if `docker` says "command not found".

- Setup note: `./mvnw test` needs Postgres running (`docker compose up -d`); repository tests use the real database, not an in-memory one.
- Editor note: VS Code's SQL Server (mssql) extension flags Postgres syntax in migrations as errors; they're false positives. Edit migrations in `src/main/resources`, never the copy in `target/`.

- Possible later polish: 400 validation responses currently say only "Invalid request content." without naming the bad field; may want field-level errors for the frontend forms (M6).
- M4 note: spending should count only money *out* (amount > 0) and exclude money moving between the user's own accounts: categories `TRANSFER_IN`, `TRANSFER_OUT`, `LOAN_PAYMENTS` (e.g. sandbox "CREDIT CARD 3333 PAYMENT"), plus `INCOME`. Sandbox amounts are a bit odd (KFC $500), which is fine for demo data.
- M4 note: Plaid reports credit and loan balances as positive amounts *owed* (sandbox mortgage `current=56302.06`). Net worth = depository + investment − credit − loan. Plaid transaction amounts are also positive for money *out*.
- Deploy note (M7): set `TOKEN_ENCRYPTION_KEY` (`openssl rand -base64 32`) on Railway/Render alongside `JWT_SECRET`; the dev default must not be used in production.

## 8. Session log
| Date | What got done | Next |
|---|---|---|
| 2026-10-24 | Scope finalized | Session 1: repo + Spring Boot skeleton |
| 2026-10-24 | Session 1: verified Java 17/22 + Git; generated Boot 4.1.2 project (3.x no longer on Initializr); added .gitignore + CLAUDE.md; created GitHub repo and pushed first commit (fixed push error "Password authentication is not supported" by logging in with GitHub CLI: `brew install gh`, `gh auth login`, `gh auth setup-git`) | Session 2: Docker Desktop + Postgres in docker-compose.yml, app starts against it |
| 2026-10-24 | Ran `./mvnw spring-boot:run`: compiles, Tomcat initializes, stops with "Failed to configure a DataSource: 'url' attribute is not specified" (expected, no DB yet) | Session 3: first Flyway migration |
| 2026-10-24 | Session 2: confirmed Docker Desktop 4.96.3 (Engine 29.9.2, Compose v5.6.1); added Docker CLI to PATH; added docker-compose.yml (Postgres 17) and datasource config with env-var overrides; app starts against Postgres in 2.8s | Session 3: first Flyway migration |
| 2026-10-24 | Session 3: added `V1__create_users_table.sql`; `docker compose up -d`, app started, Flyway logged "Successfully applied 1 migration … now at version v1"; confirmed `users` table and `flyway_schema_history` via psql. M1 checked off | Session 4: User entity + repository + test (M2) |
| 2026-10-24 | Session 4: `User` entity + `UserRepository` + `UserRepositoryTests` (save/find by email, duplicate rejected); `./mvnw test` 3/3 green | Session 5: Spring Security + BCrypt + `UserService.register` with Mockito tests |
| 2026-10-24 | Session 5: Spring Security + BCrypt `PasswordEncoder`; `UserService.register` with duplicate/race handling; `UserServiceTests` (Mockito); `./mvnw test` 6/6 green | Session 6: `JwtService` (HS256 via Spring's Nimbus support) + tests |
| 2026-10-24 | Session 6: `JwtService` (HS256, Spring Nimbus support) + secret config + 4 tests | Session 7 |
| 2026-10-24 | Session 7: `SecurityFilterChain`, `/api/auth/register`, `/api/auth/login`, `/api/me`, problem-JSON error handler, `AuthControllerTests`; 19/19 tests green; curl smoke test passed. M2 checked off | Session 8: Plaid sandbox keys + link-token endpoint (M3) |
| 2026-10-24 | Session 8: Plaid sandbox keys in `.env`; `plaid-java` client; `POST /api/plaid/link-token` + 6 tests; 25/25 green; live sandbox link token received | Session 9: V2 migration + PlaidItem/Account entities |
| 2026-10-25 | Session 9: decided to encrypt Plaid access tokens (AES-256-GCM `EncryptedStringConverter`); V2 migration (`plaid_items`, `accounts`); `PlaidItem`/`Account` entities + repositories; 10 new tests; 35/35 green; Flyway at v2 | Session 10: `POST /api/plaid/exchange` + `GET /api/accounts` |
| 2026-10-25 | Session 10: `POST /api/plaid/exchange` (exchange + balances + save in one transaction) and `GET /api/accounts`; 8 new tests; 43/43 green; live sandbox link returned 14 First Platypus accounts, token stored encrypted | Session 11: V3 transactions table + `Transaction` entity |
| 2026-10-25 | Session 11: V3 migration (`transactions` + `plaid_items.transactions_cursor`); `Transaction` entity + repository; 6 new tests; 49/49 green; Flyway at v3 | Session 12: `/transactions/sync` + `POST /api/plaid/sync` (finishes M3) |
| 2026-10-25 | Session 12: `/transactions/sync` paging + mutation retry, `POST /api/plaid/sync`; 6 new tests; 55/55 green; live sandbox sync saved 48 transactions, re-sync added 0. **M3 checked off** | Session 13: net worth endpoint (M4) |
| 2026-10-25 | Session 13: `GET /api/dashboard/net-worth` (SQL sum by account type, credit/loan as liabilities); 6 new tests; fixed a repository test that assumed an empty DB; 61/61 green; live sandbox net worth matches SQL | Session 14: spending by category |
| 2026-10-25 | Session 14: `GET /api/dashboard/spending` (last 30 days by category; excludes transfers/card payments/income; nets refunds); `Clock` bean; 5 new tests; 66/66 green; live sandbox spending $1,695.96 | Session 15: subscriptions schema |
