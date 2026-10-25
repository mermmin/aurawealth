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

---

## 4. Milestones (v1)
- [x] **M1 — Skeleton:** repo, Spring Boot app runs, Postgres in Docker Compose, first Flyway migration
- [x] **M2 — Auth:** User entity, register/login endpoints, JWT working, tests
- [ ] **M3 — Plaid:** sandbox link flow, Accounts + Transactions saved, tests
- [ ] **M4 — Logic:** net worth, category breakdown, subscription detection endpoints, tests
- [ ] **M5 — Design:** Figma mockups for Dashboard, Accounts, Subscriptions
- [ ] **M6 — Frontend:** Vite app, login, 3 pages wired to the API
- [ ] **M7 — Ship:** GitHub Actions CI, deploy to Railway/Render, README done

---

## 5. Current status
M1 in progress. Spring Boot 4.1.2 skeleton (Java 17, Maven; Web MVC, Data JPA, PostgreSQL, Flyway, Validation) generated and pushed to https://github.com/mermmin/aurawealth (first commit on `main`). `.gitignore` (incl. `.env` secrets) and `CLAUDE.md` added. Session 2 complete: `docker-compose.yml` runs Postgres 17 locally (db/user/password `aurawealth`, overridable via env vars), and `application.properties` reads `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` with local defaults. App starts against Postgres, Flyway creates its history table, Tomcat serves on 8080. `ddl-auto=validate` (Flyway owns the schema). Session 3 complete: first migration `V1__create_users_table.sql` (`users`: identity `id`, unique `email`, `password_hash`, `created_at`/`updated_at` timestamptz) applied by Flyway; table verified in Postgres. **M1 done.** M2 in progress. Session 4 complete: `User` entity + `UserRepository` (`findByEmail`, `existsByEmail`) in `com.aurawealth.user`; emails normalized (trim + lowercase) in the constructor. `UserRepositoryTests` (`@DataJpaTest` against real Postgres, rolls back) covers save/find and duplicate-email rejection. `./mvnw test`: 3 tests green. Session 5 complete: added `spring-boot-starter-security` (+ `-security-test`); `SecurityConfig` provides a BCrypt `PasswordEncoder` bean; `UserService.register(email, password)` normalizes the email, rejects duplicates with `EmailAlreadyRegisteredException` (also translates the unique-constraint race), and stores only the hash. `UserServiceTests` (JUnit 5 + Mockito, no DB) cover success, duplicate, and race. `./mvnw test`: 6 tests green. Session 6 complete: added `spring-boot-starter-security-oauth2-resource-server` (Spring's Nimbus JWT support, no third-party JWT lib). `JwtService.issue(user)` signs HS256 tokens (iss `aurawealth`, sub = user id, `email` claim, 1h TTL from `aurawealth.jwt.ttl`). Secret from `JWT_SECRET` (`aurawealth.jwt.secret`, dev default in `application.properties`; app refuses secrets < 32 bytes). Decoder validates signature, expiry, and issuer. `JwtServiceTests`: 4 tests.
Session 7 complete, **M2 done**: stateless `SecurityFilterChain` (CSRF off, no sessions; `/api/auth/**` and `/error` public, everything else needs a Bearer JWT). Endpoints: `POST /api/auth/register` (201, `{accessToken, tokenType, expiresAt}`; validates email + password 8–72 chars), `POST /api/auth/login` (200; same 401 for unknown email or wrong password), `GET /api/me` (`{id, email}` from the token). `ApiExceptionHandler` returns RFC 9457 problem JSON (409 duplicate, 401 bad credentials, 400 validation). `AuthControllerTests` (`@SpringBootTest` + MockMvc, real Postgres, rolls back): 6 tests. `./mvnw test`: 19 tests green; also smoke-tested all endpoints with curl against the running app.
M3 in progress. Session 8 complete: `plaid-java` 46.0.0 (47.0.0 was 5 days old, skipped for now). Local secrets live in `.env` (gitignored; template in `.env.example`), loaded via `spring.config.import=optional:file:.env[.properties]`; real env vars take precedence. `PlaidConfig` builds a sandbox-only `PlaidApi` from `PLAID_CLIENT_ID` / `PLAID_SECRET`. `PlaidService.createLinkToken(userId)` (product: transactions, country: US) and `POST /api/plaid/link-token` (authenticated). Plaid failures → 502 problem JSON, details logged only. Tests: `PlaidServiceTests` (3, mocked Plaid) + `PlaidControllerTests` (3, `@MockitoBean`, `jwt()` post-processor). `./mvnw test`: 25 tests green. Live check against Plaid sandbox returned a real `link-sandbox-…` token.

## 6. Next micro-task (one session, ~20 min)
**Session 9:** Schema + entities for linked banks. Write `V2__create_plaid_items_and_accounts.sql`:
- `plaid_items`: id, `user_id` FK → users, `item_id` (unique), `access_token`, `institution_name`, `created_at`
- `accounts`: id, `user_id` FK, `plaid_item_id` FK, `plaid_account_id` (unique), `name`, `mask`, `type`, `subtype`, `current_balance` / `available_balance` (NUMERIC(19,4)), `iso_currency_code`, `updated_at`

Add `PlaidItem` + `Account` entities and repositories, plus a `@DataJpaTest`.
**Done when:** Flyway applies V2, the app starts with `ddl-auto=validate`, and `./mvnw test` is green.
**Session 10:** `POST /api/plaid/exchange` (public token → access token, save item, fetch balances via `/accounts/balance/get`, upsert accounts) + `GET /api/accounts`. For testing without a frontend, Plaid's `/sandbox/public_token/create` makes a public token directly.

## 7. Blockers / open questions
- Setup note: Docker Desktop must be open and running (whale icon in the menu bar) before `docker compose up`, or you'll get a "cannot connect to Docker daemon" error.
- Setup note: the `docker` CLI lives in `~/.docker/bin`; that's now on PATH via `~/.zshrc`. Open a new terminal if `docker` says "command not found".

- Setup note: `./mvnw test` needs Postgres running (`docker compose up -d`); repository tests use the real database, not an in-memory one.
- Editor note: VS Code's SQL Server (mssql) extension flags Postgres syntax in migrations as errors; they're false positives. Edit migrations in `src/main/resources`, never the copy in `target/`.

- Possible later polish: 400 validation responses currently say only "Invalid request content." without naming the bad field; may want field-level errors for the frontend forms (M6).

- Decision to make in Session 9: Plaid access tokens are long-lived secrets. Store as plain text for v1 (sandbox only), or encrypt the column at rest. Encrypting is a small extra and reads well in a portfolio.

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
