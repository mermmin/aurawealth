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
- [ ] **M2 — Auth:** User entity, register/login endpoints, JWT working, tests
- [ ] **M3 — Plaid:** sandbox link flow, Accounts + Transactions saved, tests
- [ ] **M4 — Logic:** net worth, category breakdown, subscription detection endpoints, tests
- [ ] **M5 — Design:** Figma mockups for Dashboard, Accounts, Subscriptions
- [ ] **M6 — Frontend:** Vite app, login, 3 pages wired to the API
- [ ] **M7 — Ship:** GitHub Actions CI, deploy to Railway/Render, README done

---

## 5. Current status
M1 in progress. Spring Boot 4.1.2 skeleton (Java 17, Maven; Web MVC, Data JPA, PostgreSQL, Flyway, Validation) generated and pushed to https://github.com/mermmin/aurawealth (first commit on `main`). `.gitignore` (incl. `.env` secrets) and `CLAUDE.md` added. Session 2 complete: `docker-compose.yml` runs Postgres 17 locally (db/user/password `aurawealth`, overridable via env vars), and `application.properties` reads `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` with local defaults. App starts against Postgres, Flyway creates its history table, Tomcat serves on 8080. `ddl-auto=validate` (Flyway owns the schema). Session 3 complete: first migration `V1__create_users_table.sql` (`users`: identity `id`, unique `email`, `password_hash`, `created_at`/`updated_at` timestamptz) applied by Flyway; table verified in Postgres. **M1 done.** M2 in progress. Session 4 complete: `User` entity + `UserRepository` (`findByEmail`, `existsByEmail`) in `com.aurawealth.user`; emails normalized (trim + lowercase) in the constructor. `UserRepositoryTests` (`@DataJpaTest` against real Postgres, rolls back) covers save/find and duplicate-email rejection. `./mvnw test`: 3 tests green.

## 6. Next micro-task (one session, ~20 min)
**Session 5:** Add `spring-boot-starter-security`, a BCrypt `PasswordEncoder` bean, and a `UserService.register(email, password)` that rejects duplicate emails and stores only the hash. Unit-test it with JUnit 5 + Mockito (mock `UserRepository`, no database).
**Done when:** the `UserService` tests pass and `./mvnw test` is green.

## 7. Blockers / open questions
- Setup note: Docker Desktop must be open and running (whale icon in the menu bar) before `docker compose up`, or you'll get a "cannot connect to Docker daemon" error.
- Setup note: the `docker` CLI lives in `~/.docker/bin`; that's now on PATH via `~/.zshrc`. Open a new terminal if `docker` says "command not found".

- Setup note: `./mvnw test` needs Postgres running (`docker compose up -d`); repository tests use the real database, not an in-memory one.
- Editor note: VS Code's SQL Server (mssql) extension flags Postgres syntax in migrations as errors; they're false positives. Edit migrations in `src/main/resources`, never the copy in `target/`.

## 8. Session log
| Date | What got done | Next |
|---|---|---|
| 2026-10-24 | Scope finalized | Session 1: repo + Spring Boot skeleton |
| 2026-10-24 | Session 1: verified Java 17/22 + Git; generated Boot 4.1.2 project (3.x no longer on Initializr); added .gitignore + CLAUDE.md; created GitHub repo and pushed first commit (fixed push error "Password authentication is not supported" by logging in with GitHub CLI: `brew install gh`, `gh auth login`, `gh auth setup-git`) | Session 2: Docker Desktop + Postgres in docker-compose.yml, app starts against it |
| 2026-10-24 | Ran `./mvnw spring-boot:run`: compiles, Tomcat initializes, stops with "Failed to configure a DataSource: 'url' attribute is not specified" (expected, no DB yet) | Session 3: first Flyway migration |
| 2026-10-24 | Session 2: confirmed Docker Desktop 4.96.3 (Engine 29.9.2, Compose v5.6.1); added Docker CLI to PATH; added docker-compose.yml (Postgres 17) and datasource config with env-var overrides; app starts against Postgres in 2.8s | Session 3: first Flyway migration |
| 2026-10-24 | Session 3: added `V1__create_users_table.sql`; `docker compose up -d`, app started, Flyway logged "Successfully applied 1 migration … now at version v1"; confirmed `users` table and `flyway_schema_history` via psql. M1 checked off | Session 4: User entity + repository + test (M2) |
| 2026-10-24 | Session 4: `User` entity + `UserRepository` + `UserRepositoryTests` (save/find by email, duplicate rejected); `./mvnw test` 3/3 green | Session 5: Spring Security + BCrypt + `UserService.register` with Mockito tests |
