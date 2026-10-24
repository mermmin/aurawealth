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
- [ ] **M1 — Skeleton:** repo, Spring Boot app runs, Postgres in Docker Compose, first Flyway migration
- [ ] **M2 — Auth:** User entity, register/login endpoints, JWT working, tests
- [ ] **M3 — Plaid:** sandbox link flow, Accounts + Transactions saved, tests
- [ ] **M4 — Logic:** net worth, category breakdown, subscription detection endpoints, tests
- [ ] **M5 — Design:** Figma mockups for Dashboard, Accounts, Subscriptions
- [ ] **M6 — Frontend:** Vite app, login, 3 pages wired to the API
- [ ] **M7 — Ship:** GitHub Actions CI, deploy to Railway/Render, README done

---

## 5. Current status
Not started. Scope finalized 2026-10-24.

## 6. Next micro-task (one session, ~20 min)
**Session 1:** Create the GitHub repo `aurawealth`. Generate a Spring Boot 3 project at start.spring.io (Java 17, Maven; dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Flyway, Validation). Unzip it into the repo, run it once, commit, push.
**Done when:** the app starts locally (it may fail to connect to the database — that's fine for now) and the first commit is on GitHub.

## 7. Blockers / open questions
- None yet

## 8. Session log
| Date | What got done | Next |
|---|---|---|
| 2026-10-24 | Scope finalized | Session 1: repo + Spring Boot skeleton |
